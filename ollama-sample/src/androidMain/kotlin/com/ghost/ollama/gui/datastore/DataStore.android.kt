package com.ghost.ollama.gui.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

actual class DataStoreFactory(
    private val context: Context
) {
    actual fun createDataStore(): DataStore<Preferences> {
        return context.dataStore
    }
}

// Extension property for easier access
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ollama_preferences"
)