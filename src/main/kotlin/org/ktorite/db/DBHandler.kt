package org.ktorite.db

import io.ktor.server.application.*

class DbConfig {
    var url: String = "jdbc:h2:mem:test"
    var driver: String = "org.h2.Driver"
    var user: String = "sa"
    var password: String = ""
}

fun Application.installDatabase(config: DbConfig) {
    log.info("Database connected at ${config.url}")
}
