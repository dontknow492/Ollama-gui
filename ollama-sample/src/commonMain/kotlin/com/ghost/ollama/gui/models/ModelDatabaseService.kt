package com.ghost.ollama.gui.models

import com.ghost.ollama.gui.EntityQueries

class ModelDatabaseService(private val queries: EntityQueries) {

    /**
     * Takes the parsed JSON data and persists it to SQLite.
     */
    fun saveLibraryToDb(models: List<DownloadOllamaModel>) {
        queries.transaction {
            models.forEach { model ->
                // 1. Save main metadata
                queries.insertModel(
                    slug = model.slug,
                    name = model.name,
                    description = model.description,
                    pullCount = model.pullCount,
                    updated = model.updated,
                    readme = model.readme,
                    capabilities = model.capabilities
                )


                // 2. Save technical tags (sizes, context windows)
                model.tags.forEach { tag ->
                    queries.insertTag(
                        modelSlug = model.slug,
                        tag = tag.tag,
                        size = tag.size,
                        contextWindow = tag.contextWindow,
                        inputType = tag.inputType
                    )
                }
            }
        }
    }
}