package org.ktorite.admin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun Application.installAdmin(models: List<Table>, db: Database) {
  if (models.isEmpty()) return

  routing {
    get("/admin") { call.respondText(adminIndexPage(models), ContentType.Text.Html) }

    models.forEach { table ->
      val name = table.tableName.lowercase()
      val tbl = table

      get("/admin/$name") {
        val rows = transaction(db) { tbl.selectAll().toList() }
        call.respondText(adminListPage(tbl, rows), ContentType.Text.Html)
      }

      get("/admin/$name/new") { call.respondText(adminFormPage(tbl, null), ContentType.Text.Html) }

      post("/admin/$name") {
        val params = call.receiveParameters()
        try {
          transaction(db) { doInsert(tbl, params) }
          call.respondRedirect("/admin/$name")
        } catch (e: Exception) {
          val err = formatDbError(e)
          call.respondText(adminFormPage(tbl, null, params, err), ContentType.Text.Html)
        }
      }

      get("/admin/$name/{id}") {
        val row = transaction(db) { findByPk(tbl, call.parameters["id"]!!) }
        if (row == null) {
          call.respondText("Not found", ContentType.Text.Html, HttpStatusCode.NotFound)
          return@get
        }
        call.respondText(adminDetailPage(tbl, row), ContentType.Text.Html)
      }

      get("/admin/$name/{id}/edit") {
        val row = transaction(db) { findByPk(tbl, call.parameters["id"]!!) }
        if (row == null) {
          call.respondText("Not found", ContentType.Text.Html, HttpStatusCode.NotFound)
          return@get
        }
        call.respondText(adminFormPage(tbl, row), ContentType.Text.Html)
      }

      post("/admin/$name/{id}") {
        val params = call.receiveParameters()
        try {
          transaction(db) { doUpdate(tbl, call.parameters["id"]!!, params) }
          call.respondRedirect("/admin/$name")
        } catch (e: Exception) {
          val row = transaction(db) { findByPk(tbl, call.parameters["id"]!!) }
          val err = formatDbError(e)
          call.respondText(adminFormPage(tbl, row, params, err), ContentType.Text.Html)
        }
      }

      post("/admin/$name/{id}/delete") {
        transaction(db) { doDelete(tbl, call.parameters["id"]!!) }
        call.respondRedirect("/admin/$name")
      }
    }
  }
}
