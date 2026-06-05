package org.ktorite

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.v1.core.*
import org.ktorite.config.KtoriteConfig
import kotlin.test.Test
import kotlin.test.assertEquals

object TestTable : Table("test_table") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 64)
    override val primaryKey = PrimaryKey(id)
}

class ApplicationTest {

    @Test
    fun adminEndpointReturnsOk() = testApplication {
        application {
            module(KtoriteConfig().apply {
                enableAdmin = true
                database {
                    url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
                }
                registerModels(TestTable)
            })
        }
        client.get("/admin").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun userRouteWorks() = testApplication {
        application {
            module(KtoriteConfig().apply {
                routing {
                    get("/hello") { call.respondText("Hello World") }
                }
            })
        }
        client.get("/hello").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun databaseStartsWithoutError() = testApplication {
        application {
            module(KtoriteConfig().apply {
                database {
                    url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
                }
            })
        }
        client.get("/nonexistent").apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }
}
