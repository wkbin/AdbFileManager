package view.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import top.wkbin.filemanager.generated.resources.Res
import top.wkbin.filemanager.generated.resources.ic_adb_disconnect
import viewmodel.DeviceViewModel

/**
 * 设备连接向导
 */
@Composable
fun DeviceConnectionWizard(
    deviceViewModel: DeviceViewModel,
    onRefresh: () -> Unit
) {
    var showWizard by remember { mutableStateOf(false) }
    var showWirelessDialog by remember { mutableStateOf(false) }
    var showConnectDialog by remember { mutableStateOf(false) }
    var ipAddress by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var pairingCode by remember { mutableStateOf("") }
    var pairingPort by remember { mutableStateOf("") }



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
                painter = painterResource(Res.drawable.ic_adb_disconnect),
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
                        text = "请选择连接方式",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 连接方式按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // USB连接按钮
                        Button(
                            onClick = { showWizard = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Usb,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("USB连接")
                        }

                        // 无线连接按钮
                        Button(
                            onClick = { showWirelessDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Wifi,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("无线连接")
                        }
                    }
                }
            }
        }
    }

    // USB连接向导对话框
    if (showWizard) {
        AlertDialog(
            onDismissRequest = { showWizard = false },
            title = { Text("USB连接向导") },
            text = {
                Column {
                    Text("1. 在Android设备上启用开发者选项：")
                    Text("   - 进入设置 > 关于手机")
                    Text("   - 连续点击\"版本号\"7次")
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("2. 启用USB调试：")
                    Text("   - 进入设置 > 系统 > 开发者选项")
                    Text("   - 开启\"USB调试\"")
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("3. 连接设备：")
                    Text("   - 使用USB数据线连接设备")
                    Text("   - 在设备上允许USB调试")
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("4. 点击刷新按钮检查连接")
                }
            },
            confirmButton = {
                TextButton(onClick = { showWizard = false }) {
                    Text("关闭")
                }
            },
            dismissButton = {
                TextButton(onClick = onRefresh) {
                    Text("刷新")
                }
            }
        )
    }

    // 无线连接对话框
    if (showWirelessDialog) {
        AlertDialog(
            onDismissRequest = { showWirelessDialog = false },
            title = { Text("无线连接") },
            text = {
                Column {
                    Text("1. 确保设备和电脑在同一网络下")
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("2. 在设备上启用无线调试：")
                    Text("   - 进入设置 > 系统 > 开发者选项")
                    Text("   - 开启\"无线调试\"")
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("3. 连接方式：")
                    Text("   - 方式：手动输入配对信息（已配对设备可不输入配对端口和配对码）")
                    Spacer(modifier = Modifier.height(8.dp))

                    Row {
                        OutlinedTextField(
                            value = ipAddress,
                            onValueChange = { ipAddress = it },
                            label = { Text("IP地址") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it },
                            label = { Text("端口（不填默认：5555）") },
                            modifier = Modifier.wrapContentWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row {
                        OutlinedTextField(
                            value = pairingPort,
                            onValueChange = { pairingPort = it },
                            label = { Text("配对端口（选填）") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = pairingCode,
                            onValueChange = { pairingCode = it },
                            label = { Text("配对码（选填）") },
                            modifier = Modifier.wrapContentWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        deviceViewModel.pairAndConnectDevice(ipAddress, port, pairingPort, pairingCode)
                    },
                    enabled = ipAddress.isNotEmpty()
                ) {
                    Text("连接")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWirelessDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
} 