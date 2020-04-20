package io.github.cfstout.kacoon.kafka.consumer

import io.github.cfstout.kacoon.kafka.consumer.config.ConsumerFlowConfig
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Duration

internal class ConsumerFlowControllerTest {
    companion object {
        private val consumerFlowConfig = ConsumerFlowConfig(minPauseInterval = Duration.ofSeconds(2))
        private const val topicName = "foo"
        private val partition0 = TopicPartition(topicName, 0)
        private val partition1 = TopicPartition(topicName, 1)
        private val currentBacklog: MutableMap<TopicPartition, Int> = mutableMapOf()
    }


    @Test
    internal fun testConsumerFlowController() {
        // Start off w/ empty backlog
        val unit = ConsumerFlowController<ByteArray, ByteArray>(
                consumerFlowConfig
        ) { currentBacklog }
        var actualFlowState = unit.computeState(setOf(partition0), setOf())
        var expectedFlowState = ConsumerFlowController.FlowState(setOf(), setOf())
        Assertions.assertEquals(expectedFlowState, actualFlowState, actualFlowState.toString())

        // Flood of messages to partition
        currentBacklog[partition0] = 100
        actualFlowState = unit.computeState(setOf(partition0), setOf())
        expectedFlowState = ConsumerFlowController.FlowState(setOf(partition0), setOf())
        Assertions.assertEquals(expectedFlowState, actualFlowState, actualFlowState.toString())

        // Less than the threshold to pause, but still greater than resume threshold
        currentBacklog[partition0] = 20
        // now since partition is paused, it's not part of assigned
        actualFlowState = unit.computeState(setOf(), setOf(partition0))
        expectedFlowState = ConsumerFlowController.FlowState(setOf(), setOf())
        Assertions.assertEquals(expectedFlowState, actualFlowState, actualFlowState.toString())

        // Add another (empty) partition
        actualFlowState = unit.computeState(setOf(partition1), setOf(partition0))
        expectedFlowState = ConsumerFlowController.FlowState(setOf(), setOf())
        Assertions.assertEquals(expectedFlowState, actualFlowState, actualFlowState.toString())

        // Backlog for 0 goes below threshold
        currentBacklog[partition0] = 5
        // now since partition is paused, it's not part of assigned
        actualFlowState = unit.computeState(setOf(partition1), setOf(partition0))
        expectedFlowState = ConsumerFlowController.FlowState(setOf(), setOf(partition0))
        Assertions.assertEquals(expectedFlowState, actualFlowState, actualFlowState.toString())
        unit.markLastResumed(partition0)

        // Backlog for both goes high, but too soon to pause 0 again
        currentBacklog[partition0] = 100
        currentBacklog[partition1] = 100
        actualFlowState = unit.computeState(setOf(partition0, partition1), setOf())
        expectedFlowState = ConsumerFlowController.FlowState(setOf(partition1), setOf())
        Assertions.assertEquals(expectedFlowState, actualFlowState, actualFlowState.toString())

        // Sleep longer than the re-pause interval, but we've caught up w/ 1
        currentBacklog[partition1] = 0
        Thread.sleep(2_000)
        actualFlowState = unit.computeState(setOf(partition0), setOf(partition1))
        expectedFlowState = ConsumerFlowController.FlowState(setOf(partition0), setOf(partition1))
        Assertions.assertEquals(expectedFlowState, actualFlowState, actualFlowState.toString())
    }
}