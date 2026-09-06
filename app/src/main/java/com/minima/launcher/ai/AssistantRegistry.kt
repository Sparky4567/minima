package com.minima.launcher.ai

/** Single place to register assistants. Add yours here and the whole
 *  launcher (dock, chooser, assist handler, search) picks it up. */
object AssistantRegistry {
    val all: List<AIAssistant> = listOf(
        GeminiAssistant,
        ChatGptAssistant,
        GrokAssistant,
        ClaudeAssistant,
    )

    fun byId(id: String?): AIAssistant =
        all.firstOrNull { it.id == id } ?: GeminiAssistant
}
