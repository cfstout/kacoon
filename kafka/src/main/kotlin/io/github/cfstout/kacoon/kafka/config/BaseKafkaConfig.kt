package io.github.cfstout.kacoon.kafka.config

import com.google.common.net.HostAndPort
import io.github.cfstout.kacoon.kafka.types.ConsumerGroupName
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.StickyAssignor
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer

import java.net.InetAddress

private val hostname = InetAddress.getLocalHost().hostName

internal fun stdProducerConfig(brokers: List<HostAndPort>, overrides: Map<String, Any>): Map<String, Any> {
    return mutableMapOf(
            ProducerConfig.COMPRESSION_TYPE_CONFIG to "gzip", // https://blog.cloudflare.com/squeezing-the-firehose/
            ProducerConfig.RETRIES_CONFIG to "5", // by default retry a few times
            ProducerConfig.LINGER_MS_CONFIG to "5", // 5ms of latency before sending, but w/ large message sets = better throughput
            ProducerConfig.ACKS_CONFIG to "all", // require all partitions to ack to ensure write is durable -- override w/ caution
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to brokerString(brokers)
    ).plus(overrides).toMap()
}

@JvmOverloads
internal fun stdConsumerConfig(brokers: List<HostAndPort>,
                               consumerGroupName: ConsumerGroupName,
                               overrides: Map<String, Any> = mapOf()): Map<String, Any> {
    return mutableMapOf(
            ConsumerConfig.GROUP_ID_CONFIG to consumerGroupName.value,
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to brokerString(brokers),
            ConsumerConfig.CLIENT_ID_CONFIG to hostname,
            // Disable auto commit so we only commit offsets after processing
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to "false",
            // Internal consumers all deserialize to bytes so deserialization bugs are not swallowed
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to ByteArrayDeserializer::class.java.name,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ByteArrayDeserializer::class.java.name,
            // Necessary for proper load balancing over multi topic consumers even after rebalance: https://cwiki.apache.org/confluence/display/KAFKA/KIP-54+-+Sticky+Partition+Assignment+Strategy
            ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG to StickyAssignor::class.java.name
    ).plus(overrides)
}

internal fun standardMetricTags(consumerGroupName: ConsumerGroupName): Map<String, CharSequence> {
    return mapOf("consumer_group" to consumerGroupName.value)
}

private fun brokerString(brokers: List<HostAndPort>): String {
    return brokers.joinToString(separator = ",") { "${it.host}${if (it.hasPort()) ":${it.port}" else ""}" }
}