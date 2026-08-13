package view.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import model.AppInfo
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun AppManagerDialog(
    visible: Boolean,
    appList: List<AppInfo>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onUninstall: (packageName: String) -> Unit,
    onBackup: (packageName: String, destinationPath: String) -> Unit
) {
    if (!visible) return

    var query by remember { mutableStateOf("") }
    var pendingUninstall by remember { mutableStateOf<AppInfo?>(null) }
    val filteredApps = remember(appList, query) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) appList else appList.filter { app ->
            app.label.contains(normalizedQuery, ignoreCase = true) ||
                app.packageName.contains(normalizedQuery, ignoreCase = true)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.72f).fillMaxHeight(0.82f),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "应用管理",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "关闭")
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("搜索应用或包名") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) }
                )
                Spacer(Modifier.height(12.dp))

                when {
                    isLoading && appList.isEmpty() -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }

                    filteredApps.isEmpty() -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (query.isBlank()) "未找到第三方应用" else "没有匹配的应用",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    else -> LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            AppRow(
                                app = app,
                                onBackup = {
                                    chooseApkDestination(app)?.let { destination ->
                                        onBackup(app.packageName, destination)
                                    }
                                },
                                onUninstall = { pendingUninstall = app }
                            )
                            HorizontalDivider()
                        }
                    }
                }

                if (isLoading && appList.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("正在读取应用信息…", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    pendingUninstall?.let { app ->
        AlertDialog(
            onDismissRequest = { pendingUninstall = null },
            title = { Text("卸载应用") },
            text = { Text("确定要卸载 ${app.label}\n(${app.packageName}) 吗？") },
            confirmButton = {
                Button(onClick = {
                    pendingUninstall = null
                    onUninstall(app.packageName)
                }) { Text("卸载") }
            },
            dismissButton = {
                TextButton(onClick = { pendingUninstall = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun AppRow(
    app: AppInfo,
    onBackup: () -> Unit,
    onUninstall: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (app.label != app.packageName) {
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = onBackup) {
            Icon(Icons.Outlined.Download, contentDescription = "备份 APK")
        }
        IconButton(onClick = onUninstall) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "卸载",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun chooseApkDestination(app: AppInfo): String? {
    val chooser = JFileChooser().apply {
        dialogTitle = "保存 APK"
        dialogType = JFileChooser.SAVE_DIALOG
        fileFilter = FileNameExtensionFilter("Android package (*.apk)", "apk")
        selectedFile = File(sanitizeFileName(app.label).ifBlank { app.packageName } + ".apk")
    }
    if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return null
    val selected = chooser.selectedFile ?: return null
    return if (selected.extension.equals("apk", ignoreCase = true)) {
        selected.absolutePath
    } else {
        selected.absolutePath + ".apk"
    }
}

private fun sanitizeFileName(value: String): String =
    value.replace(Regex("[<>:\"/\\|?*]"), "_").trim().trimEnd('.')
