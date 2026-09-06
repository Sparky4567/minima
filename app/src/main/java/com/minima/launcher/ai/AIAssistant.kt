package com.minima.launcher.ai

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Universal contract for "any AI assistant app".
 * To add a new assistant (Copilot, Perplexity, on-device LLM...),
 * just add a new object implementing this interface + register it
 * in [AssistantRegistry]. The launcher UI never hardcodes assistants.
 */
interface AIAssistant {
    val id: String          // stable key persisted in DataStore, e.g. "gemini"
    val label: String       // e.g. "Gemini"
    val packageName: String // Play package for install detection
    val deepLinkUri: Uri?   // app-native new-chat deep link, null = use generic send
    val webFallbackUrl: String // https URL opened if app not installed
    val supportsVoiceInput: Boolean

    /** Fire-and-forget text prompt into the assistant app. */
    fun buildTextIntent(context: Context, prompt: String): Intent

    /** Hands-free voice entry into the assistant app. */
    fun buildVoiceIntent(context: Context): Intent
}

private fun sendIntent(pkg: String, text: String): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        `package` = pkg
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

private fun viewIntent(uri: Uri, pkg: String? = null): Intent =
    Intent(Intent.ACTION_VIEW, uri).apply {
        pkg?.let { `package` = it }
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

/** Google Gemini (also handles Assistant / AI Studio fallback packages). */
object GeminiAssistant : AIAssistant {
    override val id = "gemini"
    override val label = "Gemini"
    override val packageName = "com.google.android.apps.bard"
    override val deepLinkUri: Uri? = null
    override val webFallbackUrl = "https://gemini.google.com/"
    override val supportsVoiceInput = true

    override fun buildTextIntent(context: Context, prompt: String): Intent =
        // Gemini honours a plain SEND; keep package unset so the user can
        // pick Gemini / Assistant Go / AI Studio if several are installed.
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, prompt)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    override fun buildVoiceIntent(context: Context): Intent =
        Intent(Intent.ACTION_VOICE_COMMAND).apply {
            `package` = "com.google.android.apps.googleassistant"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
}

/** OpenAI ChatGPT. */
object ChatGptAssistant : AIAssistant {
    override val id = "chatgpt"
    override val label = "ChatGPT"
    override val packageName = "com.openai.chatgpt"
    override val deepLinkUri: Uri? = Uri.parse("chatgpt://")
    override val webFallbackUrl = "https://chat.openai.com/"
    override val supportsVoiceInput = true

    override fun buildTextIntent(context: Context, prompt: String): Intent {
        // Prefer native deep link with prefilled prompt, fall back to SEND.
        // ChatGPT Android reads EXTRA_TEXT on SEND.
        return sendIntent(packageName, prompt)
    }

    override fun buildVoiceIntent(context: Context): Intent =
        viewIntent(Uri.parse("chatgpt://voice"), packageName)
}

/** xAI Grok. */
object GrokAssistant : AIAssistant {
    override val id = "grok"
    override val label = "Grok"
    override val packageName = "com.xai.grok"
    override val deepLinkUri: Uri? = Uri.parse("grok://")
    override val webFallbackUrl = "https://x.com/i/grok"
    override val supportsVoiceInput = true

    override fun buildTextIntent(context: Context, prompt: String) =
        sendIntent(packageName, prompt)

    override fun buildVoiceIntent(context: Context): Intent =
        Intent("android.speech.action.RECOGNIZE_SPEECH").apply {
            `package` = packageName
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
}

/** Anthropic Claude. */
object ClaudeAssistant : AIAssistant {
    override val id = "claude"
    override val label = "Claude"
    override val packageName = "com.anthropic.claude"
    override val deepLinkUri: Uri? = Uri.parse("claude://new")
    override val webFallbackUrl = "https://claude.ai/new"
    override val supportsVoiceInput = false

    override fun buildTextIntent(context: Context, prompt: String) =
        sendIntent(packageName, prompt)

    override fun buildVoiceIntent(context: Context): Intent =
        // Claude has no voice entry; fall back to system recognizer,
        // launcher returns the transcript via AskAiActivity.
        Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
}
