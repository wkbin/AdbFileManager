package data.remote.adb

import kotlinx.coroutines.flow.Flow
import kotlinx.io.IOException
import data.remote.adb.env.Environment

class AdbImpl(
    private val adbPath: String,
    private val terminal: Terminal
) : Adb {
    override suspend fun devices(): List<String> {
        val result = terminal.runSafe(listOf(adbPath, "devices"))
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

    override suspend fun wifiState(deviceId: String): AdbWifiState {
        deviceIdToAdbWifiState(deviceId)?.let { return it }
        val result = terminal.runSafe(listOf(adbPath, "-s", deviceId, "shell", "ip", "route"))

        val regex = """(\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\b)$""".toRegex()
        val ipAddress = result
            .filter { line -> line.contains("dev wlan0") }
            .map { line -> regex.find(line)?.value }
            .firstOrNull()
            ?: return AdbWifiState.WIFI_UNAVAILABLE
        val isConnected = deviceId.contains("adb-tls-connect")
        return AdbWifiState(isConnected, ipAddress, null)
    }

    override suspend fun connect(
        ipAddress: String,
        port: String
    ): AdbWifiState {
        terminal.runSafe(listOf(adbPath, "connect", "$ipAddress:$port"))
        return AdbWifiState(false, ipAddress, port)
    }

    override suspend fun pair(
        ipAddress: String,
        port: String,
        code: String
    ): List<String> {
        return terminal.runSafe(listOf(adbPath, "pair", "$ipAddress:$port", code))
    }

    override suspend fun listAvds(): List<AndroidVirtualDevice> {
        val androidHome = try {
            Environment.ANDROID_HOME
        } catch (tr: Throwable) {
            null
        } ?: return emptyList()

        return try {
            terminal.runSafe(listOf("$androidHome/emulator/emulator", "-list-avds"))
                .map { name -> AndroidVirtualDevice(name) }
        } catch (e: IOException) {
            emptyList()
        }
    }

    override suspend fun shell(
        adbDevice: AdbDevice,
        vararg args: String,
        throwOnError: Boolean
    ): List<String> {
        val commandArgs = mutableListOf(adbPath, "-s", adbDevice.deviceId, "shell")
        commandArgs.addAll(args)
        return terminal.runSafe(commandArgs, throwOnError)
    }

    override suspend fun push(
        adbDevice: AdbDevice,
        localPath: String,
        remotePath: String,
        throwOnError: Boolean
    ): List<String> {
        return terminal.runSafe(
            listOf(adbPath, "-s", adbDevice.deviceId, "push", localPath, remotePath),
            throwOnError
        )
    }

    override suspend fun pull(
        adbDevice: AdbDevice,
        remotePath: String,
        localPath: String,
        throwOnError: Boolean
    ): List<String> {
        return terminal.runSafe(
            listOf(adbPath, "-s", adbDevice.deviceId, "pull", remotePath, localPath),
            throwOnError
        )
    }

    override fun pushWithProgress(
        adbDevice: AdbDevice,
        localPath: String,
        remotePath: String,
        progressCallback: AdbTransferProgress
    ): Flow<String> {
        val command = "$adbPath -s ${adbDevice.deviceId} push -p \"$localPath\" \"$remotePath\""
        return terminal.connect(command)
    }

    @Deprecated("Use shell(), push(), or pull() instead", ReplaceWith("shell(adbDevice, *args, throwOnError)"))
    override suspend fun exec(
        adbDevice: AdbDevice,
        cmd: String,
        throwOnError: Boolean
    ): List<String> {
        val args = mutableListOf(adbPath, "-s", adbDevice.deviceId)
        args.addAll(cmd.split(" "))
        return terminal.runSafe(args, throwOnError)
    }

    private fun deviceIdToAdbWifiState(deviceId: String): AdbWifiState? {
        val regex = """(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}):(\d+)""".toRegex()
        val matchResult = regex.find(deviceId)
            ?: return null
        val (ipAddress, port) = matchResult.destructured
        return AdbWifiState(true, ipAddress, port)
    }
}
