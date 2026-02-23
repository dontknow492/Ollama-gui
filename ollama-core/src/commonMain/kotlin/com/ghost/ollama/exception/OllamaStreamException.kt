package com.ghost.ollama.exception

/**
 * Thrown when an error occurs while reading a stream from the Ollama API.
 */
class OllamaStreamException(
    message: String = "Error occurred while streaming response",
    cause: Throwable? = null
) : OllamaException(message, cause)