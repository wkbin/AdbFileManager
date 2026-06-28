package view.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import model.FileItem
import model.StringsManager
import org.jetbrains.compose.resources.painterResource

/**
 * 判断文件是否可编辑
 */
fun isEditableFile(fileName: String): Boolean {
    if (fileName.isBlank()) return false

    // 获取文件扩展名
    val extension = fileName.substringAfterLast('.', "").lowercase()

    // 定义可编辑的文件类型
    val editableExtensions = setOf(
        "txt", "md", "json", "xml", "html", "css", "js", "ts",
        "jsx", "tsx", "java", "kt", "py", "sh", "bat", "c", "cpp",
        "h", "hpp", "gradle", "properties", "yaml", "yml", "toml",
        "ini", "conf", "csv", "log", "sql", "php", "rb", "cfg"
    )

    return extension in editableExtensions
}

/**
 * 文件列表项组件
 */
@Composable
fun FileListItem(
    file: FileItem,
    onFileClick: () -> Unit,
    onEditFile: () -> Unit,
    onDeleteFile: () -> Unit,
    onDownloadFile: () -> Unit,
    onChangePermissions: () -> Unit,
    onRenameFile: () -> Unit = {},
    onInstallApk: () -> Unit = {},
    onCopyFile: () -> Unit = {},
    onMoveFile: () -> Unit = {},
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // 状态
    var isHovered by remember { mutableStateOf(false) }

    val strings by StringsManager.strings.collectAsState()

    val backgroundColor = if (isHovered) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val interactionSource = remember { MutableInteractionSource() }

    // 判断文件是否可编辑
    val isEditable = !file.isDir && isEditableFile(file.fileName)

    // 主卡片
    Card(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter -> isHovered = true
                            PointerEventType.Exit -> isHovered = false
                        }
                    }
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onFileClick()
            },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 多选模式下的复选框
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onFileClick() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            // 文件图标容器
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(file.icon),
                    contentDescription = null,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 文件信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 文件名，如果是链接，显示链接信息
                Text(
                    text = file.link?.let { "${file.fileName} → $it" } ?: file.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (file.link != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 文件信息（大小和日期）
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 文件类型标签
                    if (file.isDir) {
                        Surface(
                            modifier = Modifier.padding(end = 8.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = if (file.link != null) strings.fileLinkDirectory else strings.fileDirectory,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (file.link != null) {
                        Surface(
                            modifier = Modifier.padding(end = 8.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = strings.fileLink,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // 权限标签
                    Surface(
                        modifier = Modifier.padding(end = 8.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = file.permissions,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // 文件大小
                    Text(
                        text = file.size,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.alpha(0.8f)
                    )

                    // 分隔点
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    )

                    // 修改日期
                    Text(
                        text = file.date,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.alpha(0.8f)
                    )
                }
            }

            // 操作按钮
            Row(
                modifier = Modifier
                    .height(50.dp)
                    .alpha(if (isHovered) 1f else 0f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 编辑按钮 - 只为可编辑的文件显示
                if (isEditable) {
                    IconButton(
                        onClick = onEditFile,
                        enabled = isHovered
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = strings.fileEdit,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                // 重命名按钮
                IconButton(
                    onClick = onRenameFile,
                    enabled = isHovered
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = strings.fileRename,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }

                // APK 安装按钮 - 仅为 .apk 文件显示
                val isApk = file.fileName.endsWith(".apk", ignoreCase = true)
                if (isApk) {
                    IconButton(
                        onClick = onInstallApk,
                        enabled = isHovered
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = strings.fileInstallApk,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // 修改权限按钮
                IconButton(
                    onClick = onChangePermissions,
                    enabled = isHovered
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = strings.changePermissions,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                // 复制按钮
                IconButton(
                    onClick = onCopyFile,
                    enabled = isHovered
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = strings.fileCopy,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                // 移动按钮
                IconButton(
                    onClick = onMoveFile,
                    enabled = isHovered
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DriveFileMove,
                        contentDescription = strings.fileMove,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                // 下载按钮
                IconButton(
                    onClick = onDownloadFile,
                    enabled = isHovered
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = strings.fileDownload,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // 删除按钮
                IconButton(
                    onClick = onDeleteFile,
                    enabled = isHovered
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = strings.fileDelete,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}