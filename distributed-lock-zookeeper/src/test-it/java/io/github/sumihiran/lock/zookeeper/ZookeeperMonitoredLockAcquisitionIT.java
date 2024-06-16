package io.github.sumihiran.lock.zookeeper;

import eu.rekawek.toxiproxy.model.ToxicDirection;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.sumihiran.lock.zookeeper.exceptions.ZookeeperLockReleaseException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Nuwan Bandara
 */
@Testcontainers
class ZookeeperMonitoredLockAcquisitionIT extends AbstractZookeeperIntegrationTest {

    @Test
    void shouldConsiderLockLostWhenConnectionFails() throws Exception {
        // Arrange
        CuratorFramework client = CuratorFrameworkFactory.builder()
            .connectString(getZkProxyConnectionString())
            .retryPolicy(new RetryOneTime(0))
            .sessionTimeoutMs(1000)
            .build();
        client.start();
        ZookeeperDistributedLock distributedLock = new ZookeeperDistributedLock(client);

        // Act
        Acquisition acquisition = distributedLock.acquire("/test-lock");

        // Assert
        assertNotNull(acquisition);
        ZookeeperMonitoredLockAcquisition zkAcquisition =
            assertInstanceOf(ZookeeperMonitoredLockAcquisition.class, acquisition);

        assertTrue(zkAcquisition.isAcquired());

        // Act: simulate network disconnection
        zkProxy.toxics().timeout("timeout", ToxicDirection.DOWNSTREAM, 5000);

        Thread.sleep(2000);
        zkProxy.toxics().get("timeout").remove();

        Thread.sleep(2000);

        // Assert
        assertFalse(zkAcquisition.isAcquired());
        assertTrue(zkAcquisition.isLockLost());
        assertThrows(ZookeeperLockReleaseException.class, zkAcquisition::release);
    }

    @Test
    void shouldReacquireLockAfterSessionExpiration() throws Exception {
        // Arrange
        CuratorFramework client = CuratorFrameworkFactory.builder()
            .connectString(getZkConnectionString())
            .retryPolicy(new RetryOneTime(0))
            .sessionTimeoutMs(5000)
            .build();
        client.start();
        ZookeeperDistributedLock distributedLock = new ZookeeperDistributedLock(client);

        // Act: Acquire the lock
        Acquisition acquisition = distributedLock.acquire("/test-lock");

        // Assert initial acquisition
        assertNotNull(acquisition);
        assertTrue(acquisition.isAcquired());

        // Act: simulate session expiration
        client.getZookeeperClient().getZooKeeper().getTestable().injectSessionExpiration();

        // Give some time for session expiration to be processed
        Thread.sleep(6000);

        // Assert lock is lost
        assertFalse(acquisition.isAcquired());

        // Act: Re-establish the session and try to reacquire the lock
        Acquisition newAcquisition = distributedLock.acquire("/test-lock");

        // Assert new acquisition
        assertNotNull(newAcquisition);
        assertTrue(newAcquisition.isAcquired());

        // Cleanup
        newAcquisition.release();
    }
}
