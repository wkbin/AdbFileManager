package runtime.adb

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min

object AppContext{
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
        println("cmd = $cmd")
        println("adb devices = ${AppContext.adbDevice}")
        AppContext.adbDevice ?: return@launch
        val result = adb.exec(AppContext.adbDevice!!, cmd)
        println("result = $result")
        callBack?.invoke(result)
        invalidate()
    }

    fun request(callback: (List<AndroidVirtualDevice>) -> Unit) = coroutineScope.launch {
        callback(adb.listAvds())
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