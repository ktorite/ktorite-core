package org.ktorite.auth

import org.jetbrains.exposed.v1.core.*

object UserTable : Table("users") {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 100).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)

    override val primaryKey = PrimaryKey(id)
}
