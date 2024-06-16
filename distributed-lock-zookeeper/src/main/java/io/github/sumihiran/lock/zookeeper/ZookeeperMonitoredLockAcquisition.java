package io.github.sumihiran.lock.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.state.ConnectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.sumihiran.lock.zookeeper.exceptions.ZookeeperLockReleaseException;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents an acquired lock in Zookeeper and manages its lifecycle by monitoring the connection state.
 *
 */
public class ZookeeperMonitoredLockAcquisition implements Acquisition {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperMonitoredLockAcquisition.class);

    private final InterProcessLock lock;
    private final String key;
    private final CuratorFramework client;

    private final AtomicBoolean isAcquired = new AtomicBoolean(true);
    private final AtomicBoolean isLockLost = new AtomicBoolean(false);

    /**
     * Constructs a new instance with the specified {@link CuratorFramework} {@code client}, {@code key},
     * and {@code lock}.
     *
     * @param client the CuratorFramework client
     * @param key    the lock key
     * @param lock   the InterProcessLock representing the lock
     */
    public ZookeeperMonitoredLockAcquisition(CuratorFramework client, String key, InterProcessLock lock) {
        this.client = client;
        this.lock = lock;
        this.key = key;
        client.getConnectionStateListenable().addListener(this::handleConnectionStateChange);
    }

    @Override
    public void release() throws Exception {
        if (!isAcquired.get()) {
            if (isLockLost.get()) {
                throw new ZookeeperLockReleaseException(key, "Cannot release a lost lock");
            }
            LOGGER.debug("Lock already released for key: {}", key);
            return;
        }

        lock.release();
        if (isAcquired.getAndSet(false)) {
            unregisterConnectionStateListener();
            LOGGER.debug("Lock released for key: {}", key);
        }
    }

    @Override
    public boolean isAcquired() {
        return isAcquired.get();
    }

    /**
     * Checks if the lock has been lost.
     *
     * @return true if the lock has been lost, false otherwise
     */
    public boolean isLockLost() {
        return isLockLost.get();
    }

    private void handleConnectionStateChange(CuratorFramework client, ConnectionState newState) {
        if (!newState.isConnected()) {
            LOGGER.debug("Connection state changed: {} for key: {}", newState.name(), key);
            onLockLost();
        }
    }

    private void unregisterConnectionStateListener() {
        client.getConnectionStateListenable().removeListener(this::handleConnectionStateChange);
    }

    private void onLockLost() {
        unregisterConnectionStateListener();
        isAcquired.set(false);
        isLockLost.set(true);
        LOGGER.warn("Lock is considered lost for key: {}", key);
    }
}
