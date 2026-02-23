package com.ghost.ollama

import com.ghost.ollama.enum.PullStatus
import com.ghost.ollama.enum.PushStatus
import com.ghost.ollama.exception.*
import com.ghost.ollama.exceptions.OllamaErrorResponse
import com.ghost.ollama.models.VersionResponse
import com.ghost.ollama.models.chat.*
import com.ghost.ollama.models.common.ThinkOption
import com.ghost.ollama.models.embed.EmbedRequest
import com.ghost.ollama.models.embed.EmbedResponse
import com.ghost.ollama.models.generate.GenerateRequest
import com.ghost.ollama.models.generate.GenerateResponse
import com.ghost.ollama.models.modelMGMT.CreateModelRequest
import com.ghost.ollama.models.modelMGMT.DeleteModelRequest
import com.ghost.ollama.models.modelMGMT.ShowModelRequest
import com.ghost.ollama.models.modelMGMT.ShowModelResponse
import com.ghost.ollama.models.modelMGMT.tags.ListModelsResponse
import com.ghost.ollama.models.modelMGMT.tags.ListRunningModelsResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import java.net.ConnectException


class OllamaClient(
    private val baseUrl: String = "http://localhost:11434",
    private val httpClient: HttpClient
) {
    private val jsonSerializer = Json { ignoreUnknownKeys = true }

    /**
     * Retries the block but immediately rethrows if the exception is a Client Request error (4xx)
     * or a specific Ollama error that shouldn't be retried.
     */
    private suspend fun <T> retryWithBackoff(
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
                // Do not retry on 4xx errors (e.g. Invalid Request, Model Not Found)
                if (e is OllamaInvalidRequestException ||
                    e is ModelNotFoundException ||
                    e is OllamaSerializationException
                ) {
                    throw e
                }

                // Do not retry on generic ClientRequestException (Ktor 4xx)
                if (e is ClientRequestException) {
                    throw mapToOllamaException(e)
                }

                println("Error occurred: ${e.message}. Retrying in $currentDelay ms...")
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong()
        }
        return block() // Last attempt; let it throw if it fails
    }

    /**
     * Wraps API calls to handle Ktor exceptions and map them to OllamaExceptions.
     */
    private suspend inline fun <reified T> safeApiCall(crossinline apiCall: suspend () -> HttpResponse): T {
        try {
            val response = apiCall()
            ensureSuccess(response)
            return response.body()
        } catch (e: Exception) {
            throw mapToOllamaException(e)
        }
    }

    /**
     * Checks HTTP status and throws appropriate custom exceptions for non-2xx codes.
     */
    private suspend fun ensureSuccess(response: HttpResponse) {
        if (response.status.isSuccess()) return

        val errorBody = try {
            response.bodyAsText()
        } catch (_: Exception) {
            null
        }

        // Attempt to parse the structured error from Ollama
        val ollamaError = errorBody?.let {
            try {
                jsonSerializer.decodeFromString<OllamaErrorResponse>(it)
            } catch (_: Exception) {
                null
            }
        }

        val errorMessage = ollamaError?.error ?: errorBody ?: "Unknown error"

        when {
            // Specifically catch the "unsupported" message from the parsed data class
            errorMessage.contains("does not support", ignoreCase = true) -> {
                throw OllamaUnsupportedException(errorMessage)
            }

            response.status == HttpStatusCode.NotFound -> {
                throw ModelNotFoundException(parseErrorModelName(errorBody ?: "") ?: "Model not found")
            }

            response.status == HttpStatusCode.BadRequest -> {
                throw OllamaInvalidRequestException(errorMessage)
            }

            else -> throw OllamaHttpException(response.status.value, errorMessage)
        }
    }

    private fun parseErrorModelName(errorBody: String): String? {
        // Simple heuristic to try and extract model name from error JSON if available
        return try {
            val json = jsonSerializer.parseToJsonElement(errorBody) as? JsonObject
            json?.get("error")?.toString()
        } catch (e: Exception) {
            null
        }
    }

    private fun mapToOllamaException(e: Throwable): OllamaException {
        return when (e) {
            is OllamaException -> e // Pass through if already wrapped
            is ClientRequestException -> {
                if (e.response.status == HttpStatusCode.NotFound) ModelNotFoundException("Model not found")
                else OllamaInvalidRequestException(e.message)
            }

            is ServerResponseException -> OllamaHttpException(e.response.status.value, e.message)
            is HttpRequestTimeoutException, is SocketTimeoutException -> OllamaTimeoutException("Request timed out", e)
            is ConnectException, is IOException -> OllamaNetworkException("Network error: ${e.message}", e)
            is SerializationException -> OllamaSerializationException("Failed to parse response", e)
            else -> OllamaException("Unexpected error: ${e.message}", e)
        }
    }

    // -------------------------------------------------------------------------
    // API METHODS
    // -------------------------------------------------------------------------

    /**
     * Pulls a specified model from the repository.
     *
     * This function retrieves the model based on the specified parameters and handles optional
     * configuration for security and streaming options. It also supports retries in case of failure.
     *
     * @param model The name of the model to be pulled. This parameter is required.
     * @param insecure If set to `true`, allows pulling the model over insecure channels. Defaults to `false`.
     * @param stream Determines whether the model should be pulled as a stream. Defaults to `true`.
     * @param retries The number of times to retry pulling the model in case of failure. Defaults to `3`.
     *
     * @throws IOException When there are network issues or the model is unavailable.
     * @throws IllegalArgumentException If the specified model name is empty or invalid.
     *
     * @example
     * try {
     *     pullModel(model = "exampleModel", insecure = true, stream = false, retries = 5)
     *     println("Model pulled successfully.")
     * } catch (e: IOException) {
     *     println("Failed to pull model: ${e.message}")
     * } catch (e: IllegalArgumentException) {
     *     println("Invalid model name: ${e.message}")
     * }
     *
     * Note:
     * - Ensure the model name is valid and accessible.
     * - Review security settings based on the environment requirements.
     * - Adjust retry count as needed to balance responsiveness and robustness.
     */
    fun pullModel(
        model: String,
        insecure: Boolean = false,
        stream: Boolean = true,
        retries: Int = 3
    ): Flow<PullModelProgress> = flow {
        val request = PullModelRequest(model, insecure, stream)

        retryWithBackoff(times = retries) {
            try {
                val response = httpClient.post("$baseUrl/api/pull") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                ensureSuccess(response)

                val channel = response.bodyAsChannel()
                while (!channel.isClosedForRead && currentCoroutineContext().isActive) {
                    val line = channel.readLine() ?: break
                    if (line.isBlank()) continue

                    try {
                        val progress = jsonSerializer.decodeFromString<PullModelProgress>(line)
                        emit(progress)
                    } catch (e: SerializationException) {
                        throw OllamaSerializationException("Failed to parse pull progress: $line", e)
                    }
                }
            } catch (e: Exception) {
                throw mapToOllamaException(e)
            }
        }
    }

    /**
     * Push a specified model to the repository.
     *
     * This function retrieves the model based on the specified parameters and handles optional
     * configuration for security and streaming options. It also supports retries in case of failure.
     *
     * @param model The name of the model to be pulled. This parameter is required.
     * @param insecure If set to `true`, allows pulling the model over insecure channels. Defaults to `false`.
     * @param stream Determines whether the model should be pulled as a stream. Defaults to `true`.
     * @param retries The number of times to retry pulling the model in case of failure. Defaults to `3`.
     *
     * @throws IOException When there are network issues or the model is unavailable.
     * @throws IllegalArgumentException If the specified model name is empty or invalid.
     *
     * @example
     * try {
     *     pullModel(model = "exampleModel", insecure = true, stream = false, retries = 5)
     *     println("Model pulled successfully.")
     * } catch (e: IOException) {
     *     println("Failed to pull model: ${e.message}")
     * } catch (e: IllegalArgumentException) {
     *     println("Invalid model name: ${e.message}")
     * }
     *
     * Note:
     * - Ensure the model name is valid and accessible.
     * - Review security settings based on the environment requirements.
     * - Adjust retry count as needed to balance responsiveness and robustness.
     */
    fun pushModel(
        model: String,
        path: String? = null,
        insecure: Boolean = false,
        stream: Boolean = true,
        retries: Int = 3
    ): Flow<PushModelProgress> = flow {
        path?.let { folder ->
            val dir = File(folder)
            if (!dir.exists() || !dir.isDirectory) {
                throw OllamaInvalidRequestException("Invalid local model path: $folder")
            }
        }

        val request = PushModelRequest(model, path, insecure, stream)

        retryWithBackoff(times = retries) {
            try {
                val response = httpClient.post("$baseUrl/api/push") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                ensureSuccess(response)

                val channel = response.bodyAsChannel()
                while (!channel.isClosedForRead && currentCoroutineContext().isActive) {
                    val line = channel.readLine() ?: break
                    if (line.isBlank()) continue

                    try {
                        val progress = jsonSerializer.decodeFromString<PushModelProgress>(line)
                        emit(progress)
                    } catch (e: SerializationException) {
                        throw OllamaSerializationException("Failed to parse push progress: $line", e)
                    }
                }
            } catch (e: Exception) {
                throw mapToOllamaException(e)
            }
        }
    }


    /**
     * Return ollama Version
     */
    suspend fun ollamaVersion(): VersionResponse {
        return safeApiCall {
            httpClient.get("$baseUrl/api/version")
        }
    }

    /**
     * Streams a chat using the specified model, messages, and optional parameters.
     *
     * This function initiates a chat session by sending a list of messages and can utilize various
     * tools and options to enhance the interaction. It supports keeping the session alive
     * and retrieving log probabilities if needed.
     *
     * @param model The name of the model to be used for the chat. This parameter is required.
     * @param messages A list of messages (`ChatMessage`) to be sent in the conversation. This parameter is required.
     * @param tools An optional list of chat tools (`ChatTool`) that can be utilized during the chat. Defaults to `null`.
     * @param options Optional `ChatOptions` to configure the chat behavior. Defaults to `null`.
     * @param think Optional `ThinkOption` that may influence the model's thinking process. Defaults to `null`.
     * @param keepAlive An optional string that can be used to keep the chat session alive. Defaults to `null`.
     * @param logprobs If set to `true`, retrieves log probabilities for the tokens generated. Defaults to `null`.
     * @param topLogprobs If set, specifies the number of top log probabilities to return for each generated token. Defaults to `null`.
     *
     * @throws IllegalArgumentException If the model name or messages list is invalid or empty.
     *
     * @example
     * val messages = listOf(
     *     ChatMessage(content = "Hello, how can I help you?"),
     *     ChatMessage(content = "I need assistance with my order.")
     * )
     * try {
     *     chatStream(
     *         model = "myChatModel",
     *         messages = messages,
     *         options = ChatOptions(seed = 42, temperature = 0.5f)
     *     )
     * } catch (e: IllegalArgumentException) {
     *     println("Invalid input: ${e.message}")
     * }
     *
     * Note:
     * - Ensure the model name is valid and accessible.
     * - Messages should be coherent to maintain context throughout the chat session.
     * - Review options and parameters based on your specific use case.
     */
    fun chatStream(
        model: String,
        messages: List<ChatMessage>,
        tools: List<ChatTool>? = null,
        options: ChatOptions? = null,
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
            stream = true,
            think = think,
            keepAlive = keepAlive,
            logprobs = logprobs,
            topLogprobs = topLogprobs
        )

        try {
            val response = httpClient.post("$baseUrl/api/chat") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            ensureSuccess(response)

            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead && currentCoroutineContext().isActive) {
                val line = channel.readLine() ?: break
                if (line.isBlank()) continue

                try {
                    val chatResp = jsonSerializer.decodeFromString<ChatResponse>(line)
                    emit(chatResp)
                } catch (e: SerializationException) {
                    throw OllamaSerializationException("Failed to parse chat response: $line", e)
                }
            }
        } catch (e: Exception) {
            throw mapToOllamaException(e)
        }
    }


    /**
     * Initiates a chat session with the specified model asynchronously.
     *
     * This suspend function sends a list of messages to the model and returns a response.
     * It supports optional tools, configuration options, and additional parameters to modify the behavior
     * of the chat session.
     *
     * @param model The name of the model to be used for the chat. This parameter is required.
     * @param messages A list of messages (`ChatMessage`) to be sent in the conversation. This parameter is required.
     * @param tools An optional list of chat tools (`ChatTool`) that can be utilized during the chat. Defaults to `null`.
     * @param options Optional `ChatOptions` to configure the chat behavior. Defaults to `null`.
     * @param think Optional `ThinkOption` that may influence the model's thinking process. Defaults to `null`.
     * @param keepAlive An optional string that can be used to keep the chat session alive. Defaults to `null`.
     * @param logprobs If set to `true`, retrieves log probabilities for the tokens generated. Defaults to `null`.
     * @param topLogprobs If set, specifies the number of top log probabilities to return for each generated token. Defaults to `null`.
     *
     * @return A `ChatResponse` object containing the response from the chat model.
     *
     * @throws IllegalArgumentException If the model name or messages list is invalid or empty.
     * @throws IOException If there are issues with the network or model availability during the request.
     *
     * @example
     * val messages = listOf(
     *     ChatMessage(content = "Hello, how are you?"),
     *     ChatMessage(content = "Can you assist me?")
     * )
     * try {
     *     val response = chat(
     *         model = "myChatModel",
     *         messages = messages,
     *         options = ChatOptions(seed = 42, temperature = 0.7f)
     *     )
     *     println("Response: ${response.content}")
     * } catch (e: Exception) {
     *     println("An error occurred: ${e.message}")
     * }
     *
     * Note:
     * - Ensure the model name is valid and accessible.
     * - Messages should be coherent to maintain context throughout the chat session.
     * - Consider optional parameters based on your specific needs.
     */
    suspend fun chat(
        model: String,
        messages: List<ChatMessage>,
        tools: List<ChatTool>? = null,
        options: ChatOptions? = null,
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
            stream = false,
            think = think,
            keepAlive = keepAlive,
            logprobs = logprobs,
            topLogprobs = topLogprobs
        )

        return safeApiCall {
            httpClient.post("$baseUrl/api/chat") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }


    /**
     * Generates text or images based on the provided prompt asynchronously.
     *
     * This suspend function utilizes the specified model to create output based on a prompt and optional parameters.
     * It supports various configurations to tailor the generation process, including options for suffix,
     * formatting, and retrieval of log probabilities.
     *
     * @param model The name of the model to be used for generation. This parameter is required.
     * @param prompt The input text or question for which to generate a response. This parameter is required.
     * @param suffix An optional string to append to the generated output. Defaults to `null`.
     * @param images An optional list of image URLs or identifiers to include in the generation process. Defaults to `null`.
     * @param format An optional string defining the desired output format. Defaults to `null`.
     * @param system An optional string for system instructions or context. Defaults to `null`.
     * @param options Optional `ChatOptions` to configure the generation behavior. Defaults to `null`.
     * @param stream If set to `true`, enables streaming results as they are generated. Defaults to `false`.
     * @param raw If set to `true`, returns raw output without formatting. Defaults to `null`.
     * @param think Optional `ThinkOption` that may influence the model's thinking process. Defaults to `null`.
     * @param keepAlive An optional string that can be used to keep the generation session alive. Defaults to `null`.
     * @param logprobs If set to `true`, retrieves log probabilities for the generated tokens. Defaults to `null`.
     * @param topLogprobs If set, specifies the number of top log probabilities to return for each generated token. Defaults to `null`.
     *
     * @return A `GenerateResponse` object containing the output from the model generation.
     *
     * @throws IllegalArgumentException If the model name, prompt, or options are invalid.
     * @throws IOException If there are issues with the network or model availability during the request.
     *
     * @example
     * try {
     *     val response = generate(
     *         model = "textGeneratorModel",
     *         prompt = "What is the meaning of life?",
     *         suffix = "In a philosophical context.",
     *         options = ChatOptions(seed = 42)
     *     )
     *     println("Generated Response: ${response.content}")
     * } catch (e: Exception) {
     *     println("An error occurred: ${e.message}")
     * }
     *
     * Note:
     * - Ensure the model name is valid and accessible.
     * - The prompt should be clear and coherent to obtain meaningful results.
     * - Adjust optional parameters based on specific generation needs.
     */
    suspend fun generate(
        model: String,
        prompt: String,
        suffix: String? = null,
        images: List<String>? = null,
        format: String? = null,
        system: String? = null,
        options: ChatOptions? = null,
        raw: Boolean? = null,
        think: ThinkOption? = null,
        keepAlive: String? = null,
        logprobs: Boolean? = null,
        topLogprobs: Int? = null
    ): GenerateResponse {
        val request = GenerateRequest(
            model = model,
            prompt = prompt,
            suffix = suffix,
            images = images,
            format = format,
            options = options,
            system = system,
            stream = false,
            raw = raw,
            keepAlive = keepAlive,
            think = think,
            logprobs = logprobs,
            topLogprobs = topLogprobs
        )

        return safeApiCall {
            httpClient.post("$baseUrl/api/generate") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }


    /**
     * Generates text or images based on the provided prompt and streams the output.
     *
     * This function utilizes the specified model to create output based on a prompt and optional parameters.
     * It streams the generated content in real-time, allowing for immediate processing of the results as they are produced.
     *
     * @param model The name of the model to be used for generation. This parameter is required.
     * @param prompt The input text or question for which to generate a response. This parameter is required.
     * @param suffix An optional string to append to the generated output. Defaults to `null`.
     * @param images An optional list of image URLs or identifiers to include in the generation process. Defaults to `null`.
     * @param format An optional string defining the desired output format. Defaults to `null`.
     * @param system An optional string for system instructions or context. Defaults to `null`.
     * @param options Optional `ChatOptions` to configure the generation behavior. Defaults to `null`.
     * @param stream If set to `true`, enables streaming results as they are generated. Defaults to `false`.
     * @param raw If set to `true`, returns raw output without formatting. Defaults to `null`.
     * @param think Optional `ThinkOption` that may influence the model's thinking process. Defaults to `null`.
     * @param keepAlive An optional string that can be used to keep the generation session alive. Defaults to `null`.
     * @param logprobs If set to `true`, retrieves log probabilities for the generated tokens. Defaults to `null`.
     * @param topLogprobs If set, specifies the number of top log probabilities to return for each generated token. Defaults to `null`.
     *
     * @return A `Flow<GenerateResponse>` that emits the output from the model generation in real-time.
     *
     * @throws IllegalArgumentException If the model name, prompt, or options are invalid.
     * @throws IOException If there are issues with the network or model availability during the request.
     *
     * @example
     * val responseFlow = generateStream(
     *     model = "textGeneratorModel",
     *     prompt = "What is the meaning of life?",
     *     suffix = "In a philosophical context."
     * )
     * responseFlow.collect { response ->
     *     println("Generated Response: ${response.content}")
     * }
     *
     * Note:
     * - Ensure the model name is valid and accessible.
     * - The prompt should be clear and coherent to obtain meaningful results.
     * - Adjust optional parameters based on specific generation needs.
     */
    fun generateStream(
        model: String,
        prompt: String,
        suffix: String? = null,
        images: List<String>? = null,
        format: String? = null,
        system: String? = null,
        options: ChatOptions? = null,
        raw: Boolean? = null,
        think: ThinkOption? = null,
        keepAlive: String? = null,
        logprobs: Boolean? = null,
        topLogprobs: Int? = null
    ): Flow<GenerateResponse> = flow {
        val request = GenerateRequest(
            model = model,
            prompt = prompt,
            suffix = suffix,
            images = images,
            format = format,
            options = options,
            system = system,
            stream = true,
            raw = raw,
            keepAlive = keepAlive,
            think = think,
            logprobs = logprobs,
            topLogprobs = topLogprobs
        )

        try {
            val response = httpClient.post("$baseUrl/api/response") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            ensureSuccess(response)

            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead && currentCoroutineContext().isActive) {
                val line = channel.readLine() ?: break
                if (line.isBlank()) continue

                try {
                    val chatResp = jsonSerializer.decodeFromString<GenerateResponse>(line)
                    emit(chatResp)
                } catch (e: SerializationException) {
                    throw OllamaSerializationException("Failed to parse chat response: $line", e)
                }
            }
        } catch (e: Exception) {
            throw mapToOllamaException(e)
        }
    }


    /**
     * Retrieves a list of available models.
     *
     * This suspend function fetches the available models from the API.
     *
     * @return A `ListModelsResponse` containing the list of models.
     *
     * @throws IOException If there are issues with the network or model availability during the request.
     *
     * @example
     * try {
     *     val models = listModels()
     *     println("Available Models: ${models.list}")
     * } catch (e: IOException) {
     *     println("Failed to retrieve models: ${e.message}")
     * }
     */
    suspend fun listModels(): ListModelsResponse {
        return safeApiCall {
            httpClient.get("$baseUrl/api/tags")
        }
    }

    /**
     * Shows information about a specified model.
     *
     * This suspend function retrieves details for a specific model based on its name.
     *
     * @param name The name of the model to show information for. This parameter is required.
     * @param verbose If set to `true`, includes additional details about the model. Defaults to `false`.
     *
     * @return A `ShowModelResponse` containing the details of the specified model.
     *
     * @throws IllegalArgumentException If the model name is empty or invalid.
     * @throws IOException If there are issues with the network or model availability during the request.
     *
     * @example
     * try {
     *     val modelInfo = showModel(name = "exampleModel", verbose = true)
     *     println("Model Info: ${modelInfo.details}")
     * } catch (e: Exception) {
     *     println("Error: ${e.message}")
     * }
     */
    suspend fun showModel(name: String, verbose: Boolean = false): ShowModelResponse {
        return safeApiCall {
            httpClient.post("$baseUrl/api/show") {
                contentType(ContentType.Application.Json)
                setBody(ShowModelRequest(name, verbose))
            }
        }
    }

    /**
     * Lists running models on the server.
     *
     * This suspend function retrieves the models that are currently running.
     *
     * @return A `ListRunningModelsResponse` containing the information about running models.
     *
     * @throws IOException If there are issues with the network or model availability during the request.
     *
     * @example
     * try {
     *     val runningModels = listRunningModels()
     *     println("Running Models: ${runningModels.models}")
     * } catch (e: IOException) {
     *     println("Failed to retrieve running models: ${e.message}")
     * }
     */
    suspend fun listRunningModels(): ListRunningModelsResponse {
        return safeApiCall {
            httpClient.get("$baseUrl/api/ps")
        }
    }

    /**
     * Embeds an input string using the specified model.
     *
     * This suspend function generates embeddings for the input string using the specified model.
     *
     * @param model The name of the model to use for embedding. This parameter is required.
     * @param input The input string to be embedded. This parameter is required.
     *
     * @return An `EmbedResponse` containing the embeddings for the input string.
     *
     * @throws IllegalArgumentException If the model name or input is empty or invalid.
     * @throws IOException If there are issues with the network or model availability during the request.
     *
     * @example
     * try {
     *     val embedding = embed(model = "textEmbedder", input = "Hello, world!")
     *     println("Embedding: ${embedding.vector}")
     * } catch (e: Exception) {
     *     println("Error: ${e.message}")
     * }
     */
    suspend fun embed(model: String, input: String): EmbedResponse {
        return safeApiCall {
            httpClient.post("$baseUrl/api/embed") {
                contentType(ContentType.Application.Json)
                setBody(EmbedRequest(model, input))
            }
        }
    }


    /**
     * Creates a new model with the specified parameters.
     *
     * This suspend function initializes a new model using the provided details. It handles
     * any configuration options and parameters necessary for model creation.
     *
     * @param model The name of the model to create. This parameter is required.
     * @param from An optional string specifying the source model to clone from. Defaults to `null`.
     * @param template An optional string specifying a template for the model. Defaults to `null`.
     * @param license An optional string specifying the licensing information. Defaults to `null`.
     * @param system An optional string for system instructions or context. Defaults to `null`.
     * @param parameters An optional map of additional parameters for model configuration. Defaults to `null`.
     * @param messages An optional list of messages (`ChatMessage`) for initial context. Defaults to `null`.
     *
     * @return True if the model is created successfully; throws an exception on failure.
     *
     * @throws OllamaInvalidRequestException If the request is invalid or the model name is already in use.
     * @throws IOException If there are issues with the network or model availability during the request.
     *
     * @example
     * try {
     *     val success = createModel(model = "newModel")
     *     println("Model created successfully: $success")
     * } catch (e: Exception) {
     *     println("Error creating model: ${e.message}")
     * }
     */
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
                JsonObject(map.mapValues { (_, v) -> JsonPrimitive(v.toString()) })
            },
            messages = messages
        )

        // Using manual try-catch block here because we need to return Boolean
        // But strictly, we should throw if it's a network error, and return false only if logic fails?
        // Actually, best practice: throw on error, return true on success.

        try {
            val response = httpClient.post("$baseUrl/api/create") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            ensureSuccess(response)

            // Validate specific success message if needed, or rely on status 200
            return response.status.isSuccess() && response.bodyAsText().contains("success", ignoreCase = true)
        } catch (e: Exception) {
            throw mapToOllamaException(e)
        }
    }


    /**
     * Copies an existing model to a new location.
     *
     * This suspend function duplicates a model from a source to a destination.
     * It optionally allows for specifying a base path for the copy.
     *
     * @param source The name of the source model to copy. This parameter is required.
     * @param destination The name of the destination model. This parameter is required.
     * @param basePath An optional base path for the new model's location. Defaults to `null`.
     *
     * @return True if the model is copied successfully; throws an exception on failure.
     *
     * @throws OllamaInvalidRequestException If the base path cannot be created or is invalid.
     * @throws IOException If there are issues with the network or model availability during the request.
     *
     * @example
     * try {
     *     val success = copyModel(source = "existingModel", destination = "newModelCopy")
     *     println("Model copied successfully: $success")
     * } catch (e: Exception) {
     *     println("Error copying model: ${e.message}")
     * }
     */
    suspend fun copyModel(
        source: String,
        destination: String,
        basePath: String? = null
    ): Boolean {
        basePath?.let { path ->
            val dir = File(path)
            if (!dir.exists()) {
                val created = dir.mkdirs()
                if (!created) {
                    throw OllamaInvalidRequestException("Failed to create directory: $path")
                }
            }
        }

        val request = CopyModelRequest(source, destination)

        try {
            val response = httpClient.post("$baseUrl/api/copy") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            ensureSuccess(response)
            return true
        } catch (e: Exception) {
            throw mapToOllamaException(e)
        }
    }


    /**
     * Deletes a specified model.
     *
     * This suspend function removes a model from the system based on the given name.
     *
     * @param name The name of the model to be deleted. This parameter is required.
     *
     * @return True if the model is deleted successfully; throws an exception on failure.
     *
     * @throws IOException If there are issues with the network or model availability during the request.
     *
     * @example
     * try {
     *     val success = deleteModel(name = "modelToDelete")
     *     println("Model deleted successfully: $success")
     * } catch (e: Exception) {
     *     println("Error deleting model: ${e.message}")
     * }
     */
    suspend fun deleteModel(name: String): Boolean {
        try {
            val response = httpClient.delete("$baseUrl/api/delete") {
                contentType(ContentType.Application.Json)
                setBody(DeleteModelRequest(name))
            }
            ensureSuccess(response)
            return true
        } catch (e: Exception) {
            throw mapToOllamaException(e)
        }
    }
}


expect fun createHttpClient(): HttpClient


@Serializable
data class CopyModelRequest(
    val source: String,
    val destination: String
)


@Serializable
data class PullModelRequest(
    val model: String,
    val insecure: Boolean? = false,
    val stream: Boolean? = true
)

@Serializable
data class PullModelProgress(
    val status: PullStatus,
    val message: String? = null,
    val progress: Float? = null
)

@Serializable
data class PushModelRequest(
    val model: String,
    val path: String? = null,
    val insecure: Boolean? = false,
    val stream: Boolean? = true
)

@Serializable
data class PushModelProgress(
    val status: PushStatus,
    val message: String? = null,
    val progress: Float? = null
)
