package view.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
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

/**
 * 应用顶部应用栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(
    title: String,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
            ),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            },
            navigationIcon = {
                navigationIcon?.invoke()
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

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
    canNavigateUp: Boolean = false
) {
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
            
            // 刷新按钮
            ToolbarButton(
                onClick = onRefreshClick,
                icon = Icons.Rounded.Refresh,
                text = "刷新",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
} 