package data.remote.adb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AdbFileOperations(
    private val adb: Adb,
    private val currentDeviceProvider: () -> AdbDevice?
) {
    suspend fun delete(filePath: String) {
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        adb.shell(device, "rm", "-rf", "\"$filePath\"", throwOnError = true)
    }

    suspend fun loadFiles(directoryPath: String): List<String> {
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        val safePath = if (directoryPath.isEmpty()) "/" else "/$directoryPath"
        println("Executing loadFiles for path: '$safePath'")

        // ponytail: escape | as \| so the remote shell doesn't interpret it as pipe.
        // Using cd && stat directly (no sh -c) so cd actually changes the working directory.
        val statFormat = "%F|%A|%h|%U|%G|%s|%Y|%n|%N"
        val escapedFormat = statFormat.replace("|", "\\|")
        val targetPath = if (safePath == "/") "/" else "'$safePath'"
        return adb.shell(device, "cd", targetPath, "&&", "stat", "-L", "-c", escapedFormat, "*", "2>/dev/null")
    }

    suspend fun push(originPath: String, destinationPath: String) {
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        val safeDestPath = if (destinationPath.isEmpty()) "/" else "/$destinationPath"
        adb.push(device, originPath, safeDestPath, throwOnError = true)
    }

    suspend fun pull(originPath: String, destinationPath: String) {
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        val safeOriginPath = if (originPath.isEmpty()) "/" else "/$originPath"
        adb.pull(device, safeOriginPath, destinationPath, throwOnError = true)
    }

    suspend fun checkPermission(directoryPath: String): Boolean {
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        val safePath = if (directoryPath.isEmpty()) "/" else "/$directoryPath"
        return try {
            // ponytail: same fix as loadFiles — cd directly, not via sh -c
            val result = adb.shell(device, "cd", "'$safePath'", "&&", "echo", "SUCCESS", "||", "echo", "FAILURE")
            !result.any { it.contains("Permission denied") || it.contains("FAILURE") }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun renameFile(directoryPath: String, oldName: String, newName: String) {
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        val safePath = if (directoryPath.isEmpty()) "/" else "/$directoryPath"
        adb.shell(
            device,
            "mv",
            "\"$safePath/$oldName\"",
            "\"$safePath/$newName\"",
            throwOnError = true
        )
    }

    suspend fun createDirectory(directoryPath: String, dirName: String) {
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        val safePath = if (directoryPath.isEmpty()) "/" else "/$directoryPath"
        adb.shell(
            device,
            "mkdir", "-p",
            "\"$safePath/$dirName\"",
            throwOnError = true
        )
    }

    suspend fun createFile(directoryPath: String, fileName: String, content: String) {
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
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

    suspend fun installApk(filePath: String): String {
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        val safePath = if (filePath.startsWith("/")) filePath else "/$filePath"
        val result = adb.shell(device, "pm", "install", "-r", "\"$safePath\"", throwOnError = true)
        return result.joinToString("\n")
    }

    suspend fun copyFile(sourcePath: String, destDirectory: String) {
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        val srcPath = if (sourcePath.startsWith("/")) sourcePath else "/$sourcePath"
        val destPath = if (destDirectory.startsWith("/")) destDirectory else "/$destDirectory"
        adb.shell(device, "cp", "-r", "\"$srcPath\"", "\"$destPath\"", throwOnError = true)
    }

    suspend fun moveFile(sourcePath: String, destDirectory: String) {
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        val srcPath = if (sourcePath.startsWith("/")) sourcePath else "/$sourcePath"
        val destPath = if (destDirectory.startsWith("/")) destDirectory else "/$destDirectory"
        adb.shell(device, "mv", "\"$srcPath\"", "\"$destPath\"", throwOnError = true)
    }

    suspend fun changePermissions(directoryPath: String, fileName: String, permissions: String) {
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        val safePath = if (directoryPath.isEmpty()) "" else "/$directoryPath"
        val fullPath = if (safePath.isEmpty()) "/$fileName" else "$safePath/$fileName"
        println("Changing permissions for: $fullPath to $permissions")
        val result = adb.shell(device, "chmod", permissions, "\"$fullPath\"", throwOnError = true)
        println("chmod result: $result")
    }

    // ── App management ──────────────────────────────────────────────

    suspend fun getInstalledPackages(): List<String> {
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        val result = adb.shell(device, "pm", "list", "packages", "-3", throwOnError = false)
        return result.mapNotNull { line ->
            line.removePrefix("package:").trim().takeIf { it.isNotEmpty() }
        }
    }

    suspend fun getAppLabel(packageName: String): String {
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        return try {
            val dump = adb.shell(device, "dumpsys", "package", packageName, throwOnError = false)
            dump.firstOrNull { it.trimStart().startsWith("label=") }
                ?.substringAfter("label=")?.trim()?.takeIf { it.isNotEmpty() } ?: packageName
        } catch (_: Exception) { packageName }
    }

    suspend fun getAppApkPath(packageName: String): String? {
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        val result = adb.shell(device, "pm", "path", packageName, throwOnError = false)
        return result.firstOrNull()?.removePrefix("package:")?.trim()
    }

    suspend fun uninstallApp(packageName: String) {
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        adb.shell(device, "pm", "uninstall", packageName, throwOnError = true)
    }

    suspend fun backupApk(packageName: String, destinationPath: String) {
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        val apkPath = getAppApkPath(packageName)
            ?: throw IllegalStateException("APK path not found for $packageName")
        adb.pull(device, apkPath, destinationPath, throwOnError = true)
    }

    // ── Screenshot ──────────────────────────────────────────────────

    suspend fun takeScreenshot(destinationPath: String) {
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        // ponytail: adb exec-out screencap -p dumps raw PNG to stdout, capture via runSafe
        withContext(Dispatchers.IO) {
            val file = File(destinationPath)
            file.parentFile?.mkdirs()
            file.outputStream().use { output ->
                val process = ProcessBuilder(
                    adb.getAdbPath(), "-s", device.deviceId, "exec-out", "screencap", "-p"
                ).start()
                process.inputStream.copyTo(output)
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    val err = process.errorStream.bufferedReader().readText()
                    throw AdbException("screencap failed (exit $exitCode): $err")
                }
            }
        }
    }

    // ── Clipboard ───────────────────────────────────────────────────

    suspend fun pushClipboard(text: String) {
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        // ponytail: use cmd clipboard (API 13+) which is simpler than the old service call
        // Escape single quotes: replace ' with '\'' so the shell passes the text correctly
        val escaped = text.replace("'", "'\\''")
        adb.shell(device, "cmd", "clipboard", "set", "'$escaped'", throwOnError = true)
    }
}
