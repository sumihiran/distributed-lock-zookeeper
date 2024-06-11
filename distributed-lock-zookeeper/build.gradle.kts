import java.net.URI

plugins {
    id("java-library")
    id("checkstyle")
    id("maven-publish")
    id("signing")
    id("net.nemerosa.versioning") version "2.8.2"
}

version = versioning.info.display

dependencies {
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
    withJavadocJar()

    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

checkstyle {
    toolVersion = "9.3"
    configDirectory.set(file("config/checkstyle"))
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("Release") {
            groupId = "io.github.sumihiran"
            version = versioning.info.display
            from(components["java"])

            pom {
                name.set("Distributed Lock Zookeeper")
                url.set("https://github.com/sumihiran/distributed-lock-zookeeper")
                inceptionYear.set("2024")

                licenses {
                    license {
                        name.set("Apache-2.0 License")
                        url.set("https://apache.org/licenses/LICENSE-2.0")
                    }
                }
                developers {
                    developer {
                        name.set("Nuwan Bandara")
                        email.set("nuwansumihiran@hotmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:git:github.com/sumihiran/distributed-lock-zookeeper.git")
                    developerConnection.set("scm:git:ssh://github.com/sumihiran/distributed-lock-zookeeper.git")
                    url.set("https://github.com/sumihiran/distributed-lock-zookeeper")
                }
            }
        }
    }


    repositories {
        maven {
            name = "GitHubPackages"
            url = URI.create("https://github.com/sumihiran/distributed-lock-zookeeper")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

signing {
    val signingKeyId = System.getenv("SIGNING_KEY_ID")
    val signingKey = System.getenv("SIGNING_KEY")
    val signingPassword = System.getenv("SIGNING_PASSWORD")

    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    sign(publishing.publications["Release"])
}