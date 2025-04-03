plugins {
    `java-library`
    kotlin("jvm") version "2.1.0"
    kotlin("kapt") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
}

repositories {
    maven {
        isAllowInsecureProtocol = true
        url = uri("http://localhost:8081/repository/maven-local/")
    }
    mavenCentral()
}
