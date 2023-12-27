package runtime.adb

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min

object AppContext{
    @Volatile
    var adbDevice: AdbDevice? = null
}

class AdbDevicePoller(
    private val adb: Adb,
    private val coroutineScope: CoroutineScope
) {


    companion object {
        private const val MAX_DELAY_MS = 5000L
    }

    private var currentDelay = MAX_DELAY_MS
    private var callback: (List<AdbDevice>) -> Unit = { }


    private val job = coroutineScope.launch {
        while (isActive) {
            devices()
            delay(currentDelay)
            currentDelay = min((currentDelay * 1.50).toLong(), MAX_DELAY_MS)
        }
    }

    fun poll(callback: (List<AdbDevice>) -> Unit) {
        this@AdbDevicePoller.callback = callback
        if (job.isCancelled) {
            job.start()
        }
    }

    fun exec(cmd: String, callBack: ((List<String>) -> Unit)? = null) = coroutineScope.launch {
        AppContext.adbDevice ?: return@launch
        val result = adb.exec(AppContext.adbDevice!!, cmd)
        callBack?.invoke(result)
        invalidate()
    }

    fun request(callback: (List<AndroidVirtualDevice>) -> Unit) = coroutineScope.launch {
        callback(adb.listAvds())
    }

    fun connect(adbDevice: AdbDevice) = coroutineScope.launch {
        AppContext.adbDevice = adbDevice
        val ipAddress = adbDevice.adbWifiState.ipAddress ?: return@launch
        adb.connect(adbDevice.deviceId, ipAddress)
        println("adbDevice = ${AppContext.adbDevice}")
        invalidate()
    }

    private fun invalidate() {
        devices()
        currentDelay = 500L
    }

    private fun devices() = coroutineScope.launch {
        val devices = adb.devices()
            .map { deviceId ->
                val wifiState = adb.wifiState(deviceId)
                AdbDevice(deviceId, wifiState)
            }
        if (isActive) {
            callback(devices)
        }
    }
}