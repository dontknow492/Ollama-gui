package com.ghost.ollama.models.modelMGMT

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShowModelDetails(
    @SerialName("parent_model")
    val parentModel: String? = null,

    val format: String? = null,
    val family: String? = null,
    val families: List<String>? = null,

    @SerialName("parameter_size")
    val parameterSize: String? = null,

    @SerialName("quantization_level")
    val quantizationLevel: String? = null
)

