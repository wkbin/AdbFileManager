package runtime.adb

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import runtime.adb.env.AppContext

/**
 * Class for polling ADB device information and executing ADB commands
 */
class AdbDevicePoller(
    private val adb: Adb,
    private val coroutineScope: CoroutineScope
) {
    private var pollingJob: Job? = null
    private var currentDevice: AdbDevice? = null
    
    /**
     * Start polling for connected devices
     * @param onResult Callback invoked with the list of connected devices
     */
    fun poll(onResult: (List<AdbDevice>) -> Unit) {
        stopPolling()
        
        pollingJob = coroutineScope.launch {
            while (true) {
                try {
                    val deviceIds = adb.devices()
                    val devices = deviceIds.map { deviceId ->
                        val wifiState = adb.wifiState(deviceId)
                        AdbDevice(deviceId, wifiState)
                    }
                    onResult(devices)
                } catch (e: Exception) {
                    // Handle error silently, will try again on next poll
                }
                delay(POLLING_INTERVAL_MS)
            }
        }
    }
    
    /**
     * Stop polling for devices
     */
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }
    
    /**
     * Set the current device for ADB commands
     */
    fun connect(device: AdbDevice) {
        currentDevice = device
        AppContext.adbDevice = device
    }
    
    /**
     * Clear the current device
     */
    fun disconnect() {
        currentDevice = null
        AppContext.adbDevice = null
    }
    
    /**
     * Request information about available Android Virtual Devices
     * @param onResult Callback invoked with the list of available AVDs
     */
    fun request(onResult: (List<AndroidVirtualDevice>) -> Unit) {
        coroutineScope.launch {
            try {
                val avds = adb.listAvds()
                onResult(avds)
            } catch (e: Exception) {
                onResult(emptyList())
            }
        }
    }
    
    /**
     * Execute an ADB command on the current device
     * @param cmd The ADB command to execute
     * @param onResult Callback invoked with the command output
     */
    suspend fun exec(cmd: String, onResult: (List<String>) -> Unit = {}) {
        if (currentDevice == null) {
            onResult(listOf("No device connected"))
            return
        }
        
        try {
            val result = adb.exec(currentDevice!!, cmd)
            onResult(result)
        } catch (e: Exception) {
            onResult(listOf("Error: ${e.message}"))
        }
    }
    
    companion object {
        private const val POLLING_INTERVAL_MS = 3000L
    }
}