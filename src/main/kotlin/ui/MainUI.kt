package ui

import LocalAdb
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import runtime.adb.AdbDevice
import runtime.adb.AdbDevicePoller
import runtime.adb.AppContext

@Composable
fun MainUI(){
    var connectedDevices by remember { mutableStateOf(emptyList<AdbDevice>()) }
    val adbDevicePoller = rememberAdbDeviceManager()
    adbDevicePoller.poll {
        AppContext.adbDevice = it.firstOrNull()
        connectedDevices = it
    }
    if (connectedDevices.isEmpty()){
        AdbDisconnectUI()
    }else{
        FileManagerUI(adbDevicePoller)
    }
}

@Composable
private fun rememberAdbDeviceManager(): AdbDevicePoller {
    val coroutineScope = rememberCoroutineScope()
    return AdbDevicePoller(LocalAdb.current, coroutineScope)
}