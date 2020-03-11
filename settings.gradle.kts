rootProject.name = "kacoon"

listOf(
    "kafka",
    "ktor",
    "util"
).forEach {
    include(":$it")
    project(":$it").projectDir = file(it)
}
