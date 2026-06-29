package data.remote.adb


interface Adb {
    suspend fun devices(): List<String>
    suspend fun wifiState(deviceId: String): AdbWifiState
    suspend fun connect(ipAddress: String, port: String): AdbWifiState
    suspend fun pair(ipAddress: String, port: String, code: String): List<String>
    suspend fun listAvds(): List<AndroidVirtualDevice>
    
    suspend fun shell(adbDevice: AdbDevice, vararg args: String, throwOnError: Boolean = false): List<String>
    suspend fun push(adbDevice: AdbDevice, localPath: String, remotePath: String, throwOnError: Boolean = false): List<String>
    suspend fun pull(adbDevice: AdbDevice, remotePath: String, localPath: String, throwOnError: Boolean = false): List<String>
    
    fun pushWithProgress(adbDevice: AdbDevice, localPath: String, remotePath: String, progressCallback: AdbTransferProgress): kotlinx.coroutines.flow.Flow<String>
    fun pullWithProgress(adbDevice: AdbDevice, remotePath: String, localPath: String, progressCallback: AdbTransferProgress): kotlinx.coroutines.flow.Flow<String>

    @Deprecated("Use shell(), push(), or pull() instead", ReplaceWith("shell(adbDevice, *args, throwOnError)"))
    suspend fun exec(adbDevice: AdbDevice, cmd: String, throwOnError: Boolean = false): List<String>
}