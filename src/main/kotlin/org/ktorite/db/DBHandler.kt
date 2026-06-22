package org.ktorite.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.v1.jdbc.Database

class DbConfig {
    var url: String = "jdbc:h2:file:./data/db;DB_CLOSE_DELAY=-1"
    var driver: String = ""
    var username: String = "sa"
    var password: String = ""
    var maxPoolSize: Int = 10
    var connectionTimeout: Long = 3000
    var maxLifetime: Long = 1_800_000
    var idleTimeout: Long = 600_000
}

fun Application.installDatabase(config: DbConfig): Database {
    val hikariConfig = HikariConfig().apply {
        jdbcUrl = config.url
        if (config.driver.isNotBlank()) {
            driverClassName = config.driver
        }
        username = config.username
        password = config.password
        maximumPoolSize = config.maxPoolSize
        connectionTimeout = config.connectionTimeout
        maxLifetime = config.maxLifetime
        idleTimeout = config.idleTimeout
        validate()
    }
    val dataSource = HikariDataSource(hikariConfig)
    monitor.subscribe(ApplicationStopping) {
        dataSource.close()
    }
    val db = Database.connect(dataSource)
    log.info("Database connected: ${config.url}")
    return db
}
