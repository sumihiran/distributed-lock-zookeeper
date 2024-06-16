package io.github.sumihiran.lock.zookeeper;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.Toxic;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

@Testcontainers
public class AbstractZookeeperIntegrationTest {

    protected static final int ZOOKEEPER_PORT = 2181;
    protected static final int TOXIPROXY_PORT = 8474;

    static final Network network = Network.newNetwork();
    static final DockerImageName ZOOKEEPER_IMAGE = DockerImageName.parse("zookeeper:3.9.2");
    static final DockerImageName TOXIPROXY_IMAGE = DockerImageName.parse("ghcr.io/shopify/toxiproxy:2.9.0");

    @SuppressWarnings("resource")
    @Container
    protected static final GenericContainer<?> zookeeperContainer = new GenericContainer<>(ZOOKEEPER_IMAGE)
        .withExposedPorts(ZOOKEEPER_PORT)
        .withNetwork(network)
        .withNetworkAliases("zookeeper");

    @SuppressWarnings("resource")
    @Container
    protected static GenericContainer<?> toxiproxyContainer =
        new GenericContainer<>(TOXIPROXY_IMAGE)
            .withExposedPorts(TOXIPROXY_PORT, ZOOKEEPER_PORT)
            .withNetwork(network)
            .waitingFor(Wait.forHttp("/version").forPort(TOXIPROXY_PORT));

    protected static Proxy zkProxy;

    @BeforeAll
    static void initialize() throws IOException {
        ToxiproxyClient toxiproxyClient = new ToxiproxyClient(
            toxiproxyContainer.getHost(), toxiproxyContainer.getMappedPort(8474)
        );
        zkProxy = toxiproxyClient
            .createProxy("zookeeper", "0.0.0.0:2181", "zookeeper:2181");
    }

    @BeforeEach
    void setUp() throws IOException {
        for (Toxic toxic : zkProxy.toxics().getAll()) {
            toxic.remove();
        }
        zkProxy.enable();
    }

    protected static String getZkConnectionString() {
        return String.format("%s:%d", zookeeperContainer.getHost(), zookeeperContainer.getMappedPort(ZOOKEEPER_PORT));
    }


    protected static String getZkProxyConnectionString() {
        return String.format("%s:%d", toxiproxyContainer.getHost(), toxiproxyContainer.getMappedPort(ZOOKEEPER_PORT));
    }
}
