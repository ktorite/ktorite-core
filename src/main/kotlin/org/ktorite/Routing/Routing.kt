package org.ktorite.Routing

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import korlibs.time.seconds
import kotlin.time.Duration

data class WebSocketConfig(
    val pingPeriod: Duration = 15.seconds,
    val timeout: Duration = 15.seconds,
    val maxFrameSize: Long = Long.MAX_VALUE,
    val masking: Boolean = false
)

fun Application.installRoutes(
    enableWebSocket: Boolean = false,
    webSocketConfig: WebSocketConfig? = null,
    routes: Route.() -> Unit
) {
    if (enableWebSocket && webSocketConfig != null) {
        install(WebSockets) {
            pingPeriod = webSocketConfig.pingPeriod
            timeout = webSocketConfig.timeout
            maxFrameSize = webSocketConfig.maxFrameSize
            masking = webSocketConfig.masking
        }
    }

    routing {
        routes()
    }
}
