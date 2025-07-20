package org.ktorite

import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
//    configureAdministration()
//    configureSockets()
//    configureFrameworks()
//    configureSerialization()
//    configureDatabases()
//    configureMonitoring()
//    configureTemplating()
//    configureSecurity()
//    configureHTTP()
    configureRouting()
}
