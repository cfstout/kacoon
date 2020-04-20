package io.github.cfstout.kacoon.kafka.types

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

data class PartitionKey @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    @JsonValue val value: String
)

