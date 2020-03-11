package io.github.cfstout.kacoon.ktor.endpoints

import com.google.inject.Inject
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing

class HealthEndpoints @Inject constructor(app: Application) {
    init {
        app.routing {
            get("/healthcheck") {
                call.respond(HttpStatusCode.OK, "healthy")
            }
        }
    }
}
