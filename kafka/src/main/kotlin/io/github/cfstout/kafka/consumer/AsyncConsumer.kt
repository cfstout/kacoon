@file:Suppress("UnstableApiUsage")

package io.github.cfstout.kafka.consumer

import com.fasterxml.jackson.databind.ObjectReader
import com.google.common.reflect.TypeToken
import io.github.cfstout.kafka.config.stdConsumerConfig
import io.github.cfstout.kafka.consumer.config.ConsumerGroupConfig
import io.github.cfstout.kafka.serializers.JacksonDeserializer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.WakeupException
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class AsyncConsumer<T> private constructor(private val deserializer: Deserializer<T>,
                                           brokers: List<String>,
                                           private val config: ConsumerGroupConfig,
                                           private val consume: suspend (T) -> Unit) : Runnable, AutoCloseable {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(AsyncConsumer::class.java)
        private val keyDeserializer = StringDeserializer()
        val GROUP_TAG = "consumer_group"

        fun <T> forJson(objectReader: ObjectReader,
                        typeToken: TypeToken<T>,
                        brokers: List<String>,
                        config: ConsumerGroupConfig,
                        consume: suspend (T) -> Unit): AsyncConsumer<T> {
            return AsyncConsumer(
                    JacksonDeserializer(objectReader, typeToken),
                    brokers,
                    config,
                    consume)
        }
    }

    private val rawConsumer: KafkaConsumer<ByteArray, ByteArray> =
            KafkaConsumer(stdConsumerConfig(brokers, config.groupName, config.overrides))
    private val shutdown = AtomicBoolean()
    private val dead = AtomicBoolean()
    private val shutdownLatch = CountDownLatch(1)
    private val offsetTracking = OffsetTracking()

    // todo metrics
//    val tags = standardMetricTags(config.groupName)
//    private val loopTimer = metricFactory.timer("kafka_consumer.loop", tags)
//    private val batchSize = metricFactory.histogram("kafka_consumer.batch_size", tags)
//    private val consumed = metricFactory.meter("kafka_consumer.consumed", tags)
//    private val deadGauge = metricFactory.registerGauge("kafka_consumer.dead", Gauge { if (isDead()) 1 else 0 }, tags)

    // Actors
    private val groupManagement = GroupManagement<ByteArray, ByteArray>(
            config.pattern, config.groupName, config.commitOffsets, rawConsumer, offsetTracking
    )
    private val flowController = ConsumerFlowController<ByteArray, ByteArray>(
        config.groupName, config.consumerFlowConfig) { offsetTracking.backlogSizes() }
    private val worker = UnorderedAsyncWorker<ByteArray, ByteArray>(offsetTracking) { process(it) }

    private val actors: List<ConsumerLoopActor<ByteArray, ByteArray>> = listOf(
            groupManagement, flowController, worker
    )

    override fun run() {
        Loop().use {
            try {
                while (!shutdown.get()) {
                    it.iterate()
                }
            } catch (e: WakeupException) {
                // expected when shutting down
            } catch (e: Throwable) {
                logger.error("Consumer died!!", e)
                // any failure => we're shutdown because we cannot skip anything
                dead.set(true)
                shutdown.set(true)
            }
        }
    }

    /**
     * Signals to the thread that it should clean up and shut down.
     * This method will block up to 2 seconds waiting for the thread to terminate.
     * No actual cleanup is done on this thread so it's only purpose is to be called
     * by the DI container when we've been directed to shutdown.
     */
    override fun close() {
        shutdown.set(true)
        actors.forEach {
            it.close()
        }
        shutdownLatch.countDown()
        rawConsumer.wakeup()
        // wait for the thread to terminate
        var clean = false
        try {
            clean = shutdownLatch.await(2, TimeUnit.SECONDS)
        } catch (ignored: InterruptedException) {
        }

        if (!clean) {
            logger.error("Consumer {} not shutdown after 2 seconds.", config)
        }
    }

    private suspend fun process(record: ConsumerRecord<ByteArray, ByteArray>) {
        // todo wrap in a try and write failure to a consumer group specific failure topic
        consume.invoke(deserialize(record).value())
    }

    private fun deserialize(record: ConsumerRecord<ByteArray, ByteArray>): ConsumerRecord<String, T> {
        return ConsumerRecord(
                record.topic(),
                record.partition(),
                record.offset(),
                record.timestamp(),
                record.timestampType(),
                ConsumerRecord.NULL_CHECKSUM.toLong(),
                ConsumerRecord.NULL_SIZE,
                ConsumerRecord.NULL_SIZE,
                keyDeserializer.deserialize(record.topic(), record.key()),
                deserializer.deserialize(record.topic(), record.value())
        )
    }

    // Wrapper for all our polling loop logic to allow things to be managed consistently
    // and shutdown in a sane manner
    inner class Loop : AutoCloseable {

        /**
         * Calling this will do a single consumer poll and scheduled the resulting work,
         * commit offsets, pause/resume etc. then return the number of raw records that
         * were read from the consumer.
         */
        fun iterate(): Int {
//            val context = loopTimer.time()
            val rawRecords = rawConsumer.poll(config.pollTimeout)
            actors.forEach {
                it.actInLoop(rawConsumer, rawRecords)
            }
//            batchSize.update(rawRecords.count())
//            consumed.mark(rawRecords.count().toLong())
//            context.stop()
            return rawRecords.count()
        }

        override fun close() {
            for (actor in actors) {
                actor.close()
            }
            rawConsumer.close()
            shutdownLatch.countDown()
        }
    }

    private fun isDead(): Boolean {
        return dead.get()
    }
}
