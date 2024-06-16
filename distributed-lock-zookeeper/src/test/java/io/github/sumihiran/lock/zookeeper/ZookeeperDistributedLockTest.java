package io.github.sumihiran.lock.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.listen.Listenable;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.sumihiran.lock.zookeeper.exceptions.ZookeeperLockAcquisitionException;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Nuwan Bandara
 */
class ZookeeperDistributedLockTest {

    static String lockNodePath = "/path/to/lock";

    CuratorFramework client;
    InterProcessLock lock;
    ZookeeperDistributedLock distributedLock;

    @BeforeEach
    void setUp() {
        client = mock(CuratorFramework.class);
        Listenable stateListenable = mock(Listenable.class);
        when(client.getConnectionStateListenable()).thenReturn(stateListenable);
        when(client.getState()).thenReturn(CuratorFrameworkState.STARTED);

        lock = mock(InterProcessSemaphoreMutex.class);
        distributedLock = new ZookeeperDistributedLock(client, path -> lock);
    }

    @Test
    void shouldAcquireAndReleaseLockSuccessfully() throws Exception {
        // Arrange
        when(lock.acquire(anyLong(), any(TimeUnit.class))).thenReturn(true);

        // Act
        Acquisition handle = distributedLock.acquire(lockNodePath);
        handle.release();

        // Assert
        verify(lock).acquire(anyLong(), any(TimeUnit.class));
        verify(lock).release();
        assertFalse(handle.isAcquired());
    }

    @Test
    void shouldReleaseLockWithAutoCloseable() throws Exception {
        // Arrange
        when(lock.acquire(anyLong(), any(TimeUnit.class))).thenReturn(true);

        // Act
        try (Acquisition handle = distributedLock.acquire(lockNodePath)) {
            assertInstanceOf(AutoCloseable.class, handle);
            assertTrue(handle.isAcquired());
        }

        // Assert
        verify(lock).release();
    }

    @Test
    void shouldThrowLockAcquisitionExceptionWhenAcquireFails() throws Exception {
        // Arrange
        when(lock.acquire(anyLong(), any(TimeUnit.class))).thenThrow(new RuntimeException("Acquire failed"));

        // Act & Assert
        ZookeeperLockAcquisitionException exception = assertThrows(
            ZookeeperLockAcquisitionException.class,
            () -> distributedLock.acquire(lockNodePath)
        );

        assertEquals("Failed to acquire lock for key: /path/to/lock", exception.getMessage());
    }

    @Test
    void shouldThrowLockAcquisitionExceptionWhenAcquireTimeout() throws Exception {
        // Arrange
        when(lock.acquire(anyLong(), any(TimeUnit.class))).thenReturn(false);

        // Act & Assert
        ZookeeperLockAcquisitionException exception = assertThrows(
            ZookeeperLockAcquisitionException.class,
            () -> distributedLock.acquire(lockNodePath, Duration.ofSeconds(1))
        );

        assertEquals(
            "Failed to acquire lock for key: /path/to/lock", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenClientNotStarted() throws Exception {
        // Arrange
        when(client.getState()).thenReturn(CuratorFrameworkState.LATENT);
        when(lock.acquire(anyLong(), any(TimeUnit.class))).thenReturn(true);

        // Act & Assert
        ZookeeperLockAcquisitionException exception =
            assertThrows(ZookeeperLockAcquisitionException.class, () -> distributedLock.acquire("/test-key"));

        assertInstanceOf(IllegalStateException.class, exception.getCause());

        IllegalStateException cause = (IllegalStateException) exception.getCause();
        assertEquals(
            "CuratorFramework client is not started",
            cause.getMessage()
        );
    }


}
