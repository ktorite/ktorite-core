package org.ktorite.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.ktorite.config.SessionAuthConfig

@Serializable
data class UserSession(val userId: Int, val username: String)

fun Application.installSessionAuth(config: SessionAuthConfig, db: Database) {
    val sessionName = config.sessionName

    install(Sessions) {
        cookie<UserSession>(sessionName) {
            cookie.path = "/"
            cookie.maxAgeInSeconds = config.maxAge.inWholeSeconds
            cookie.sameSite = SameSite.Lax
            cookie.httpOnly = true
            cookie.secure = false
        }
    }

    routing {
        post(config.loginPath) {
            val params = call.receiveParameters()
            val username = params["username"] ?: run {
                call.respondText("Username required", status = HttpStatusCode.BadRequest)
                return@post
            }
            val password = params["password"] ?: run {
                call.respondText("Password required", status = HttpStatusCode.BadRequest)
                return@post
            }

            val rows = transaction(db) {
                UserTable.selectAll().where { UserTable.username eq username }.toList()
            }

            if (rows.isEmpty()) {
                call.respondText("Invalid credentials", status = HttpStatusCode.Unauthorized)
                return@post
            }

            val row = rows[0]
            val storedHash = row[UserTable.passwordHash]

            if (!BCrypt.verifyer().verify(password.toCharArray(), storedHash).verified) {
                call.respondText("Invalid credentials", status = HttpStatusCode.Unauthorized)
                return@post
            }

            call.sessions.set(UserSession(row[UserTable.id], row[UserTable.username]))
            config.onLogin?.invoke(call)
            if (!call.response.isCommitted) {
                call.respondText("Logged in", status = HttpStatusCode.OK)
            }
        }

        post(config.logoutPath) {
            call.sessions.clear<UserSession>()
            config.onLogout?.invoke(call)
            if (!call.response.isCommitted) {
                call.respondText("Logged out", status = HttpStatusCode.OK)
            }
        }
    }
}
