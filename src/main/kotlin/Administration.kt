package org.ktorite

import io.github.flaxoos.ktor.server.plugins.ratelimiter.RateLimiting
import io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations.TokenBucket
import io.github.flaxoos.ktor.server.plugins.taskscheduling.TaskScheduling
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database.DefaultTaskLockTable
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database.jdbc
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.redis.redis
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import kotlin.time.Duration.Companion.seconds

object Settings : Table("settings") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val lastUpdated = long("last_updated")
    override val primaryKey = PrimaryKey(id)
}
fun Application.configureAdministration() {
    install(ShutDownUrl.ApplicationCallPlugin) {
        shutDownUrl = "/shutdown"
        exitCodeSupplier = { 0 }
    }
    install(TaskScheduling){
        redis {
            connectionPoolInitialSize = 1
            host = "localhost"
            port = 6379
            username = "my_username"
            password = "my_password"
            connectionAcquisitionTimeoutMs = 1_000
            lockExpirationMs = 60_000
        }
        jdbc("my jdbc manager") {
            database = org.jetbrains.exposed.sql.Database.connect(
                url = "jdbc:postgresql://localhost:5432/",
                driver = "org.postgresql.Driver",
                user = "user",
                password = "pass"
            ).also {
                transaction { SchemaUtils.create(DefaultTaskLockTable) }
            }
        }

        task {
            name = "My task"
            task = { taskExecutionTime ->
                log.info("My task is running: $taskExecutionTime")
            }
            kronSchedule = {
                hours {
                    from(0).every(12)
                }
                minutes {
                    from(10).every(30)
                }
            }
            concurrency = 2
        }

        task(taskManagerName = "my jdbc manager") {
            name = "My Jdbc task"
            task = { taskExecutionTime ->
                transaction {
                    SchemaUtils.create(Settings)
                    Settings.update({ Settings.id greaterEq 0 }) {
                        it[lastUpdated] = System.currentTimeMillis()
                    }
                }
                log.info("Updated settings timestamps at $taskExecutionTime")
            }
            kronSchedule = {
                minutes {
                    from(0).every(1)
                }
            }
            concurrency = 2
        }
    }
    routing {
        route("/health"){
            install(RateLimiting) {
                rateLimiter {
                    type = TokenBucket::class
                    capacity = 100
                    rate = 1.seconds

                }
            }
            get("/health") {
                call.respondText("OK")
            }
        }
    }
}