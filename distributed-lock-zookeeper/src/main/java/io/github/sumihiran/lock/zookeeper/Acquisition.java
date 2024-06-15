package io.github.sumihiran.lock.zookeeper;

/**
 * Represents an acquired lock that can be released.
 *
 * @author Nuwan Bandara
 */
public interface Acquisition extends AutoCloseable {

    /**
     * Releases the acquired lock.
     */
    void release();

    /**
     * Checks if the lock is still acquired.
     *
     * @return true if the lock is still acquired, false otherwise
     */
    boolean isAcquired();

    @Override
    default void close() {
        release();
    }
}
