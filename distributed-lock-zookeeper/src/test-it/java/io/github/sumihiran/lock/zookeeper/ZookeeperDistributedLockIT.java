package io.github.sumihiran.lock.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Nuwan Bandara
 */
@Testcontainers
class ZookeeperDistributedLockIT {

    static final int ZOOKEEPER_PORT = 2181;

    @Container
    static final GenericContainer<?> zookeeper = new GenericContainer<>("zookeeper:3.9.2")
        .withExposedPorts(ZOOKEEPER_PORT);

    CuratorFramework client;


    @BeforeEach
    void setUp() {
        String zkConnectionString =
            String.format("%s:%d", zookeeper.getHost(), zookeeper.getMappedPort(ZOOKEEPER_PORT));

        client = CuratorFrameworkFactory.builder()
            .connectString(zkConnectionString)
            .retryPolicy(new RetryOneTime(100))
            .build();
        client.start();
    }

    @Test
    void testAcquireAndReleaseLock() {
        ZookeeperDistributedLock distributedLock = new ZookeeperDistributedLock(client);
        Acquisition acquisition = distributedLock.acquire("/test-lock");

        assertNotNull(acquisition);
        assertTrue(acquisition.isAcquired());

        acquisition.release();

        assertFalse(acquisition.isAcquired());
    }
}
