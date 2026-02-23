package com.ghost.ollama.exception

/**
 * Thrown when the response from the Ollama API cannot be serialized or deserialized.
 */
class OllamaSerializationException(
    message: String = "Failed to serialize or deserialize Ollama response",
    cause: Throwable? = null
) : OllamaException(message, cause)