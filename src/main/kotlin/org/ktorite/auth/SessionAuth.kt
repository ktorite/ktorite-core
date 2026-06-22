package org.ktorite.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.ktorite.config.SessionAuthConfig

@Serializable
data class UserSession(val userId: Int, val username: String)

fun Application.installSessionAuth(config: SessionAuthConfig, db: Database) {
    val sessionName = config.sessionName
    val provider = config.userTableProvider
    val userTable = provider.table
    val idCol = provider.idColumn
    val usernameCol = provider.usernameColumn
    val passwordCol = provider.passwordColumn

    install(Sessions) {
        cookie<UserSession>(sessionName) {
            cookie.path = "/"
            cookie.maxAgeInSeconds = config.maxAge.inWholeSeconds
            cookie.sameSite = SameSite.Lax
            cookie.httpOnly = true
            cookie.secure = config.secure
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
                @Suppress("UNCHECKED_CAST")
                userTable.selectAll().where { (usernameCol as Column<String>) eq username }.toList()
            }

            if (rows.isEmpty()) {
                call.respondText("Invalid credentials", status = HttpStatusCode.Unauthorized)
                return@post
            }

            val row = rows[0]
            val storedHash = row[passwordCol] as? String ?: run {
                call.respondText("Invalid credentials", status = HttpStatusCode.Unauthorized)
                return@post
            }

            if (!BCrypt.verifyer().verify(password.toCharArray(), storedHash).verified) {
                call.respondText("Invalid credentials", status = HttpStatusCode.Unauthorized)
                return@post
            }

            if (config.requireSuperuser && userTable is UserTable) {
                val isSuper = row[UserTable.isSuperuser]
                if (isSuper != true) {
                    call.respondText("Not authorized", status = HttpStatusCode.Forbidden)
                    return@post
                }
            }

            call.sessions.set(UserSession(row[idCol] as Int, row[usernameCol] as String))
            config.onLogin?.invoke(call)
            if (!call.response.isCommitted) {
                call.respondRedirect("/admin")
            }
        }

        post(config.logoutPath) {
            call.sessions.clear<UserSession>()
            config.onLogout?.invoke(call)
            if (!call.response.isCommitted) {
                call.respondRedirect("/admin")
            }
        }
    }
}
