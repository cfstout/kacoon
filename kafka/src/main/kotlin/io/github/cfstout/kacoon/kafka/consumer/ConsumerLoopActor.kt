package io.github.cfstout.kacoon.kafka.consumer

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecords

/**
 * Implements an action that needs to be taken in the Kafka consumer poll loop.
 *
 * Kafka consumers may only be manipulated by a single thread, so many different
 * concerns need to coexist in the consumer polling loop. e.g.,
 * offset management [GroupManagement],
 * partition pausing/resuming [ConsumerFlowController]
 * todo -- metrics collection,
 * processing the records [UnorderedAsyncWorker], etc.
 *
 * The combination of multiple consumer loop actors defines a Kafka consumer and is all
 * put together in the [AsyncConsumer] class.
 */
interface ConsumerLoopActor<K, V>: AutoCloseable {
    /**
     * This method must not throw exceptions, unless the exception is fatal (it will kill the consumer.)
     * Blocking calls should be avoided when using Kafka consumer groups, as they may cause the consumer
     * to time out from the group.
     */
    val actInLoop: (Consumer<K, V>, ConsumerRecords<K, V>) -> Unit
    override fun close() {}
}