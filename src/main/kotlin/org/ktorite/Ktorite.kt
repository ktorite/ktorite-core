package org.ktorite

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.csrf.CSRF
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.hsts.HSTS
import io.ktor.server.plugins.statuspages.*
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.ktorite.Routing.installRoutes
import org.ktorite.admin.installAdmin
import org.ktorite.config.KtoriteConfig
import org.ktorite.db.installDatabase
import org.ktorite.plugins.configureSerialization


object Ktorite {
    fun start(configure: KtoriteConfig.() -> Unit) {
        val config = KtoriteConfig().apply(configure)
        embeddedServer(Netty, port = config.port) {
            module(config)
        }.start(wait = true)
    }
}


fun Application.module(config: KtoriteConfig) {
    configureSerialization()
    install(CallLogging)
    install(DefaultHeaders)
    install(StatusPages)

    config.securityConfig?.let { sec ->
        if (sec.corsConfig.hosts.isNotEmpty() || sec.corsConfig.allowSameOrigin) {
            install(CORS) {
                sec.corsConfig.hosts.forEach { entry ->
                    allowHost(entry.host, entry.schemes)
                }
                sec.corsConfig.methods.forEach { allowMethod(it) }
                sec.corsConfig.headers.forEach { allowHeader(it) }
                allowCredentials = sec.corsConfig.allowCredentials
                allowSameOrigin = sec.corsConfig.allowSameOrigin
                maxAgeInSeconds = sec.corsConfig.maxAgeInSeconds
            }
        }
        install(HSTS) {
            maxAgeInSeconds = sec.hstsConfig.maxAgeInSeconds
            includeSubDomains = sec.hstsConfig.includeSubDomains
            preload = sec.hstsConfig.preload
        }
        install(CSRF) {
            sec.csrfConfig.allowedOrigins.forEach { allowOrigin(it) }
            sec.csrfConfig.headerChecks.forEach { checkHeader(it.name) }
            if (sec.csrfConfig.originMatchesHost) originMatchesHost()
        }
    }

    config.authConfig?.let { authCfg ->
        authCfg.jwtConfig?.let { jwtCfg ->
            install(Authentication) {
                jwt("jwt") {
                    realm = jwtCfg.realm
                    verifier(
                        JWT.require(Algorithm.HMAC256(jwtCfg.secret))
                            .withIssuer(jwtCfg.issuer)
                            .build()
                    )
                    validate { credential ->
                        jwtCfg.validate(this, credential)
                    }
                }
            }
        }
    }

    if (config.enableAdmin) {
        installAdmin()
    }
    if (config.dbConfig != null) {
        val db = installDatabase(config.dbConfig!!)
        if (config.models.isNotEmpty()) {
            transaction(db) {
                SchemaUtils.create(*config.models.toTypedArray())
            }
        }
    }
    installRoutes(false){
        config.routes.forEach { routeDef ->
            routeDef()
        }
    }
}
