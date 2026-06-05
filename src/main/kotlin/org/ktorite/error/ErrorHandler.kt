package org.ktorite.error

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.respondText
import org.ktorite.config.ErrorConfig

fun Application.installErrorHandler(config: ErrorConfig) {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            val msg = if (config.developmentMode) {
                (cause.cause?.message ?: cause.message) ?: "Internal server error"
            } else {
                "Internal server error"
            }
            call.respondJsonError(HttpStatusCode.InternalServerError, msg)
        }
        status(HttpStatusCode.NotFound) { call, _ ->
            call.respondJsonError(HttpStatusCode.NotFound, "Not found")
        }
    }
}

fun formatDbError(e: Exception): String {
    val msg = e.message ?: return "An error occurred"
    return when {
        msg.contains("Unique index or primary key violation") ->
            "Duplicate value: a record with this value already exists."
        msg.contains("NULL not allowed") -> "This field cannot be empty."
        else -> "An error occurred while saving the record."
    }
}

suspend fun ApplicationCall.respondJsonError(status: HttpStatusCode, message: String) {
    respondText("""{"error":"${message.replace("\"", "\\\"")}"}""", ContentType.Application.Json, status)
}
