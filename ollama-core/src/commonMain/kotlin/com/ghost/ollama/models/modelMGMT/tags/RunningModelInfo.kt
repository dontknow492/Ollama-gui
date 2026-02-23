package com.ghost.ollama.models.modelMGMT.tags

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RunningModelInfo(
    val name: String,
    val model: String,
    val size: Long,
    val digest: String,

    val details: ModelDetails? = null,

    @SerialName("expires_at")
    val expiresAt: String? = null,

    @SerialName("size_vram")
    val sizeVram: Long? = null,

    @SerialName("context_length")
    val contextLength: Int? = null
)


