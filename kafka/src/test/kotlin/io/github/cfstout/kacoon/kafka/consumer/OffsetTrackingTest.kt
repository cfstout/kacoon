package io.github.cfstout.kacoon.kafka.consumer

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class OffsetTrackingTest {

    companion object {
        val offsetTracking = OffsetTracking()
        val partition = TopicPartition("foo", 0)
        val record0 = record(partition, 0)
        val record1 = record(partition, 1)
        val record2 = record(partition, 2)

        private fun record(topicPartition: TopicPartition, offset: Long): ConsumerRecord<*, *> {
            return ConsumerRecord(topicPartition.topic(), topicPartition.partition(), offset, null, null)
        }
    }

    @Test
    fun testBacklogSizes() {
        offsetTracking.started(record0)
        offsetTracking.started(record1)

        var backlogSizes: Map<TopicPartition, Int> = offsetTracking.backlogSizes()
        var expected = mapOf(partition to 2)
        Assertions.assertEquals(expected, backlogSizes)

        offsetTracking.complete(record1)
        backlogSizes = offsetTracking.backlogSizes()
        expected = mapOf(partition to 1)
        Assertions.assertEquals(expected, backlogSizes)

        offsetTracking.complete(record0)
        offsetTracking.started(record2)
        backlogSizes = offsetTracking.backlogSizes()
        expected = mapOf(partition to 1)
        Assertions.assertEquals(expected, backlogSizes)
    }

    @Test
    fun testOffsetsToCommit() {
        offsetTracking.started(record0)
        offsetTracking.started(record1)

        var pair: Pair<TopicPartition, OffsetAndMetadata> = offsetTracking.offsetsToCommit(partition, OffsetAndMetadata(0))
        // Nothing completed yet
        Assertions.assertEquals(0, pair.second.offset())

        offsetTracking.complete(record1)
        pair = offsetTracking.offsetsToCommit(partition, OffsetAndMetadata(0))
        // the second record was completed, but that means we still need to process the first so
        // basically the same as nothing being completed
        Assertions.assertEquals(0, pair.second.offset())

        offsetTracking.complete(record0)
        pair = offsetTracking.offsetsToCommit(partition, OffsetAndMetadata(0))
        // Now we complete the first so position should be the record after record "1" (e.g. 2)
        Assertions.assertEquals(2, pair.second.offset())
    }
}