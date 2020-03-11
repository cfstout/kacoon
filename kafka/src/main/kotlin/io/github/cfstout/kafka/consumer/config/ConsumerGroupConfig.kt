package io.github.cfstout.kafka.consumer.config

import com.fasterxml.jackson.annotation.JsonProperty
import io.github.cfstout.kafka.consumer.config.ConsumerFlowConfig
import io.github.cfstout.kafka.types.ConsumerGroupName
import java.time.Duration
import java.util.regex.Pattern

data class ConsumerGroupConfig
@JvmOverloads
constructor(@JsonProperty("groupName") val groupName: ConsumerGroupName,
            @JsonProperty("pattern") private val patternString: String,
            @JsonProperty("consumerOverrideConfig") val overrides: Map<String, Any> = mapOf(),
            @JsonProperty("commitOffsets") val commitOffsets: Duration = Duration.ofSeconds(5), // how often to commit offsets
            @JsonProperty("consumerFlowConfig") val consumerFlowConfig: ConsumerFlowConfig = ConsumerFlowConfig(),
            @JsonProperty("consumerPollTimeout") val pollTimeout: Duration = Duration.ofMillis(10) // timeout we pass to consumer.poll()
) {
    val pattern: Pattern
        get() = Pattern.compile(patternString)
}