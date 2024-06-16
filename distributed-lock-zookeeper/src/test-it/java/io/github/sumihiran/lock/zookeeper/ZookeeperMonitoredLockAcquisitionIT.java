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
}
