package com.ghost.ollama.exception

/**
 * Thrown when a low-level network error occurs (e.g., ConnectException, IOException).
 */
class OllamaNetworkException(
    message: String = "Network error occurred while calling Ollama API",
    cause: Throwable? = null
) : OllamaException(message, cause)