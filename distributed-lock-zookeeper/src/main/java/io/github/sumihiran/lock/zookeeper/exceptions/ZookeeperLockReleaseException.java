package io.github.sumihiran.lock.zookeeper.exceptions;

/**
 * Thrown when a lock cannot be released.
 *
 * @author Nuwan Bandara
 */
public final class ZookeeperLockReleaseException extends Exception {

    private final String key;

    /**
     * Initialize the exception with given lock {@code key} and {@code message}.
     *
     * @param key     the lock key
     * @param message a message describing the cause of the exception
     */
    public ZookeeperLockReleaseException(String key, String message) {
        super(message);
        this.key = key;
    }

    /**
     * Returns the lock key associated with this exception.
     *
     * @return the lock key
     */
    public String getKey() {
        return key;
    }
}
