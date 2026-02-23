package com.ghost.ollama.models.modelMGMT.tags

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModelInfo(
    val name: String,
    val model: String,

    @SerialName("modified_at")
    val modifiedAt: String,

    val size: Long,
    val digest: String,

    val details: ModelDetails? = null
)


