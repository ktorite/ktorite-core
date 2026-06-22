package org.ktorite.auth

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File
import java.util.Properties

fun main(args: Array<String>) {
    if (args.size < 2) {
        println("Usage: ./gradlew createsuperuser -Pargs=\"<username> <password>\"")
        return
    }
    val username = args[0]
    val password = args[1]

    val props = File("ktorite.properties").takeIf { it.exists() }?.inputStream()?.use { Properties().apply { load(it) } }
        ?: error("ktorite.properties not found in project root")

    val url = props.getProperty("db.url") ?: error("db.url missing in ktorite.properties")
    val driver = props.getProperty("db.driver") ?: error("db.driver missing in ktorite.properties")
    val dbUser = props.getProperty("db.user") ?: error("db.user missing in ktorite.properties")
    val dbPass = props.getProperty("db.password") ?: ""

    val db = Database.connect(url, driver, dbUser, dbPass)
    transaction(db) { SchemaUtils.createMissingTablesAndColumns(UserTable) }
    UserTable.createSuperuser(db, username, password)
    println("Superuser '$username' created.")
}
