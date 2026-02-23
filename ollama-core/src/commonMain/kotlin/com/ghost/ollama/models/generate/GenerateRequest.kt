package com.ghost.ollama.models.generate

import com.ghost.ollama.models.chat.ChatOptions
import com.ghost.ollama.models.common.ThinkOption
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GenerateRequest(
    val model: String,
    val prompt: String,
    val suffix: String? = null,
    val images: List<String>? = null,
    val format: String? = null, // Missing field from your snippet
    val system: String? = null,
    val options: ChatOptions? = null,
    val stream: Boolean = true,
    val think: ThinkOption? = null,
    @SerialName("keep_alive")
    val keepAlive: String? = null,
    val raw: Boolean? = null,
    val logprobs: Boolean? = null,
    @SerialName("top_logprobs")
    val topLogprobs: Int? = null
)