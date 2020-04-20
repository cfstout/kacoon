rootProject.name = "kacoon"

listOf(
    "examples",
    "kafka"
).forEach {
    include(":$it")
    project(":$it").projectDir = file(it)
}
