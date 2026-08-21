package view.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import view.state.ScrcpyPhase

@Composable
fun ScrcpyDialog(
    visible: Boolean,
    phase: ScrcpyPhase,
    downloadProgress: Float,
    downloadedBytes: Long,
    totalBytes: Long,
    statusText: String,
    version: String,
    error: String?,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit
) {
    if (!visible) return

    Dialog(
        onDismissRequest = {
            // 仅在非下载中状态允许关闭
            if (phase != ScrcpyPhase.DOWNLOADING && phase != ScrcpyPhase.EXTRACTING) {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .width(420.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 标题行
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Outlined.Cast,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = "一键投屏 (scrcpy)",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (version.isNotEmpty()) {
                            Text(
                                text = "v$version",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider()

                // 主体内容
                when (phase) {
                    ScrcpyPhase.CHECKING -> {
                        CheckingPhaseContent(statusText)
                    }
                    ScrcpyPhase.DOWNLOADING -> {
                        DownloadingPhaseContent(downloadProgress, downloadedBytes, totalBytes, statusText)
                    }
                    ScrcpyPhase.EXTRACTING -> {
                        ExtractingPhaseContent(statusText)
                    }
                    ScrcpyPhase.ERROR -> {
                        ErrorPhaseContent(
                            errorMessage = error ?: "发生未知错误",
                            onRetry = onRetry,
                            onDismiss = onDismiss,
                            onOpenSettings = onOpenSettings
                        )
                    }
                    ScrcpyPhase.IDLE -> {
                        // 显示检查中作为默认
                        CheckingPhaseContent("正在初始化…")
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckingPhaseContent(statusText: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            strokeWidth = 3.dp
        )
        Text(
            text = statusText.ifEmpty { "正在检查 scrcpy 最新版本…" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DownloadingPhaseContent(
    progress: Float,
    downloadedBytes: Long,
    totalBytes: Long,
    statusText: String
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 200),
        label = "download_progress"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = statusText.ifEmpty { "正在下载 scrcpy…" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = buildString {
                append("${(animatedProgress * 100).toInt()}%")
                if (totalBytes > 0) {
                    append("  ·  ${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)}")
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "请勿关闭应用，下载完成后将自动启动投屏",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ExtractingPhaseContent(statusText: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            strokeWidth = 3.dp
        )
        Text(
            text = statusText.ifEmpty { "正在解压文件…" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorPhaseContent(
    errorMessage: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "下载失败",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        // 错误详情
        Surface(
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(12.dp),
                textAlign = TextAlign.Start
            )
        }
        Text(
            text = "请检查网络连接后重试，或在「设置」中手动指定 scrcpy 路径。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(onClick = onDismiss) {
                Text("关闭")
            }
            OutlinedButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("手动指定路径")
            }
            Button(onClick = onRetry) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("重试")
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB")
    var value = bytes.toDouble()
    var unitIndex = -1
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex++
    }
    return "%.1f %s".format(value, units[unitIndex])
}
