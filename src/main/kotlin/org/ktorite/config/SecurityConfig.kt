package org.ktorite.config

import io.ktor.http.HttpMethod

class SecurityConfig {
    internal val corsConfig = CorsConfig()
    internal val hstsConfig = HstsConfig()
    internal val csrfConfig = CsrfConfig()

    fun cors(block: CorsConfig.() -> Unit) { corsConfig.apply(block) }
    fun hsts(block: HstsConfig.() -> Unit) { hstsConfig.apply(block) }
    fun csrf(block: CsrfConfig.() -> Unit) { csrfConfig.apply(block) }
}

class CorsConfig {
    internal val hosts = mutableListOf<CorsHostEntry>()
    internal val methods = mutableListOf<HttpMethod>()
    internal val headers = mutableListOf<String>()
    var allowCredentials: Boolean = false
    var allowSameOrigin: Boolean = true
    var maxAgeInSeconds: Long = 3600

    fun allowHost(host: String, schemes: List<String> = listOf("http", "https")) {
        hosts += CorsHostEntry(host, schemes)
    }

    fun allowMethod(method: HttpMethod) {
        methods += method
    }

    fun allowHeader(header: String) {
        headers += header
    }
}

data class CorsHostEntry(val host: String, val schemes: List<String>)

class HstsConfig {
    var maxAgeInSeconds: Long = 31536000L
    var includeSubDomains: Boolean = true
    var preload: Boolean = false
}

class CsrfConfig {
    var disabled: Boolean = false
    internal val allowedOrigins = mutableListOf<String>()
    internal val headerChecks = mutableListOf<CsrfHeaderCheck>()
    var originMatchesHost: Boolean = false

    fun allowOrigin(origin: String) {
        allowedOrigins += origin
    }

    fun checkHeader(name: String) {
        headerChecks += CsrfHeaderCheck(name)
    }
}

data class CsrfHeaderCheck(val name: String)
