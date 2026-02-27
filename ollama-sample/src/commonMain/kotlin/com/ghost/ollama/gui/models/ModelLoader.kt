package com.ghost.ollama.gui.models

import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json
import ollama_kmp.ollama_sample.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

object ModelLoader {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @OptIn(ExperimentalResourceApi::class)
    suspend fun loadLibraryFromAssets(): List<DownloadOllamaModel> {
        return try {
            // "Res.readBytes" is the common way in Compose Multiplatform
            // to access files in the composeResources/files folder
            val bytes = Res.readBytes("files/ollama_detailed_library.json")
            val jsonString = bytes.decodeToString()
            json.decodeFromString<List<DownloadOllamaModel>>(jsonString)
        } catch (e: Exception) {
            Napier.e("Error while loading library from the assets!", e)
            emptyList()
        }
    }
}