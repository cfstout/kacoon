package io.github.cfstout.kacoon.ktor

import com.classpass.bespoke.proxy.modules.GuiceConfigModule
import com.classpass.bespoke.proxy.modules.JacksonModule
import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Stage
import io.github.cfstout.kacoon.ktor.config.AppEndpointsModule
import io.github.cfstout.kacoon.ktor.config.AppLogicModule
import io.github.cfstout.kacoon.ktor.config.Settings
import io.github.cfstout.kacoon.util.jackson.ObjectMapperProvider
import io.ktor.application.Application
import io.ktor.application.feature
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.jackson.JacksonConverter
import io.ktor.routing.HttpMethodRouteSelector
import io.ktor.routing.Route
import io.ktor.routing.Routing
import io.ktor.server.netty.Netty
import org.apache.commons.configuration.CompositeConfiguration
import org.apache.commons.configuration.EnvironmentConfiguration
import org.apache.commons.configuration.PropertiesConfiguration
import java.nio.file.Paths
import io.ktor.server.engine.embeddedServer
import io.ktor.util.extension
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.system.exitProcess

object KacoonKtorServer {
    val logger = LoggerFactory.getLogger(KacoonKtorServer::class.java)
}

fun main(args: Array<String>) {
    if (args.size != 2) {
        print(
            """
            Usage: kacoon COMMAND [config_folder]
            | Commands:
            | run       Starts the application
        """.trimIndent()
        )
        exitProcess(-1)
    }

    val objectMapper = ObjectMapperProvider.objectMapper

    val config = CompositeConfiguration()
        .apply {
            addConfiguration(EnvironmentConfiguration())
            applyConfigFiles(args.getOrElse(1) { throw RuntimeException() }, this)
        }.let { Settings(it) }

    when (args[0]) {
        "run" -> {
            val injector = Guice.createInjector(
                Stage.PRODUCTION,
                GuiceConfigModule(),
                JacksonModule(objectMapper),
                AppLogicModule(config)
            )

            embeddedServer(
                Netty,
                port = config.PORT
            ) {
                install(CallLogging)
                install(ContentNegotiation) {
                    register(ContentType.Application.Json, JacksonConverter(objectMapper))
                }
                install(StatusPages) {
                    // todo custom error response?
                }
                injector.createChildInjector(
                    object : AbstractModule() {
                        override fun configure() {
                            bind(Application::class.java).toInstance(this@embeddedServer)
                        }
                    },
                    AppEndpointsModule()
                )
                val root = feature(Routing)
                val allRoutes = allRoutes(root)
                val allRoutesWithMethod = allRoutes.filter { it.selector is HttpMethodRouteSelector }
                allRoutesWithMethod.forEach {
                    KacoonKtorServer.logger.info("route: $it")
                }
            }.start(wait = false)
        }
        else -> throw IllegalArgumentException("Unknown command, valid commands: run | migrate")
    }
}

private fun allRoutes(root: Route): List<Route> {
    return listOf(root) + root.children.flatMap { allRoutes(it) }
}

/**
 * Apply properties files in the provided directory to the compsite configuration in sorted order.
 *
 * Later files overwrite previous files. In other words, data in 02-bar.properties will take precedence over
 * 01-foo.properties.
 */
fun applyConfigFiles(configDir: String, composite: CompositeConfiguration) {
    Files.newDirectoryStream(Paths.get(configDir))
        .filter { p -> p.extension == "properties" }
        .toList()
        .sorted()
        // CompositeConfiguration uses "first match wins" but we want the opposite
        .reversed()
        .forEach { p ->
            val props = PropertiesConfiguration().apply {
                encoding = StandardCharsets.UTF_8.name()
                isDelimiterParsingDisabled = true
            }
            Files.newInputStream(p).use { i ->
                props.load(i)
            }
            composite.addConfiguration(props)
        }
}