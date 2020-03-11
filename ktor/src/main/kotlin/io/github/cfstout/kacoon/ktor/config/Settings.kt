package io.github.cfstout.kacoon.ktor.config

import org.apache.commons.configuration.CompositeConfiguration

class Settings(config: CompositeConfiguration) {
    val PORT = config.getInt("CONFIG_PORT", 8080)
}
