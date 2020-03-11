rootProject.name = "kacoon"

listOf(
    "ktor",
    "util"
).forEach {
    include(":$it")
    project(":$it").projectDir = file(it)
}
