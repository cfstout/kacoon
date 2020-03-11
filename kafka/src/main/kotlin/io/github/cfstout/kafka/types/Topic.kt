package io.github.cfstout.kafka.types

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StringDeserializer

@JsonDeserialize(using = StringDeserializer::class)
data class Topic(val value: String)