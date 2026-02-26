package com.ghost.ollama.gui.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ghost.ollama.models.chat.ChatOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class AppTheme {
    LIGHT, DARK, SYSTEM
}

// Global settings (app-wide defaults)
@Serializable
data class GlobalSettings(
    val defaultChatOptions: ChatOptions = ChatOptions(temperature = 0.7f),
    val baseUrl: String = "http://localhost:11434",
    val theme: AppTheme = AppTheme.SYSTEM,
    val defaultModel: String = "llama3.2",
    val maxHistoryLength: Int = 100,
    val autoSave: Boolean = true
)

// Per-session settings (overrides global)
@Serializable
data class SessionSettings(
    val sessionId: String,
    val chatOptions: ChatOptions? = null, // Overrides global
    val modelName: String? = null, // Overrides global defaultModel
    val customInstructions: String? = null,
    val temperature: Float? = null, // Convenience override
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Merge with global settings to get effective settings
    fun getEffectiveChatOptions(global: GlobalSettings): ChatOptions {
        return global.defaultChatOptions.mergeWith(chatOptions)
    }

    fun getEffectiveModel(global: GlobalSettings): String {
        return modelName ?: global.defaultModel
    }
}

// Helper extension to merge ChatOptions
fun ChatOptions.mergeWith(override: ChatOptions?): ChatOptions {
    if (override == null) return this

    return ChatOptions(
        seed = override.seed ?: this.seed,
        temperature = override.temperature ?: this.temperature,
        topK = override.topK ?: this.topK,
        topP = override.topP ?: this.topP,
        minP = override.minP ?: this.minP,
        stop = override.stop ?: this.stop,
        numCtx = override.numCtx ?: this.numCtx,
        numPredict = override.numPredict ?: this.numPredict,
        format = override.format ?: this.format
    )
}


//@Single
class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val applicationScope: CoroutineScope
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        // Global settings
        val GLOBAL_SETTINGS_KEY = stringPreferencesKey("global_settings")

        // Session settings prefix
        fun sessionSettingsKey(sessionId: String) =
            stringPreferencesKey("session_settings_$sessionId")

        // Active session
        val ACTIVE_SESSION_KEY = stringPreferencesKey("active_session_id")

        // Recent sessions
        val RECENT_SESSIONS_KEY = stringPreferencesKey("recent_sessions")

        // ============= LEGACY KEYS (for migration) =============
        private val THEME_KEY = stringPreferencesKey("theme")
        private val MODEL_KEY = stringPreferencesKey("default_model")
        private val TEMPERATURE_KEY = floatPreferencesKey("temperature")
        private val SESSION_ID_KEY = stringPreferencesKey("last_session_id")
    }

    init {
        // Run migration when repository is created
        applicationScope.launch {
            migrateFromLegacy()
        }

    }

    // ============= MIGRATION =============

    private suspend fun migrateFromLegacy() {
        val preferences = dataStore.data.first()

        // Skip if already migrated
        if (preferences[GLOBAL_SETTINGS_KEY] != null) return

        // Check if legacy data exists
        val hasLegacyData = preferences[THEME_KEY] != null ||
                preferences[MODEL_KEY] != null ||
                preferences[TEMPERATURE_KEY] != null

        if (hasLegacyData) {
            // Migrate to new format
            val theme = when (preferences[THEME_KEY]) {
                "LIGHT" -> AppTheme.LIGHT
                "DARK" -> AppTheme.DARK
                else -> AppTheme.SYSTEM
            }

            val globalSettings = GlobalSettings(
                defaultModel = preferences[MODEL_KEY] ?: "llama3.2",
                theme = theme,
                defaultChatOptions = ChatOptions(
                    temperature = preferences[TEMPERATURE_KEY] ?: 0.7f
                ),
                baseUrl = "http://localhost:11434" // Default
            )

            // Save migrated data
            dataStore.edit { prefs ->
                prefs[GLOBAL_SETTINGS_KEY] = json.encodeToString(globalSettings)

                // Optionally clean up legacy keys
                prefs.remove(THEME_KEY)
                prefs.remove(MODEL_KEY)
                prefs.remove(TEMPERATURE_KEY)
                // Keep SESSION_ID_KEY if needed
            }
        }
    }

    // ============= GLOBAL SETTINGS =============

    suspend fun saveGlobalSettings(settings: GlobalSettings) {
        dataStore.edit { preferences ->
            preferences[GLOBAL_SETTINGS_KEY] = json.encodeToString(settings)
        }
    }

    fun getGlobalSettings(): Flow<GlobalSettings> =
        dataStore.data.map { preferences ->
            preferences[GLOBAL_SETTINGS_KEY]?.let {
                json.decodeFromString<GlobalSettings>(it)
            } ?: GlobalSettings()
        }

    suspend fun updateGlobalSettings(update: (GlobalSettings) -> GlobalSettings) {
        val current = getGlobalSettings().first()
        saveGlobalSettings(update(current))
    }

    // ============= SESSION SETTINGS =============

    suspend fun saveSessionSettings(settings: SessionSettings) {
        dataStore.edit { preferences ->
            preferences[sessionSettingsKey(settings.sessionId)] = json.encodeToString(settings)
        }
    }

    fun getSessionSettings(sessionId: String): Flow<SessionSettings?> =
        dataStore.data.map { preferences ->
            preferences[sessionSettingsKey(sessionId)]?.let {
                json.decodeFromString<SessionSettings>(it)
            }
        }

    suspend fun getSessionSettingsSync(sessionId: String): SessionSettings? {
        return dataStore.data.first()[sessionSettingsKey(sessionId)]?.let {
            json.decodeFromString<SessionSettings>(it)
        }
    }

    // ============= LEGACY COMPATIBILITY METHODS =============
    // Keep these if other parts of your app still use the old keys

    suspend fun saveThemeLegacy(theme: String) {
        updateGlobalSettings { it.copy(theme = AppTheme.valueOf(theme)) }
    }

    fun getThemeLegacy(): Flow<String?> = getGlobalSettings()
        .map { it.theme.name }

    fun getAppTheme(): Flow<AppTheme> = getGlobalSettings()
        .map { it.theme }

    suspend fun saveDefaultModelLegacy(model: String) {
        updateGlobalSettings { it.copy(defaultModel = model) }
    }

    fun getDefaultModelLegacy(): Flow<String?> = getGlobalSettings()
        .map { it.defaultModel }

    suspend fun saveTemperatureLegacy(temperature: Float) {
        updateGlobalSettings {
            it.copy(defaultChatOptions = it.defaultChatOptions.copy(temperature = temperature))
        }
    }

    fun getTemperatureLegacy(): Flow<Float?> = getGlobalSettings()
        .map { it.defaultChatOptions.temperature }

    // For last session ID
    suspend fun setLastSessionId(sessionId: String) {
        dataStore.edit { preferences ->
            preferences[SESSION_ID_KEY] = sessionId
        }
    }

    fun getLastSessionId(): Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[SESSION_ID_KEY]
        }
}

