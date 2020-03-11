buildscript {
    repositories {
        jcenter()
    }
}

val deps: Map<String, String> by extra

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:${deps["jackson"]}")
    implementation("com.google.guava:guava:${deps["guava"]}")
    implementation("org.apache.kafka:kafka-clients:${deps["kafka"]}")
//    <dependency>
//    <groupId>io.dropwizard.metrics</groupId>
//    <artifactId>metrics-healthchecks</artifactId>
//    <version>${dropwizard.metrics.version}</version>
//    </dependency>


    // todo
    // implementation("com.wix.greyhound:greyhound-core_2.12:0.1.0-SNAPSHOT")
}
