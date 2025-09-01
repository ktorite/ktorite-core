package org.ktorite.admin

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.installAdmin() {
    routing {
        get("/admin") {
            call.respondText("Welcome to Ktorite Admin Panel")
        }
    }
}
