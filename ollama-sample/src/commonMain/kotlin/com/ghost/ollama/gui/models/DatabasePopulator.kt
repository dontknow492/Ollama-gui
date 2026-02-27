package com.ghost.ollama.gui.models

import com.ghost.ollama.gui.EntityQueries

class DatabasePopulator(
    private val modelLoader: ModelLoader,
    private val databaseService: ModelDatabaseService,
    private val queries: EntityQueries
) {
    suspend fun populateIfEmpty() {
        // Check if we already have models to avoid redundant work
        val count = queries.countModels().executeAsOne()

        if (count == 0L) {
            println("[DB] Library is empty. Populating from JSON...")
            val models = modelLoader.loadLibraryFromAssets()
            if (models.isNotEmpty()) {
                databaseService.saveLibraryToDb(models)
                println("[DB] Successfully populated ${models.size} models.")
            }
        } else {
            println("[DB] Library already contains $count models. Skipping population.")
        }
    }
}


