plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "distributed-lock-zk"
include("distributed-lock-zookeeper")
