package com.classpass.bespoke.proxy.modules

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.ObjectWriter
import com.google.inject.AbstractModule
import com.google.inject.Provides

class JacksonModule(private val mapper: ObjectMapper) : AbstractModule() {
    override fun configure() {
        bind(ObjectReader::class.java).toInstance(mapper.reader())
        bind(ObjectWriter::class.java).toInstance(mapper.writer())
    }

    @Provides
    fun objectMapperCopy(): ObjectMapper = mapper.copy()
}
