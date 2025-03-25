package view.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import model.FileItem
import kotlin.math.min

/**
 * 文件列表项组件
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun FileListItem(
    file: FileItem,
    onFileClick: () -> Unit,
    onEditFile: () -> Unit,
    onDeleteFile: () -> Unit,
    onDownloadFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    
    // 状态
    var isHovered by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    var showDropdown by remember { mutableStateOf(false) }
    
    // 动画
    val elevation by animateDpAsState(
        targetValue = if (isHovered) 2.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "elevationAnimation"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.01f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "scaleAnimation"
    )
    
    val backgroundColor = if (isHovered) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val interactionSource = remember { MutableInteractionSource() }
    
    // 主卡片
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
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
                indication = null
            ) {
                if (file.isDir) {
                    coroutineScope.launch {
                        isExpanded = true
                        // 添加一个短暂的延迟来显示动画效果
                        kotlinx.coroutines.delay(150)
                        onFileClick()
                    }
                } else {
                    onFileClick()
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 文件图标容器
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
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
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 文件信息（大小和日期）
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 文件类型标签（仅对目录显示）
                    if (file.isDir) {
                        Surface(
                            modifier = Modifier.padding(end = 8.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "目录",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
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
            AnimatedVisibility(
                visible = isHovered,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 编辑按钮 - 只为非目录文件显示
                    if (!file.isDir) {
                        IconButton(onClick = onEditFile) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "编辑",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                    
                    // 下载按钮
                    IconButton(onClick = onDownloadFile) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = "下载",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // 删除按钮
                    IconButton(onClick = onDeleteFile) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // 更多按钮 - 用于非悬停状态
            AnimatedVisibility(
                visible = !isHovered,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box {
                    IconButton(onClick = { showDropdown = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "更多操作",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false }
                    ) {
                        // 编辑选项 - 仅对非目录文件显示
                        if (!file.isDir) {
                            DropdownMenuItem(
                                text = { Text("编辑") },
                                onClick = { 
                                    showDropdown = false
                                    onEditFile() 
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                        
                        // 下载选项
                        DropdownMenuItem(
                            text = { Text("下载") },
                            onClick = { 
                                showDropdown = false
                                onDownloadFile() 
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = null
                                )
                            }
                        )
                        
                        // 删除选项
                        DropdownMenuItem(
                            text = { Text("删除") },
                            onClick = { 
                                showDropdown = false
                                onDeleteFile() 
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }
        }
    }
} 