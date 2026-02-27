package com.ghost.ollama.gui.repository

import app.cash.paging.PagingData
import app.cash.sqldelight.paging3.QueryPagingSource
import com.ghost.ollama.OllamaClient
import com.ghost.ollama.PullModelProgress
import com.ghost.ollama.gui.EntityQueries
import com.ghost.ollama.gui.ModelEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart


class DownloadModelRepository(
    private val ollamaClient: OllamaClient,
    private val entityQueries: EntityQueries,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    fun getModelsPaged(
        searchQuery: String = "",
        capability: String? = null
    ): Flow<PagingData<ModelEntity>> {
        // Prepare the search string for SQL 'LIKE'
        val formattedSearch = "%$searchQuery%"

        return app.cash.paging.Pager(
            config = app.cash.paging.PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = {
                QueryPagingSource(
                    countQuery = entityQueries.countFilteredModels(formattedSearch, capability),
                    transacter = entityQueries,
                    context = Dispatchers.IO,
                    queryProvider = { limit, offset ->
                        entityQueries.selectModelsFilteredPaged(
                            searchQuery = formattedSearch,
                            capabilityFilter = capability,
                            limit = limit,
                            offset = offset
                        )
                    }
                )
            }
        ).flow
    }

    /**
     * Returns the flow directly so the ViewModel can observe progress.
     * We also wrap it to handle side effects like updating the DB.
     */
    fun pullModel(name: String): Flow<PullModelProgress> {
        return ollamaClient.pullModel(name)
            .onStart {
                // Optional: Mark model as "Downloading" in DB
                // queries.updateDownloadStatus(name, isDownloading = true)
            }
            .onCompletion { cause ->
                // Optional: Mark as "Downloaded" or "Error"
                // if (cause == null) queries.updateDownloadStatus(name, isDownloaded = true)
            }.flowOn(ioDispatcher)
    }


    /**
     * Helper to get tags for a specific model.
     */
    fun getTagsForModel(model: ModelEntity): ModelWithTags {
        val tags = entityQueries.getTagsForModel(model.slug).executeAsList()
        return ModelWithTags(model, tags)

    }

}