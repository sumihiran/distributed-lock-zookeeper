package io.github.sumihiran.lock.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.sumihiran.lock.zookeeper.exceptions.ZookeeperLockAcquisitionException;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Provides factory methods to acquire distributed locks using Zookeeper's InterProcessLock implementations.
 *
 * @author Nuwan Bandara
 */
public class ZookeeperDistributedLock {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperDistributedLock.class);

    private final CuratorFramework client;
    private final Function<String, InterProcessLock> lockFunction;

    /**
     * Constructs a new instance with the specified CuratorFramework client.
     *
     * @param client the CuratorFramework client
     */
    public ZookeeperDistributedLock(CuratorFramework client) {
        this(client, path -> new InterProcessSemaphoreMutex(client, path));
    }

    /**
     * Constructs a new instance with the specified CuratorFramework client and a lock factory.
     *
     * @param client       the CuratorFramework client
     * @param lockFunction the function to create InterProcessLock instances
     */
    public ZookeeperDistributedLock(CuratorFramework client, Function<String, InterProcessLock> lockFunction) {
        this.client = client;
        this.lockFunction = lockFunction;
    }

    /**
     * Acquires a distributed lock for the specified key.
     *
     * @param key the lock key
     * @return an Acquisition representing the acquired lock
     * @throws ZookeeperLockAcquisitionException if the lock could not be acquired
     */
    public Acquisition acquire(String key) throws ZookeeperLockAcquisitionException {
        return acquire(key, Duration.ofMillis(Long.MAX_VALUE));
    }

    /**
     * Acquires a distributed lock for the specified key, with a timeout.
     *
     * @param key     the lock key
     * @param timeout the duration to wait for the lock
     * @return an Acquisition representing the acquired lock
     * @throws ZookeeperLockAcquisitionException if the lock could not be acquired within the timeout
     */
    public Acquisition acquire(String key, Duration timeout) throws ZookeeperLockAcquisitionException {
        InterProcessLock lock = lockFunction.apply(key);
        try {
            boolean acquired = lock.acquire(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (acquired) {
                LOGGER.debug("Lock acquired for key: {}", key);
                return new ZookeeperLockAcquisition(client, key, lock);
            } else {
                LOGGER.warn("Failed to acquire lock for key: {} within timeout: {}", key, timeout);
                throw new ZookeeperLockAcquisitionException(
                    "Failed to acquire lock for key: " + key + " within timeout: " + timeout);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to acquire lock for key: {}", key, e);
            throw new ZookeeperLockAcquisitionException("Failed to acquire lock for key: " + key, e);
        }
    }
}
