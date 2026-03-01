package com.ghost.ollama.gui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.paging.PagingData
import app.cash.paging.cachedIn
import com.ghost.ollama.gui.SessionView
import com.ghost.ollama.gui.repository.GlobalSettings
import com.ghost.ollama.gui.repository.OllamaRepository
import com.ghost.ollama.gui.repository.SettingsRepository
import com.ghost.ollama.gui.ui.components.TuneOptions
import com.ghost.ollama.gui.utils.applyTuneOptions
import io.github.aakira.napier.Napier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ==========================================
// 1. UI STATE & SIDE EFFECTS
// ==========================================

sealed interface SessionUiState {
    data object Loading : SessionUiState

    data class Success(
        val pagedSessions: Flow<PagingData<SessionView>>,
        val searchQuery: String = "",
        val selectedSessionIds: Set<String> = emptySet(),
        val isSelectionModeActive: Boolean = false,
        val isExporting: Boolean = false,
        val ollamaVersion: String,
        val isOllamaRunning: Boolean
    ) : SessionUiState

    data class Error(val message: String) : SessionUiState
}

// Side effects are one-off events for the UI (e.g., showing a Toast or a File Save Dialog)
sealed interface SessionSideEffect {
    data class ShowToast(val message: String) : SessionSideEffect
    data class ExportFileReady(val defaultFileName: String, val jsonContent: String) : SessionSideEffect
    data class NewSessionCreated(val sessionId: String) : SessionSideEffect
}

// ==========================================
// 2. USER INTENTS (EVENTS)
// ==========================================

sealed interface SessionEvent {
    data class CreateNew(val title: String? = null) : SessionEvent
    data class Search(val query: String) : SessionEvent
    data class RenameSession(val sessionId: String, val newTitle: String) : SessionEvent
    data class DeleteSession(val sessionId: String) : SessionEvent
    data class ToggleSelection(val sessionId: String) : SessionEvent
    data object ClearSelection : SessionEvent
    data object BatchDelete : SessionEvent
    data class ExportSession(val sessionId: String) : SessionEvent
    data object BatchExport : SessionEvent
    data class ToggleSessionPin(val sessionId: String) : SessionEvent
    data object Retry : SessionEvent
    data class UpdateSessionTuneOptions(val tuneOptions: TuneOptions, val session: SessionView) : SessionEvent
}

// ==========================================
// 3. VIEW MODEL
// ==========================================

