package org.ktorite.migration

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant

data class Migration(
    val name: String,
    val up: JdbcTransaction.() -> Unit
)

internal object MigrationTable : Table("_ktorite_migrations") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val appliedAt = varchar("applied_at", 30)
    override val primaryKey = PrimaryKey(id)
}

internal fun runMigrations(db: Database, migrations: List<Migration>) {
    transaction(db) {
        SchemaUtils.create(MigrationTable)
    }
    val applied = transaction(db) {
        MigrationTable.selectAll().map { it[MigrationTable.name] }.toSet()
    }
    for (migration in migrations) {
        if (migration.name !in applied) {
            transaction(db) {
                migration.up(this)
                val now = Instant.now().toString()
                exec("INSERT INTO _ktorite_migrations (name, applied_at) VALUES (?, ?)", listOf(MigrationTable.name.columnType to migration.name, MigrationTable.appliedAt.columnType to now))
            }
        }
    }
}
