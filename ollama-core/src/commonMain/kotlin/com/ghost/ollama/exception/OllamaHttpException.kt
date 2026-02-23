package com.ghost.ollama.exception

/**
 * Thrown when the Ollama API returns a non-2xx HTTP status code that isn't handled by a more specific exception.
 */
class OllamaHttpException(
    val statusCode: Int,
    val responseBody: String? = null,
    cause: Throwable? = null
) : OllamaException(
    message = "HTTP $statusCode error from Ollama API: ${responseBody ?: "No response body"}",
    cause = cause
)