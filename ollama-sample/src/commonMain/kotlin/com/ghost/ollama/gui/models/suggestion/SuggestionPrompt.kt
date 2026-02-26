package com.ghost.ollama.gui.models.suggestion

data class SuggestionPrompt(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val template: String,
    val systemPrompt: String? = null,
    val temperature: Double? = null,
    val category: SuggestionCategory = SuggestionCategory.GENERAL,
    val autoSend: Boolean = false
) {
    fun buildPrompt(userInput: String): String {
        return template
            .replace("{{input}}", userInput)
            .replace("{{language}}", "Kotlin") // or dynamic
    }
}