buildscript {
    repositories {
        jcenter()
    }
}

val deps: Map<String, String> by extra

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:${deps["jackson"]}")
    api("com.google.guava:guava:${deps["guava"]}")
    api("org.apache.kafka:kafka-clients:${deps["kafka"]}")
}
