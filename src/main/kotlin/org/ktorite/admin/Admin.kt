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
import java.util.UUID

private fun csrfToken(): String = UUID.randomUUID().toString()

private suspend fun ApplicationCall.setCsrfCookie(token: String) {
  response.header("Set-Cookie", "csrf_token=$token; Path=/admin; SameSite=Strict")
}

private suspend fun ApplicationCall.csrfFromCookie(): String? {
  val cookies = request.headers["Cookie"] ?: return null
  return cookies.split(";").map { it.trim() }.firstOrNull { it.startsWith("csrf_token=") }?.substringAfter("=")
}

private suspend fun ApplicationCall.verifyCsrf(params: Parameters): Boolean {
  val cookieToken = csrfFromCookie()
  val formToken = params["csrf_token"]
  return cookieToken != null && formToken != null && cookieToken == formToken
}

fun Route.installAdmin(models: List<Table>, db: Database) {
  if (models.isEmpty()) return

  get("/admin") { call.respondText(adminIndexPage(models), ContentType.Text.Html) }

  models.forEach { table ->
    val name = table.tableName.lowercase()
    val tbl = table

    get("/admin/$name") {
      val rows = transaction(db) { tbl.selectAll().toList() }
      call.respondText(adminListPage(tbl, rows), ContentType.Text.Html)
    }

    get("/admin/$name/new") {
      val token = csrfToken()
      call.setCsrfCookie(token)
      call.respondText(adminFormPage(tbl, null, csrfToken = token), ContentType.Text.Html)
    }

    post("/admin/$name") {
      val params = call.receiveParameters()
      if (!call.verifyCsrf(params)) {
        call.respondText("CSRF validation failed", ContentType.Text.Html, HttpStatusCode.Forbidden)
        return@post
      }
      try {
        transaction(db) { doInsert(tbl, params) }
        call.respondRedirect("/admin/$name")
      } catch (e: Exception) {
        val err = formatDbError(e)
        val token = csrfToken()
        call.setCsrfCookie(token)
        call.respondText(adminFormPage(tbl, null, params, err, token), ContentType.Text.Html)
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
      val token = csrfToken()
      call.setCsrfCookie(token)
      call.respondText(adminFormPage(tbl, row, csrfToken = token), ContentType.Text.Html)
    }

    post("/admin/$name/{id}") {
      val params = call.receiveParameters()
      if (!call.verifyCsrf(params)) {
        call.respondText("CSRF validation failed", ContentType.Text.Html, HttpStatusCode.Forbidden)
        return@post
      }
      try {
        transaction(db) { doUpdate(tbl, call.parameters["id"]!!, params) }
        call.respondRedirect("/admin/$name")
      } catch (e: Exception) {
        val row = transaction(db) { findByPk(tbl, call.parameters["id"]!!) }
        val err = formatDbError(e)
        val token = csrfToken()
        call.setCsrfCookie(token)
        call.respondText(adminFormPage(tbl, row, params, err, token), ContentType.Text.Html)
      }
    }

    post("/admin/$name/{id}/delete") {
      val params = call.receiveParameters()
      if (!call.verifyCsrf(params)) {
        call.respondText("CSRF validation failed", ContentType.Text.Html, HttpStatusCode.Forbidden)
        return@post
      }
      transaction(db) { doDelete(tbl, call.parameters["id"]!!) }
      call.respondRedirect("/admin/$name")
    }
  }
}
