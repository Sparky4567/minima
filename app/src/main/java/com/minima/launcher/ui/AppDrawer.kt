package com.minima.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.minima.launcher.data.AppModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerSheet(
    apps: List<AppModel>,
    query: String,
    onQuery: (String) -> Unit,
    onAppClick: (AppModel) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxHeight(0.85f).padding(16.dp)) {
            TextField(
                value = query,
                onValueChange = onQuery,
                placeholder = { Text("Search apps…") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(apps, key = { it.packageName + it.className }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAppClick(app) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        app.icon?.let {
                            Image(
                                it.toBitmap().asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(app.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
