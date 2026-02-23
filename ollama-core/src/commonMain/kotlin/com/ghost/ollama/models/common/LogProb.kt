package com.ghost.ollama.models.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LogProb(
    val token: String,
    val logprob: Float,
    val bytes: List<Int>? = null,
    @SerialName("top_logprobs") val topLogprobs: List<TopLogProb>? = null
)