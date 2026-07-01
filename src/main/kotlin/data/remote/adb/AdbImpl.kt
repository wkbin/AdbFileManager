package data.remote.adb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.io.IOException
import data.remote.adb.env.Environment
import java.io.File

class AdbImpl(
    private val adbPath: String,
    private val terminal: Terminal
) : Adb {
    override fun getAdbPath(): String = adbPath

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
        val commandArgs = listOf(adbPath, "-s", adbDevice.deviceId, "push", "-p", localPath, remotePath)
        val progressRegex = Regex("""\[(\d+)%]""")
        return terminal.connectSafe(commandArgs).onEach { line ->
            progressRegex.find(line)?.groupValues?.get(1)?.toIntOrNull()?.let {
                progressCallback.onProgress(it)
            }
        }
    }

    override fun pullWithProgress(
        adbDevice: AdbDevice,
        remotePath: String,
        localPath: String,
        progressCallback: AdbTransferProgress
    ): Flow<String> = channelFlow {
        // ponytail: poll local file size while adb pull runs, since adb pull has no -p flag
        val sizeOutput = terminal.runSafe(
            listOf(adbPath, "-s", adbDevice.deviceId, "shell", "stat", "-c", "%s", remotePath),
            throwOnError = false
        )
        val totalSize = sizeOutput.firstOrNull()?.trim()?.toLongOrNull() ?: 0L

        val pullDeferred = async(Dispatchers.IO) {
            terminal.runSafe(
                listOf(adbPath, "-s", adbDevice.deviceId, "pull", remotePath, localPath),
                throwOnError = true
            )
        }

        val localFile = File(localPath)
        if (totalSize > 0) {
            while (!pullDeferred.isCompleted) {
                delay(100)
                if (localFile.exists()) {
                    val percent = (localFile.length() * 100 / totalSize).toInt().coerceIn(0, 100)
                    progressCallback.onProgress(percent)
                }
            }
        }
        progressCallback.onProgress(100)

        val result = pullDeferred.await()
        result.forEach { send(it) }
    }.flowOn(Dispatchers.IO)

    @Deprecated(
        message = "Use shell(), push(), or pull() instead. String-based commands break with paths containing spaces.",
        replaceWith = ReplaceWith("shell(adbDevice, *args, throwOnError)")
    )
    override suspend fun exec(
        adbDevice: AdbDevice,
        cmd: String,
        throwOnError: Boolean
    ): List<String> {
        val args = mutableListOf(adbPath, "-s", adbDevice.deviceId)
        // WARNING: split(" ") breaks quoted arguments with spaces.
        // Use shell() / push() / pull() which accept List<String> args.
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
