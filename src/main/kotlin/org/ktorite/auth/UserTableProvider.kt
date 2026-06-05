package org.ktorite.auth

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table

interface UserTableProvider {
    val table: Table
    val idColumn: Column<*>
    val usernameColumn: Column<*>
    val passwordColumn: Column<*>
}

object DefaultUserTableProvider : UserTableProvider {
    override val table: Table = UserTable
    override val idColumn = UserTable.id
    override val usernameColumn = UserTable.username
    override val passwordColumn = UserTable.passwordHash
}
