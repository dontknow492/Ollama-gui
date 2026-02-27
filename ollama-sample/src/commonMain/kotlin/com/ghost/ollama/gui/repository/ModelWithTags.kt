package com.ghost.ollama.gui.repository

import com.ghost.ollama.gui.ModelEntity
import com.ghost.ollama.gui.TagEntity

/**
 * A combined class for your UI that actually contains the tags.
 * This is the model used in the Discovery/Download list.
 */
data class ModelWithTags(
    val model: ModelEntity,
    val tags: List<TagEntity>
)