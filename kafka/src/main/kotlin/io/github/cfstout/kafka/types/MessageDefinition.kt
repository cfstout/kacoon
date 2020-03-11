package io.github.cfstout.kafka.types

import com.google.common.reflect.TypeToken


/**
 * Definition for messages as they will be sent and read from kafka
 *
 * @property dataClass binds the message to a particular type e.g. dataClass = object: TypeToken<List<String>>() {}
 * @property messageKeyFunction allows you to define a specific key based on the contents of your data
 * @property partitionKeyFunction optionally allows you to provide a partition key that is separate from the message key.
 * If not present, the [messageKeyFunction] will be used as the partition key
 * @property topicSuffixSupplier provide an optional suffix to the default topic name. NOTE: If you are defining a message
 * for a complex type (such as a List<String> it is **HIGHLY** recommended that you use this parameter. Otherwise your
 * topic will be in the form "<env>-<purpose>-List"
 * @property numPartitions the number of partitions the topic is originally created with. Override this if you want more
 * partitions when the topic is first created than the default of 1.
 */
open class MessageDefinition<T: Any>
@JvmOverloads
constructor(private val dataClass: TypeToken<T>,
            private val messageKeyFunction: (T) -> String,
            private val partitionKeyFunction: ((T) -> String)? = null,
            private val topicSuffixSupplier: () -> String? = { null },
            private val numPartitions: Int = 1) {
    companion object {
        private val environment: String? = System.getenv("CP_CONFIG_ENV")
        private val purpose: String? = System.getenv("CP_CONFIG_PURPOSE")
        fun topicPrefix(): String {
            return if (environment == null || purpose == null) {
                "local"
            } else {
                "$environment-$purpose"
            }
        }

    }

    fun getMessageKey(data: T): String {
        return messageKeyFunction.invoke(data)
    }

    fun getPartitionKey(data: T): String {
        if (partitionKeyFunction != null)
            return partitionKeyFunction.invoke(data)
        return messageKeyFunction.invoke(data)
    }

    open fun getTopic(): Topic {
        val topicSuffix = topicSuffixSupplier.invoke()?.let { "-$it" } ?: ""
        return Topic("${topicPrefix()}-${dataClass.rawType.simpleName}$topicSuffix".toLowerCase())
    }
}


abstract class Message<T: Any>() {

    abstract fun getDefinition(): MessageDefinition<T>

    abstract fun getData(): T
}