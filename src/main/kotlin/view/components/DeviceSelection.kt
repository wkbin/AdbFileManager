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
import model.StringsManager
import org.jetbrains.compose.resources.painterResource
import top.wkbin.filemanager.generated.resources.Res
import top.wkbin.filemanager.generated.resources.ic_adb_disconnect

/**
 * 设备连接向导
 */
@Composable
fun DeviceConnectionWizard(
    onRefresh: () -> Unit,
    onPairAndConnectDevice: (ipAddress: String, port: String, pairingPort: String, pairingCode: String) -> Unit
) {
    var showWizard by remember { mutableStateOf(false) }
    var showWirelessDialog by remember { mutableStateOf(false) }
    var ipAddress by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var pairingCode by remember { mutableStateOf("") }
    var pairingPort by remember { mutableStateOf("") }
    val strings by StringsManager.strings.collectAsState()

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
                        text = strings.deviceNotConnected,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = strings.deviceSelectMethod,
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
                            Text(strings.deviceConnectUSB)
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
                            Text(strings.deviceConnectWireless)
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
            title = { Text(strings.deviceUSBGuide) },
            text = {
                Column {
                    Text(strings.deviceUSBGuideStep1())
                    Text("   - Settings > About phone")
                    Text("   - Tap 'Build number' 7 times")
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(strings.deviceUSBGuideStep2())
                    Text("   - Settings > System > Developer options")
                    Text("   - Enable 'USB debugging'")
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(strings.deviceUSBGuideStep3())
                    Text("   - Use USB cable to connect device")
                    Text("   - Allow USB debugging on device")
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(strings.deviceUSBGuideStep4())
                }
            },
            confirmButton = {
                TextButton(onClick = { showWizard = false }) {
                    Text(strings.aboutClose)
                }
            },
            dismissButton = {
                TextButton(onClick = onRefresh) {
                    Text(strings.deviceRefresh)
                }
            }
        )
    }

    // 无线连接对话框
    if (showWirelessDialog) {
        AlertDialog(
            onDismissRequest = { showWirelessDialog = false },
            title = { Text(strings.deviceWirelessGuide) },
            text = {
                Column {
                    Text(strings.deviceWirelessGuideStep1())
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(strings.deviceWirelessGuideStep2())
                    Text("   - Settings > System > Developer options")
                    Text("   - Enable 'Wireless debugging'")
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(strings.deviceWirelessGuideStep3())
                    Text("   - ${strings.deviceWirelessGuideStep4()}")
                    Spacer(modifier = Modifier.height(8.dp))

                    Row {
                        OutlinedTextField(
                            value = ipAddress,
                            onValueChange = { ipAddress = it },
                            label = { Text(strings.deviceWirelessGuideIP) },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it },
                            label = { Text(strings.deviceWirelessGuidePort) },
                            modifier = Modifier.wrapContentWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row {
                        OutlinedTextField(
                            value = pairingPort,
                            onValueChange = { pairingPort = it },
                            label = { Text(strings.deviceWirelessGuidePairPort) },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = pairingCode,
                            onValueChange = { pairingCode = it },
                            label = { Text(strings.deviceWirelessGuidePairCode) },
                            modifier = Modifier.wrapContentWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPairAndConnectDevice.invoke(ipAddress, port, pairingPort, pairingCode)
                    },
                    enabled = ipAddress.isNotEmpty()
                ) {
                    Text(strings.deviceConnect)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWirelessDialog = false }) {
                    Text(strings.fileCancel)
                }
            }
        )
    }
} 