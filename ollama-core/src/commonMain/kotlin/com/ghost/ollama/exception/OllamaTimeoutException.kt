package com.ghost.ollama.exception

/**
 * Thrown when a request to the Ollama API times out (HTTP 408 or client-side timeout).
 */
class OllamaTimeoutException(
    message: String = "Ollama request timed out",
    cause: Throwable? = null
) : OllamaException(message, cause)