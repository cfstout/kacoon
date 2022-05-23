package io.github.cfstout.kacoon.examples.kafka

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.common.net.HostAndPort
import io.github.cfstout.kacoon.examples.Main
import io.github.cfstout.kacoon.examples.Main.configuredObjectMapper
import io.github.cfstout.kacoon.kafka.producer.AsyncProducer
import io.github.cfstout.kacoon.kafka.types.Message
import io.github.cfstout.kacoon.kafka.types.PartitionKey
import io.github.cfstout.kacoon.kafka.types.Topic
import kotlinx.coroutines.runBlocking
import java.util.UUID

/**
 * To use:
 * $ docker-compose up kafka
 *
 * Then run this main method, messages will be sent for each argument
 * passed into the main method
 */
class KafkaProducerExample {
    private val kafkaProducer: AsyncProducer<Data> by lazy {
        AsyncProducer<Data>(
            configuredObjectMapper.writer(),
            // From docker-compose file
            listOf(HostAndPort.fromParts("localhost", 9092)),
            Topic("kafka-test")
        )
    }

    fun produceDataToKafka() {
        runBlocking {
            kafkaProducer.sendAsyncKt(
                Message(
                    Data(UUID.randomUUID().toString()),
                    Data::partitionKey
                )
            )
        }
    }
}

data class Data(@JsonProperty("value") val value: String) {
    @JsonIgnore
    val partitionKey = PartitionKey(value)
}
