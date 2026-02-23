package com.ghost.ollama.models.chat

import com.ghost.ollama.models.common.ToolCall
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val role: Role,
    val content: String,
    val images: List<String>? = null,
    val thinking: String? = null,
    @SerialName("tool_calls")
    val toolCalls: List<ToolCall>? = null
) {
    @Serializable
    enum class Role {
        @SerialName("system")
        SYSTEM,

        @SerialName("user")
        USER,

        @SerialName("assistant")
        ASSISTANT,

        @SerialName("tool")
        TOOL
    }
}