package io.github.cfstout.kafka.producer

import com.fasterxml.jackson.databind.ObjectWriter
import io.github.cfstout.kafka.config.stdProducerConfig
import io.github.cfstout.kafka.serializers.JacksonSerializer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.StringSerializer

open class GebJacksonProducer<T: Any>
@JvmOverloads
constructor(brokers: List<String>,
            overrides: Map<String, Any> = mapOf(),
            objectWriter: ObjectWriter): AbstractGebProducer<T>(brokers, overrides) {

    private val internalProducer = KafkaProducer(
            stdProducerConfig(this.brokers, this.configOverrides),
            StringSerializer(),
            JacksonSerializer<T>(objectWriter)
    )

    override fun getKafkaProducer(): KafkaProducer<String, T> = this.internalProducer

}