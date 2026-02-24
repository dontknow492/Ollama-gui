package com.ghost.ollama

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

actual fun createHttpClient(): HttpClient {
    return HttpClient(OkHttp) {
        // Install timeout plugin
        install(HttpTimeout) {
            connectTimeoutMillis = TimeUnit.SECONDS.toMillis(30)
            socketTimeoutMillis = TimeUnit.SECONDS.toMillis(60)
            requestTimeoutMillis = TimeUnit.MINUTES.toMillis(5)
        }

        // Content negotiation
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        followRedirects = true

        // Configure OkHttp engine
        engine {
            // Configure OkHttp's own timeout settings
            config {
                connectTimeout(30, TimeUnit.SECONDS)
                readTimeout(60, TimeUnit.SECONDS)
                writeTimeout(60, TimeUnit.SECONDS)

                // For long-lived streaming connections
                retryOnConnectionFailure(true)

                // Connection pool settings
                connectionPool(
                    okhttp3.ConnectionPool(
                        maxIdleConnections = 5,
                        keepAliveDuration = 5, TimeUnit.MINUTES
                    )
                )

                // Add network interceptors for logging
                addInterceptor { chain ->
                    val request = chain.request()
                    val startTime = System.currentTimeMillis()

                    try {
                        val response = chain.proceed(request)
                        val duration = System.currentTimeMillis() - startTime
                        println("OkHttp: ${request.method} ${request.url} completed in ${duration}ms")
                        response
                    } catch (e: Exception) {
                        println("OkHttp: Request failed - ${e.message}")
                        throw e
                    }
                }


            }

            // Enable HTTP/2 for better streaming
            config {
                protocols(listOf(okhttp3.Protocol.HTTP_2, okhttp3.Protocol.HTTP_1_1))


            }

        }
    }
}