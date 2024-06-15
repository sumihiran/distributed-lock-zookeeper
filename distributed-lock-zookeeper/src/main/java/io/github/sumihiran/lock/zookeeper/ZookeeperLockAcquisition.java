package io.github.sumihiran.lock.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.sumihiran.lock.zookeeper.exceptions.ZookeeperLockReleaseException;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents an acquired lock in Zookeeper and manages its lifecycle by monitoring the connection state.
 *
 * @author Nuwan Bandara
 */
public class ZookeeperLockAcquisition implements Acquisition {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperLockAcquisition.class);

    private final InterProcessLock lock;
    private final String key;
    private final AtomicBoolean isAcquired = new AtomicBoolean(true);
    private final LockStateMonitor monitor;
    private final CuratorFramework client;

    /**
     * Constructs a new instance with the specified CuratorFramework client, lock, and key.
     *
     * @param client the CuratorFramework client
     * @param key    the lock key
     * @param lock   the InterProcessLock representing the lock
     */
    public ZookeeperLockAcquisition(CuratorFramework client, String key, InterProcessLock lock) {
        this.client = client;
        this.lock = lock;
        this.key = key;
        this.monitor = new LockStateMonitor(key, this::onLockLost);
        client.getConnectionStateListenable().addListener(monitor);
    }

    @Override
    public void release() {
        try {
            lock.release();
            isAcquired.set(false);
            client.getConnectionStateListenable().removeListener(monitor);
            LOGGER.debug("Lock released for key: {}", lock);
        } catch (Exception e) {
            LOGGER.error("Failed to release lock for key: {}", lock, e);
            throw new ZookeeperLockReleaseException("Failed to release lock", e);
        }
    }

    @Override
    public boolean isAcquired() {
        return isAcquired.get();
    }

    @Override
    public void close() {
        release();
    }

    private void onLockLost() {
        isAcquired.set(false);
        LOGGER.warn("Lock is considered lost for key: {}", key);
    }

    /**
     * LockStateMonitor monitors the connection state and updates the lock status.
     * This is necessary to ensure that the lock status is accurately reflected when the connection state changes,
     * such as when the connection is suspended or lost.
     */
    private static class LockStateMonitor implements ConnectionStateListener {

        private static final Logger LOGGER = LoggerFactory.getLogger(LockStateMonitor.class);
        private final String key;
        private final Runnable lockLostCallback;

        public LockStateMonitor(String key, Runnable lockLostCallback) {
            this.key = key;
            this.lockLostCallback = lockLostCallback;
        }

        @Override
        public void stateChanged(CuratorFramework client, ConnectionState newState) {
            if (!newState.isConnected()) {
                LOGGER.debug("Connection state changed: {} for key: {}", newState.name(), key);
                lockLostCallback.run();
            }
        }
    }
}
