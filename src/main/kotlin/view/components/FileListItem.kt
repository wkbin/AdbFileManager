package view.components

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.ArrowDownload
import io.github.composefluent.icons.regular.Copy
import io.github.composefluent.icons.regular.Delete
import io.github.composefluent.icons.regular.Edit
import io.github.composefluent.icons.regular.FolderArrowRight
import io.github.composefluent.icons.regular.LockClosed
import io.github.composefluent.icons.regular.MoreVertical
import io.github.composefluent.icons.regular.Play
import io.github.composefluent.icons.regular.ReOrderDotsVertical

import io.github.composefluent.FluentTheme
import io.github.composefluent.component.Text
import io.github.composefluent.component.Icon
import view.fluent.*

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
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
 * 判断文件是否为图片
 */
fun isImageFile(fileName: String): Boolean {
    if (fileName.isBlank()) return false
    val extension = fileName.substringAfterLast('.', "").lowercase()
    return extension in setOf("png", "jpg", "jpeg", "webp", "bmp", "gif", "ico")
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
    onPreviewImage: () -> Unit = {},
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    isHighlighted: Boolean = false,
    onSelectionChange: () -> Unit = {},
    compact: Boolean = false,
    allowEdit: Boolean = true,
    allowAdministrativeActions: Boolean = true,
    allowMove: Boolean = true,
    copyEnabled: Boolean = true,
    moveEnabled: Boolean = true,
    showDragHandle: Boolean = false,
    dragHandleModifier: Modifier = Modifier,
    copyIcon: ImageVector = Icons.Regular.Copy,
    modifier: Modifier = Modifier
) {
    // 状态
    var isHovered by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    val strings by StringsManager.strings.collectAsState()

    val backgroundColor = when {
        isSelected -> FluentTheme.colors.fillAccent.default.copy(alpha = 0.12f)
        isHighlighted -> FluentTheme.colors.fillAccent.default.copy(alpha = 0.08f)
        isHovered -> FluentTheme.colors.subtleFill.secondary
        else -> FluentTheme.colors.background.solid.tertiary
    }

    val border = if (isHighlighted && !isSelected) {
        androidx.compose.foundation.BorderStroke(1.5.dp, FluentTheme.colors.text.accent.primary)
    } else null

    val interactionSource = remember { MutableInteractionSource() }

    // 判断文件是否可编辑
    val isEditable = allowEdit && !file.isDir && isEditableFile(file.fileName)

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
                if (selectionMode) onSelectionChange() else onFileClick()
            },
        border = border,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = FluentTheme.shapes.control
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (compact) 8.dp else 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelectionChange() }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // 文件图标容器
            Box(
                modifier = Modifier
                    .size(32.dp)
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

            Spacer(modifier = Modifier.width(if (compact) 10.dp else 16.dp))

            // 文件信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 文件名，如果是链接，显示链接信息
                Text(
                    text = file.link?.let { "${file.fileName} → $it" } ?: file.fileName,
                    style = FluentTheme.typography.bodyStrong,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (file.link != null) FluentTheme.colors.text.accent.primary else FluentTheme.colors.text.text.primary
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
                            color = FluentTheme.colors.text.accent.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = if (file.link != null) strings.fileLinkDirectory else strings.fileDirectory,
                                style = FluentTheme.typography.caption,
                                color = FluentTheme.colors.text.accent.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (file.link != null) {
                        Surface(
                            modifier = Modifier.padding(end = 8.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = FluentTheme.colors.text.accent.secondary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = strings.fileLink,
                                style = FluentTheme.typography.caption,
                                color = FluentTheme.colors.text.accent.secondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // 权限标签
                    if (!compact) Surface(
                        modifier = Modifier.padding(end = 8.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = FluentTheme.colors.text.accent.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = file.permissions,
                            style = FluentTheme.typography.caption,
                            color = FluentTheme.colors.text.accent.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // 文件大小
                    Text(
                        text = file.size,
                        style = FluentTheme.typography.body,
                        color = FluentTheme.colors.text.text.secondary,
                        modifier = Modifier.alpha(0.8f)
                    )

                    // 分隔点
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(FluentTheme.colors.text.text.secondary.copy(alpha = 0.3f))
                    )

                    // 修改日期
                    Text(
                        text = file.date,
                        style = FluentTheme.typography.body,
                        color = FluentTheme.colors.text.text.secondary,
                        modifier = Modifier.alpha(0.8f)
                    )
                }
            }

            // 操作按钮
            Row(
                modifier = Modifier
                    .height(32.dp)
                    .alpha(if (!selectionMode && isHovered) 1f else 0f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 拖放仅从独立手柄开始，避免点击进入目录时因鼠标轻微移动而误触复制。
                if (!selectionMode && showDragHandle) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .then(dragHandleModifier),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Regular.ReOrderDotsVertical,
                            contentDescription = strings.fileDragToCopy,
                            tint = FluentTheme.colors.text.text.secondary
                        )
                    }
                }

                // 编辑按钮 - 只为可编辑的文件显示
                if (!selectionMode && isEditable) {
                    IconButton(
                        onClick = onEditFile,
                        enabled = isHovered
                    ) {
                        Icon(
                            imageVector = Icons.Regular.Edit,
                            contentDescription = strings.fileEdit,
                            tint = FluentTheme.colors.text.accent.primary
                        )
                    }
                }

                // 重命名按钮
                if (!selectionMode && !compact) IconButton(
                    onClick = onRenameFile,
                    enabled = isHovered
                ) {
                    Icon(
                        imageVector = Icons.Regular.Edit,
                        contentDescription = strings.fileRename,
                        tint = FluentTheme.colors.text.accent.primary
                    )
                }

                // APK 安装按钮 - 仅为 .apk 文件显示
                val isApk = file.fileName.endsWith(".apk", ignoreCase = true)
                if (!selectionMode && isApk && !compact) {
                    IconButton(
                        onClick = onInstallApk,
                        enabled = isHovered
                    ) {
                        Icon(
                            imageVector = Icons.Regular.Play,
                            contentDescription = strings.fileInstallApk,
                            tint = FluentTheme.colors.text.accent.primary
                        )
                    }
                }

                // 修改权限按钮
                if (!selectionMode && !compact) IconButton(
                    onClick = onChangePermissions,
                    enabled = isHovered
                ) {
                    Icon(
                        imageVector = Icons.Regular.LockClosed,
                        contentDescription = strings.changePermissions,
                        tint = FluentTheme.colors.text.accent.secondary
                    )
                }

                // 复制按钮
                if (!selectionMode) IconButton(
                    onClick = onCopyFile,
                    enabled = isHovered && copyEnabled
                ) {
                    Icon(
                        imageVector = copyIcon,
                        contentDescription = strings.fileCopy,
                        tint = FluentTheme.colors.text.accent.secondary
                    )
                }

                if (!selectionMode && compact && allowAdministrativeActions) Box {
                    IconButton(onClick = { showMoreMenu = true }, enabled = isHovered) {
                        Icon(
                            imageVector = Icons.Regular.MoreVertical,
                            contentDescription = "更多操作",
                            tint = FluentTheme.colors.text.text.secondary
                        )
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        if (isApk) DropdownMenuItem(
                            text = { Text(strings.fileInstallApk) },
                            onClick = { showMoreMenu = false; onInstallApk() }
                        )
                        if (isImageFile(file.fileName)) DropdownMenuItem(
                            text = { Text("预览图片") },
                            onClick = { showMoreMenu = false; onPreviewImage() }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.fileRename) },
                            onClick = { showMoreMenu = false; onRenameFile() }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.changePermissions) },
                            onClick = { showMoreMenu = false; onChangePermissions() }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.fileDownload) },
                            onClick = { showMoreMenu = false; onDownloadFile() }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.fileDelete) },
                            onClick = { showMoreMenu = false; onDeleteFile() }
                        )
                    }
                }

                // 移动按钮
                if (!selectionMode && allowMove) IconButton(
                    onClick = onMoveFile,
                    enabled = isHovered && moveEnabled
                ) {
                    Icon(
                        imageVector = Icons.Regular.FolderArrowRight,
                        contentDescription = strings.fileMove,
                        tint = FluentTheme.colors.text.accent.secondary
                    )
                }

                // 下载按钮
                if (!selectionMode && !compact) IconButton(
                    onClick = onDownloadFile,
                    enabled = isHovered
                ) {
                    Icon(
                        imageVector = Icons.Regular.ArrowDownload,
                        contentDescription = strings.fileDownload,
                        tint = FluentTheme.colors.text.accent.primary
                    )
                }

                // 删除按钮
                if (!selectionMode && !compact) IconButton(
                    onClick = onDeleteFile,
                    enabled = isHovered
                ) {
                    Icon(
                        imageVector = Icons.Regular.Delete,
                        contentDescription = strings.fileDelete,
                        tint = FluentTheme.colors.system.critical
                    )
                }
            }
        }
    }
}
