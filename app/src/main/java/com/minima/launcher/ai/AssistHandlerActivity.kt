package com.minima.launcher.ai

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.minima.launcher.ui.theme.MinimaTheme
import kotlinx.coroutines.launch

/**
 * Fired on system ASSIST / VOICE_COMMAND (long-press home, corner swipe,
 * power-long-press where OEM allows). Shows a bottom-sheet style overlay
 * with a prompt field routed to the user's default AI.
 *
 * To activate: Settings -> Apps -> Default apps -> Digital assistant ->
 * select Minima (see AIManager.requestAssistantRole).
 */
class AssistHandlerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ai = AIManager(applicationContext)
        val voiceRequested =
            intent?.action == Intent.ACTION_VOICE_COMMAND ||
            intent?.action == "android.intent.action.VOICE_ASSIST"

        setContent {
            MinimaTheme {
                AssistSheet(
                    voiceRequested = voiceRequested,
                    onSubmit = { prompt, assistant ->
                        ai.ask(prompt, assistant)
                        finish()
                    },
                    onVoice = { assistant ->
                        // Delegate to recognizer, then forward transcript.
                        val rec = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                            )
                        }
                        try {
                            @Suppress("DEPRECATION")
                            startActivityForResult(rec, 4101)
                        } catch (_: Exception) {
                            ai.openVoice(assistant)
                            finish()
                        }
                    },
                    onDismiss = { finish() },
                )
            }
        }

        if (voiceRequested) {
            // Best effort: jump straight to the assistant's own voice UI.
            // User can still type in the sheet if that fails.
            kotlinx.coroutines.MainScope().launch {
                ai.defaultAssistant.collect { assistant ->
                    ai.openVoice(assistant)
                    finish()
                }
            }
        }
    }

    @Deprecated("legacy voice result")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 4101) {
            val text = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!text.isNullOrBlank()) {
                val ai = AIManager(applicationContext)
                kotlinx.coroutines.MainScope().launch {
                    ai.defaultAssistant.collect { ai.ask(text, it); finish() }
                }
                return
            }
        }
        finish()
    }
}

@Composable
private fun AssistSheet(
    voiceRequested: Boolean,
    onSubmit: (String, AIAssistant) -> Unit,
    onVoice: (AIAssistant) -> Unit,
    onDismiss: () -> Unit,
) {
    var prompt by remember { mutableStateOf("") }
    var assistant by remember { mutableStateOf(AssistantRegistry.all.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (voiceRequested) "Listening… (type instead)" else "Ask ${assistant.label}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AssistantRow(selected = assistant, onSelect = { assistant = it })
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    placeholder = { Text("Ask anything, or type an app name…") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minLines = 2,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(prompt, assistant) }, enabled = prompt.isNotBlank()) {
                Text("Ask ${assistant.label}")
            }
        },
        dismissButton = {
            TextButton(onClick = { onVoice(assistant) }) { Text("Voice") }
        }
    )
}

@Composable
internal fun AssistantRow(
    selected: AIAssistant,
    onSelect: (AIAssistant) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AssistantRegistry.all.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { a ->
                    FilterChip(
                        selected = a.id == selected.id,
                        onClick = { onSelect(a) },
                        label = { Text(a.label) },
                    )
                }
            }
        }
    }
}
