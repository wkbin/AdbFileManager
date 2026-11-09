package view.components

import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.ArrowClockwise
import io.github.composefluent.icons.regular.Cast
import io.github.composefluent.icons.regular.ErrorCircle
import io.github.composefluent.icons.regular.Settings

import io.github.composefluent.FluentTheme
import io.github.composefluent.component.Text
import io.github.composefluent.component.Icon
import view.fluent.*

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
            shape = FluentTheme.shapes.overlay,

            color = FluentTheme.colors.background.solid.tertiary
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
                        Icons.Regular.Cast,
                        contentDescription = null,
                        tint = FluentTheme.colors.text.accent.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = "一键投屏 (scrcpy)",
                            style = FluentTheme.typography.subtitle,
                            color = FluentTheme.colors.text.text.primary
                        )
                        if (version.isNotEmpty()) {
                            Text(
                                text = "v$version",
                                style = FluentTheme.typography.caption,
                                color = FluentTheme.colors.text.text.secondary
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
            style = FluentTheme.typography.body,
            color = FluentTheme.colors.text.text.secondary,
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
            style = FluentTheme.typography.body,
            color = FluentTheme.colors.text.text.primary
        )
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth().height(8.dp),

            color = FluentTheme.colors.text.accent.primary
        )
        Text(
            text = buildString {
                append("${(animatedProgress * 100).toInt()}%")
                if (totalBytes > 0) {
                    append("  ·  ${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)}")
                }
            },
            style = FluentTheme.typography.caption,
            color = FluentTheme.colors.text.text.secondary
        )
        Text(
            text = "请勿关闭应用，下载完成后将自动启动投屏",
            style = FluentTheme.typography.caption,
            color = FluentTheme.colors.text.text.secondary,
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
            style = FluentTheme.typography.body,
            color = FluentTheme.colors.text.text.secondary,
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
            Icons.Regular.ErrorCircle,
            contentDescription = null,
            tint = FluentTheme.colors.system.critical,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "下载失败",
            style = FluentTheme.typography.bodyStrong,
            color = FluentTheme.colors.system.critical
        )
        // 错误详情
        Surface(
            color = FluentTheme.colors.system.criticalBackground.copy(alpha = 0.4f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = errorMessage,
                style = FluentTheme.typography.caption.copy(fontFamily = FontFamily.Monospace),
                color = FluentTheme.colors.system.critical,
                modifier = Modifier.padding(12.dp),
                textAlign = TextAlign.Start
            )
        }
        Text(
            text = "请检查网络连接后重试，或在「设置」中手动指定 scrcpy 路径。",
            style = FluentTheme.typography.caption,
            color = FluentTheme.colors.text.text.secondary,
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
                Icon(Icons.Regular.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("手动指定路径")
            }
            Button(onClick = onRetry) {
                Icon(
                    Icons.Regular.ArrowClockwise,
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
