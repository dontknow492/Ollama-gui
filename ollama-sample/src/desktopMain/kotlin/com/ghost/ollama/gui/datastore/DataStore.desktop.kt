package com.ghost.ollama.gui.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import java.io.File

// Alternative: Using application support directory (better for different OS)
actual class DataStoreFactory {

    private fun getAppDataDir(): String {
        val os = System.getProperty("os.name").lowercase()

        return when {
            os.contains("win") -> {
                // Windows: %APPDATA%\ollama-koin
                System.getenv("APPDATA") + File.separator + "ollama-koin"
            }

            os.contains("mac") -> {
                // macOS: ~/Library/Application Support/ollama-koin
                System.getProperty("user.home") +
                        "/Library/Application Support/ollama-koin"
            }

            else -> {
                // Linux/Unix: ~/.config/ollama-koin
                System.getProperty("user.home") + "/.config/ollama-koin"
            }
        }
    }

    actual fun createDataStore(): DataStore<Preferences> {
        val appDataDir = File(getAppDataDir())

        if (!appDataDir.exists()) {
            appDataDir.mkdirs()
        }

        val preferencesFile = File(appDataDir, "ollama_preferences.preferences_pb")

        return PreferenceDataStoreFactory.createWithPath(
            produceFile = {
                preferencesFile.absolutePath.toPath()
            }
        )
    }
}