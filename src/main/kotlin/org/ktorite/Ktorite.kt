package org.ktorite

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.thymeleaf.Thymeleaf
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.thymeleaf.templatemode.TemplateMode
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.csrf.CSRF
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.hsts.HSTS
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.ThymeleafContent
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.ktorite.Routing.installRoutes
import org.ktorite.admin.installAdmin
import org.ktorite.auth.installSessionAuth
import org.ktorite.config.KtoriteConfig
import org.ktorite.db.installDatabase
import org.ktorite.error.installErrorHandler
import org.ktorite.migration.runMigrations
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
    install(Thymeleaf) {
        addTemplateResolver(ClassLoaderTemplateResolver().apply {
            prefix = "templates/"
            suffix = ".html"
            templateMode = TemplateMode.HTML
            characterEncoding = "UTF-8"
        })
    }
    installErrorHandler(config.errorConfig)

    config.securityConfig?.let { sec ->
        if (sec.csrfConfig.disabled != true) {
            install(CSRF) {
                sec.csrfConfig.allowedOrigins.forEach { allowOrigin(it) }
                sec.csrfConfig.headerChecks.forEach { checkHeader(it.name) }
                if (sec.csrfConfig.originMatchesHost) originMatchesHost()
            }
        }
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
    }

    config.authConfig?.sessionConfig?.let { sessionCfg ->
        require(sessionCfg.secret != "change-me") {
            "Session auth secret must be configured. Set a secure random string via: auth { session { secret = \"...\" } }"
        }
        val userTable = sessionCfg.userTableProvider.table
        if (config.models.none { it === userTable }) {
            config.models.add(0, userTable)
        }
    }

    val db = if (config.dbConfig != null) {
        installDatabase(config.dbConfig!!).also { database ->
            config.db = database
            if (config.models.isNotEmpty()) {
                transaction(database) {
                    SchemaUtils.createMissingTablesAndColumns(*config.models.toTypedArray())
                }
            }
            if (config.migrations.isNotEmpty()) {
                transaction(database) {
                    SchemaUtils.create(org.ktorite.migration.MigrationTable)
                }
                runMigrations(database, config.migrations)
            }
        }
    } else null

    config.onStart?.invoke()

    if (config.authConfig?.sessionConfig != null && db != null) {
        installSessionAuth(config.authConfig!!.sessionConfig!!, db!!)
    }

    install(Authentication) {
        config.authConfig?.jwtConfig?.let { jwtCfg ->
            require(jwtCfg.secret.isNotBlank()) {
                "JWT secret must be set. Configure it via: auth { jwt { secret = \"your-secret\" } }"
            }
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
        config.authConfig?.sessionConfig?.let {
            session<org.ktorite.auth.UserSession>("session") {
                validate { session ->
                    session?.let { UserIdPrincipal(it.username) }
                }
            }
        }
    }

    routing {
        if (config.enableAdmin && db != null) {
            val sessionCfg = config.authConfig?.sessionConfig
            require(sessionCfg != null) {
                "Admin panel requires session auth. Configure auth { session { ... } }"
            }
            get("/admin") {
                val session = call.sessions.get<org.ktorite.auth.UserSession>()
                if (session == null) {
                    call.respond(ThymeleafContent("admin/login", mapOf("loginPath" to sessionCfg.loginPath)))
                } else {
                    call.respond(ThymeleafContent("admin/index", mapOf("models" to config.models, "modelCount" to config.models.size)))
                }
            }
            authenticate("session") {
                installAdmin(config.models, db)
            }
        }
        config.routes.forEach { it() }
    }
}
