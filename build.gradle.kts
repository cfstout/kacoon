plugins {
    java
    kotlin("jvm") version "1.6.20" apply false
//    id("com.avast.gradle.docker-compose") version "0.9.4"
//    id("org.jlleitschuh.gradle.ktlint") version "9.0.0"
}

buildscript {
    repositories {
        jcenter()
    }
}


subprojects {
    // until plugins{} block is available in sub projects, do it the hard way
    apply(plugin = "org.jetbrains.kotlin.jvm")

    group = "io.github.cfstout.kacoon"
    description = "Kafka tools wrapper library"

    repositories {
        jcenter()
    }

    val deps by extra {
        mapOf(
            "commons-configuration" to "1.10",
            "guava" to "28.2-jre",
            "guice" to "4.2.2",
            "jackson" to "2.10.2",
            "junit" to "5.6.0",
            "kafka" to "2.2.1",
            "kotlinx" to "1.3.4",
            "ktor" to "1.3.1",
            "logback" to "1.2.3"
        )
    }

    dependencies {
        implementation(kotlin("stdlib-jdk8"))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${deps["kotlinx"]}")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${deps["kotlinx"]}")

        testImplementation("org.junit.jupiter", "junit-jupiter-api", deps["junit"])
        testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", deps["junit"])
    }

    tasks.test {
        useJUnitPlatform()
    }

    tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        withSourcesJar()
    }
}
