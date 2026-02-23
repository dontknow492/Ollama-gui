package com.ghost.ollama.models.common

import kotlinx.serialization.Serializable

@Serializable
data class TopLogProb(
    val token: String,
    val logprob: Float,
    val bytes: List<Int>? = null
)