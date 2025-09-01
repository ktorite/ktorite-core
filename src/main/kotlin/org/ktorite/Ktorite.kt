package org.ktorite

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.ktorite.Routing.installRoutes
import org.ktorite.admin.installAdmin
import org.ktorite.config.KtoriteConfig
import org.ktorite.db.installDatabase


object Ktorite {
    fun start(configure: KtoriteConfig.() -> Unit) {
        val config = KtoriteConfig().apply(configure)
        embeddedServer(Netty, port = config.port) {
            module(config)
        }.start(wait = true)
    }
}


fun Application.module(config: KtoriteConfig) {
    if (config.enableAdmin) {
        installAdmin()
    }
    if (config.dbConfig != null) {
        installDatabase(config.dbConfig!!)
    }
    installRoutes(false){
        config.routes.forEach { routeDef ->
            this.routeDef()
        }
    }
}
