val ktorVersion: String by project
val kotlinVersion: String by project
val postgresqlVersion: String by project

val exposedVersion: String by project
val assertKVersion: String by project

plugins {
    application
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.serialization")
}

repositories {
    maven {
        isAllowInsecureProtocol = true
        url = uri("http://localhost:8081/repository/maven-local/")
    }
    mavenCentral()
}

dependencies {
    implementation(project(":annotations"))
    kapt(project(":processor"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    implementation("com.zaxxer:HikariCP:5.0.1")

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")

    testImplementation(project(":processor"))
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:$assertKVersion")
    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")

    // DB Container
    testImplementation("org.testcontainers:testcontainers:1.17.4")
    testImplementation("org.testcontainers:postgresql:1.17.4")
    testImplementation("org.postgresql:postgresql:$postgresqlVersion")
    testImplementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
}
