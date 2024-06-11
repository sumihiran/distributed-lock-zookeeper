plugins {
    id("java-library")
    id("checkstyle")
    id("maven-publish")
}

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
