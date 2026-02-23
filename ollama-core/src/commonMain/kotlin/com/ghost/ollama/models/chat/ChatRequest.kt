package com.ghost.ollama.models.chat

import com.ghost.ollama.enum.ResponseFormat
import com.ghost.ollama.enum.ToolType
import com.ghost.ollama.models.common.ThinkOption
import com.ghost.ollama.models.common.ToolFunction
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val tools: List<ChatTool>? = null,
    val options: ChatOptions? = null,
    val stream: Boolean = true,
    val think: ThinkOption? = null,
    @SerialName("keep_alive")
    val keepAlive: String? = null,
    val logprobs: Boolean? = null,
    @SerialName("top_logprobs")
    val topLogprobs: Int? = null
)

@Serializable
data class ChatTool(
    val type: ToolType = ToolType.FUNCTION,
    val function: ToolFunction
)

@Serializable
data class ChatOptions(
    val seed: Int? = null,
    val temperature: Float? = null,
    @SerialName("top_k")
    val topK: Int? = null,
    @SerialName("top_p")
    val topP: Float? = null,
    @SerialName("min_p")
    val minP: Float? = null,
    val stop: String? = null,
    @SerialName("num_ctx")
    val numCtx: Int? = null,
    @SerialName("num_predict")
    val numPredict: Int? = null,
    val format: ResponseFormat? = null
)




