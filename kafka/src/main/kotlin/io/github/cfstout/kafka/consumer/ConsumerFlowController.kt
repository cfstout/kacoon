package io.github.cfstout.kafka.consumer

import com.google.common.annotations.VisibleForTesting
import com.google.common.collect.Maps
import io.github.cfstout.kafka.consumer.config.ConsumerFlowConfig
import io.github.cfstout.kafka.types.ConsumerGroupName
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * Regulates back-pressure in Kafka consumers by pausing and resuming partitions
 * at configurable thresholds.
 */
class ConsumerFlowController<K, V>(consumerGroupName: ConsumerGroupName,
                                   private val consumerFlowConfig: ConsumerFlowConfig,
                                   private val backlogSizeSupplier: () -> Map<TopicPartition, Int>) :
    ConsumerLoopActor<K, V> {

    private val lastResume: MutableMap<TopicPartition, Instant> = Maps.newConcurrentMap()
//    private val backlogSizes: LoadingCache<TopicPartition, Histogram> = CacheBuilder.newBuilder()
//            .build(object : CacheLoader<TopicPartition, Histogram>() {
//                override fun load(key: TopicPartition): Histogram {
//                    return metricFactory.histogram("kafka_consumer.backlog", MoreMaps.merge(
//                            standardMetricTags(consumerGroupName),
//                            mapOf("topic" to key.topic(),
//                                    "partition" to key.partition().toString())
//                    ))
//                }
//
//            })

    override val actInLoop: (Consumer<K, V>, ConsumerRecords<K, V>) -> Unit = { consumer, _ ->
        run {
            val flowState = computeState(consumer.assignment(), consumer.paused())
//            consumer.assignment().forEach {
//                backlogSizes.get(it).update(backlogSizeSupplier.invoke().getOrDefault(it, 0))
//            }
            if (flowState.toPause.isNotEmpty()) {
                logger.info("pausing partitions {}", flowState.toPause.joinToString { it.toString() })
                consumer.pause(flowState.toPause)
            }

            if (flowState.toResume.isNotEmpty()) {
                logger.info("resuming partitions {}", flowState.toResume.joinToString { it.toString() })
                consumer.resume(flowState.toResume)

                // Record resumed times
                val now = Instant.now()
                flowState.toResume.forEach { this.lastResume[it] = now }
            }
        }
    }

    @VisibleForTesting
    internal fun markLastResumed(partition: TopicPartition) {
        lastResume[partition] = Instant.now()
    }

    @VisibleForTesting
    internal fun computeState(currentAssignments: Set<TopicPartition>,
                              currentlyPaused: Set<TopicPartition>): FlowState {
        val now = Instant.now()
        val sizes = backlogSizeSupplier.invoke()

        // Check if any unpaused partitions need to be paused
        val maxResumed = now.minusMillis(consumerFlowConfig.minPauseInterval.toMillis())
        val behind: Set<TopicPartition> = (currentAssignments - currentlyPaused)
                .filter { sizes.getOrDefault(it, 0) >= consumerFlowConfig.pausePartitionThreshold }
                // Don't re-pause any that were resumed recently
                .filter { lastResume[it]?.isBefore(maxResumed) ?: true }
                .toSet()

        // Check any paused partitions that can be resumed
        val caughtUp: Set<TopicPartition> = currentlyPaused
                .filter { sizes.getOrDefault(it, 0) < consumerFlowConfig.resumePartitionThreshold }
                .toSet()

        return FlowState(behind, caughtUp)
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ConsumerFlowController::class.java)
    }

    internal data class FlowState(val toPause: Set<TopicPartition>,
                                 val toResume: Set<TopicPartition>)
}