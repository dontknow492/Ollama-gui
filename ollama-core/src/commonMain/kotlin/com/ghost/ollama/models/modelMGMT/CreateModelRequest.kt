package com.ghost.ollama.models.modelMGMT

import com.ghost.ollama.models.chat.ChatMessage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class CreateModelRequest(
    val model: String,
    val from: String? = null,
    val template: String? = null,
    val license: String? = null,
    val system: String? = null,
    val parameters: JsonObject? = null,
    val messages: List<ChatMessage>? = null
)