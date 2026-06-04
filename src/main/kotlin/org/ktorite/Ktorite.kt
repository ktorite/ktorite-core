package org.ktorite

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
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

    if (config.enableAdmin) {
        installAdmin()
    }
    if (config.dbConfig != null) {
        installDatabase(config.dbConfig!!)
    }
    installRoutes(false){
        config.routes.forEach { routeDef ->
            routeDef()
        }
    }
}
