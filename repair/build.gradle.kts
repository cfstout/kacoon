val deps: Map<String, String> by extra

dependencies {
    implementation(project(":util"))

    implementation("com.fasterxml.jackson.core:jackson-databind:${deps["jackson"]}")
    implementation("com.google.guava:guava:${deps["guava"]}")
    implementation("com.wix.greyhound:greyhound-core_2.12:0.1.0-SNAPSHOT")
    implementation("com.wix.greyhound:greyhound-java_2.12:0.1.0-SNAPSHOT")
}
