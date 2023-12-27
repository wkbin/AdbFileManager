package ui

import LocalAdb
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
import runtime.adb.AdbDevice
import runtime.adb.AdbDevicePoller
import runtime.adb.AndroidVirtualDevice
import runtime.adb.AppContext

@Composable
fun MainUI() {
    var connectedDevices by remember { mutableStateOf(emptyList<AdbDevice>()) }
    val adbDevicePoller = rememberAdbDeviceManager()
    adbDevicePoller.poll {
        connectedDevices = it
    }
    Box {
        if (connectedDevices.isEmpty()) {
            AdbDisconnectUI()
        } else {
            FileManagerUI(adbDevicePoller)
        }
        DeviceCheckoutUI(Modifier.align(Alignment.TopEnd), adbDevicePoller, connectedDevices)
    }
}

@Composable
private fun DeviceCheckoutUI(modifier: Modifier, adbDevicePoller: AdbDevicePoller, connectedDevices: List<AdbDevice>) {
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf("未找到设备") }
    var androidVirtualDevices by remember { mutableStateOf(emptyList<AndroidVirtualDevice>()) }
    adbDevicePoller.request { androidVirtualDevices = it }

    Row(modifier = modifier.padding(20.dp).clickable {
        isDropdownExpanded = true
    }) {
        if (connectedDevices.isEmpty()) {
            selectedOption = "未找到设备"
            AppContext.adbDevice = null
        } else {
            if (selectedOption == "未找到设备") {
                connectedDevices.firstOrNull()?.let {
                    selectedOption = it.deviceId
                    adbDevicePoller.connect(it)
                }
            }
            DropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = {
                    isDropdownExpanded = false
                }
            ) {
                connectedDevices.forEach { adbDevice ->
                    DropdownMenuItem(
                        content = {
                            Text(adbDevice.deviceId)
                        },
                        onClick = {
                            selectedOption = adbDevice.deviceId
                            adbDevicePoller.connect(adbDevice)
                            isDropdownExpanded = false
                        }
                    )
                }
            }
        }

        Box(
            modifier = Modifier.size(10.dp)
                .background(if (selectedOption == "未找到设备") Color.Gray else Color.Green, shape = CircleShape)
                .align(Alignment.CenterVertically)
        )
        Spacer(modifier = Modifier.size(10.dp))
        Text(selectedOption, Modifier.align(Alignment.CenterVertically))
    }
}

@Composable
private fun rememberAdbDeviceManager(): AdbDevicePoller {
    val coroutineScope = rememberCoroutineScope()
    return AdbDevicePoller(LocalAdb.current, coroutineScope)
}