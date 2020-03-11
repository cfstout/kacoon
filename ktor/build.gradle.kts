plugins {
    application
    id("com.github.johnrengelman.shadow") version "5.0.0"
}

buildscript {
    repositories {
        jcenter()
    }
}

val deps: Map<String, String> by extra

dependencies {
    implementation(project(":util"))
    implementation("commons-configuration:commons-configuration:${deps["commons-configuration"]}")
    implementation("ch.qos.logback:logback-classic:${deps["logback-classic"]}")
    implementation("com.google.inject:guice:${deps["guice"]}")
    implementation("io.ktor:ktor-server-core:${deps["ktor"]}")
    implementation("io.ktor:ktor-server-netty:${deps["ktor"]}")
    implementation("io.ktor:ktor-jackson:${deps["ktor"]}")

}

tasks {
    shadowJar {
        archiveBaseName.set("kacoon-ktor")
        mergeServiceFiles()
    }
}