package io.github.cfstout.kacoon.examples

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.github.cfstout.kacoon.examples.kafka.KafkaConsumerExample
import io.github.cfstout.kacoon.examples.kafka.KafkaProducerExample
import io.github.cfstout.kacoon.kafka.util.Coroutines
import io.ktor.application.call
import io.ktor.application.feature
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.JacksonConverter
import io.ktor.response.respond
import io.ktor.routing.HttpMethodRouteSelector
import io.ktor.routing.Route
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

object Main {
    private val logger = LoggerFactory.getLogger(Main::class.java)
    private val producerExample = KafkaProducerExample()
    val configuredObjectMapper by lazy {
        ObjectMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            registerModule(KotlinModule())
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        logger.info("Starting up server")
        val server = embeddedServer(Netty, port = 8080) {
            install(CallLogging) {
                level = Level.INFO
            }
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(configuredObjectMapper))
            }
            install(StatusPages) {
                exception<Throwable> {
                    logger.error("Internal server error", it)
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }
            routing {
                get("/produce") {
                    producerExample.produceDataToKafka()
                    call.respond(HttpStatusCode.OK)
                }
            }
            val root = feature(Routing)
            val allRoutes = allRoutes(root)
            val allRoutesWithMethod = allRoutes.filter { it.selector is HttpMethodRouteSelector }
            allRoutesWithMethod.forEach {
                logger.info("route: $it")
            }

        }
        Coroutines.runAsCoroutine(KafkaConsumerExample.generateConsumer(), "kafka-consumer-example")
        server.start(wait = true)
    }

    private fun allRoutes(root: Route): List<Route> {
        return listOf(root) + root.children.flatMap { allRoutes(it) }
    }
}