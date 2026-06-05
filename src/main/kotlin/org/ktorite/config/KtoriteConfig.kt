package org.ktorite.config

import io.ktor.server.routing.*
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.ktorite.db.DbConfig

class KtoriteConfig {
    var port: Int = 8080
    var enableAdmin: Boolean = false
    var adminUsername: String? = null
    var adminPassword: String? = null
    var dbConfig: DbConfig? = null
    var authConfig: AuthConfig? = null
    var securityConfig: SecurityConfig? = null
    val errorConfig: ErrorConfig = ErrorConfig()
    var db: Database? = null
        internal set
    var onStart: (() -> Unit)? = null

    internal val routes = mutableListOf<Route.() -> Unit>()
    internal val webSocketConfigs = mutableListOf<Route.() -> Unit>()
    internal val models = mutableListOf<Table>()

    fun routing(block: Route.() -> Unit) {
        routes += block
    }

    fun websockets(block: Route.() -> Unit) {
        webSocketConfigs += block
    }

    fun database(block: DbConfig.() -> Unit) {
        dbConfig = DbConfig().apply(block)
    }

    fun registerModels(vararg tables: Table) {
        models += tables
    }

    fun auth(block: AuthConfig.() -> Unit) {
        authConfig = AuthConfig().apply(block)
    }

    fun security(block: SecurityConfig.() -> Unit) {
        securityConfig = SecurityConfig().apply(block)
    }

    fun error(block: ErrorConfig.() -> Unit) {
        errorConfig.apply(block)
    }
}
