package io.github.cfstout.kafka.producer

import io.github.cfstout.kafka.types.Message
import io.github.cfstout.kafka.types.Topic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.slf4j.LoggerFactory
import java.util.Properties
import java.util.concurrent.CompletableFuture

abstract class AbstractGebProducer<T: Any>(protected val brokers: List<String>,
                                           protected var configOverrides: Map<String, Any> = mapOf()) {

//    init {
//        if (brokers.filter {
//                    it.host.toLowerCase().contains("development") ||
//                            it.host.toLowerCase().contains("production")
//                }.any()) {
//            configOverrides = MoreMaps.merge(configOverrides,
//                    mapOf(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SSL"))
//        }
//    }

    private val replicationFactor: Short = brokers.size.toShort()

    private val logger = LoggerFactory.getLogger(this.javaClass)

    private val kafkaAdminClient = getAdminClient()

    private val availableTopics: MutableMap<String, Topic> = mutableMapOf()

    private fun getAdminClient(): AdminClient {
        val properties = Properties()
        properties.setProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
                brokers.joinToString(","){ it })
        val securityProtocol = configOverrides[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG]?.toString()
        if(securityProtocol != null)
            properties.setProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol)
        return AdminClient.create(properties)
    }

    abstract fun getKafkaProducer(): KafkaProducer<String, T>

    suspend fun sendAsyncKt(message: Message<T>): RecordMetadata {
        val data = message.getData()
        logger.debug("writing message: $data")

        // Since we want this coroutine to be non-blocking, we create a CompletableFuture, and set the values in
        // the producer's callback, and then rely on the coroutine `await` on the future to ensure it's executed
        val future = CompletableFuture<RecordMetadata>()
        val internalProducer = getKafkaProducer()
        withContext(Dispatchers.IO) {
            internalProducer.send(
                    ProducerRecord(getTopic(message).value,
                            message.getDefinition().getMessageKey(data), data))
            { recordMetadata, exception ->
                if (recordMetadata != null) {
                    logger.debug("Successfully sent record {}", recordMetadata)
                    future.complete(recordMetadata)
                } else {
                    logger.error("Failed to send record", exception)
                    future.completeExceptionally(exception)
                }
            }
        }
        return future.await()
    }

    fun send(data: Message<T>): RecordMetadata {
        return runBlocking {
            sendAsyncKt(data)
        }
    }

    fun sendBatch(messages: Set<Message<T>>): Map<Message<T>, RecordMetadata> {
        return runBlocking {
            messages.associateWith { sendAsyncKt(it) }
        }
    }

    /**
     * Gets message topic by first checking local cache, then making a request to the clusters to get topics and if
     * it doesn't find the topic in either the local cache or clusters then it creates the topic.
     */
    private fun getTopic(message: Message<T>): Topic {
        val topicName = message.getDefinition().getTopic().value
        val internalProducer = getKafkaProducer()
        var topic = availableTopics[topicName]
        if (topic != null) {
            return topic
        }
        val topics  = kafkaAdminClient.listTopics().names().get()
        if (topics.contains(topicName)) {
            topic = Topic(topicName)
            availableTopics[topic.value] = topic
            return topic
        }
        val topicToCreate = message.getDefinition().getTopic()
        kafkaAdminClient.createTopics(listOf(NewTopic(topicToCreate.value,
                1, replicationFactor))).all().get()
        availableTopics[topicToCreate.value] = topicToCreate
        return topicToCreate
    }

}