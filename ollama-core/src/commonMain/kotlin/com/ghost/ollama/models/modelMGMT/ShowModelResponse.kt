package com.ghost.ollama.models.modelMGMT

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement


@Serializable
data class ShowModelResponse(

    val parameters: String? = null,
    val license: String? = null,
    val capabilities: List<String>? = null,

    @SerialName("modified_at")
    val modifiedAt: String? = null,

    val details: ShowModelDetails? = null,

    @SerialName("model_info")
    val modelInfo: Map<String, JsonElement>? = null,
)

