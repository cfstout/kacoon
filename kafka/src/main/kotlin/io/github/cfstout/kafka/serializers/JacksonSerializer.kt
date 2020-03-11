package io.github.cfstout.kafka.serializers

import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.ObjectWriter
import com.google.common.reflect.TypeToken
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serializer

class JacksonSerializer<T: Any>(private val writer: ObjectWriter): Serializer<T> {

    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}

    override fun serialize(topic: String?, data: T): ByteArray = writer.writeValueAsBytes(data)

    override fun close() { }
}

class JacksonDeserializer<T>(objectReader: ObjectReader,
                             private val type: TypeToken<T>): Deserializer<T> {
    private val reader = objectReader.forType(type.rawType)

    override fun deserialize(topic: String, data: ByteArray): T {
        try {
            return reader.readValue(data)
        } catch (e: Exception) {
            // todo better exception here
            throw IllegalStateException("Unable to read data into type ${type.rawType.simpleName} for topic $topic: Raw: ${String(data)}")
        }
    }

    override fun configure(configs: Map<String, *>?, isKey: Boolean) {
        // nothing to do
    }

    override fun close() {
        // nothing to do
    }
}
