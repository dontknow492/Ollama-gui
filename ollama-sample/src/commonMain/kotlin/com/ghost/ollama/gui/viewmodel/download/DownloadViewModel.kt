package com.ghost.ollama.gui.viewmodel.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.ghost.ollama.enum.PullStatus
import com.ghost.ollama.gui.ModelEntity
import com.ghost.ollama.gui.models.DatabasePopulator
import com.ghost.ollama.gui.repository.DownloadModelRepository
import com.ghost.ollama.gui.repository.ModelWithTags
import io.github.aakira.napier.Napier
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
                Napier.e("Failed to populate database: ${e.message}", e)
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
        Napier.d("🚀 startDownload called for tag=$tag")

        if (downloadJobs[tag]?.isActive == true) {
            Napier.d("⚠️ Download already active for $tag")
            return
        }

        _activeDownloads.update {
            Napier.d("📥 Adding $tag to activeDownloads (status=pulling)")
            it + (tag to ActiveDownload(tag, modelEntity, PullStatus.pulling))
        }

        val job = viewModelScope.launch {

            Napier.d("🧵 Coroutine launched for $tag")

            try {
                repository.pullModel(tag)
                    .onStart {
                        Napier.d("🔄 pullModel flow started for $tag")
                    }
                    .onCompletion { cause ->
                        if (cause == null) {
                            Napier.d("✅ pullModel flow completed normally for $tag")
                        } else {
                            Napier.e("❌ pullModel flow completed with error for $tag: ${cause.message}", cause)
                        }
                    }
                    .collect { progressUpdate ->

                        Napier.d(
                            """
                        📡 Progress update for $tag
                        status=${progressUpdate.status}
                        progress=${progressUpdate.progress}
                        message=${progressUpdate.message}
                        """.trimIndent()
                        )

                        _activeDownloads.update { currentMap ->
                            val existing = currentMap[tag]

                            if (existing == null) {
                                Napier.w("⚠️ No existing download entry for $tag while updating")
                                return@update currentMap
                            }

                            val updated = existing.copy(
                                status = progressUpdate.status?.toPullStatus() ?: existing.status,
                                progress = progressUpdate.progress ?: existing.progress,
                                message = progressUpdate.message,
                                total = progressUpdate.total ?: existing.total,
                                completed = progressUpdate.completed ?: existing.completed
                            )

                            Napier.d("📝 Updating state for $tag -> status=${updated.status}, progress=${updated.progress}")

                            currentMap + (tag to updated)
                        }
                    }

                Napier.d("🎉 Marking $tag as done")

                _activeDownloads.update { currentMap ->
                    val existing = currentMap[tag] ?: return@update currentMap
                    currentMap + (
                            tag to existing.copy(
                                status = PullStatus.done,
                                progress = 1f,
                                message = "Done"
                            )
                            )
                }

                Napier.d("📤 Sending success side effect for $tag")
                _sideEffects.send(
                    DownloadSideEffect.ShowSuccess("Successfully downloaded $tag")
                )

            } catch (e: CancellationException) {
                Napier.w("⏸️ Download cancelled for $tag")
                throw e
            } catch (e: Exception) {
                Napier.e("💥 Download failed for $tag: ${e.message}", e)

                _activeDownloads.update { currentMap ->
                    val existing = currentMap[tag] ?: return@update currentMap
                    currentMap + (
                            tag to existing.copy(
                                status = PullStatus.error,
                                message = e.message
                            )
                            )
                }

                _sideEffects.send(
                    DownloadSideEffect.ShowError("Download failed: ${e.message}")
                )
            } finally {
                Napier.d("🧹 Cleaning up job for $tag")
                downloadJobs.remove(tag)
            }
        }

        downloadJobs[tag] = job
    }

    private fun resumeDownload(tag: String) {
        Napier.d("▶️ resumeDownload called for $tag")

        val existing = _activeDownloads.value[tag]

        if (existing == null) {
            Napier.w("⚠️ resumeDownload: No active download entry found for $tag")
            return
        }

        if (downloadJobs[tag]?.isActive == true) {
            Napier.w("⚠️ resumeDownload: Job already active for $tag")
            return
        }

        Napier.d("🔁 Restarting download for $tag")
        startDownload(tag, existing.modelEntity)
    }

    private fun pauseDownload(tag: String) {
        Napier.d("⏸️ pauseDownload called for $tag")

        val job = downloadJobs[tag]

        if (job == null) {
            Napier.w("⚠️ pauseDownload: No job found for $tag")
        } else {
            Napier.d("🛑 Cancelling job for $tag (isActive=${job.isActive})")
            job.cancel()
        }

        downloadJobs.remove(tag)

        _activeDownloads.update { currentMap ->
            val existing = currentMap[tag]

            if (existing == null) {
                Napier.w("⚠️ pauseDownload: No state entry found for $tag")
                return@update currentMap
            }

            Napier.d("📝 Updating state to QUEUED for $tag")

            currentMap + (
                    tag to existing.copy(
                        status = PullStatus.queued,
                        message = "Paused"
                    )
                    )
        }
    }

    private fun cancelDownload(tag: String) {
        Napier.d("❌ cancelDownload called for $tag")

        val job = downloadJobs[tag]

        if (job == null) {
            Napier.w("⚠️ cancelDownload: No job found for $tag")
        } else {
            Napier.d("🛑 Cancelling job for $tag (isActive=${job.isActive})")
            job.cancel()
        }

        downloadJobs.remove(tag)

        _activeDownloads.update { currentMap ->
            Napier.d("🗑 Removing $tag from activeDownloads")
            currentMap - tag
        }
    }

    private fun dismissDownload(tag: String) {
        Napier.d("🧹 dismissDownload called for $tag")

        if (downloadJobs[tag]?.isActive == true) {
            Napier.w("⚠️ dismissDownload: Job still active for $tag — cancelling first")
            downloadJobs[tag]?.cancel()
            downloadJobs.remove(tag)
        }

        _activeDownloads.update { currentMap ->
            Napier.d("🗑 Removing $tag from state (dismiss)")
            currentMap - tag
        }
    }
}


fun String.toPullStatus(): PullStatus {
    val normalized = trim().lowercase()

    return when {
        // Actively downloading / working
        normalized.contains("pull") ||
        normalized.contains("download") ||
        normalized.contains("manifest") ||
        normalized.contains("verify") ||
        normalized.contains("write") ||
        normalized.contains("layer") -> PullStatus.pulling

        // Explicit queued / paused state
        normalized.contains("queue") ||
        normalized.contains("paused") -> PullStatus.queued

        // Completed successfully
        normalized.contains("done") ||
        normalized.contains("success") ||
        normalized.contains("completed") -> PullStatus.done

        // Any failure
        normalized.contains("error") ||
        normalized.contains("fail") -> PullStatus.error

        // Default fallback (safe assumption)
        else -> PullStatus.pulling
    }
}