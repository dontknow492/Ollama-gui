package com.ghost.ollama.gui.viewmodel.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.ghost.ollama.enum.PullStatus
import com.ghost.ollama.gui.ModelEntity
import com.ghost.ollama.gui.models.DatabasePopulator
import com.ghost.ollama.gui.repository.DownloadModelRepository
import com.ghost.ollama.gui.repository.ModelWithTags
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException


class DownloadViewModel(
    private val repository: DownloadModelRepository,
    private val databasePopulator: DatabasePopulator,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _capabilityFilter = MutableStateFlow<String?>(null)
    private val _activeDownloads = MutableStateFlow<Map<String, ActiveDownload>>(emptyMap())
    private val _selectedModel = MutableStateFlow<ModelWithTags?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagedModels = combine(
        _searchQuery,
        _capabilityFilter
    ) { search, capability ->
        search to capability
    }
        .flatMapLatest { (search, capability) ->
            repository.getModelsPaged(search, capability)
        }
        .cachedIn(viewModelScope)



    // Combine states into one UI state
    val state: StateFlow<DownloadUiState> = combine(
        _searchQuery,
        _capabilityFilter,
        _activeDownloads,
        _selectedModel
    ) { search, capability, downloads, selectedModel ->

        DownloadUiState(
            searchQuery = search,
            activeCapability = capability,
            activeDownloads = downloads,
            selectedModel = selectedModel
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DownloadUiState()
        )

    private val _sideEffects = Channel<DownloadSideEffect>(Channel.BUFFERED)
    val sideEffects = _sideEffects.receiveAsFlow()

    // Track active jobs to allow Pause/Cancel
    private val downloadJobs = mutableMapOf<String, Job>()

    init {
        // Automatically try to populate the database when the ViewModel is created
        viewModelScope.launch {
            try {
                databasePopulator.populateIfEmpty()
            } catch (e: Exception) {
                _sideEffects.send(DownloadSideEffect.ShowError("Failed to fetch models: ${e.message}"))
            }
        }
    }

    fun onEvent(event: DownloadEvent) {
        when (event) {
            is DownloadEvent.SearchQueryChanged -> _searchQuery.value = event.query
            is DownloadEvent.CapabilityFilterChanged -> _capabilityFilter.value = event.capability
            is DownloadEvent.ModelSelected -> {
                viewModelScope.launch {
                    if (event.model != null) {
                        _selectedModel.value = repository.getTagsForModel(event.model)
                    } else {
                        _selectedModel.value = null
                    }
                }
            }

            is DownloadEvent.StartDownload -> startDownload(event.tag, event.modelEntity)
            is DownloadEvent.ResumeDownload -> resumeDownload(event.tag)
            is DownloadEvent.PauseDownload -> pauseDownload(event.tag)
            is DownloadEvent.CancelDownload -> cancelDownload(event.tag)
            is DownloadEvent.DismissDownload -> dismissDownload(event.tag)
        }
    }

    private fun startDownload(tag: String, modelEntity: ModelEntity) {
        if (downloadJobs[tag]?.isActive == true) return

        _activeDownloads.update { it + (tag to ActiveDownload(tag, modelEntity, PullStatus.pulling)) }

        val job = viewModelScope.launch {
            try {
                repository.pullModel(tag).collect { progressUpdate ->
                    _activeDownloads.update { currentMap ->
                        val existing = currentMap[tag] ?: return@update currentMap
                        val updated = existing.copy(
                            status = progressUpdate.status,
                            progress = progressUpdate.progress ?: existing.progress,
                            message = progressUpdate.message
                        )
                        currentMap + (tag to updated)
                    }
                }

                _activeDownloads.update { currentMap ->
                    val existing = currentMap[tag] ?: return@update currentMap
                    currentMap + (tag to existing.copy(status = PullStatus.done, progress = 1f, message = "Done"))
                }


                _sideEffects.send(DownloadSideEffect.ShowSuccess("Successfully downloaded $tag"))

            } catch (e: CancellationException) {
                // Expected when user clicks 'Pause' or 'Cancel'
                throw e
            } catch (e: Exception) {
                // Network error handling
                _activeDownloads.update { currentMap ->
                    val existing = currentMap[tag] ?: return@update currentMap
                    currentMap + (tag to existing.copy(status = PullStatus.error, message = e.message))
                }
                _sideEffects.send(DownloadSideEffect.ShowError("Download failed: ${e.message}"))
            } finally {
                downloadJobs.remove(tag)
            }
        }

        downloadJobs[tag] = job
    }

    private fun resumeDownload(tag: String) {
        // We already have the ModelEntity in our state, so we reuse it
        _activeDownloads.value[tag]?.modelEntity?.let { startDownload(tag, it) }
    }

    private fun pauseDownload(tag: String) {
        downloadJobs[tag]?.cancel()
        downloadJobs.remove(tag)
        _activeDownloads.update { currentMap ->
            val existing = currentMap[tag] ?: return@update currentMap
            currentMap + (tag to existing.copy(status = PullStatus.queued, message = "Paused"))
        }
    }

    private fun cancelDownload(tag: String) {
        downloadJobs[tag]?.cancel()
        downloadJobs.remove(tag)
        _activeDownloads.update { it - tag }
    }

    private fun dismissDownload(tag: String) {
        _activeDownloads.update { it - tag }
    }
}
