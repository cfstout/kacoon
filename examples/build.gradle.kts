val deps: Map<String, String> by extra

dependencies {
    implementation(project(":kafka"))
    implementation("ch.qos.logback", "logback-classic", deps["logback"])
    implementation("io.ktor", "ktor-jackson", "${deps["ktor"]}")
    implementation("io.ktor", "ktor-server-netty", deps["ktor"])
}