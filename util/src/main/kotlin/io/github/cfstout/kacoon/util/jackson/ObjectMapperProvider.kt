package io.github.cfstout.kacoon.util.jackson

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module


open class ObjectMapperProvider {
    private object HOLDER {
        val instance = configure(ObjectMapper())
    }
    companion object {

        val objectMapper: ObjectMapper by lazy {
            HOLDER.instance
        }

        fun configure(objectMapper: ObjectMapper): ObjectMapper {
            return objectMapper.apply {
                registerModule(JavaTimeModule())
                registerModule(Jdk8Module())
                registerModule(JavaTimeModule())
                registerModule(KotlinModule())
                setSerializationInclusion(JsonInclude.Include.NON_NULL)
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
            }
        }
    }
}