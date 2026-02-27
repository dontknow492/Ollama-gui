package com.ghost.ollama.gui.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DownloadModelTags(

    @SerialName("tag") var tag: String,
    @SerialName("size") var size: String,
    @SerialName("context_window") var contextWindow: String,
    @SerialName("input_type") var inputType: String

)


