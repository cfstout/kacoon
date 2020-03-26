package io.github.cfstout.kacoon.repair

import com.wixpress.dst.greyhound.java.GreyhoundConfig
import com.wixpress.dst.greyhound.java.GreyhoundConsumer
import com.wixpress.dst.greyhound.java.GreyhoundConsumersBuilder
import com.wixpress.dst.greyhound.java.RecordHandler
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.common.internals.Topic
import org.apache.kafka.common.serialization.StringDeserializer
import org.checkerframework.checker.units.qual.K
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

class Repair(
    private val getRepairs: (String) -> RepairJob<*>,
    private val producer: Producer<String, ByteArray>,
    private val kafkaBrokers: List<String>,
    private val stateTopic: String = "repair-state-topic") {

    val config: GreyhoundConfig = GreyhoundConfig(kafkaBrokers.toSet())
    val recordHandler = RecordHandler { a: ConsumerRecord<String, String>, b -> null}
    val consumerBuilder = GreyhoundConsumersBuilder(config).withConsumer(
        GreyhoundConsumer(stateTopic, "Repairs", recordHandler, StringDeserializer(), StringDeserializer())
    )
}