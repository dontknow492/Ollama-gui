package com.ghost.ollama.exception

/**
 * Thrown when the requested model is not found (HTTP 404).
 */
class ModelNotFoundException(
    val modelName: String,
    cause: Throwable? = null
) : OllamaException(
    message = "Model '$modelName' was not found",
    cause = cause
)