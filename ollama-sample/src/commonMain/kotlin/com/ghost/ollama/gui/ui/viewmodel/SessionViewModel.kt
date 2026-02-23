package com.ghost.ollama.gui.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ghost.ollama.gui.GetSessionsPaged
import com.ghost.ollama.gui.repository.OllamaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ==========================================
// 1. UI STATE & SIDE EFFECTS
// ==========================================

sealed interface SessionUiState {
    data object Loading : SessionUiState

    data class Success(
        val pagedSessions: Flow<PagingData<GetSessionsPaged>>,
        val searchQuery: String = "",
        val selectedSessionIds: Set<String> = emptySet(),
        val isSelectionModeActive: Boolean = false,
        val isExporting: Boolean = false
    ) : SessionUiState

    data class Error(val message: String) : SessionUiState
}

// Side effects are one-off events for the UI (e.g., showing a Toast or a File Save Dialog)
sealed interface SessionSideEffect {
    data class ShowToast(val message: String) : SessionSideEffect
    data class ExportFileReady(val defaultFileName: String, val jsonContent: String) : SessionSideEffect
}

// ==========================================
// 2. USER INTENTS (EVENTS)
// ==========================================

sealed interface SessionEvent {
    data class Search(val query: String) : SessionEvent
    data class RenameSession(val sessionId: String, val newTitle: String) : SessionEvent
    data class DeleteSession(val sessionId: String) : SessionEvent
    data class ToggleSelection(val sessionId: String) : SessionEvent
    data object ClearSelection : SessionEvent
    data object BatchDelete : SessionEvent
    data class ExportSession(val sessionId: String) : SessionEvent
    data object BatchExport : SessionEvent
}

// ==========================================
// 3. VIEW MODEL
// ==========================================

class SessionViewModel(
    private val repository: OllamaRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedSessionIds = MutableStateFlow<Set<String>>(emptySet())
    private val _isExporting = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    // Side effects channel
    private val _sideEffects = MutableSharedFlow<SessionSideEffect>()
    val sideEffects: SharedFlow<SessionSideEffect> = _sideEffects.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _pagedSessions: Flow<PagingData<GetSessionsPaged>> = _searchQuery
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
        _error
    ) { query, selectedIds, isExporting, error ->
        if (error != null) {
            SessionUiState.Error(error)
        } else {
            SessionUiState.Success(
                pagedSessions = _pagedSessions,
                searchQuery = query,
                selectedSessionIds = selectedIds,
                isSelectionModeActive = selectedIds.isNotEmpty(),
                isExporting = isExporting
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SessionUiState.Loading
    )

    fun onEvent(event: SessionEvent) {
        when (event) {
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
                viewModelScope.launch {
                    repository.deleteSession(event.sessionId)
                    _selectedSessionIds.value -= event.sessionId
                }
            }

            is SessionEvent.ToggleSelection -> {
                val current = _selectedSessionIds.value
                if (current.contains(event.sessionId)) {
                    _selectedSessionIds.value = current - event.sessionId
                } else {
                    _selectedSessionIds.value = current + event.sessionId
                }
            }

            SessionEvent.ClearSelection -> {
                _selectedSessionIds.value = emptySet()
            }

            SessionEvent.BatchDelete -> {
                viewModelScope.launch {
                    val idsToDelete = _selectedSessionIds.value
                    if (idsToDelete.isNotEmpty()) {
                        repository.batchDeleteSessions(idsToDelete)
                        _selectedSessionIds.value = emptySet()
                        emitSideEffect(SessionSideEffect.ShowToast("${idsToDelete.size} sessions deleted"))
                    }
                }
            }

            is SessionEvent.ExportSession -> {
                exportSessions(setOf(event.sessionId))
            }

            SessionEvent.BatchExport -> {
                exportSessions(_selectedSessionIds.value)
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