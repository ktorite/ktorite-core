package org.ktorite.config

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.server.routing.Route
import org.jetbrains.exposed.v1.core.Table
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class AuthConfig {
    internal var jwtConfig: JwtAuthConfig? = null
    internal var sessionConfig: SessionAuthConfig? = null

    fun jwt(block: JwtAuthConfig.() -> Unit) {
        jwtConfig = JwtAuthConfig().apply(block)
    }

    fun session(block: SessionAuthConfig.() -> Unit) {
        sessionConfig = SessionAuthConfig().apply(block)
    }
}

class JwtAuthConfig {
    var secret: String = ""
        set(value) {
            require(value.isNotBlank()) { "JWT secret must not be blank" }
            field = value
        }
    var issuer: String = "ktorite"
    var realm: String = "ktorite"
    var validate: suspend (ApplicationCall, JWTCredential) -> Any? = { _, credential -> credential.payload }
}

class SessionAuthConfig {
    var userTable: Table? = null
    var loginPath: String = "/login"
    var logoutPath: String = "/logout"
    var sessionName: String = "ktorite_session"
    var secret: String = "change-me"
    var maxAge: Duration = 24.hours
    var onLogin: (suspend (ApplicationCall) -> Unit)? = null
    var onLogout: (suspend (ApplicationCall) -> Unit)? = null
}
