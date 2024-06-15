package io.github.sumihiran.lock.zookeeper.exceptions;

/**
 * Thrown when a lock cannot be acquired.
 *
 * @author Nuwan Bandara
 */
public final class ZookeeperLockAcquisitionException extends ZookeeperDistributedLockException {

    /**
     * Initialize the exception with given {@code message}.
     *
     * @param message a message describing the cause of the exception
     */
    public ZookeeperLockAcquisitionException(String message) {
        super(message);
    }

    /**
     * Initialize the exception with given {@code message} and {@code cause}.
     *
     * @param message a message describing the cause of the exception
     * @param cause   the cause of this exception
     */
    public ZookeeperLockAcquisitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
