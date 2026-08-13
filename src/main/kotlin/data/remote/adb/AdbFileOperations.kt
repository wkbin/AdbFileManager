package data.remote.adb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.jetbrains.skiko.hostOs
import java.io.File
import java.util.UUID

class AdbFileOperations(
    private val adb: Adb,
    private val currentDeviceProvider: () -> AdbDevice?,
    private val windowsPushWorkaroundEnabled: Boolean = hostOs.isWindows,
    private val stagingFileFactory: () -> File = { File.createTempFile(LOCAL_STAGE_PREFIX, ".tmp") }
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
        val safeDestPath = absoluteRemotePath(destinationPath)
        if (windowsPushWorkaroundEnabled) {
            pushPathFromWindows(device, File(originPath), safeDestPath)
        } else {
            adb.push(device, originPath, safeDestPath, throwOnError = true)
        }
    }

    fun pushWithProgress(
        originPath: String,
        destinationPath: String,
        progressCallback: AdbTransferProgress
    ): Flow<String> {
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        val safeDestPath = absoluteRemotePath(destinationPath)
        if (!windowsPushWorkaroundEnabled) {
            return adb.pushWithProgress(device, originPath, safeDestPath, progressCallback)
        }

        return flow {
            val source = File(originPath)
            require(source.isFile) { "Progress upload requires a file: $originPath" }
            pushFileFromWindows(device, source, safeDestPath, progressCallback) { emit(it) }
        }
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
            val remoteFilePath = appendRemotePath(destPath, fileName)
            if (windowsPushWorkaroundEnabled) {
                pushFileFromWindows(device, tempFile, remoteFilePath)
            } else {
                adb.push(device, tempFile.absolutePath, remoteFilePath, throwOnError = true)
            }
        } finally {
            tempFile.delete()
        }
    }

    private suspend fun pushPathFromWindows(device: AdbDevice, source: File, destinationPath: String) {
        require(source.exists()) { "Local path does not exist: ${source.absolutePath}" }
        if (source.isDirectory) {
            adb.shell(device, "mkdir", "-p", shellQuote(destinationPath), throwOnError = true)
            source.listFiles()
                ?.sortedBy { it.name }
                ?.forEach { child ->
                    pushPathFromWindows(device, child, appendRemotePath(destinationPath, child.name))
                }
        } else {
            pushFileFromWindows(device, source, destinationPath)
        }
    }

    private suspend fun pushFileFromWindows(
        device: AdbDevice,
        source: File,
        destinationPath: String,
        progressCallback: AdbTransferProgress? = null,
        emitOutput: suspend (String) -> Unit = {}
    ) {
        val localStage = withContext(Dispatchers.IO) {
            val stage = stagingFileFactory()
            try {
                source.copyTo(stage, overwrite = true)
                stage
            } catch (e: Exception) {
                stage.delete()
                throw e
            }
        }
        val remoteStage = "$REMOTE_STAGE_DIRECTORY/$REMOTE_STAGE_PREFIX${UUID.randomUUID()}"

        try {
            if (progressCallback == null) {
                adb.push(device, localStage.absolutePath, remoteStage, throwOnError = true)
            } else {
                adb.pushWithProgress(device, localStage.absolutePath, remoteStage, progressCallback)
                    .collect { emitOutput(it) }
            }

            val parentPath = destinationPath.substringBeforeLast('/', missingDelimiterValue = "/")
                .ifEmpty { "/" }
            adb.shell(device, "mkdir", "-p", shellQuote(parentPath), throwOnError = true)
            adb.shell(
                device,
                "mv", "-f", "--",
                shellQuote(remoteStage),
                shellQuote(destinationPath),
                throwOnError = true
            )
        } finally {
            withContext(Dispatchers.IO) { localStage.delete() }
            runCatching {
                adb.shell(device, "rm", "-f", "--", shellQuote(remoteStage), throwOnError = false)
            }
        }
    }

    private fun appendRemotePath(parent: String, name: String): String =
        if (parent == "/") "/$name" else "${parent.trimEnd('/')}/$name"

    private fun absoluteRemotePath(path: String): String =
        if (path.isEmpty()) "/" else "/${path.trimStart('/')}"

    private fun shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"

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
        val currentUser = adb.shell(
            device,
            "am", "get-current-user",
            throwOnError = true
        ).firstOrNull()?.trim()?.toIntOrNull() ?: 0
        val result = adb.shell(
            device,
            "pm", "list", "packages", "-3", "--user", currentUser.toString(),
            throwOnError = true
        )
        return result.mapNotNull { line ->
            line.trim().takeIf { it.startsWith("package:") }
                ?.removePrefix("package:")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        }
    }

    suspend fun getAppLabel(packageName: String): String {
        require(packageName.isNotBlank()) { "Package name must not be blank" }
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        return try {
            val dump = adb.shell(device, "dumpsys", "package", packageName, throwOnError = false)
            dump.firstOrNull { it.trimStart().startsWith("label=") }
                ?.substringAfter("label=")?.trim()?.takeIf { it.isNotEmpty() } ?: packageName
        } catch (_: Exception) { packageName }
    }

    suspend fun getAppApkPath(packageName: String): String? {
        require(packageName.isNotBlank()) { "Package name must not be blank" }
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        val result = adb.shell(device, "pm", "path", packageName, throwOnError = false)
        return result.firstOrNull()?.removePrefix("package:")?.trim()
    }

    suspend fun uninstallApp(packageName: String) {
        require(packageName.isNotBlank()) { "Package name must not be blank" }
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        adb.shell(device, "pm", "uninstall", packageName, throwOnError = true)
    }

    suspend fun backupApk(packageName: String, destinationPath: String) {
        require(packageName.isNotBlank()) { "Package name must not be blank" }
        require(destinationPath.isNotBlank()) { "Backup destination must not be blank" }
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        val apkPath = getAppApkPath(packageName)
            ?: throw IllegalStateException("APK path not found for $packageName")
        withContext(Dispatchers.IO) {
            File(destinationPath).parentFile?.mkdirs()
        }
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
        require(text.isNotBlank()) { "Clipboard text must not be blank" }
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        // ponytail: use cmd clipboard (API 13+) which is simpler than the old service call
        // Escape single quotes: replace ' with '\'' so the shell passes the text correctly
        val escaped = text.replace("'", "'\\''")
        adb.shell(device, "cmd", "clipboard", "set", "'$escaped'", throwOnError = true)
    }

    companion object {
        private const val LOCAL_STAGE_PREFIX = "afm_push_"
        private const val REMOTE_STAGE_DIRECTORY = "/data/local/tmp"
        private const val REMOTE_STAGE_PREFIX = "adb-file-manager-"
    }
}
