package viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import data.remote.adb.Adb
import data.remote.adb.AdbDevice
import data.remote.adb.AdbDevicePoller
import utils.UpdateInfo
import utils.VersionChecker

class MainViewModel(
    private val adbDevicePoller: AdbDevicePoller,
    private val adb: Adb
) : ViewModel() {

    val devices: StateFlow<List<AdbDevice>> get() = adbDevicePoller.devices
    val currentDevice: StateFlow<AdbDevice?> get() = adbDevicePoller.currentDevice

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> get() = _updateInfo

    init {
        adbDevicePoller.startPolling(viewModelScope)
        checkUpdate()
    }

    private fun checkUpdate() {
        viewModelScope.launch(Dispatchers.IO) {
            _updateInfo.value = VersionChecker.checkForUpdates()
        }
    }

    fun pairAndConnectDevice(ipAddress: String, port: String, pairingPort: String, pairingCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (pairingPort.isNotEmpty() && pairingCode.isNotEmpty()) {
                    // Pair first using Adb directly
                    adb.pair(ipAddress, pairingPort, pairingCode)
                }
                // Connect using Adb directly
                val ipPort = port.ifEmpty { "5555" }
                adb.connect(ipAddress, ipPort)
                
                // Update device list manually or wait for poller to pick it up
                // The poller should detect the new connection
            } catch (e: Exception) {
                println("Error pairing/connecting device: ${e.message}")
            }
        }
    }

    fun connect(device: AdbDevice) {
        adbDevicePoller.connect(device)
    }

    fun disconnect() {
        adbDevicePoller.disconnect()
    }
}