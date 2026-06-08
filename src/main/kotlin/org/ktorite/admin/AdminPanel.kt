package org.ktorite.admin

import io.ktor.server.application.Application
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database

interface AdminPanel {
    fun install(app: Application, models: List<Table>, db: Database, loginPath: String)
}
