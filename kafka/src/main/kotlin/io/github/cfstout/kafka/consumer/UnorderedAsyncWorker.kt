package io.github.cfstout.kafka.consumer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import java.util.concurrent.atomic.AtomicReference

/**
 * Async consumer record processor that is fully async. As records are received they are processed,
 * with no ordering guarantees.
 */
class UnorderedAsyncWorker<K, V>(private val offsetTracking: OffsetTracking,
                                 private val processor: suspend (ConsumerRecord<K, V>) -> Unit) :
    ConsumerLoopActor<K, V> {
    private val lastFailure = AtomicReference<Throwable>()

    override val actInLoop: (Consumer<K, V>, ConsumerRecords<K, V>) -> Unit = { _, consumerRecords ->
        run {
            val throwable = lastFailure.get()
            if (throwable != null) {
                throw throwable
            }

            GlobalScope.launch(Dispatchers.IO) {
                consumerRecords.forEach {
                    offsetTracking.started(it)
                    try {
                        processor.invoke(it)
                        offsetTracking.complete(it)
                    } catch (e: Exception) {
                        lastFailure.set(e)
                    }
                }
            }
        }
    }
}