class SessionViewModel(
    private val repository: OllamaRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _globalSettings: Flow<GlobalSettings> = settingsRepository.getGlobalSettings()

    private val _searchQuery = MutableStateFlow("")
    private val _selectedSessionIds = MutableStateFlow<Set<String>>(emptySet())
    private val _isExporting = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _ollamaVersion = MutableStateFlow<String?>(null)
    private val _isOllamaRunning = repository.observerOllamaStatus()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _connectionState =
        combine(_ollamaVersion, _isOllamaRunning) { version, isRunning ->
            version to isRunning
        }


    init {
        fetchOllamaVersion()
        viewModelScope.launch {
            val settings = _globalSettings.first()

            val session = repository.getOrCreateActiveSessionOnce(settings)

            emitSideEffect(SessionSideEffect.NewSessionCreated(session.id))
        }
    }


    // Side effects channel
    private val _sideEffects = MutableSharedFlow<SessionSideEffect>()
    val sideEffects: SharedFlow<SessionSideEffect> = _sideEffects.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private val _pagedSessions: Flow<PagingData<SessionView>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.getSessionsPaged()
            } else {
                repository.searchSessionsPaged(query)
            }
        }
        .cachedIn(viewModelScope)

    // Combine all states into the final UI State
    val uiState: StateFlow<SessionUiState> = combine(
        _searchQuery,
        _selectedSessionIds,
        _isExporting,
        _error,
        _connectionState
    ) { query,
        selectedIds,
        isExporting,
        error,
        connection ->

        val (version, isRunning) = connection

        if (error != null) {
            SessionUiState.Error(error)
        } else {
            SessionUiState.Success(
                pagedSessions = _pagedSessions,
                searchQuery = query,
                selectedSessionIds = selectedIds,
                isSelectionModeActive = selectedIds.isNotEmpty(),
                isExporting = isExporting,
                ollamaVersion = version ?: "Unknown",
                isOllamaRunning = isRunning
            )
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SessionUiState.Loading
        )


    private fun fetchOllamaVersion() {
        Napier.d("Fetching Ollama version")

        viewModelScope.launch {
            try {
                _ollamaVersion.value = repository.ollamaVersion()
            } catch (e: Exception) {
                Napier.w("Ollama version fetch failed: ${e.message}")

                // Do NOT treat this as fatal UI error
                _ollamaVersion.value = null
            }
        }
    }

    fun onEvent(event: SessionEvent) {
        when (event) {
            is SessionEvent.CreateNew -> {
                createNewSession(event.title)
            }

            is SessionEvent.Search -> {
                _searchQuery.value = event.query
                // Clear selection when searching to prevent accidental operations
                _selectedSessionIds.value = emptySet()
            }

            is SessionEvent.RenameSession -> {
                viewModelScope.launch {
                    try {
                        repository.renameSession(event.sessionId, event.newTitle)
                    } catch (e: Exception) {
                        emitSideEffect(SessionSideEffect.ShowToast("Failed to rename session"))
                    }
                }
            }

            is SessionEvent.DeleteSession -> {
                deleteSession(event.sessionId)
            }

            is SessionEvent.ToggleSelection -> {
                val current = _selectedSessionIds.value
                if (current.contains(event.sessionId)) {
                    _selectedSessionIds.value = current - event.sessionId
                } else {
                    _selectedSessionIds.value = current + event.sessionId
                }
            }

            is SessionEvent.UpdateSessionTuneOptions -> {
                viewModelScope.launch {
                    try {
                        val newSession = event.session.applyTuneOptions(event.tuneOptions)
                        repository.updateSession(newSession)
                    } catch (e: Exception) {
                        emitSideEffect(SessionSideEffect.ShowToast("Failed to update tune options"))
                    }
                }
            }

            SessionEvent.ClearSelection -> {
                _selectedSessionIds.value = emptySet()
            }

            SessionEvent.BatchDelete -> {
                batchDeleteSessions(_selectedSessionIds.value)
            }

            is SessionEvent.ExportSession -> {
                exportSessions(setOf(event.sessionId))
            }

            is SessionEvent.ToggleSessionPin -> {
                viewModelScope.launch {
                    try {
                        repository.toggleSessionPin(event.sessionId)
                    } catch (e: Exception) {
                        emitSideEffect(SessionSideEffect.ShowToast("Failed to toggle pin status"))
                    }
                }
            }

            is SessionEvent.Retry -> {
                fetchOllamaVersion()
            }

            SessionEvent.BatchExport -> {
                exportSessions(_selectedSessionIds.value)
            }

        }
    }

    private fun createNewSession(title: String?) {
        viewModelScope.launch {
            try {
                _globalSettings.collectLatest { settings ->
                    val sessionId = repository.createOrReuseSession(settings)
                    emitSideEffect(SessionSideEffect.NewSessionCreated(sessionId))
                }

            } catch (e: Exception) {
                emitSideEffect(SessionSideEffect.ShowToast("Failed to create new session"))
            }
        }
    }

    private fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            try {
                val last = repository.getLastMessage() ?: return@launch
                repository.deleteSession(sessionId)
                _selectedSessionIds.value -= sessionId
                if (last.sessionId == sessionId) {
                    createNewSession(null)
                }
            } catch (e: Exception) {
                emitSideEffect(SessionSideEffect.ShowToast("Failed to delete session"))
            }
        }
    }

    private fun batchDeleteSessions(sessionIds: Set<String>) {
        viewModelScope.launch {
            try {
                val last = repository.getLastMessage()
                repository.batchDeleteSessions(sessionIds)
                _selectedSessionIds.value = emptySet()
                if (last != null) {
                    if (sessionIds.contains(last.sessionId)) {
                        createNewSession(null)
                    }
                }
                emitSideEffect(SessionSideEffect.ShowToast("${sessionIds.size} sessions deleted"))
            } catch (e: Exception) {
                emitSideEffect(SessionSideEffect.ShowToast("Failed to delete sessions"))
            }
        }
    }

    /**
     * Exports one or multiple sessions, combining them if necessary,
     * and emits a side-effect with the raw JSON for the UI to save.
     */
    private fun exportSessions(sessionIds: Set<String>) {
        if (sessionIds.isEmpty()) return

        viewModelScope.launch {
            _isExporting.value = true
            try {
                if (sessionIds.size == 1) {
                    val sessionId = sessionIds.first()
                    val json = repository.exportSessionAsJson(sessionId)
                    emitSideEffect(SessionSideEffect.ExportFileReady("chat_export_$sessionId.json", json))
                } else {
                    // Combine multiple exports into a JSON Array-like structure
                    val combinedJsonList = sessionIds.map { repository.exportSessionAsJson(it) }
                    val combinedJsonString = "[${combinedJsonList.joinToString(",")}]"
                    emitSideEffect(
                        SessionSideEffect.ExportFileReady(
                            "batch_export_${System.currentTimeMillis()}.json",
                            combinedJsonString
                        )
                    )
                }
                // Clear selection after successful export
                _selectedSessionIds.value = emptySet()
            } catch (e: Exception) {
                emitSideEffect(SessionSideEffect.ShowToast("Failed to export: ${e.message}"))
            } finally {
                _isExporting.value = false
            }
        }
    }

    private suspend fun emitSideEffect(effect: SessionSideEffect) {
        _sideEffects.emit(effect)
    }
}