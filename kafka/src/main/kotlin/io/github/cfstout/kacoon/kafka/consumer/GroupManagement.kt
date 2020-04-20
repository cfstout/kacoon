package io.github.cfstout.kacoon.kafka.consumer

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.regex.Pattern

/**
 * Class to handle consumer group subscriptions, rebalancing, and committing of offsets
 */
class GroupManagement<K, V>(
    topicPattern: Pattern,
    private val commitOffsets: Duration,
    private val internalConsumer: Consumer<*, *>,
    private val offsetTracking: OffsetTracking
) : ConsumerLoopActor<K, V> {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(GroupManagement::class.java)
    }

    private val rebalanceListener = object : ConsumerRebalanceListener {
        override fun onPartitionsAssigned(partitions: Collection<TopicPartition>) {
            if (partitions.isEmpty()) {
                return
            }
            // when partitions are added, we need to initialize w/ our current position so we have something
            // to commit
            partitions.forEach {
                val position = internalConsumer.position(it)
                offsetTracking.partitionAdded(it, position)
            }
        }

        override fun onPartitionsRevoked(partitions: Collection<TopicPartition>) {
            if (partitions.isEmpty()) {
                return
            }
            // when partitions are removed, try to minimize extra work and commit what we've done for them
            // since rebalances are rare and we want to minimize overlapping work, blocking commit to ensure
            // it goes through
            val offsets = buildOffsets(partitions)
            internalConsumer.commitSync(offsets)
            offsetTracking.partitionsRemoved(partitions)
        }
    }

    private var lastCommit = Instant.now()

    init {
        internalConsumer.subscribe(topicPattern, rebalanceListener)
    }

    override val actInLoop: (Consumer<K, V>, ConsumerRecords<K, V>) -> Unit = { consumer, _ ->
        run {
            require(internalConsumer == consumer) { "Got different internal consumer!!" }
            val lastAssignment = internalConsumer.assignment()

            // Commit if it's time to
            if (Instant.now().isAfter(nextCommitTime())) {
                val offsets = buildOffsets(lastAssignment)
                internalConsumer.commitAsync(offsets) { _, exception ->
                    run {
                        if (exception != null) {
                            logger.error("Failed to commit offsets", exception)
                        }
                    }
                }
                this.lastCommit = Instant.now()
            }
        }
    }

    override fun close() {
        internalConsumer.commitSync(buildOffsets(internalConsumer.assignment()))
    }

    private fun nextCommitTime(): Instant {
        return this.lastCommit.plusMillis(commitOffsets.toMillis())
    }

    private fun buildOffsets(partitions: Collection<TopicPartition>): Map<TopicPartition, OffsetAndMetadata> {
        return partitions
            .map { offsetTracking.offsetsToCommit(it, internalConsumer.committed(it) ?: OffsetAndMetadata(0)) }
            .toMap()
    }
}