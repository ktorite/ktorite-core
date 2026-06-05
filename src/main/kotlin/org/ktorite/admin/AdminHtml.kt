package org.ktorite.admin

import io.ktor.http.Parameters
import io.ktor.server.request.*
import org.jetbrains.exposed.v1.core.*

internal fun formatDbError(e: Exception): String {
  val msg = e.message ?: return "An error occurred"
  return when {
    msg.contains("Unique index or primary key violation") ->
            "Duplicate value: a record with this value already exists."
    msg.contains("NULL not allowed") -> "This field cannot be empty."
    else -> msg
  }
}

private fun h(value: Any?): String =
  value?.toString()
    ?.replace("&", "&amp;")
    ?.replace("<", "&lt;")
    ?.replace(">", "&gt;")
    ?.replace("\"", "&quot;")
    ?.replace("'", "&#x27;") ?: ""

private fun attr(value: Any?): String =
  value?.toString()
    ?.replace("&", "&amp;")
    ?.replace("<", "&lt;")
    ?.replace(">", "&gt;")
    ?.replace("\"", "&quot;")
    ?.replace("'", "&#x27;") ?: ""

private const val STYLE =
        """
<style>
body { font-family: system-ui, sans-serif; max-width: 960px; margin: 0 auto; padding: 1rem; background: #eff1f5; color: #4c4f69; }
nav { margin-bottom: 0.5rem; font-size: 0.85rem; color: #6c6f85; }
nav a { text-decoration: none; color: #1e66f5; }
nav span { color: #4c4f69; font-weight: 600; }
table { width: 100%; border-collapse: collapse; background: #eff1f5; }
th, td { text-align: left; padding: 0.5rem; border-bottom: 1px solid #dce0e8; }
th { background: #e6e9ef; color: #4c4f69; }
tr:hover { background: #ccd0da; }
.btn { display: inline-block; padding: 0.3rem 0.7rem; border-radius: 4px; text-decoration: none; font-size: 0.875rem; }
.btn-primary { background: #1e66f5; color: #fff; }
.btn-danger { background: #d20f39; color: #fff; }
.btn-sm { padding: 0.2rem 0.5rem; font-size: 0.75rem; }
form label { display: block; margin: 0.5rem 0 0.25rem; font-weight: 600; color: #4c4f69; }
form input, form textarea, form select { width: 100%; padding: 0.4rem; border: 1px solid #bcc0cc; border-radius: 4px; box-sizing: border-box; background: #eff1f5; color: #4c4f69; }
form input[type=checkbox] { width: auto; }
form .actions { margin-top: 1rem; }
.alert { padding: 0.5rem; border-radius: 4px; margin-bottom: 1rem; }
.alert-info { background: #e6e9ef; color: #1e66f5; }
.alert-danger { background: #e6e9ef; color: #d20f39; }
</style>
"""

private fun page(
        title: String,
        body: String,
        breadcrumbs: List<Pair<String, String?>> = emptyList()
): String {
  val nav = buildString {
    append("<nav>")
    append("<a href='/admin'>Home</a>")
    for (crumb in breadcrumbs) {
      append(" › ")
      if (crumb.second != null) {
        append("<a href='${crumb.second}'>${h(crumb.first)}</a>")
      } else {
        append("<span>${h(crumb.first)}</span>")
      }
    }
    append("</nav>")
  }
  val safeTitle = h(title)
  return """<!DOCTYPE html>
<html lang="en"><head><meta charset="utf-8"><title>$safeTitle</title>$STYLE</head>
<body>$nav<h1>$safeTitle</h1>$body</body></html>"""
}

internal fun adminIndexPage(models: List<Table>): String =
        page(
                "Ktorite Admin",
                buildString {
                  appendLine("<p class='alert alert-info'>${models.size} model(s) registered</p>")
                  appendLine("<ul>")
                  models.forEach { t ->
                    appendLine(
                            "<li><a href='/admin/${t.tableName.lowercase()}'>${h(t.tableName)}</a></li>"
                    )
                  }
                  appendLine("</ul>")
                }
        )

