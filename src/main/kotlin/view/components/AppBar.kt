package view.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.res.painterResource
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
 * 工具栏按钮组件
 */
@Composable
private fun ToolbarButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    icon: ImageVector,
    text: String,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.padding(horizontal = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = tint
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

/**
 * 文件管理器工具栏
 */
@Composable
fun FileManagerToolbar(
    onCreateDirectoryClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onBackClick: () -> Unit,
    canNavigateUp: Boolean = false
) {
    val elevation by animateDpAsState(
        targetValue = if (canNavigateUp) 2.dp else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "toolbarElevation"
    )
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = elevation,
        shadowElevation = elevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
            }
            
            // 创建文件夹按钮
            ToolbarButton(
                onClick = onCreateDirectoryClick,
                icon = Icons.Rounded.CreateNewFolder,
                text = "创建文件夹",
                tint = MaterialTheme.colorScheme.secondary
            )
            
            // 刷新按钮
            ToolbarButton(
                onClick = onRefreshClick,
                icon = Icons.Rounded.Refresh,
                text = "刷新",
                tint = MaterialTheme.colorScheme.tertiary
            )
            
            Spacer(Modifier.weight(1f))
        }
    }
} 