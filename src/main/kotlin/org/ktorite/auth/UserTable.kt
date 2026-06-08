package org.ktorite.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object UserTable : Table("users") {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 100).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val isSuperuser = bool("is_superuser").default(false)

    override val primaryKey = PrimaryKey(id)

    fun createSuperuser(db: Database, username: String, password: String, rounds: Int = 12) {
        val hash = BCrypt.withDefaults().hashToString(rounds, password.toCharArray())
        transaction(db) {
            UserTable.insert { r ->
                r[UserTable.username] = username
                r[UserTable.passwordHash] = hash
                r[UserTable.isSuperuser] = true
            }
        }
    }
}
