package view.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import view.theme.ThemeState


/**
 * 工具栏按钮
 */
@Composable
fun ToolbarButton(
    onClick: () -> Unit,
    icon: ImageVector,
    text: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = modifier.padding(horizontal = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 文件管理器工具栏
 */
@Composable
fun FileManagerToolbar(
    onCreateDirectoryClick: () -> Unit,
    onCreateFileClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onBackClick: () -> Unit,
    canNavigateUp: Boolean = false,
    onImportClick: () -> Unit = {}
) {
    // 获取当前主题模式状态
    val isSystemDark = isSystemInDarkTheme()
    val isDarkValue = ThemeState.isDarkMode.value
    val isDark = isDarkValue ?: isSystemDark
    
    // 确定主题图标
    val themeIcon = when {
        isDarkValue == null -> Icons.Outlined.Brightness6 // 跟随系统
        isDark -> Icons.Outlined.DarkMode // 暗色模式
        else -> Icons.Outlined.LightMode // 亮色模式
    }
    
    // 主题切换菜单状态
    var showThemeMenu by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween // 改为两端对齐
        ) {
            // 左侧按钮组
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                // 返回按钮
                AnimatedVisibility(
                    visible = canNavigateUp,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
                ) {
                    ToolbarButton(
                        onClick = onBackClick,
                        icon = Icons.Rounded.ArrowBack,
                        text = "返回",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                }
                
                // 创建文件夹按钮
                ToolbarButton(
                    onClick = onCreateDirectoryClick,
                    icon = Icons.Rounded.CreateNewFolder,
                    text = "新建文件夹",
                    tint = MaterialTheme.colorScheme.secondary
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                // 创建文件按钮
                ToolbarButton(
                    onClick = onCreateFileClick,
                    icon = Icons.Rounded.NoteAdd,
                    text = "新建文件",
                    tint = MaterialTheme.colorScheme.tertiary
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                // 导入文件按钮
                ToolbarButton(
                    onClick = onImportClick,
                    icon = Icons.Rounded.Upload,
                    text = "导入文件",
                    tint = MaterialTheme.colorScheme.secondary
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                // 刷新按钮
                ToolbarButton(
                    onClick = onRefreshClick,
                    icon = Icons.Rounded.Refresh,
                    text = "刷新",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            // 右侧主题切换按钮
            Box {
                IconButton(
                    onClick = { showThemeMenu = true }
                ) {
                    Icon(
                        imageVector = themeIcon,
                        contentDescription = "切换主题",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                // 主题切换菜单
                DropdownMenu(
                    expanded = showThemeMenu,
                    onDismissRequest = { showThemeMenu = false }
                ) {
                    // 跟随系统选项
                    DropdownMenuItem(
                        text = { Text("跟随系统") },
                        onClick = { 
                            ThemeState.useSystemTheme()
                            showThemeMenu = false 
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Brightness6,
                                contentDescription = null
                            )
                        },
                        trailingIcon = if (isDarkValue == null) {
                            {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else null
                    )
                    
                    // 亮色模式选项
                    DropdownMenuItem(
                        text = { Text("亮色模式") },
                        onClick = { 
                            ThemeState.isDarkMode.value = false
                            showThemeMenu = false 
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.LightMode,
                                contentDescription = null
                            )
                        },
                        trailingIcon = if (isDarkValue == false) {
                            {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else null
                    )
                    
                    // 暗色模式选项
                    DropdownMenuItem(
                        text = { Text("暗色模式") },
                        onClick = { 
                            ThemeState.isDarkMode.value = true
                            showThemeMenu = false 
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.DarkMode,
                                contentDescription = null
                            )
                        },
                        trailingIcon = if (isDarkValue == true) {
                            {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else null
                    )
                }
            }
        }
    }
} 