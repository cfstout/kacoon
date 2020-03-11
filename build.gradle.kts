import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    java
    kotlin("jvm") version "1.3.61"
    id("com.avast.gradle.docker-compose") version "0.9.4"
    id("org.jlleitschuh.gradle.ktlint") version "9.0.0"
}


subprojects {
    // until plugins{} block is available in sub projects, do it the hard way
    apply(plugin = "org.jetbrains.kotlin.jvm")

    group = "io.github.cfstout.kacoon"
    description = "Kafka tools wrapper library"

    repositories {
        jcenter()
        mavenLocal()
    }

    val deps by extra {
        mapOf(
            "commons-configuration" to "1.10",
            "guice" to "4.2.2",
            "jackson" to "2.10.2",
            "junit" to "5.6.0",
            "ktor" to "1.3.1",
            "logback-classic" to "1.2.3"
        )
    }

    dependencies {
        implementation(kotlin("stdlib-jdk8"))

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