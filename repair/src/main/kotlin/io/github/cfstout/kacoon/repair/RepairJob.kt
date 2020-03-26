package io.github.cfstout.kacoon.repair

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.common.reflect.TypeToken

interface RepairJob<T>: RepairConsumer<T>, RepairProducer<T>

interface RepairProducer<T> {
    fun produceRepairLogs(startState: String?, logProducer: LogProducer<T>)

    interface LogProducer<T> {
        fun produce(key: String? = null, message: T? = null, currentState: State? = null)

        fun updateState(state: State) {
            produce(currentState = state)
        }
    }

    enum class Running {
        YES, COMPLETED, PAUSED
    }

    data class State(@JsonProperty("running") val running: Running,
                     @JsonProperty("message") val message: String?)
}

interface RepairConsumer<T> {
    suspend fun repair(message: T)

    suspend fun undoRepair(message: T)

    fun type(): TypeToken<T>
}