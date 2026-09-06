package com.minima.launcher.ai

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.aiStore by preferencesDataStore("ai_prefs")
private val KEY_DEFAULT_AI = stringPreferencesKey("default_ai_id")

/**
 * Central hub for all AI launches. Handles:
 *  - default-assistant persistence (DataStore)
 *  - installed-check + Play Store / web fallback
 *  - safe launches (no crashes if assistant missing)
 *  - contextual prompt building (selected text, foreground app)
 */
class AIManager(private val context: Context) {

    val defaultAssistant: Flow<AIAssistant> =
        context.aiStore.data.map { AssistantRegistry.byId(it[KEY_DEFAULT_AI]) }

    suspend fun setDefault(id: String) {
        context.aiStore.edit { it[KEY_DEFAULT_AI] = id }
    }

    fun isInstalled(a: AIAssistant): Boolean = try {
        context.packageManager.getPackageInfo(a.packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        // Gemini also resolves via Assistant package.
        if (a is GeminiAssistant) isPackageInstalled("com.google.android.apps.googleassistant") ||
            isPackageInstalled("com.google.android.googlequicksearchbox")
        else false
    }

    private fun isPackageInstalled(pkg: String) = try {
        context.packageManager.getPackageInfo(pkg, 0); true
    } catch (_: Exception) { false }

    /** Main entry: send [prompt] to assistant [assistant] (or default). */
    fun ask(prompt: String, assistant: AIAssistant, useVoice: Boolean = false) {
        val intent = if (useVoice && assistant.supportsVoiceInput)
            assistant.buildVoiceIntent(context) else assistant.buildTextIntent(context, prompt)
        safeLaunch(intent, assistant, prompt)
    }

    suspend fun askDefault(prompt: String) {
        val id = context.aiStore.data.map { it[KEY_DEFAULT_AI] }.let {
            kotlinx.coroutines.flow.first(it)
        }
        ask(prompt, AssistantRegistry.byId(id))
    }

    fun askWithContext(
        rawPrompt: String,
        assistant: AIAssistant,
        foregroundAppLabel: String? = null,
        selectedText: String? = null,
    ) {
        val ctx = buildString {
            if (!foregroundAppLabel.isNullOrBlank()) append("Context: user is in $foregroundAppLabel. ")
            if (!selectedText.isNullOrBlank()) append("Selected text: \"$selectedText\". ")
            append(rawPrompt)
        }
        ask(ctx.trim(), assistant)
    }

    fun openVoice(assistant: AIAssistant) = ask("", assistant, useVoice = true)

    /** Prompt user to make Minima the system Assistant (long-press home). */
    fun requestAssistantRole(activity: android.app.Activity, requestCode: Int = 9001) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = context.getSystemService(RoleManager::class.java) ?: return
            if (!rm.isRoleHeld(RoleManager.ROLE_ASSISTANT)) {
                try {
                    activity.startActivityForResult(
                        rm.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT), requestCode
                    )
                } catch (_: Exception) {
                    Toast.makeText(context, "Assistant role not available", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // -- internals --

    private fun safeLaunch(intent: Intent, assistant: AIAssistant, prompt: String) {
        try {
            // If targeting a package that isn't installed, go to web/Play instead.
            val targetPkg = intent.`package`
            if (targetPkg != null && !isPackageInstalled(targetPkg) && assistant.packageName == targetPkg) {
                openFallback(assistant, prompt)
                return
            }
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            openFallback(assistant, prompt)
        }
    }

    private fun openFallback(assistant: AIAssistant, prompt: String) {
        try {
            // Try Play Store first so user can install the assistant...
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${assistant.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            // ...otherwise open the web app with the prompt prefilled where possible.
            val url = assistant.webFallbackUrl
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        Toast.makeText(context, "${assistant.label} app not found — opening fallback", Toast.LENGTH_SHORT).show()
    }
}
