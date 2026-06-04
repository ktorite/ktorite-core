package org.ktorite.config

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTCredential

class AuthConfig {
    internal var jwtConfig: JwtAuthConfig? = null

    fun jwt(block: JwtAuthConfig.() -> Unit) {
        jwtConfig = JwtAuthConfig().apply(block)
    }
}

class JwtAuthConfig {
    var secret: String = "change-me"
    var issuer: String = "ktorite"
    var realm: String = "ktorite"
    var validate: suspend (ApplicationCall, JWTCredential) -> Any? = { _, _ -> null }
}
