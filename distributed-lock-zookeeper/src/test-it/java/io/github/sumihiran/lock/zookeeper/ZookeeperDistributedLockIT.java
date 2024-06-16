package io.github.sumihiran.lock.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ZookeeperDistributedLockIT extends AbstractZookeeperIntegrationTest{

    @Test
    void shouldAcquireAndReleaseLockSuccessfully() throws IOException {
        // Arrange
        CuratorFramework client = CuratorFrameworkFactory.builder()
            .connectString(getZkConnectionString())
            .retryPolicy(new RetryOneTime(100))
            .build();
        client.start();
        ZookeeperDistributedLock distributedLock = new ZookeeperDistributedLock(client);

        // Act: acquire lock
        Acquisition acquisition = distributedLock.acquire("/test-lock");

        // Assert
        assertNotNull(acquisition);
        assertTrue(acquisition.isAcquired());

        // Act: release lock
        assertDoesNotThrow(acquisition::release);

        // Assert
        assertFalse(acquisition.isAcquired());
    }
}
