package com.ghost.ollama

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

actual fun createHttpClient(): HttpClient {
    return HttpClient(CIO) {
        install(HttpTimeout) {
            // More generous timeouts for LLM streaming
            connectTimeoutMillis = 60.seconds.inWholeMilliseconds  // 60s to connect
            socketTimeoutMillis = 120.seconds.inWholeMilliseconds  // 2m between chunks
            requestTimeoutMillis = 10.minutes.inWholeMilliseconds  // 10m total
        }

        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = false
                    isLenient = true
                    ignoreUnknownKeys = true
                }
            )
        }

        engine {
            // CIO specific configuration
            maxConnectionsCount = 100
            endpoint {
                keepAliveTime = 60.seconds.inWholeMilliseconds
                connectTimeout = 60.seconds.inWholeMilliseconds
            }
            requestTimeout = 10.minutes.inWholeMilliseconds

            // For long-running streams, we want to reuse connections
            pipelining = true

        }

        // Add logging for debugging timeouts
        install(Logging) {
            level = LogLevel.INFO
        }
    }
}