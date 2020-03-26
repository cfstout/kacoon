rootProject.name = "kacoon"

listOf(
    "kafka",
    "ktor",
    "repair",
    "util"
).forEach {
    include(":$it")
    project(":$it").projectDir = file(it)
}
