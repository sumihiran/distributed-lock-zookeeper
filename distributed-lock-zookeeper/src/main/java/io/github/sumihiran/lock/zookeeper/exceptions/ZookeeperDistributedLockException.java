package io.github.sumihiran.lock.zookeeper.exceptions;

/**
 * Thrown when there is an error related to acquiring or releasing zookeeper based distributed locks.
 *
 * @author Nuwan Bandara
 */
public class ZookeeperDistributedLockException extends RuntimeException {

    /**
     * Initialize the exception with given {@code message}.
     *
     * @param message a message describing the cause of the exception
     */
    public ZookeeperDistributedLockException(String message) {
        super(message);
    }

    /**
     * Initialize the exception with given {@code message} and {@code cause}.
     *
     * @param message a message describing the cause of the exception
     * @param cause   the cause of this exception
     */
    public ZookeeperDistributedLockException(String message, Throwable cause) {
        super(message, cause);
    }
}
