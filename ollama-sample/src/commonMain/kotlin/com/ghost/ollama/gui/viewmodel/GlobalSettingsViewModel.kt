package com.ghost.ollama.gui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghost.ollama.enum.ResponseFormat
import com.ghost.ollama.gui.repository.AppTheme
import com.ghost.ollama.gui.repository.GlobalSettings
import com.ghost.ollama.gui.repository.SettingsRepository
import com.ghost.ollama.models.chat.ChatOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GlobalSettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(GlobalSettingsUiState())
    val uiState: StateFlow<GlobalSettingsUiState> = _uiState.asStateFlow()

    // Edit State (for forms/dialogs)
    private val _editState = MutableStateFlow(GlobalSettingsEditState())
    val editState: StateFlow<GlobalSettingsEditState> = _editState.asStateFlow()

    // Validation errors
    private val _validationErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val validationErrors: StateFlow<Map<String, String>> = _validationErrors.asStateFlow()

    init {
        loadGlobalSettings()
    }

    private fun loadGlobalSettings() {
        viewModelScope.launch {
            settingsRepository.getGlobalSettings().collect { settings ->
                _uiState.update {
                    it.copy(
                        settings = settings,
                        isLoading = false
                    )
                }
                // Sync edit state with loaded settings
                syncEditStateWithSettings(settings)
            }
        }
    }

    private fun syncEditStateWithSettings(settings: GlobalSettings) {
        _editState.update {
            it.copy(
                defaultModel = settings.defaultModel,
                baseUrl = settings.baseUrl,
                theme = settings.theme,
                temperature = settings.defaultChatOptions.temperature ?: 0.7f,
                maxHistoryLength = settings.maxHistoryLength,
                autoSave = settings.autoSave,
                seed = settings.defaultChatOptions.seed,
                topK = settings.defaultChatOptions.topK,
                topP = settings.defaultChatOptions.topP,
                minP = settings.defaultChatOptions.minP,
                stop = settings.defaultChatOptions.stop ?: "",
                numCtx = settings.defaultChatOptions.numCtx,
                numPredict = settings.defaultChatOptions.numPredict,
                format = settings.defaultChatOptions.format
            )
        }
    }

    // ============= UI Actions =============

    fun updateDefaultModel(model: String) {
        _editState.update { it.copy(defaultModel = model) }
        validateField("defaultModel", model)
    }

    fun updateBaseUrl(url: String) {
        _editState.update { it.copy(baseUrl = url) }
        validateField("baseUrl", url)
    }

    fun updateTheme(theme: AppTheme) {
        _editState.update { it.copy(theme = theme) }
    }

    fun updateTemperature(temperature: Float) {
        _editState.update { it.copy(temperature = temperature) }
    }

    fun updateMaxHistoryLength(length: Int) {
        _editState.update { it.copy(maxHistoryLength = length) }
    }

    fun updateAutoSave(autoSave: Boolean) {
        _editState.update { it.copy(autoSave = autoSave) }
    }

    // Advanced Chat Options
    fun updateSeed(seed: Int?) {
        _editState.update { it.copy(seed = seed) }
    }

    fun updateTopK(topK: Int?) {
        _editState.update { it.copy(topK = topK) }
    }

    fun updateTopP(topP: Float?) {
        _editState.update { it.copy(topP = topP) }
    }

    fun updateMinP(minP: Float?) {
        _editState.update { it.copy(minP = minP) }
    }

    fun updateStop(stop: String) {
        _editState.update { it.copy(stop = stop) }
    }

    fun updateNumCtx(numCtx: Int?) {
        _editState.update { it.copy(numCtx = numCtx) }
    }

    fun updateNumPredict(numPredict: Int?) {
        _editState.update { it.copy(numPredict = numPredict) }
    }

    fun updateFormat(format: ResponseFormat?) {
        _editState.update { it.copy(format = format) }
    }

    // Response Format helpers
    fun setFormatJson() {
        _editState.update { it.copy(format = ResponseFormat.JSON) }
    }

    fun setFormatText() {
        _editState.update { it.copy(format = null) }
    }

    // ============= Form Actions =============

    fun saveSettings() {
        if (!validateAll()) return

        viewModelScope.launch {
            val currentSettings = _uiState.value.settings
            val editState = _editState.value

            val updatedSettings = currentSettings.copy(
                defaultModel = editState.defaultModel,
                baseUrl = editState.baseUrl,
                theme = editState.theme,
                defaultChatOptions = ChatOptions(
                    seed = editState.seed,
                    temperature = editState.temperature,
                    topK = editState.topK,
                    topP = editState.topP,
                    minP = editState.minP,
                    stop = editState.stop.ifBlank { null },
                    numCtx = editState.numCtx,
                    numPredict = editState.numPredict,
                    format = editState.format
                ),
                maxHistoryLength = editState.maxHistoryLength,
                autoSave = editState.autoSave
            )

            settingsRepository.saveGlobalSettings(updatedSettings)
            _uiState.update { it.copy(showSuccessMessage = true) }

            // Auto-hide success message after 3 seconds
            viewModelScope.launch {
                delay(3000)
                _uiState.update { it.copy(showSuccessMessage = false) }
            }
        }
    }

    fun resetToDefaults() {
        val defaultSettings = GlobalSettings()
        syncEditStateWithSettings(defaultSettings)
        _validationErrors.update { emptyMap() }
    }

    fun discardChanges() {
        syncEditStateWithSettings(_uiState.value.settings)
        _validationErrors.update { emptyMap() }
    }

    fun dismissSuccessMessage() {
        _uiState.update { it.copy(showSuccessMessage = false) }
    }

    // ============= Validation =============

    private fun validateField(field: String, value: String): Boolean {
        val error = when (field) {
            "baseUrl" -> {
                when {
                    value.isBlank() -> "Base URL cannot be empty"
                    !value.startsWith("http://") && !value.startsWith("https://") ->
                        "Base URL must start with http:// or https://"

                    else -> null
                }
            }

            "defaultModel" -> {
                if (value.isBlank()) "Model name cannot be empty" else null
            }

            else -> null
        }

        _validationErrors.update { currentErrors ->
            if (error == null) {
                currentErrors - field
            } else {
                currentErrors + (field to error)
            }
        }

        return error == null
    }

    private fun validateAll(): Boolean {
        val errors = mutableMapOf<String, String>()

        with(_editState.value) {
            if (baseUrl.isBlank()) {
                errors["baseUrl"] = "Base URL cannot be empty"
            } else if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
                errors["baseUrl"] = "Base URL must start with http:// or https://"
            }

            if (defaultModel.isBlank()) {
                errors["defaultModel"] = "Model name cannot be empty"
            }

            if (maxHistoryLength <= 0) {
                errors["maxHistoryLength"] = "History length must be positive"
            }

            if (temperature !in 0.0f..2.0f) {
                errors["temperature"] = "Temperature must be between 0 and 2"
            }

            topK?.let {
                if (it <= 0) errors["topK"] = "Top K must be positive"
            }

            topP?.let {
                if (it !in 0.0f..1.0f) errors["topP"] = "Top P must be between 0 and 1"
            }

            minP?.let {
                if (it !in 0.0f..1.0f) errors["minP"] = "Min P must be between 0 and 1"
            }

            numCtx?.let {
                if (it <= 0) errors["numCtx"] = "Context size must be positive"
            }

            numPredict?.let {
                if (it <= 0) errors["numPredict"] = "Num predict must be positive"
            }
        }

        _validationErrors.update { errors }
        return errors.isEmpty()
    }

    // ============= Helper Functions =============

    fun getThemeDisplayName(theme: AppTheme): String {
        return when (theme) {
            AppTheme.LIGHT -> "Light"
            AppTheme.DARK -> "Dark"
            AppTheme.SYSTEM -> "System Default"
        }
    }

    fun formatTemperatureForDisplay(temperature: Float): String {
        return String.format("%.1f", temperature)
    }

    fun isValidBaseUrl(url: String): Boolean {
        return url.isNotBlank() &&
                (url.startsWith("http://") || url.startsWith("https://"))
    }

    fun isFormatJson(): Boolean {
        return _editState.value.format == ResponseFormat.JSON
    }
}

// ============= UI State Classes =============

data class GlobalSettingsUiState(
    val settings: GlobalSettings = GlobalSettings(),
    val isLoading: Boolean = true,
    val showSuccessMessage: Boolean = false
)

data class GlobalSettingsEditState(
    val defaultModel: String = "llama3.2",
    val baseUrl: String = "http://localhost:11434",
    val theme: AppTheme = AppTheme.SYSTEM,
    val temperature: Float = 0.7f,
    val maxHistoryLength: Int = 100,
    val autoSave: Boolean = true,

    // Advanced Chat Options
    val seed: Int? = null,
    val topK: Int? = null,
    val topP: Float? = null,
    val minP: Float? = null,
    val stop: String = "",
    val numCtx: Int? = null,
    val numPredict: Int? = null,
    val format: ResponseFormat? = null
)

// ============= Extension Functions =============

fun GlobalSettingsEditState.toChatOptions(): ChatOptions {
    return ChatOptions(
        seed = seed,
        temperature = temperature,
        topK = topK,
        topP = topP,
        minP = minP,
        stop = stop.ifBlank { null },
        numCtx = numCtx,
        numPredict = numPredict,
        format = format
    )
}