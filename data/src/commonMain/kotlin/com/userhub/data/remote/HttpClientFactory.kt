package com.userhub.data.remote

import com.userhub.data.isDebug
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun createHttpClient(
    engine: HttpClientEngine,
    isDebugBuild: Boolean = isDebug,
): HttpClient = HttpClient(engine) {
    expectSuccess = true
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
            }
        )
    }
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                println("HTTP: $message")
            }
        }
        level = if (isDebugBuild) LogLevel.HEADERS else LogLevel.NONE
        sanitizeHeader { it == HttpHeaders.Authorization }
    }
    defaultRequest {
        header("Authorization", "Bearer ${ApiConfig.API_TOKEN}")
        contentType(ContentType.Application.Json)
    }
}
