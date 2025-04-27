package view.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import model.FileItem
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import com.darkrockstudios.libraries.mpfilepicker.DirectoryPicker
import org.jetbrains.compose.resources.painterResource
import viewmodel.FileManagerViewModel

/**
 * 搜索对话框组件
 */
@Composable
fun SearchDialog(
    viewModel:FileManagerViewModel,
    visible: Boolean,
    onDismiss: () -> Unit,
    onFileClick: (FileItem) -> Unit,
    onEditFile: (FileItem) -> Unit = {},
    onDeleteFile: (FileItem) -> Unit = {},
    searchResults: List<FileItem>,
    onSearch: (String) -> Unit,
    isLoading: Boolean
) {
    if (visible) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // 标题栏
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "搜索文件",
                            style = MaterialTheme.typography.titleLarge
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "关闭")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 搜索框
                    val focusRequester = remember { FocusRequester() }
                    var searchQuery by remember { mutableStateOf("") }
                    
                    // 防抖搜索
                    LaunchedEffect(searchQuery) {
                        if (searchQuery.isNotEmpty()) {
                            // 延迟500毫秒后执行搜索
                            delay(500)
                            onSearch(searchQuery)
                        }
                    }

                    // 自动聚焦搜索框
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { 
                            searchQuery = it
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        placeholder = { Text("输入文件名搜索...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 搜索结果
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotEmpty()) "没有找到匹配的文件" else "输入关键字开始搜索",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(searchResults) { file ->
                                var showDirectoryPicker by remember { mutableStateOf(false) }
                                DirectoryPicker(showDirectoryPicker) { path ->
                                    showDirectoryPicker = false
                                    path?.let {
                                        viewModel.pullFile(file.fileName, it) {
                                            // 拉取完成
                                        }
                                    }
                                }

                                SearchResultItem(
                                    file = file,
                                    onClick = { onFileClick(file) },
                                    onEditFile = { onEditFile(file) },
                                    onDeleteFile = { onDeleteFile(file) },
                                    onDownloadFile = { showDirectoryPicker = true }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 搜索结果项组件
 */
@Composable
fun SearchResultItem(
    file: FileItem,
    onClick: () -> Unit,
    onEditFile: () -> Unit,
    onDeleteFile: () -> Unit,
    onDownloadFile: () -> Unit
) {
    // 状态
    var isHovered by remember { mutableStateOf(false) }

    // 动画
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "scaleAnimation"
    )

    val backgroundColor = if (isHovered) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val interactionSource = remember { MutableInteractionSource() }

    // 判断文件是否可编辑
    val isEditable = !file.isDir && isEditableFile(file.fileName)

    // 主卡片
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .scale(scale)
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
                indication = null,
                onClick = onClick
            ),
        shape = MaterialTheme.shapes.medium,
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 文件图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        if (file.isDir)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.secondaryContainer
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(file.icon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(
                        if (file.isDir)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 文件信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.fileName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = if (file.isDir) "目录" else file.size,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                            contentDescription = "编辑",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                // 下载按钮
                IconButton(
                    onClick = onDownloadFile,
                    enabled = isHovered
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = "下载",
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
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
} 