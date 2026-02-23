package com.ghost.ollama.exception


/**
 * Base exception for all Ollama-related errors.
 */
open class OllamaException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)


