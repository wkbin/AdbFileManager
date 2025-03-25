package view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import runtime.adb.AdbDevice
import viewmodel.DeviceViewModel

/**
 * Component for selecting ADB devices
 */
@Composable
fun DeviceSelectionScreen(
    viewModel: DeviceViewModel,
    modifier: Modifier = Modifier
) {
    val connectedDevices by viewModel.connectedDevices.collectAsState(initial = emptyList())
    val selectedDeviceId = viewModel.selectedDeviceId.value
    
    var isDropdownExpanded by remember { mutableStateOf(false) }
    
    Row(
        modifier = modifier
            .padding(20.dp)
            .clickable { isDropdownExpanded = true },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Device status indicator
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(
                    if (selectedDeviceId == "未找到设备") Color.Gray else Color.Green,
                    shape = CircleShape
                )
                .align(Alignment.CenterVertically)
        )
        
        Spacer(modifier = Modifier.size(10.dp))
        
        // Device name
        Text(
            text = selectedDeviceId,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        
        // Dropdown menu for device selection
        if (connectedDevices.isNotEmpty()) {
            DropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false }
            ) {
                connectedDevices.forEach { device ->
                    DropdownMenuItem(
                        onClick = {
                            viewModel.connectToDevice(device)
                            isDropdownExpanded = false
                        }
                    ) {
                        Text(device.deviceId)
                    }
                }
            }
        }
    }
}

/**
 * Screen shown when no device is connected
 */
@Composable
fun NoDeviceScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("未找到已连接的设备", fontSize = 24.sp)
            Text("请连接一个Android设备或启动模拟器")
        }
    }
} 