package data.remote.adb

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AdbDevicePoller(private val adb: Adb) {
    private val _devices = MutableStateFlow<List<AdbDevice>>(emptyList())
    val devices: StateFlow<List<AdbDevice>> = _devices

    private val _currentDevice = MutableStateFlow<AdbDevice?>(null)
    val currentDevice: StateFlow<AdbDevice?> = _currentDevice

    private val fileOperations = AdbFileOperations(adb) { _currentDevice.value }

    fun startPolling(scope: CoroutineScope) = scope.launch(Dispatchers.IO) {
        while (isActive) {
            try {
                val deviceIds = adb.devices()
                val list = deviceIds.map { id -> AdbDevice(id, adb.wifiState(id)) }
                _devices.value = list

                val current = _currentDevice.value
                if (current == null || list.none { it.deviceId == current.deviceId }) {
                    _currentDevice.value = pickFallbackDevice(list)
                }
            } catch (e: Exception) {
                println("ADB polling error: ${e.message}")
            }
            delay(POLLING_INTERVAL_MS)
        }
    }

    fun connect(device: AdbDevice) {
        _currentDevice.value = device
    }

    fun disconnect() {
        _currentDevice.value = null
    }

    private fun pickFallbackDevice(list: List<AdbDevice>): AdbDevice? {
        return list.firstOrNull()
    }

    suspend fun delete(filePath: String) = fileOperations.delete(filePath)
    suspend fun loadFiles(directoryPath: String): List<String> = fileOperations.loadFiles(directoryPath)
    suspend fun push(originPath: String, destinationPath: String) = fileOperations.push(originPath, destinationPath)
    suspend fun pull(originPath: String, destinationPath: String) = fileOperations.pull(originPath, destinationPath)
    suspend fun checkPermission(directoryPath: String): Boolean = fileOperations.checkPermission(directoryPath)
    suspend fun renameFile(directoryPath: String, oldName: String, newName: String) = fileOperations.renameFile(directoryPath, oldName, newName)
    suspend fun createDirectory(directoryPath: String, dirName: String) = fileOperations.createDirectory(directoryPath, dirName)
    suspend fun createFile(directoryPath: String, fileName: String, content: String) = fileOperations.createFile(directoryPath, fileName, content)
    suspend fun changePermissions(directoryPath: String, fileName: String, permissions: String) = fileOperations.changePermissions(directoryPath, fileName, permissions)
    suspend fun installApk(filePath: String): String = fileOperations.installApk(filePath)
    suspend fun copyFile(sourcePath: String, destDirectory: String) = fileOperations.copyFile(sourcePath, destDirectory)
    suspend fun moveFile(sourcePath: String, destDirectory: String) = fileOperations.moveFile(sourcePath, destDirectory)

    fun pushWithProgress(
        localPath: String,
        remotePath: String,
        progressCallback: AdbTransferProgress
    ): kotlinx.coroutines.flow.Flow<String> {
        val device = _currentDevice.value ?: throw AdbDeviceNotFoundException()
        return adb.pushWithProgress(device, localPath, remotePath, progressCallback)
    }

    suspend fun pair(ipAddress: String, port: String, pairingPort: String, pairingCode: String) {
        val device = _currentDevice.value ?: return
        val ipPort = port.ifEmpty { "5555" }
        if (pairingPort.isNotEmpty() && pairingCode.isNotEmpty()) {
            adb.pair(ipAddress, pairingPort, pairingCode)
        } else {
            adb.shell(device, "tcpip", ipPort, throwOnError = true)
        }
    }

    companion object {
        private const val POLLING_INTERVAL_MS = 3000L
    }
}
