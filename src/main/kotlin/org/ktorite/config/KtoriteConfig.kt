package org.ktorite.config

import io.ktor.server.routing.*
import org.ktorite.db.DbConfig

class KtoriteConfig {
    var port: Int = 8080
    var enableAdmin: Boolean = false
    var dbConfig: DbConfig? = null

    internal val routes = mutableListOf<Route.() -> Unit>()
    internal val webSocketConfigs = mutableListOf<Route.() -> Unit>()

    fun routing(block: Route.() -> Unit) {
        routes += block
    }

    fun websockets(block: Route.() -> Unit) {
        webSocketConfigs += block
    }

    fun database(block: DbConfig.() -> Unit) {
        dbConfig = DbConfig().apply(block)
    }
}
