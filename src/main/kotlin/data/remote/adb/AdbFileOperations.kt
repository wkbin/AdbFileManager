package data.remote.adb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AdbFileOperations(
    private val adb: Adb,
    private val currentDeviceProvider: () -> AdbDevice?
) {
    suspend fun delete(filePath: String) {
        val device = currentDeviceProvider() ?: return
        adb.shell(device, "rm", "-rf", filePath, throwOnError = true)
    }

    suspend fun loadFiles(directoryPath: String): List<String> {
        val device = currentDeviceProvider() ?: return emptyList()
        val safePath = if (directoryPath.isEmpty()) "" else "/$directoryPath"
        println("Executing loadFiles for path: '$safePath'")
        
        val cmd = if (safePath.isEmpty()) {
            "shell stat -L -c '%F|%A|%h|%U|%G|%s|%Y|%n|%N' * 2>/dev/null"
        } else {
            "shell cd '$safePath' && stat -L -c '%F|%A|%h|%U|%G|%s|%Y|%n|%N' * 2>/dev/null"
        }
        return adb.exec(device, cmd)
    }

    suspend fun push(originPath: String, destinationPath: String) {
        val device = currentDeviceProvider() ?: return
        val safeDestPath = if (destinationPath.isEmpty()) "/" else "/$destinationPath"
        adb.push(device, originPath, safeDestPath, throwOnError = true)
    }

    suspend fun pull(originPath: String, destinationPath: String) {
        val device = currentDeviceProvider() ?: return
        val safeOriginPath = if (originPath.isEmpty()) "/" else "/$originPath"
        adb.pull(device, safeOriginPath, destinationPath, throwOnError = true)
    }

    suspend fun checkPermission(directoryPath: String): Boolean {
        val device = currentDeviceProvider() ?: return false
        val safePath = if (directoryPath.isEmpty()) "/" else "/$directoryPath"
        return try {
            val checkCmd = "cd '$safePath' && echo SUCCESS || echo FAILURE"
            val result = adb.shell(device, "sh", "-c", checkCmd)
            !result.any { it.contains("Permission denied") || it.contains("FAILURE") }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun renameFile(directoryPath: String, oldName: String, newName: String) {
        val device = currentDeviceProvider() ?: return
        val safePath = if (directoryPath.isEmpty()) "/" else "/$directoryPath"
        adb.shell(
            device,
            "mv",
            "$safePath/$oldName",
            "$safePath/$newName",
            throwOnError = true
        )
    }

    suspend fun createDirectory(directoryPath: String, dirName: String) {
        val device = currentDeviceProvider() ?: return
        val safePath = if (directoryPath.isEmpty()) "/" else "/$directoryPath"
        adb.shell(
            device,
            "mkdir", "-p",
            "$safePath/$dirName",
            throwOnError = true
        )
    }

    suspend fun createFile(directoryPath: String, fileName: String, content: String) {
        val device = currentDeviceProvider() ?: return
        val tempFile = withContext(Dispatchers.IO) {
            File.createTempFile("create_", ".tmp")
        }
        try {
            tempFile.writeText(content)
            val destPath = if (directoryPath.isEmpty()) "/" else "/$directoryPath"
            adb.push(device, tempFile.absolutePath, "$destPath/$fileName", throwOnError = true)
        } finally {
            tempFile.delete()
        }
    }

    suspend fun changePermissions(directoryPath: String, fileName: String, permissions: String) {
        val device = currentDeviceProvider() ?: return
        val safePath = if (directoryPath.isEmpty()) "" else "/$directoryPath"
        val fullPath = if (safePath.isEmpty()) "/$fileName" else "$safePath/$fileName"
        println("Changing permissions for: $fullPath to $permissions")
        val cmd = "shell chmod $permissions \"$fullPath\""
        val result = adb.exec(device, cmd, throwOnError = true)
        println("chmod result: $result")
        println("chmod command executed: $cmd")
    }
}
