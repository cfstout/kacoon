package io.github.cfstout.kacoon.examples.kafka

import com.google.common.net.HostAndPort
import io.github.cfstout.kacoon.examples.Main.configuredObjectMapper
import io.github.cfstout.kacoon.kafka.consumer.AsyncConsumer
import io.github.cfstout.kacoon.kafka.consumer.config.ConsumerGroupConfig
import io.github.cfstout.kacoon.kafka.types.ConsumerGroupName
import org.slf4j.LoggerFactory

class KafkaConsumerExample {
    companion object {
        private val logger = LoggerFactory.getLogger(KafkaConsumerExample::class.java)
        private val objectReader = configuredObjectMapper.reader()
        private val consumerGroupConfig = ConsumerGroupConfig(
            ConsumerGroupName("consumer-example"),
            "kafka-test"
        )

        fun generateConsumer(): AsyncConsumer<Data> {
            return AsyncConsumer(
                objectReader,
                listOf(HostAndPort.fromParts("localhost", 9092)),
                Data::class.java,
                consumerGroupConfig) { process(it) }
        }

        suspend fun process(data: Data) {
            logger.info("data recieved: {}", data)
        }
    }
}
