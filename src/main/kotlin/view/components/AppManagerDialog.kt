package view.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import model.AppInfo
import org.jetbrains.skia.Image as SkiaImage
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun AppManagerDialog(
    visible: Boolean,
    appList: List<AppInfo>,
    isLoading: Boolean,
    onRequestDetails: (packageName: String) -> Unit,
    onDismiss: () -> Unit,
    onUninstall: (packageName: String) -> Unit,
    onBackup: (packageName: String, destinationPath: String) -> Unit,
    onLaunch: (packageName: String) -> Unit = {},
    onForceStop: (packageName: String) -> Unit = {},
    onClearData: (packageName: String) -> Unit = {}
) {
    if (!visible) return

    var query by remember { mutableStateOf("") }
    var pendingUninstall by remember { mutableStateOf<AppInfo?>(null) }
    var pendingClearData by remember { mutableStateOf<AppInfo?>(null) }

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
            modifier = Modifier.fillMaxWidth(0.76f).fillMaxHeight(0.85f),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Apps,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
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
                    label = { Text("搜索应用名称或包名") },
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
                            text = if (query.isBlank()) "未找到已安装的第三方应用" else "没有匹配的应用",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    else -> LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            AppRow(
                                app = app,
                                onRequestDetails = { onRequestDetails(app.packageName) },
                                onLaunch = { onLaunch(app.packageName) },
                                onForceStop = { onForceStop(app.packageName) },
                                onClearData = { pendingClearData = app },
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
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        val target = pendingUninstall?.packageName
                        pendingUninstall = null
                        if (target != null) onUninstall(target)
                    }
                ) { Text("卸载") }
            },
            dismissButton = {
                TextButton(onClick = { pendingUninstall = null }) { Text("取消") }
            }
        )
    }

    pendingClearData?.let { app ->
        AlertDialog(
            onDismissRequest = { pendingClearData = null },
            title = { Text("清除应用数据") },
            text = { Text("确定要清除 ${app.label} 的全部数据与缓存吗？此操作不可逆。") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        val target = pendingClearData?.packageName
                        pendingClearData = null
                        if (target != null) onClearData(target)
                    }
                ) { Text("清除数据") }
            },
            dismissButton = {
                TextButton(onClick = { pendingClearData = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun AppRow(
    app: AppInfo,
    onRequestDetails: () -> Unit,
    onLaunch: () -> Unit,
    onForceStop: () -> Unit,
    onClearData: () -> Unit,
    onBackup: () -> Unit,
    onUninstall: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val iconBitmap = remember(app.iconData) {
        app.iconData?.let { data ->
            runCatching { SkiaImage.makeFromEncoded(data).toComposeImageBitmap() }.getOrNull()
        }
    }

    LaunchedEffect(app.packageName, app.detailsLoaded) {
        if (!app.detailsLoaded) onRequestDetails()
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp)
        ) {
            when {
                iconBitmap != null -> Image(
                    bitmap = iconBitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(3.dp),
                    contentScale = ContentScale.Fit
                )
                !app.detailsLoaded -> Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                }
                else -> Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Apps,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
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

        // 启动应用
        IconButton(onClick = onLaunch) {
            Icon(
                Icons.Outlined.PlayArrow,
                contentDescription = "运行应用",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        // 备份 APK
        IconButton(onClick = onBackup) {
            Icon(Icons.Outlined.Download, contentDescription = "备份 APK")
        }

        // 更多操作
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "更多操作")
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("运行应用") },
                    leadingIcon = { Icon(Icons.Outlined.PlayArrow, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onLaunch()
                    }
                )
                DropdownMenuItem(
                    text = { Text("强行停止") },
                    leadingIcon = { Icon(Icons.Outlined.Stop, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onForceStop()
                    }
                )
                DropdownMenuItem(
                    text = { Text("清除数据与缓存") },
                    leadingIcon = { Icon(Icons.Outlined.CleaningServices, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onClearData()
                    }
                )
                DropdownMenuItem(
                    text = { Text("导出 APK") },
                    leadingIcon = { Icon(Icons.Outlined.Download, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onBackup()
                    }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("卸载应用", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    onClick = {
                        showMenu = false
                        onUninstall()
                    }
                )
            }
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
