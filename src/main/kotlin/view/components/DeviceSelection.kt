package view.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.SignalWifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import runtime.adb.AdbDevice

/**
 * 设备选择组件
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun DeviceSelector(
    selectedDeviceId: String,
    devices: List<AdbDevice>,
    onDeviceSelected: (AdbDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    // 箭头动画
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "arrowRotation"
    )
    
    Box(modifier = modifier) {
        // 设备选择器按钮
        OutlinedCard(
            onClick = { expanded = true },
            enabled = devices.isNotEmpty(),
            modifier = Modifier.width(IntrinsicSize.Max),
            colors = CardDefaults.outlinedCardColors(
                containerColor = when {
                    selectedDeviceId == "未找到设备" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                    else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                },
                contentColor = when {
                    selectedDeviceId == "未找到设备" -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                }
            ),
            border = CardDefaults.outlinedCardBorder(enabled = false),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 设备图标
                Icon(
                    imageVector = if (selectedDeviceId == "未找到设备") 
                        Icons.Rounded.SignalWifiOff 
                    else 
                        Icons.Outlined.PhoneAndroid,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(Modifier.width(12.dp))
                
                // 设备ID
                Text(
                    text = selectedDeviceId,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(Modifier.width(8.dp))
                
                // 下拉箭头
                AnimatedVisibility(
                    visible = devices.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = "展开选择设备",
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(arrowRotation)
                    )
                }
            }
        }
        
        // 设备下拉菜单
        DropdownMenu(
            expanded = expanded && devices.isNotEmpty(),
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .heightIn(max = 350.dp)
        ) {
            // 标题
            Text(
                text = "选择设备",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            Divider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
            
            // 设备列表
            devices.forEach { device ->
                DeviceMenuItem(
                    deviceId = device.deviceId,
                    isSelected = device.deviceId == selectedDeviceId,
                    onClick = {
                        onDeviceSelected(device)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * 设备菜单项
 */
@Composable
private fun DeviceMenuItem(
    deviceId: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = if (isSelected) 
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        else
            Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 设备图标
            Icon(
                imageVector = Icons.Outlined.PhoneAndroid,
                contentDescription = null,
                tint = if (isSelected) 
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(Modifier.width(16.dp))
            
            // 设备ID
            Text(
                text = deviceId,
                style = MaterialTheme.typography.bodyMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                color = if (isSelected) 
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            // 选中标记
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "已选择",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 无设备连接时的提示屏幕
 */
@Composable
fun NoDeviceScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 使用动画图标
            val infiniteTransition = rememberInfiniteTransition(label = "disconnectAnimation")
            val iconAlpha by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "iconAlphaAnimation"
            )
            
            Icon(
                painter = androidx.compose.ui.res.painterResource("res/icons/ic_adb_disconnect.png"),
                contentDescription = null,
                modifier = Modifier.size(150.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = iconAlpha)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 标题和描述
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .widthIn(max = 400.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "未找到已连接的设备",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "请连接一个Android设备或启动模拟器，然后刷新设备列表",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { /* 刷新按钮，可以实现刷新功能 */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text("刷新设备列表")
                    }
                }
            }
        }
    }
} 