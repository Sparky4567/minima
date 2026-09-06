package com.minima.launcher.ai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.minima.launcher.ui.theme.MinimaTheme

/**
 * Share / text-selection target ("Ask Minima AI").
 * Receives PROCESS_TEXT / SEND from any app, lets the user pick
 * which assistant handles it, with the selected text as context.
 */
class AskAiActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedText: String? = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_PROCESS_TEXT ->
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            else -> null
        }
        val ai = AIManager(applicationContext)

        setContent {
            MinimaTheme {
                var assistant by remember { mutableStateOf(AssistantRegistry.all.first()) }
                AlertDialog(
                    onDismissRequest = { finish() },
                    title = { Text("Ask AI about selection") },
                    text = {
                        Column {
                            Text(sharedText?.take(500) ?: "No text received.")
                            Spacer(Modifier.height(12.dp))
                            AssistantRow(selected = assistant, onSelect = { assistant = it })
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (!sharedText.isNullOrBlank()) {
                                ai.askWithContext(
                                    "Explain / act on this:",
                                    assistant,
                                    selectedText = sharedText
                                )
                            }
                            finish()
                        }) { Text("Send to ${assistant.label}") }
                    },
                    dismissButton = {
                        TextButton(onClick = { finish() }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}
