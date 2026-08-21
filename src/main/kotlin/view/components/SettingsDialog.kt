package view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import model.AdbMode
import model.AppSettings
import model.ScrcpyManager
import model.SortType
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun SettingsDialog(
    visible: Boolean,
    bundledAdbPath: String,
    onDismiss: () -> Unit,
    onSaved: () -> Unit = {}
) {
    if (!visible) return

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var adbMode by remember(visible) { mutableStateOf(AppSettings.adbMode) }
    var customPath by remember(visible) { mutableStateOf(AppSettings.customAdbPath) }
    var defaultExportDir by remember(visible) { mutableStateOf(AppSettings.defaultExportDir) }
    var showHiddenFiles by remember(visible) { mutableStateOf(AppSettings.showHiddenFiles) }
    var defaultSortType by remember(visible) { mutableStateOf(AppSettings.defaultSortType) }
    var scrcpyMaxFps by remember(visible) { mutableStateOf(AppSettings.scrcpyMaxFps.toString()) }
    var scrcpyBitrate by remember(visible) { mutableStateOf(AppSettings.scrcpyBitrate.toString()) }
    var scrcpyTurnScreenOff by remember(visible) { mutableStateOf(AppSettings.scrcpyTurnScreenOff) }
    var scrcpyStayAwake by remember(visible) { mutableStateOf(AppSettings.scrcpyStayAwake) }
    var scrcpyCustomPath by remember(visible) { mutableStateOf(AppSettings.scrcpyCustomPath) }
    var isUpdatingScrcpy by remember(visible) { mutableStateOf(false) }
    var scrcpyUpdateStatus by remember(visible) { mutableStateOf<String?>(null) }

    // Test ADB state
    var isTestingAdb by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val effectivePath = remember(adbMode, customPath, bundledAdbPath) {
        when (adbMode) {
            AdbMode.BUNDLED -> bundledAdbPath
            AdbMode.CUSTOM -> customPath.trim().ifEmpty { bundledAdbPath }
            AdbMode.SYSTEM -> if (org.jetbrains.skiko.hostOs.isWindows) "adb.exe" else "adb"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "应用设置",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "关闭")
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(16.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                ) {
                    // ── ADB Configuration ──────────────────────────────────
                    Text(
                        text = "ADB 运行与版本配置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "选择 ADB 服务的执行源。可使用内置经过兼容性适配的版本，或指定系统自备版本。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    // Mode selections
                    AdbMode.entries.forEach { mode ->
                        val isSelected = adbMode == mode
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { adbMode = mode },
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            else
                                MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { adbMode = mode }
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = mode.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    Text(
                                        text = when (mode) {
                                            AdbMode.BUNDLED -> "使用应用内置自带的 ADB 运行时 ($bundledAdbPath)"
                                            AdbMode.CUSTOM -> "手动选择自定义的 adb 或 adb.exe 绝对路径"
                                            AdbMode.SYSTEM -> "直接通过系统全局 PATH 环境变量调用 adb"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Custom path input (shown when CUSTOM is selected)
                    if (adbMode == AdbMode.CUSTOM) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customPath,
                                onValueChange = { customPath = it },
                                label = { Text("自定义 ADB 可执行文件路径") },
                                placeholder = { Text("例如 C:\\platform-tools\\adb.exe 或 /usr/bin/adb") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            FilledTonalButton(
                                onClick = {
                                    val chooser = JFileChooser().apply {
                                        dialogTitle = "选择 adb 可执行文件"
                                        fileSelectionMode = JFileChooser.FILES_ONLY
                                    }
                                    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                        customPath = chooser.selectedFile.absolutePath
                                    }
                                }
                            ) {
                                Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("浏览...")
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Test ADB Version Button & Result Box
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                isTestingAdb = true
                                testResult = null
                                coroutineScope.launch {
                                    val result = AppSettings.testAdbVersion(effectivePath)
                                    testResult = result
                                    isTestingAdb = false
                                }
                            },
                            enabled = !isTestingAdb
                        ) {
                            if (isTestingAdb) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Outlined.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("测试 ADB 状态与版本")
                        }
                    }

                    testResult?.let { (success, msg) ->
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (success) Color(0xFF1E3A2F) else Color(0xFF3B2020),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (success) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (success) Color(0xFF4CAF50) else Color(0xFFEF5350),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (success) "ADB 测试通过，服务正常" else "ADB 测试失败",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (success) Color(0xFF81C784) else Color(0xFFE57373)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = msg,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = Color(0xFFE0E0E0)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))

                    // ── General Preferences ────────────────────────────────
                    Text(
                        text = "通用与导出偏好",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))

                    // Default Export Directory
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = defaultExportDir,
                            onValueChange = { defaultExportDir = it },
                            label = { Text("默认下载/导出目录") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = {
                                val chooser = JFileChooser().apply {
                                    dialogTitle = "选择默认导出目录"
                                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                                    selectedFile = File(defaultExportDir)
                                }
                                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                    defaultExportDir = chooser.selectedFile.absolutePath
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("更改...")
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Show hidden files switch
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showHiddenFiles = !showHiddenFiles },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("显示隐藏文件", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("列出以点 (.) 开头的隐藏文件与文件夹", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = showHiddenFiles,
                                onCheckedChange = { showHiddenFiles = it }
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Scrcpy 投屏配置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "留空自定义路径时，首次投屏会自动下载官方最新版本。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = scrcpyBitrate,
                            onValueChange = { value ->
                                if (value.all(Char::isDigit) && value.length <= 3) scrcpyBitrate = value
                            },
                            label = { Text("最大码率 (Mbps，0=默认)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = scrcpyMaxFps,
                            onValueChange = { value ->
                                if (value.all(Char::isDigit) && value.length <= 3) scrcpyMaxFps = value
                            },
                            label = { Text("最大帧率 (0=默认)") },
                            singleLine = true,
                            isError = (scrcpyMaxFps.toIntOrNull() ?: 0) !in 0..120,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ScrcpySettingSwitch(
                            title = "投屏时关闭设备屏幕",
                            checked = scrcpyTurnScreenOff,
                            onCheckedChange = { scrcpyTurnScreenOff = it },
                            modifier = Modifier.weight(1f)
                        )
                        ScrcpySettingSwitch(
                            title = "投屏期间保持常亮",
                            checked = scrcpyStayAwake,
                            onCheckedChange = { scrcpyStayAwake = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = scrcpyCustomPath,
                            onValueChange = { scrcpyCustomPath = it },
                            label = { Text("手动指定 scrcpy 路径（可选）") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = {
                                val chooser = JFileChooser().apply {
                                    dialogTitle = "选择 scrcpy 可执行文件"
                                    fileSelectionMode = JFileChooser.FILES_ONLY
                                    scrcpyCustomPath.takeIf(String::isNotBlank)?.let { selectedFile = File(it) }
                                }
                                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                    scrcpyCustomPath = chooser.selectedFile.absolutePath
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("浏览")
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        enabled = !isUpdatingScrcpy,
                        onClick = {
                            isUpdatingScrcpy = true
                            scrcpyUpdateStatus = "正在检查最新版本…"
                            coroutineScope.launch {
                                runCatching {
                                    ScrcpyManager.forceUpdate(
                                        onProgress = { progress ->
                                            coroutineScope.launch {
                                                scrcpyUpdateStatus = "正在下载… ${(progress.fraction * 100).toInt()}%"
                                            }
                                        },
                                        onStatusText = { status ->
                                            coroutineScope.launch { scrcpyUpdateStatus = status }
                                        }
                                    )
                                }.onSuccess {
                                    scrcpyUpdateStatus = "scrcpy 已更新并可供下次投屏使用"
                                }.onFailure {
                                    scrcpyUpdateStatus = "更新失败：${it.message ?: "未知错误"}"
                                }
                                isUpdatingScrcpy = false
                            }
                        }
                    ) {
                        if (isUpdatingScrcpy) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.CloudDownload, contentDescription = null, Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("重新下载 scrcpy（强制检查）")
                    }
                    scrcpyUpdateStatus?.let { status ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            status,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (status.startsWith("更新失败")) {
                                MaterialTheme.colorScheme.error
                            } else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(16.dp))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            AppSettings.resetToDefaults()
                            adbMode = AppSettings.adbMode
                            customPath = AppSettings.customAdbPath
                            defaultExportDir = AppSettings.defaultExportDir
                            showHiddenFiles = AppSettings.showHiddenFiles
                            defaultSortType = AppSettings.defaultSortType
                            scrcpyMaxFps = AppSettings.scrcpyMaxFps.toString()
                            scrcpyBitrate = AppSettings.scrcpyBitrate.toString()
                            scrcpyTurnScreenOff = AppSettings.scrcpyTurnScreenOff
                            scrcpyStayAwake = AppSettings.scrcpyStayAwake
                            scrcpyCustomPath = AppSettings.scrcpyCustomPath
                            testResult = null
                        }
                    ) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("恢复默认")
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onDismiss) {
                            Text("取消")
                        }
                        Button(
                            onClick = {
                                AppSettings.adbMode = adbMode
                                AppSettings.customAdbPath = customPath
                                AppSettings.defaultExportDir = defaultExportDir
                                AppSettings.showHiddenFiles = showHiddenFiles
                                AppSettings.defaultSortType = defaultSortType
                                AppSettings.scrcpyMaxFps = (scrcpyMaxFps.toIntOrNull() ?: 0).coerceIn(0, 120)
                                AppSettings.scrcpyBitrate = (scrcpyBitrate.toIntOrNull() ?: 0).coerceAtLeast(0)
                                AppSettings.scrcpyTurnScreenOff = scrcpyTurnScreenOff
                                AppSettings.scrcpyStayAwake = scrcpyStayAwake
                                AppSettings.scrcpyCustomPath = scrcpyCustomPath.trim()
                                onSaved()
                                onDismiss()
                            }
                        ) {
                            Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("保存设置")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScrcpySettingSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!checked) },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
