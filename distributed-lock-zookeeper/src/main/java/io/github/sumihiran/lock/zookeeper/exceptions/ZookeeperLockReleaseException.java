package io.github.sumihiran.lock.zookeeper.exceptions;

/**
 * Thrown when a lock cannot be released.
 *
 * @author Nuwan Bandara
 */
public final class ZookeeperLockReleaseException extends ZookeeperDistributedLockException {

    /**
     * Initialize the exception with given {@code message}.
     *
     * @param message a message describing the cause of the exception
     */
    public ZookeeperLockReleaseException(String message) {
        super(message);
    }

    /**
     * Initialize the exception with given {@code message} and {@code cause}.
     *
     * @param message a message describing the cause of the exception
     * @param cause   the cause of this exception
     */
    public ZookeeperLockReleaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
