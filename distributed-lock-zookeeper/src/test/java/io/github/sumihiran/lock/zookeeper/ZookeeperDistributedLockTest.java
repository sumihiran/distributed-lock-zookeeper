package io.github.sumihiran.lock.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.listen.Listenable;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.sumihiran.lock.zookeeper.exceptions.ZookeeperLockAcquisitionException;
import io.github.sumihiran.lock.zookeeper.exceptions.ZookeeperLockReleaseException;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Nuwan Bandara
 */
class ZookeeperDistributedLockTest {

    CuratorFramework client;
    InterProcessLock lock;
    ZookeeperDistributedLock distributedLock;

    @BeforeEach
    void setUp() {
        client = mock(CuratorFramework.class);
        Listenable stateListenable = mock(Listenable.class);
        when(client.getConnectionStateListenable()).thenReturn(stateListenable);

        lock = mock(InterProcessSemaphoreMutex.class);
        distributedLock = new ZookeeperDistributedLock(client, path -> lock);
    }

    @Test
    void shouldAcquireAndReleaseLockSuccessfully() throws Exception {
        // Arrange
        when(lock.acquire(anyLong(), any(TimeUnit.class))).thenReturn(true);

        // Act
        Acquisition handle = distributedLock.acquire("test-key");
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
        try (Acquisition handle = distributedLock.acquire("test-key")) {
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
            () -> distributedLock.acquire("test-key")
        );

        assertEquals("Failed to acquire lock for key: test-key", exception.getMessage());
    }

    @Test
    void shouldThrowLockAcquisitionExceptionWhenAcquireTimeout() throws Exception {
        // Arrange
        when(lock.acquire(anyLong(), any(TimeUnit.class))).thenReturn(false);

        // Act & Assert
        ZookeeperLockAcquisitionException exception = assertThrows(
            ZookeeperLockAcquisitionException.class,
            () -> distributedLock.acquire("test-key", Duration.ofSeconds(1))
        );

        assertEquals(
            "Failed to acquire lock for key: test-key", exception.getMessage());
    }

    @Test
    void shouldThrowLockReleaseExceptionWhenReleaseFails() throws Exception {
        // Arrange
        when(lock.acquire(anyLong(), any(TimeUnit.class))).thenReturn(true);
        doThrow(new Exception("Test exception")).when(lock).release();

        // Act
        Acquisition handle = distributedLock.acquire("test-key");
        Exception exception = assertThrows(ZookeeperLockReleaseException.class, handle::release);

        // Assert
        assertEquals("Failed to release lock", exception.getMessage());
    }
}