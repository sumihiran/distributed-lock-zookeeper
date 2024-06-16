package io.github.sumihiran.lock.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.listen.Listenable;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.github.sumihiran.lock.zookeeper.exceptions.ZookeeperLockReleaseException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Nuwan Bandara
 */
class ZookeeperMonitoredLockAcquisitionTest {

    CuratorFramework client;
    InterProcessSemaphoreMutex lock;
    Listenable<ConnectionStateListener> stateListenable;

    ZookeeperMonitoredLockAcquisition lockAcquisition;

    @BeforeEach
    void setUp() {
        client = mock(CuratorFramework.class);
        lock = mock(InterProcessSemaphoreMutex.class);
        stateListenable = mock(Listenable.class);
        when(client.getConnectionStateListenable()).thenReturn(stateListenable);
        when(client.getState()).thenReturn(CuratorFrameworkState.STARTED);

        lockAcquisition = new ZookeeperMonitoredLockAcquisition(client, "/test-key", lock);

        assertTrue(lockAcquisition.isAcquired());
    }

    @Test
    void shouldReleaseLockSuccessfully() throws Exception {
        // Act
        lockAcquisition.release();

        // Assert
        verify(lock).release();
        assertFalse(lockAcquisition.isAcquired());

        verify(stateListenable).removeListener(any(ConnectionStateListener.class));
    }

    @Test
    void shouldReleaseLockWhenClosed() throws Exception {
        // Act
        lockAcquisition.close();

        // Assert
        verify(lock).release();
        assertFalse(lockAcquisition.isAcquired());

        verify(client.getConnectionStateListenable()).removeListener(any(ConnectionStateListener.class));
    }

    @Test
    void shouldReleaseLockIdempotent() throws Exception {
        // Act
        lockAcquisition.release();
        lockAcquisition.release();

        // Assert
        verify(lock, times(1)).release();
        assertFalse(lockAcquisition.isAcquired());
        verify(stateListenable, times(1)).removeListener(any(ConnectionStateListener.class));
    }


    @Test
    void shouldConsiderLockLostWhenConnectionStateIsLost() {
        // Arrange
        ArgumentCaptor<ConnectionStateListener> listenerCaptor = ArgumentCaptor.forClass(ConnectionStateListener.class);
        verify(client.getConnectionStateListenable()).addListener(listenerCaptor.capture());
        ConnectionStateListener listener = listenerCaptor.getValue();

        // Act
        listener.stateChanged(client, ConnectionState.LOST);

        // Assert
        assertFalse(lockAcquisition.isAcquired());
        assertTrue(lockAcquisition.isLockLost());
    }

    @Test
    void shouldConsiderLockLostWhenConnectionStateIsSuspended() {
        // Arrange
        ArgumentCaptor<ConnectionStateListener> listenerCaptor = ArgumentCaptor.forClass(ConnectionStateListener.class);
        verify(client.getConnectionStateListenable()).addListener(listenerCaptor.capture());
        ConnectionStateListener listener = listenerCaptor.getValue();

        // Act
        listener.stateChanged(client, ConnectionState.SUSPENDED);

        // Assert
        assertFalse(lockAcquisition.isAcquired());
        assertTrue(lockAcquisition.isLockLost());
    }

    @Test
    void shouldRemainLostWhenConnectionIsLostAndReconnected() {
        // Arrange
        ArgumentCaptor<ConnectionStateListener> listenerCaptor = ArgumentCaptor.forClass(ConnectionStateListener.class);
        verify(client.getConnectionStateListenable()).addListener(listenerCaptor.capture());
        ConnectionStateListener listener = listenerCaptor.getValue();

        // Act
        listener.stateChanged(client, ConnectionState.LOST);

        // Assert lock is lost
        assertFalse(lockAcquisition.isAcquired());
        assertTrue(lockAcquisition.isLockLost());

        // Act: Simulate reconnection
        listener.stateChanged(client, ConnectionState.RECONNECTED);

        // Assert lock remains lost
        assertFalse(lockAcquisition.isAcquired());
        assertTrue(lockAcquisition.isLockLost());
    }


    @Test
    void shouldThrowLockReleaseExceptionWhenReleaseFails() throws Exception {
        // Arrange
        doThrow(new Exception("Test exception")).when(lock).release();

        // Act & Assert
        assertThrows(Exception.class, () -> lockAcquisition.release());
    }

    @Test
    void shouldThrowExceptionWhenReleasingLostLock() {
        // Arrange
        ArgumentCaptor<ConnectionStateListener> listenerCaptor = ArgumentCaptor.forClass(ConnectionStateListener.class);
        verify(client.getConnectionStateListenable()).addListener(listenerCaptor.capture());
        ConnectionStateListener listener = listenerCaptor.getValue();
        listener.stateChanged(client, ConnectionState.LOST);

        // Act & Assert
        ZookeeperLockReleaseException exception =
            assertThrows(ZookeeperLockReleaseException.class, () -> lockAcquisition.release());

        assertTrue(lockAcquisition.isLockLost());
        assertFalse(lockAcquisition.isAcquired());
        assertEquals("/test-key", exception.getKey());
    }
}
