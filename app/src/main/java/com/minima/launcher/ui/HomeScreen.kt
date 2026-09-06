package com.minima.launcher.ui

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.minima.launcher.ai.AIManager
import com.minima.launcher.ai.AssistantRegistry
import com.minima.launcher.data.LauncherViewModel

/**
 * Home = search + AI bar + favorites grid + full drawer bottom sheet.
 * Swipe-up / drawer button opens [AppDrawerSheet].
 */
@Composable
fun HomeScreen(vm: LauncherViewModel, activity: Activity) {
    val apps by vm.apps.collectAsState()
    val query by vm.query.collectAsState()
    val defaultAi by vm.defaultAi.collectAsState()
    var drawerOpen by remember { mutableStateOf(false) }
    var aiPickerOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val favorites = remember(apps) { apps.take(8) }
    val filtered = remember(apps, query) { vm.filteredApps() }

    Scaffold(
        bottomBar = {
            AiDockBar(
                aiLabel = defaultAi.label,
                query = query,
                onQuery = vm::onQuery,
                onSubmit = {
                    if (filtered.isEmpty()) vm.askAi(query)
                    else filtered.firstOrNull()?.let { vm.launch(it) }
                },
                onVoice = { vm.ai.openVoice(defaultAi) },
                onSwitchAi = { aiPickerOpen = true },
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Minima", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    AIManager(context).requestAssistantRole(activity)
                }) { Icon(Icons.Default.MoreVert, contentDescription = "Set as assistant") }
                Button(onClick = { drawerOpen = true }) { Text("All apps") }
            }

            // Favorites grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(favorites, key = { it.packageName + it.className }) { app ->
                    AppTile(
                        label = app.label,
                        icon = {
                            app.icon?.let {
                                Image(
                                    it.toBitmap().asImageBitmap(),
                                    contentDescription = app.label,
                                    modifier = Modifier.size(48.dp),
                                )
                            }
                        },
                        onClick = { vm.launch(app) },
                    )
                }
            }

            // Inline results when searching from the dock
            if (query.isNotBlank()) {
                Card {
                    Column(Modifier.padding(8.dp)) {
                        Text(
                            if (filtered.isEmpty()) "No apps — Enter sends to ${defaultAi.label}"
                            else "${filtered.size} app(s)",
                            style = MaterialTheme.typography.labelMedium,
                        )
                        filtered.take(4).forEach { app ->
                            Text(
                                app.label,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { vm.launch(app) }
                                    .padding(vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    if (drawerOpen) {
        AppDrawerSheet(
            apps = if (query.isBlank()) apps else filtered,
            query = query,
            onQuery = vm::onQuery,
            onAppClick = { vm.launch(it); drawerOpen = false },
            onDismiss = { drawerOpen = false },
        )
    }

    if (aiPickerOpen) {
        AiPickerSheet(
            installedCheck = { vm.ai.isInstalled(it) },
            currentId = defaultAi.id,
            onPick = { vm.setDefaultAi(it.id); aiPickerOpen = false },
            onDismiss = { aiPickerOpen = false },
        )
    }
}

@Composable
private fun AppTile(label: String, icon: @Composable () -> Unit, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon()
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 2)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiDockBar(
    aiLabel: String,
    query: String,
    onQuery: (String) -> Unit,
    onSubmit: () -> Unit,
    onVoice: () -> Unit,
    onSwitchAi: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Search, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            TextField(
                value = query,
                onValueChange = onQuery,
                placeholder = { Text("Search apps or ask $aiLabel…") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            IconButton(onClick = onVoice) {
                Icon(Icons.Default.Mic, contentDescription = "Voice to $aiLabel")
            }
            AssistChip(onClick = onSwitchAi, label = { Text(aiLabel) })
        }
    }
}

@Composable
private fun AiPickerSheet(
    installedCheck: (com.minima.launcher.ai.AIAssistant) -> Boolean,
    currentId: String,
    onPick: (com.minima.launcher.ai.AIAssistant) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Default AI assistant") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AssistantRegistry.all.forEach { a ->
                    val installed = installedCheck(a)
                    ListItem(
                        headlineContent = { Text(a.label + if (a.id == currentId) " ✓" else "") },
                        supportingContent = {
                            Text(if (installed) a.packageName else "Not installed — tap to get it")
                        },
                        modifier = Modifier.clickable { onPick(a) },
                    )
                }
                Text(
                    "Long-press Home triggers your pick via the system Assist gesture.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}
