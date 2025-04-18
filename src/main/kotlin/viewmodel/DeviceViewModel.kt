package viewmodel

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import runtime.adb.AdbDevice
import runtime.adb.AdbDevicePoller
import runtime.adb.AndroidVirtualDevice
import kotlinx.coroutines.launch

/**
 * ViewModel for device management
 */
class DeviceViewModel(
    private val adbDevicePoller: AdbDevicePoller,
    private val coroutineScope: CoroutineScope
) {
    // List of connected devices
    private val _connectedDevices = MutableStateFlow<List<AdbDevice>>(emptyList())
    val connectedDevices: StateFlow<List<AdbDevice>> = _connectedDevices.asStateFlow()

    // List of available virtual devices
    private val _virtualDevices = MutableStateFlow<List<AndroidVirtualDevice>>(emptyList())
    val virtualDevices: StateFlow<List<AndroidVirtualDevice>> = _virtualDevices.asStateFlow()

    // Currently selected device
    val selectedDeviceId = mutableStateOf("未找到设备")

    init {
        startDevicePolling()
        loadVirtualDevices()
    }

    /**
     * Start polling for connected devices
     */
    private fun startDevicePolling() {
        adbDevicePoller.poll { devices ->
            _connectedDevices.value = devices
            updateSelectedDevice(devices)
        }
    }

    /**
     * Load available Android Virtual Devices
     */
    fun loadVirtualDevices() {
        adbDevicePoller.request { devices ->
            _virtualDevices.value = devices
        }
    }

    /**
     * Update the selected device when the device list changes
     */
    private fun updateSelectedDevice(devices: List<AdbDevice>) {
        if (devices.isEmpty()) {
            selectedDeviceId.value = "未找到设备"
            adbDevicePoller.disconnect()
        } else if (selectedDeviceId.value == "未找到设备") {
            // Auto-select first device if none selected
            devices.firstOrNull()?.let { device ->
                selectedDeviceId.value = device.deviceId
                adbDevicePoller.connect(device)
            }
        }
    }

    /**
     * Connect to a specific device
     */
    fun connectToDevice(device: AdbDevice) {
        selectedDeviceId.value = device.deviceId
        adbDevicePoller.connect(device)
    }

    /**
     * Disconnect from the current device
     */
    fun disconnect() {
        adbDevicePoller.disconnect()
        selectedDeviceId.value = "未找到设备"
    }

    /**
     * 配对并设备
     */
    fun pairAndConnectDevice(ipAddress: String, port: String, pairingPort: String, pairingCode: String) {
        val ipPort = port.ifEmpty { "5555" }
        coroutineScope.launch {
            if (pairingPort.isNotEmpty() && pairingCode.isNotEmpty()) {
                adbDevicePoller.pair(ipAddress, pairingPort, pairingCode) {
                    launch {
                        println("suc = ${it.firstOrNull()}")
                        if (it.firstOrNull()?.startsWith("Successfully") == true) {
                            adbDevicePoller.connect(ipAddress, ipPort)
                        }
                    }
                }
            } else {
                adbDevicePoller.connect(ipAddress, ipPort)
            }
        }
    }
} 