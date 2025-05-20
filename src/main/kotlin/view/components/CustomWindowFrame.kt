package view.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Minimize
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import view.theme.ThemeState
import utils.UpdateInfo

/**
 * 自定义窗口框架，提供自定义标题栏和内容区域
 */
@Composable
fun FrameWindowScope.CustomWindowFrame(
    title: String,
    onCloseRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    // 检测当前是否为暗色模式
    val isDarkMode = ThemeState.isDark()
    
    // 关于对话框状态
    var showAboutDialog by remember { mutableStateOf(false) }
    
    // 更新对话框状态
    var showUpdateDialog by remember { mutableStateOf(false) }
    val updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    
    Surface(
        modifier = Modifier
            .fillMaxSize(),
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 自定义标题栏 - 可拖动
            Surface(
                color = if (isDarkMode) 
                           MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                       else 
                           MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 可拖动区域
                MoveableWindowArea { dragModifier ->
                    Row(
                        modifier = dragModifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 应用图标
                        Icon(
                            imageVector = Icons.Filled.PhoneAndroid,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        // 应用标题
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // GitHub 图标按钮
                        IconButton(
                            onClick = { showAboutDialog = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Code,
                                contentDescription = "关于",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        // 窗口控制按钮区域
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 最小化按钮
                            IconButton(
                                onClick = { window.isMinimized = true },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Minimize,
                                    contentDescription = "最小化",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            // 关闭按钮
                            Surface(
                                color = Color.Transparent,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.size(40.dp)
                            ) {
                                IconButton(
                                    onClick = onCloseRequest
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "关闭",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // 内容区域
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                content()
            }
            
            // 关于对话框
            AboutDialog(
                visible = showAboutDialog,
                onDismiss = { showAboutDialog = false }
            )
            
            // 更新对话框
            updateInfo?.let { info ->
                UpdateDialog(
                    visible = showUpdateDialog,
                    updateInfo = info,
                    onDismiss = { showUpdateDialog = false }
                )
            }
        }
    }
} 