package io.github.cfstout.kacoon.ktor.config

import com.google.inject.AbstractModule
import io.github.cfstout.kacoon.ktor.endpoints.HealthEndpoints

class AppEndpointsModule() : AbstractModule() {
    override fun configure() {
        listOf(HealthEndpoints::class.java)
                .forEach { bind(it).asEagerSingleton() }
    }
}
