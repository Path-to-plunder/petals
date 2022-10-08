import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File
import java.io.FileInputStream
import java.util.*

val compileKotlin: KotlinCompile by tasks

val kotlinpoetVersion: String by project
val kotlinVersion: String by project
val googleAutoServiceVersion: String by project

val exposedVersion: String by project

val assertKVersion: String by project

plugins {
    `java-library`
    `maven-publish`
    signing
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.serialization")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":annotations"))
    implementation("com.casadetasha:annotation-parser:0.1.0")
    implementation("com.casadetasha:kotlin-generation-dsl:0.2.0")

    implementation("com.google.auto.service:auto-service:$googleAutoServiceVersion")
    kapt("com.google.auto.service:auto-service:$googleAutoServiceVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.5.0")
    implementation("com.squareup:kotlinpoet:$kotlinpoetVersion")
    implementation("com.squareup:kotlinpoet-metadata:$kotlinpoetVersion")

    implementation("com.zaxxer:HikariCP:5.0.0")

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")

    // Escape SQL Strings
    implementation("org.apache.commons:commons-text:1.9")

    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.4.9")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:$assertKVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "11"
            freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
        }
    }

    withType<Test> {
        minHeapSize = "512m"
        maxHeapSize = "4096m"
        jvmArgs = listOf("-XX:MaxPermSize=1024m")
    }
}

val prop = Properties().apply {
    load(FileInputStream(File(project.gradle.gradleUserHomeDir, "local.properties")))
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    dependsOn("javadoc")
    from(tasks.javadoc.get().destinationDir)
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

publishing {
    repositories {
        maven {
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = prop.getProperty("ossrhUsername")
                password = prop.getProperty("ossrhPassword")
            }
        }
    }

    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])

            group = "com.casadetasha"
            artifactId = "petals-processor"
            version = "1.6.3-alpha-1"

            artifact(sourcesJar.get())
            artifact(javadocJar.get())

            pom {
                name.set("Petals processor for exposed with PostgreSql")
                description.set("KAPT processor to manage boilderplate for using Exposed to manage PostgreSql DB. Use" +
                        " in conjunction with petals")
                url.set("https://github.com/konk3r/petals")

                scm {
                    connection.set("scm:git://github.com/konk3r/petals")
                    developerConnection.set("scm:git:git@github.com:konk3r/petals.git")
                    url.set("https://github.com/konk3r/petals")
                }

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("gabriel")
                        name.set("Gabriel Spencer")
                        email.set("gabriel@casadetasha.dev")
                    }
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}
