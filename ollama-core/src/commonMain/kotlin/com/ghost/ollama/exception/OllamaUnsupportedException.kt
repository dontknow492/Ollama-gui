package com.ghost.ollama.exception

/**
 * Thrown when an operation is not supported by the specific model
 * (e.g., calling /api/embed on a non-embedding model).
 */
class OllamaUnsupportedException(
    message: String,
    cause: Throwable? = null
) : OllamaException(message, cause)