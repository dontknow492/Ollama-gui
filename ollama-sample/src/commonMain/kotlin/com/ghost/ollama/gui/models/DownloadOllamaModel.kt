package com.ghost.ollama.gui.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DownloadOllamaModel(

    @SerialName("slug") var slug: String,
    @SerialName("name") var name: String,
    @SerialName("description") var description: String,
    @SerialName("pull_count") var pullCount: String,
    @SerialName("updated") var updated: String,
    @SerialName("capabilities") var capabilities: List<String> = listOf(),
    @SerialName("tags") var tags: List<DownloadModelTags> = listOf(),
    @SerialName("readme") var readme: String

)


