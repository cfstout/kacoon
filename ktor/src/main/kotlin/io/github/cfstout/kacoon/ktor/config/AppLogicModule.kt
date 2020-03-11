package io.github.cfstout.kacoon.ktor.config

import com.google.inject.AbstractModule

class AppLogicModule(private val config: Settings) : AbstractModule() {
    override fun configure() {
        bind(Settings::class.java).toInstance(config)
    }
}
