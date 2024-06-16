package io.github.sumihiran.lock.zookeeper;

import io.github.sumihiran.lock.zookeeper.exceptions.ZookeeperLockReleaseException;

/**
 * Represents an acquired lock that can be released.
 *
 * @author Nuwan Bandara
 */
public interface Acquisition extends AutoCloseable {

    /**
     * Releases the acquired lock.
     *
     * <p>The lock release is idempotent when successful.</p>
     *
     *  @throws ZookeeperLockReleaseException if the lock has been lost and cannot be released
     *  @throws Exception if there is an error releasing the lock (ZK errors, connection interruptions)
     */
    void release() throws Exception;

    /**
     * Checks if the lock is still acquired.
     *
     * @return true if the lock is still acquired, false otherwise
     */
    boolean isAcquired();

    @Override
    default void close() throws Exception {
        release();
    }
}
