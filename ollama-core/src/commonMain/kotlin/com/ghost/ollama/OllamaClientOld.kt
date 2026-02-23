package com.ghost.ollama

import com.ghost.ollama.models.VersionResponse
import com.ghost.ollama.models.chat.*
import com.ghost.ollama.models.common.ThinkOption
import com.ghost.ollama.models.embed.EmbedRequest
import com.ghost.ollama.models.embed.EmbedResponse
import com.ghost.ollama.models.modelMGMT.CreateModelRequest
import com.ghost.ollama.models.modelMGMT.DeleteModelRequest
import com.ghost.ollama.models.modelMGMT.ShowModelRequest
import com.ghost.ollama.models.modelMGMT.ShowModelResponse
import com.ghost.ollama.models.modelMGMT.tags.ListModelsResponse
import com.ghost.ollama.models.modelMGMT.tags.ListRunningModelsResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File

class OllamaClientOld(
    private val baseUrl: String = "http://localhost:11434",
    private val httpClient: HttpClient
) {

    suspend fun <T> retryWithBackoff(
        times: Int = 3,
        initialDelayMs: Long = 500,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMs
        repeat(times - 1) {
            try {
                return block()
            } catch (e: Exception) {
                // log retry attempt
                println("Retry failed: ${e.message}, retrying in $currentDelay ms")
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong()
        }
        return block() // last attempt
    }


    /**
     * Pulls a model from Ollama and streams progress updates.
     *
     * @param model Name of the model to download
     * @param insecure Allow insecure connections
     * @param stream Stream progress updates
     * @param retries Number of times to retry the request in case of failure
     * @return Flow of PullModelProgress objects
     *
     * @throws Exception on HTTP or parsing errors
     */
    fun pullModel(
        model: String,
        insecure: Boolean = false,
        stream: Boolean = true,
        retries: Int = 3
    ): Flow<PullModelProgress> = flow {
        val request = PullModelRequest(model, insecure, stream)
        val json = Json { ignoreUnknownKeys = true }

        retryWithBackoff(times = retries) {
            val response = httpClient.post("$baseUrl/api/pull") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val channel = response.bodyAsChannel()

            while (!channel.isClosedForRead && currentCoroutineContext().isActive) {
                val line = channel.readLine() ?: break
                if (line.isBlank()) continue

                try {
                    val progress = json.decodeFromString<PullModelProgress>(line)
                    emit(progress)
                } catch (e: Exception) {
                    println("Failed to parse progress line: $line -> ${e.message}")
                }
            }
        }
    }

    /**
     * Push a model from Ollama and streams progress updates.
     *
     * @param model Name of the model to download
     * @param path Path to the model folder
     * @param insecure Allow insecure connections
     * @param stream Stream progress updates
     * @param retries Number of times to retry the request in case of failure
     * @return Flow of PullModelProgress objects
     *
     * @throws Exception on HTTP or parsing errors
     */
    fun pushModel(
        model: String,
        path: String? = null,
        insecure: Boolean = false,
        stream: Boolean = true,
        retries: Int = 3
    ): Flow<PushModelProgress> = flow {
        // Ensure local path exists
        path?.let { folder ->
            val dir = File(folder)
            if (!dir.exists() || !dir.isDirectory) {
                throw IllegalArgumentException("Invalid model folder: $folder")
            }
        }

        val request = PushModelRequest(
            model = model,
            path = path,
            insecure = insecure,
            stream = stream
        )

        val json = Json { ignoreUnknownKeys = true }

        retryWithBackoff(times = retries) {
            val response = httpClient.post("$baseUrl/api/push") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val channel = response.bodyAsChannel()

            while (!channel.isClosedForRead && currentCoroutineContext().isActive) {
                val line = channel.readLine() ?: break
                if (line.isBlank()) continue

                try {
                    val progress = json.decodeFromString<PushModelProgress>(line)
                    emit(progress)
                } catch (e: Exception) {
                    println("Failed to parse progress line: $line -> ${e.message}")
                }
            }
        }
    }

    suspend fun ollamaVersion(): VersionResponse {
        return httpClient
            .get("$baseUrl/api/version")
            .body()
    }


    fun chatStream(
        model: String,
        messages: List<ChatMessage>,
        tools: List<ChatTool>? = null,
        options: ChatOptions? = null,
        stream: Boolean = true,
        think: ThinkOption? = null,
        keepAlive: String? = null,
        logprobs: Boolean? = null,
        topLogprobs: Int? = null
    ): Flow<ChatResponse> = flow {
        val request = ChatRequest(
            model = model,
            messages = messages,
            tools = tools,
            options = options,
            stream = stream,
            think = think,
            keepAlive = keepAlive,
            logprobs = logprobs,
            topLogprobs = topLogprobs
        )

        val response = httpClient.post("$baseUrl/api/chat") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        val channel = response.bodyAsChannel()
        val json = Json { ignoreUnknownKeys = true }

        while (!channel.isClosedForRead && currentCoroutineContext().isActive) {
            val line = channel.readLine() ?: break
            if (line.isBlank()) continue

            try {
                val chatResp = json.decodeFromString<ChatResponse>(line)
                emit(chatResp)
            } catch (e: Exception) {
                println("Failed to parse chat line: $line -> ${e.message}")
            }
        }
    }

    suspend fun chat(
        model: String,
        messages: List<ChatMessage>,
        tools: List<ChatTool>? = null,
        options: ChatOptions? = null,
        stream: Boolean = true,
        think: ThinkOption? = null,
        keepAlive: String? = null,
        logprobs: Boolean? = null,
        topLogprobs: Int? = null
    ): ChatResponse {
        val request = ChatRequest(
            model = model,
            messages = messages,
            tools = tools,
            options = options,
            stream = stream,
            think = think,
            keepAlive = keepAlive,
            logprobs = logprobs,
            topLogprobs = topLogprobs
        )

        return httpClient.post("$baseUrl/api/chat") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }


    suspend fun listModels(): ListModelsResponse {
        return httpClient
            .get("$baseUrl/api/tags")
            .body()
    }

    suspend fun showModel(name: String, verbose: Boolean = false): ShowModelResponse {
        return httpClient.post("$baseUrl/api/show") {
            contentType(ContentType.Application.Json)
            setBody(ShowModelRequest(name, verbose))
        }.body()
    }

    suspend fun listRunningModels(): ListRunningModelsResponse {
        return httpClient
            .get("$baseUrl/api/ps")
            .body()
    }

    suspend fun embed(
        model: String,
        input: String
    ): EmbedResponse {
        return httpClient.post("$baseUrl/api/embed") {
            contentType(ContentType.Application.Json)
            setBody(EmbedRequest(model, input))
        }.body()
    }

    suspend fun createModel(
        model: String,
        from: String? = null,
        template: String? = null,
        license: String? = null,
        system: String? = null,
        parameters: Map<String, Any>? = null,
        messages: List<ChatMessage>? = null
    ): Boolean {
        val request = CreateModelRequest(
            model = model,
            from = from,
            template = template,
            license = license,
            system = system,
            parameters = parameters?.let { map ->
                JsonObject(
                    map.mapValues { (_, _) ->
                        JsonPrimitive(value = false)
//                            when (v) {
//                                is Number -> v
//                                is Boolean -> v
//                                else -> v.toString()
                    }
                )
            },
            messages = messages
        )

        val response = httpClient.post("$baseUrl/api/create") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return response.status.value in 200..299 && response.bodyAsText().contains("success")
    }

    suspend fun copyModel(
        source: String,
        destination: String,
        basePath: String? = null  // Optional dedicated path on disk
    ): Boolean {
        // Handle local path
        basePath?.let { path ->
            val dir = File(path)
            if (!dir.exists()) {
                val created = dir.mkdirs()
                if (!created) {
                    throw IllegalStateException("Failed to create directory: $path")
                }
            }
        }

        // Prepare request
        val request = CopyModelRequest(
            source = source,
            destination = destination
        )

        // Call Ollama API
        val response = httpClient.post("$baseUrl/api/copy") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        return response.status.value in 200..299
    }


    suspend fun deleteModel(name: String): Boolean {
        val response = httpClient.delete("$baseUrl/api/delete") {
            contentType(ContentType.Application.Json)
            setBody(DeleteModelRequest(name))
        }
        return response.status.value in 200..299
    }

}