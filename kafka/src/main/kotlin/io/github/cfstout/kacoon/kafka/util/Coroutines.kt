package io.github.cfstout.kacoon.kafka.util

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking

class Coroutines {
    companion object {
        fun <T> runAsCoroutine(thing: T, name: String): AutoCloseable where T : Runnable, T : AutoCloseable {
            return runAsCoroutine(thing, thing, name)
        }

        fun runAsCoroutine(runnable: Runnable, autoCloseable: AutoCloseable, name: String): AutoCloseable {
            val launch: Job = GlobalScope.launch(context = newSingleThreadContext(name)) {
                runnable.run()
            }
            return AutoCloseable {
                autoCloseable.close()
                runBlocking {
                    launch.join()
                }
            }
        }
    }
}