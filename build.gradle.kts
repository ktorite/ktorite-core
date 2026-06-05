
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
    id("java-library")
    id("maven-publish")
}

group = "org.ktorite"
version = "1.0.0"

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    enabled = false
}

repositories {
    mavenCentral()
    maven { url = uri("https://packages.confluent.io/maven/") }
}
kotlin {
    jvmToolchain(17)
}
publishing {
    publications {
        create<MavenPublication>("core") {
            from(components["java"])
            groupId = "com.github.ktorite"
            artifactId = "ktorite"
            version = "1.0.0"
        }
    }
}

dependencies {
    api(libs.ktor.server.task.scheduling.core)
    api(libs.ktor.server.task.scheduling.redis)
    api(libs.ktor.server.task.scheduling.mongodb)
    api(libs.ktor.server.task.scheduling.jdbc)
    api(libs.ktor.server.rate.limiting)
    api(libs.ktor.server.core)
    api(libs.ktor.server.websockets)
    api(libs.ktor.server.rabbitmq)
    api(libs.ktor.serialization.kotlinx.json)
    api(libs.ktor.server.content.negotiation)
    api(libs.hikaricp)
    api(libs.bcrypt)
    api(libs.postgresql)
    api(libs.h2)
    api(libs.mongodb.driver.core)
    api(libs.mongodb.driver.sync)
    api(libs.bson)
    api(libs.exposed.core)
    api(libs.exposed.jdbc)
    api(libs.ktor.serialization.jackson)
    api(libs.ktor.serialization.gson)
    api(libs.ktor.server.call.logging)
    api(libs.ktor.server.host.common)
    api(libs.ktor.server.status.pages)
    api(libs.ktor.server.sse)
    api(libs.ktor.server.request.validation)
    api(libs.ktor.server.thymeleaf)
    api(libs.ktor.server.sessions)
    api(libs.ktor.server.auth)
    api(libs.ktor.server.csrf)
    api(libs.ktor.client.core)
    api(libs.ktor.client.apache)
    api(libs.ktor.server.auth.jwt)
    api(libs.ktor.server.compression)
    api(libs.ktor.server.swagger)
    api(libs.ktor.simple.cache)
    api(libs.ktor.simple.memory.cache)
    api(libs.ktor.simple.redis.cache)
    api(libs.ktor.server.openapi)
    api(libs.ktor.server.http.redirect)
    api(libs.ktor.server.hsts)
    api(libs.ktor.server.default.headers)
    api(libs.ktor.server.cors)
    api(libs.ktor.server.netty)
    api(libs.logback.classic)
    api(libs.ktor.server.config.yaml)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}
