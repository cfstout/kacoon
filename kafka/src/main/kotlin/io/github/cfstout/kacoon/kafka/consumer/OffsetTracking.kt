package io.github.cfstout.kacoon.kafka.consumer

import com.google.common.collect.Maps
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

/**
 * Tracks partitions and the completed and pending offsets for those partitions
 */
class OffsetTracking {
    private val offsets: MutableMap<TopicPartition, Offsets> = Maps.newConcurrentMap()

    fun partitionAdded(partition: TopicPartition, position: Long) {
        offsets[partition] = withInitialOffset(position)
    }

    fun partitionsRemoved(partitions: Collection<TopicPartition>) {
        partitions.forEach {
            offsets.remove(it)
        }
    }

    fun started(record: ConsumerRecord<*, *>) {
        offsets.computeIfAbsent(topicPartition(record)) { Offsets() }
                .add(record.offset())
    }

    fun complete(record: ConsumerRecord<*, *>) {
        offsets.computeIfAbsent(topicPartition(record)) { Offsets() }
                .complete(record.offset())
    }

    fun offsetsToCommit(partition: TopicPartition, committed: OffsetAndMetadata): Pair<TopicPartition, OffsetAndMetadata> {
        return Pair(partition, OffsetAndMetadata(max(committed.offset(), offsets.getOrDefault(partition, Offsets()).nextOffset())))
    }

    fun backlogSizes(): Map<TopicPartition, Int> {
        return offsets.mapValues { it.value.pendingCount }
    }

    private class Offsets(private val pending: PriorityBlockingQueue<Long> = PriorityBlockingQueue(),
                          private val maxCompleted: AtomicLong = AtomicLong()) {
        /**
         * The offset to commit is the *next* offset you intend to work with -- the message to resume from
         * if there *is* pending work, then that is the smallest pending offset
         * if there isn't, then it's the max offset we have seen + 1
         */
        fun nextOffset(): Long {
            return pending.peek() ?: maxCompleted.get() + 1
        }

        fun complete(offset: Long) {
            pending.remove(offset)
            maxCompleted.updateAndGet { max(it, offset) }
        }

        fun add(offset: Long) {
            pending.add(offset)
        }

        val pendingCount: Int get() = pending.size
    }

    companion object {
        private fun withInitialOffset(offset: Long): Offsets {
            /**
             * Offset is the next to consume so we've completed the one before [offset]
             */
            return Offsets(maxCompleted = AtomicLong(offset - 1))
        }

        private fun topicPartition(record: ConsumerRecord<*, *>): TopicPartition {
            return TopicPartition(record.topic(), record.partition())
        }
    }
}