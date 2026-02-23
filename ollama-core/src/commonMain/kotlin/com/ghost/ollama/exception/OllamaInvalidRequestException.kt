package com.ghost.ollama.exception

/**
 * Thrown when the request is invalid or malformed (HTTP 400).
 */
class OllamaInvalidRequestException(
    message: String,
    cause: Throwable? = null
) : OllamaException(message, cause)