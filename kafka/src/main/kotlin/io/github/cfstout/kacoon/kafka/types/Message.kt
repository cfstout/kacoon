package io.github.cfstout.kacoon.kafka.types

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

open class Message<T>(@JsonProperty("data") val data: T,
                 @JsonIgnore private val partitionKeyFunction: (T) -> PartitionKey) {
    @JsonIgnore
    fun getPartitionKey(): PartitionKey {
        return partitionKeyFunction.invoke(data)
    }
}