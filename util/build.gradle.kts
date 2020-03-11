plugins {
    application
}

buildscript {
    repositories {
        jcenter()
    }
}

val deps: Map<String, String> by extra

dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:${deps["jackson"]}")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${deps["jackson"]}")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:${deps["jackson"]}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${deps["jackson"]}")
    implementation("com.google.inject:guice:${deps["guice"]}")
}
