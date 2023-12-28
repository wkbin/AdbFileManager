package runtime.adb

import runtime.adb.env.Environment

class Adb(
    private val adbPath: String,
    private val terminal: Terminal
) {


    suspend fun devices(): List<String> {
        val cmd = "$adbPath devices"
        val result = terminal.run(cmd)
        val regex = """(.+)(device)""".toRegex()
        if (result.firstOrNull()?.trim() != "List of devices attached") {
            return emptyList()
        }
        return result
            .filter(String::isNotEmpty)
            .drop(1)
            .mapNotNull { line -> regex.find(line) }
            .map { matchResult -> matchResult.groupValues[1].trim() }
    }

    suspend fun wifiState(deviceId: String): AdbWifiState {
        deviceIdToAdbWifiState(deviceId)?.let { return it }

        val cmd = "$adbPath -s $deviceId shell ip route"
        val result = terminal.run(cmd)

        val regex = """(\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\b)$""".toRegex()
        val ipAddress = result
            .filter { line -> line.contains("dev wlan0") }
            .map { line -> regex.find(line)?.value }
            .firstOrNull()
            ?: return WIFI_UNAVAILABLE
        val isConnected = deviceId.contains("adb-tls-connect")
        return AdbWifiState(isConnected, ipAddress, null)
    }

    private fun deviceIdToAdbWifiState(deviceId: String): AdbWifiState? {
        val regex = """(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}):(\d+)""".toRegex()
        val matchResult = regex.find(deviceId)
            ?: return null
        val (ipAddress, port) = matchResult.destructured
        return AdbWifiState(true, ipAddress, port)
    }

    suspend fun connect(deviceId: String, ipAddress: String): AdbWifiState {
        val port = "5555"
        val commandConnect = "$adbPath -s $deviceId connect $ipAddress"
        terminal.run(commandConnect)
        println("commandConnect = $commandConnect")
        return AdbWifiState(false, ipAddress, port)
    }

    suspend fun listAvds(): List<AndroidVirtualDevice> {
        val androidHome = try {
            Environment.ANDROID_HOME
        } catch (tr: Throwable) {
            null
        }
            ?: return emptyList()
        val cmd = "$androidHome/emulator/emulator -list-avds"
        val result = terminal.run(cmd)
        return result.map { name -> AndroidVirtualDevice(name) }
    }

    suspend fun exec(adbDevice: AdbDevice,cmd:String):List<String>{
        val jCmd = "$adbPath -s ${adbDevice.deviceId} $cmd"
        val result =  terminal.run(jCmd)
        println("jcmd = $jCmd , reuslt = $result")
        return result
    }

}

data class AdbDevice(
    val deviceId: String,
    val adbWifiState: AdbWifiState
) {
    fun isEmulator(): Boolean = deviceId.startsWith("emulator-")
}

data class AdbWifiState(
    val connected: Boolean,
    val ipAddress: String?,
    val port: String?,
)

data class AndroidVirtualDevice(
    val name: String
)


val WIFI_UNAVAILABLE = AdbWifiState(false, null, null)