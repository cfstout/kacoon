package io.github.cfstout.kacoon.kafka.consumer.config

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Duration

data class ConsumerFlowConfig
@JvmOverloads
constructor(
        // If the backlog for any partition grows beyond this value, pause until it drops below the resume threshold
        @JsonProperty("pausePartitionThreshold") val pausePartitionThreshold: Int = 40,
        // Size backlog must fall below to resume fetching
        @JsonProperty("resumePartitionThreshold") val resumePartitionThreshold: Int = 10,
        /**
         * Time we must wait after resuming to pause again. This means that [pausePartitionThreshold] is _not_ the
         * ceiling for on heap messages. The effective ceiling is resumePartitionThreshold + number of messages
         * that can be fetched w/in this [minPauseInterval]
         *
         * tldr: a partition resumed at time t cannot be paused again until time >= t + [minPauseInterval]
         */
        @JsonProperty("minPauseInterval") val minPauseInterval: Duration = Duration.ofMillis(250))