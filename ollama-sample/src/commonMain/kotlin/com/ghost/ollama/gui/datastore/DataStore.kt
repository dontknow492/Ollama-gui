package com.ghost.ollama.gui.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ghost.ollama.gui.repository.AppTheme
import com.ghost.ollama.gui.repository.GlobalSettings
import com.ghost.ollama.models.chat.ChatOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

expect class DataStoreFactory {
    fun createDataStore(): DataStore<Preferences>
}

// Mark as deprecated to encourage migration
@Deprecated("Use SettingsRepository instead for better organization")
class PreferencesManager(
    private val dataStore: DataStore<Preferences>
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        // Old keys (keep for backward compatibility)
        val THEME_KEY = stringPreferencesKey("theme")
        val MODEL_KEY = stringPreferencesKey("default_model")
        val TEMPERATURE_KEY = floatPreferencesKey("temperature")
        val SESSION_ID_KEY = stringPreferencesKey("last_session_id")

        // New global settings key (to eventually replace old ones)
        val GLOBAL_SETTINGS_KEY = stringPreferencesKey("global_settings")
    }

    // ============= OLD METHODS (Deprecated) =============

    @Deprecated("Use SettingsRepository.saveTheme()")
    suspend fun saveTheme(theme: String) {
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
        // Also update new format for migration
        migrateToGlobalSettings()
    }

    @Deprecated("Use SettingsRepository.getTheme()")
    fun getTheme(): Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[THEME_KEY]
        }

    @Deprecated("Use SettingsRepository.saveDefaultModel()")
    suspend fun saveDefaultModel(model: String) {
        dataStore.edit { preferences ->
            preferences[MODEL_KEY] = model
        }
        migrateToGlobalSettings()
    }

    @Deprecated("Use SettingsRepository.getDefaultModel()")
    fun getDefaultModel(): Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[MODEL_KEY]
        }

    @Deprecated("Use SettingsRepository.saveTemperature()")
    suspend fun saveTemperature(temperature: Float) {
        dataStore.edit { preferences ->
            preferences[TEMPERATURE_KEY] = temperature
        }
        migrateToGlobalSettings()
    }

    @Deprecated("Use SettingsRepository.getTemperature()")
    fun getTemperature(): Flow<Float?> = dataStore.data
        .map { preferences ->
            preferences[TEMPERATURE_KEY]
        }

    // ============= MIGRATION HELPER =============

    private suspend fun migrateToGlobalSettings() {
        val preferences = dataStore.data.first()

        // Check if we already have global settings
        if (preferences[GLOBAL_SETTINGS_KEY] != null) return

        // Migrate old values to new format
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
            )
        )

        dataStore.edit { prefs ->
            prefs[GLOBAL_SETTINGS_KEY] = json.encodeToString(globalSettings)
        }
    }

    // ============= NEW METHODS (Optional) =============

    suspend fun saveGlobalSettings(settings: GlobalSettings) {
        dataStore.edit { preferences ->
            preferences[GLOBAL_SETTINGS_KEY] = json.encodeToString(settings)
        }
    }

    fun getGlobalSettings(): Flow<GlobalSettings?> = dataStore.data
        .map { preferences ->
            preferences[GLOBAL_SETTINGS_KEY]?.let {
                json.decodeFromString<GlobalSettings>(it)
            }
        }
}