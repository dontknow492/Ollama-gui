package com.ghost.ollama.gui.utils

import app.cash.sqldelight.ColumnAdapter
import kotlinx.serialization.json.Json

val listOfStringAdapter = object : ColumnAdapter<List<String>, String> {

    private val json = Json { ignoreUnknownKeys = true }

    override fun decode(databaseValue: String): List<String> {
        return if (databaseValue.isEmpty()) {
            emptyList()
        } else {
            json.decodeFromString(databaseValue)
        }
    }

    override fun encode(value: List<String>): String {
        return json.encodeToString(value)
    }
}