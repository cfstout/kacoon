package io.github.cfstout.kacoon.kafka.producer

import io.github.cfstout.kacoon.kafka.types.Message
import com.fasterxml.jackson.databind.ObjectWriter
import com.google.common.net.HostAndPort
import io.github.cfstout.kacoon.kafka.config.stdProducerConfig
import io.github.cfstout.kacoon.kafka.types.Topic
import io.github.cfstout.kacoon.kafka.util.JacksonSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

/**
 * Binds a topic to a given producer record type and allows async sending of messages
 */

class AsyncProducer<V>
@JvmOverloads
constructor(objectWriter: ObjectWriter,
            brokers: List<HostAndPort>,
            private val topic: Topic,
            configOverrides: Map<String, Any> = mapOf()) {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(AsyncProducer::class.java)
    }

    private val internalProducer: Producer<String, V> = KafkaProducer(
            stdProducerConfig(brokers, configOverrides),
            StringSerializer(),
            JacksonSerializer<V>(objectWriter)
    )

    suspend fun sendAsyncKt(message: Message<V>): RecordMetadata {
        logger.debug("writing message: {}", message.data.toString())
        // Since we want this coroutine to be non-blocking, we create a CompletableFuture, and set the values in
        // the producer's callback, and then rely on the coroutine `await` on the future to ensure it's executed
        val future = CompletableFuture<RecordMetadata>()
        withContext(Dispatchers.IO) {
            sendRecordAndHandleResponse(message, future)
        }
        return future.await()
    }

    fun sendAsync(message: Message<V>): CompletableFuture<RecordMetadata> {
        logger.debug("writing message: {}", message.data.toString())
        return sendRecordAndHandleResponse(message, CompletableFuture())
    }

    private fun sendRecordAndHandleResponse(
        message: Message<V>,
        future: CompletableFuture<RecordMetadata>
    ): CompletableFuture<RecordMetadata> {
        internalProducer.send(
            ProducerRecord(topic.value, message.getPartitionKey().value, message.data)
        ) { m, e ->
            if (m != null) {
                logger.info("Successfully sent record {}", m)
                future.complete(m)
            } else {
                logger.error("Failed to send record", e)
                future.completeExceptionally(e)
            }
        }
        return future
    }


}