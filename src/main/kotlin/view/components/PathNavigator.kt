package view.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * 路径导航组件
 * 
 * @param currentPath 当前路径列表
 * @param onPathClick 路径点击回调，参数为路径索引
 * @param onPathInput 路径输入回调，参数为输入的完整路径
 */
@Composable
fun PathNavigator(
    currentPath: List<String>,
    onPathClick: (Int) -> Unit,
    onPathInput: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 是否应显示完整路径
    var showFullPath by remember { mutableStateOf(false) }
    // 是否显示路径输入对话框
    var showPathInputDialog by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 根目录按钮
            PathItem(
                text = "根目录",
                isRoot = true,
                onClick = { onPathClick(-1) }
            )
            
            // 路径分隔符
            if (currentPath.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 检查路径长度，如果过长则折叠
            if (currentPath.size > 3 && !showFullPath) {
                // 显示省略号按钮
                PathItem(
                    text = "...",
                    onClick = { showFullPath = true },
                    isSpecial = true
                )
                
                // 分隔符
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 只显示最后两级路径
                currentPath.takeLast(2).forEachIndexed { i, pathSegment ->
                    val index = currentPath.size - 2 + i
                    
                    // 显示路径项
                    PathItem(
                        text = pathSegment,
                        onClick = { onPathClick(index) },
                        isLast = index == currentPath.size - 1
                    )
                    
                    // 添加分隔符（除了最后一项）
                    if (index < currentPath.size - 1) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // 显示完整路径
                currentPath.forEachIndexed { index, pathSegment ->
                    // 显示路径项
                    PathItem(
                        text = pathSegment,
                        onClick = { onPathClick(index) },
                        isLast = index == currentPath.size - 1
                    )
                    
                    // 添加分隔符（除了最后一项）
                    if (index < currentPath.size - 1) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // 显示折叠按钮（如果路径很长）
                if (currentPath.size > 3 && showFullPath) {
                    TextButton(
                        onClick = { showFullPath = false },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = "折叠",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // 添加编辑按钮
            IconButton(
                onClick = { showPathInputDialog = true },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "编辑路径",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
    
    // 路径输入对话框
    if (showPathInputDialog) {
        var inputPath by remember { mutableStateOf(currentPath.joinToString("/")) }
        val focusRequester = remember { FocusRequester() }
        
        Dialog(
            onDismissRequest = { showPathInputDialog = false }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "输入路径",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = inputPath,
                        onValueChange = { inputPath = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                onPathInput(inputPath)
                                showPathInputDialog = false
                            }
                        ),
                        singleLine = true,
                        placeholder = { Text("输入目标路径") }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showPathInputDialog = false }
                        ) {
                            Text("取消")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                onPathInput(inputPath)
                                showPathInputDialog = false
                            }
                        ) {
                            Text("确定")
                        }
                    }
                }
            }
        }
        
        // 自动聚焦到输入框
        LaunchedEffect(showPathInputDialog) {
            if (showPathInputDialog) {
                focusRequester.requestFocus()
            }
        }
    }
}

/**
 * 路径项组件
 */
@Composable
private fun PathItem(
    text: String,
    onClick: () -> Unit,
    isRoot: Boolean = false,
    isLast: Boolean = false,
    isSpecial: Boolean = false
) {
    val backgroundColor = when {
        isLast -> MaterialTheme.colorScheme.primaryContainer
        isSpecial -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
    
    val contentColor = when {
        isLast -> MaterialTheme.colorScheme.onPrimaryContainer
        isSpecial -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 根目录图标
            if (isRoot) {
                Icon(
                    imageVector = Icons.Rounded.PhoneAndroid,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            
            // 文本
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
} 