internal fun adminListPage(table: Table, rows: List<ResultRow>): String {
  val name = table.tableName.lowercase()
  val cols = table.columns
  val crumbs = listOf(name to "/admin/$name")
  val body = buildString {
    appendLine("<a href='/admin/$name/new' class='btn btn-primary'>+ New</a>")
    appendLine("<table><thead><tr>")
    cols.forEach { appendLine("<th>${h(it.name)}</th>") }
    appendLine("<th>Actions</th></tr></thead><tbody>")
    for (row in rows) {
      val pkVal = row[pkCol(table)]
      appendLine("<tr>")
      for (col in cols) {
        val v = row[col]
        appendLine("<td>${h(v)}</td>")
      }
      appendLine("<td>")
      appendLine("<a href='/admin/$name/$pkVal' class='btn btn-sm'>View</a> ")
      appendLine("<a href='/admin/$name/$pkVal/edit' class='btn btn-sm btn-primary'>Edit</a> ")
      appendLine(
              "<form method='post' action='/admin/$name/$pkVal/delete' style='display:inline' onsubmit='return confirm(\"Delete?\")'>"
      )
      appendLine("<button type='submit' class='btn btn-sm btn-danger'>Delete</button>")
      appendLine("</form></td></tr>")
    }
    appendLine("</tbody></table>")
  }
  return page("${h(table.tableName)} — Admin", body, crumbs)
}

internal fun adminDetailPage(table: Table, row: ResultRow): String {
  val name = table.tableName.lowercase()
  val pkVal = row[pkCol(table)]
  val crumbs = listOf(name to "/admin/$name", "$pkVal" to null)
  val body = buildString {
    appendLine("<table>")
    for (col in table.columns) {
      val v = row[col]
      appendLine("<tr><th>${h(col.name)}</th><td>${h(v)}</td></tr>")
    }
    appendLine("</table>")
    appendLine("<div class='actions'>")
    appendLine("<a href='/admin/$name/$pkVal/edit' class='btn btn-primary'>Edit</a> ")
    appendLine("<a href='/admin/$name' class='btn'>Back</a>")
    appendLine("</div>")
  }
  return page("${h(table.tableName)} — Detail", body, crumbs)
}

internal fun adminFormPage(
        table: Table,
        existing: ResultRow?,
        params: Parameters? = null,
        error: String? = null,
        csrfToken: String? = null
): String {
  val name = table.tableName.lowercase()
  val isEdit = existing != null
  val pkCol = pkCol(table)
  val pkVal = existing?.get(pkCol)
  val action = if (isEdit) "/admin/$name/$pkVal" else "/admin/$name"
  val title = if (isEdit) "Edit ${table.tableName}" else "New ${table.tableName}"

  val body = buildString {
    if (error != null) {
      appendLine("<div class='alert alert-danger'>${h(error)}</div>")
    }
    appendLine("<form method='post' action='$action'>")
    if (csrfToken != null) {
      appendLine("<input type='hidden' name='csrf_token' value='${attr(csrfToken)}'>")
    }
    for (col in table.columns) {
      if (col.columnType is AutoIncColumnType<*> && !isEdit) continue
      val value =
              when {
                params != null -> params[col.name]
                else -> existing?.get(col)
              }
      val readonly = isEdit && col == pkCol
      appendLine("<label>${h(col.name)}</label>")
      appendLine(inputField(col, value, readonly))
    }
    appendLine("<div class='actions'>")
    appendLine("<button type='submit' class='btn btn-primary'>Save</button> ")
    appendLine("<a href='/admin/$name' class='btn'>Cancel</a>")
    appendLine("</div></form>")
  }
  val crumbs =
          if (isEdit) {
            listOf(name to "/admin/$name", "$pkVal" to "/admin/$name/$pkVal", "Edit" to null)
          } else {
            listOf(name to "/admin/$name", "New" to null)
          }
  return page(title, body, crumbs)
}

internal fun inputField(col: Column<*>, value: Any?, readonly: Boolean): String {
  val inputName = col.name
  val strValue = attr(value)
  val ro = if (readonly) "readonly" else ""
  return when (col.columnType) {
    is BooleanColumnType -> {
      val checked = if (value == true) "checked" else ""
      "<input type='checkbox' name='$inputName' value='true' $checked $ro>"
    }
    is TextColumnType, is LargeTextColumnType -> {
      "<textarea name='$inputName' $ro>$strValue</textarea>"
    }
    is IntegerColumnType,
    is LongColumnType,
    is ShortColumnType,
    is ByteColumnType,
    is AutoIncColumnType<*> -> {
      "<input type='number' name='$inputName' value='$strValue' $ro>"
    }
    is DoubleColumnType, is FloatColumnType -> {
      "<input type='number' step='any' name='$inputName' value='$strValue' $ro>"
    }
    else -> {
      "<input type='text' name='$inputName' value='$strValue' $ro>"
    }
  }
}
