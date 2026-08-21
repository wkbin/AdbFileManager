package data.remote.adb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import model.AppInfo
import net.dongliu.apk.parser.ApkFile
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.skiko.hostOs
import top.wkbin.filemanager.generated.resources.Res
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Base64
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class AdbFileOperations(
    private val adb: Adb,
    private val currentDeviceProvider: () -> AdbDevice?,
    private val windowsPushWorkaroundEnabled: Boolean = hostOs.isWindows,
    private val stagingFileFactory: () -> File = { File.createTempFile(LOCAL_STAGE_PREFIX, ".tmp") },
    private val clipboardHelperLoader: suspend () -> ByteArray = { loadClipboardHelper() }
) {
    suspend fun delete(filePath: String) {
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        adb.shell(device, "rm", "-rf", "--", shellQuote(absoluteRemotePath(filePath)), throwOnError = true)
    }

    suspend fun loadFiles(directoryPath: String): List<String> {
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        val safePath = if (directoryPath.isEmpty()) "/" else "/$directoryPath"
        println("Executing loadFiles for path: '$safePath'")

        // ponytail: escape | as \| so the remote shell doesn't interpret it as pipe.
        // Using cd && stat directly (no sh -c) so cd actually changes the working directory.
        val statFormat = "%F|%A|%h|%U|%G|%s|%Y|%n|%N"
        val escapedFormat = statFormat.replace("|", "\\|")
        val targetPath = shellQuote(safePath)
        return adb.shell(device, "cd", targetPath, "&&", "stat", "-L", "-c", escapedFormat, "*", "2>/dev/null")
    }

    suspend fun searchFiles(directoryPath: String, query: String, maxDepth: Int = 4): List<String> {
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        val safePath = if (directoryPath.isEmpty()) "/" else "/$directoryPath"
        val statFormat = "%F|%A|%h|%U|%G|%s|%Y|%n|%N"
        val escapedFormat = statFormat.replace("|", "\\|")
        val targetPath = shellQuote(safePath)
        val findPattern = "*$query*"
        return adb.shell(
            device,
            "find", targetPath,
            "-maxdepth", maxDepth.toString(),
            "-iname", shellQuote(findPattern),
            "-exec", "stat", "-L", "-c", escapedFormat, "{}", "+",
            "2>/dev/null"
        )
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
            "--",
            shellQuote(appendRemotePath(safePath, oldName)),
            shellQuote(appendRemotePath(safePath, newName)),
            throwOnError = true
        )
    }

    suspend fun createDirectory(directoryPath: String, dirName: String) {
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        val safePath = if (directoryPath.isEmpty()) "/" else "/$directoryPath"
        adb.shell(
            device,
            "mkdir", "-p",
            "--", shellQuote(appendRemotePath(safePath, dirName)),
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
            withContext(NonCancellable + Dispatchers.IO) {
                localStage.delete()
                runCatching {
                    adb.shell(device, "rm", "-f", "--", shellQuote(remoteStage), throwOnError = false)
                }
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
        val result = adb.shell(device, "pm", "install", "-r", shellQuote(safePath), throwOnError = true)
        return result.joinToString("\n")
    }

    suspend fun copyFile(sourcePath: String, destDirectory: String) {
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        val srcPath = if (sourcePath.startsWith("/")) sourcePath else "/$sourcePath"
        val destPath = if (destDirectory.startsWith("/")) destDirectory else "/$destDirectory"
        adb.shell(device, "cp", "-r", "--", shellQuote(srcPath), shellQuote(destPath), throwOnError = true)
    }

    suspend fun moveFile(sourcePath: String, destDirectory: String) {
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        val srcPath = if (sourcePath.startsWith("/")) sourcePath else "/$sourcePath"
        val destPath = if (destDirectory.startsWith("/")) destDirectory else "/$destDirectory"
        adb.shell(device, "mv", "--", shellQuote(srcPath), shellQuote(destPath), throwOnError = true)
    }

    suspend fun changePermissions(directoryPath: String, fileName: String, permissions: String) {
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        val safePath = if (directoryPath.isEmpty()) "" else "/$directoryPath"
        val fullPath = if (safePath.isEmpty()) "/$fileName" else "$safePath/$fileName"
        println("Changing permissions for: $fullPath to $permissions")
        val result = adb.shell(device, "chmod", permissions, "--", shellQuote(fullPath), throwOnError = true)
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

    suspend fun getAppInfo(packageName: String, includeIcon: Boolean = true): AppInfo {
        require(packageName.isNotBlank()) { "Package name must not be blank" }
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        val apkPath = getAppApkPath(packageName)
            ?: return AppInfo(packageName = packageName, detailsLoaded = includeIcon)
        val token = UUID.randomUUID().toString()
        val remoteDirectory = "$REMOTE_APP_METADATA_DIRECTORY/$token"
        val remoteArchive = "$REMOTE_APP_METADATA_DIRECTORY/$token.tar.gz"
        val localRoot = withContext(Dispatchers.IO) { Files.createTempDirectory("afm-app-meta-").toFile() }
        val localArchive = File(localRoot, "metadata.tar.gz")
        val extractedDirectory = File(localRoot, "contents")
        val metadataApk = File(localRoot, "metadata.apk")

        return try {
            extractAppMetadataOnDevice(
                device = device,
                apkPath = apkPath,
                remoteDirectory = remoteDirectory,
                remoteArchive = remoteArchive,
                entries = listOf("AndroidManifest.xml", "resources.arsc")
            )
            adb.pull(device, remoteArchive, localArchive.absolutePath, throwOnError = true)
            extractTarArchive(localArchive, extractedDirectory)
            createMetadataApk(extractedDirectory, metadataApk)

            val initialMetadata = readApkMetadata(metadataApk, packageName, includeIcon = false)
            if (!includeIcon) return initialMetadata
            val iconPaths = ApkFile(metadataApk).use { apk ->
                apk.preferredLocale = Locale.getDefault()
                apk.iconPaths.map { it.path }.filter { it.isNotBlank() }.distinct()
            }
            if (iconPaths.isEmpty()) return initialMetadata.copy(detailsLoaded = true)

            extractAppMetadataOnDevice(
                device = device,
                apkPath = apkPath,
                remoteDirectory = remoteDirectory,
                remoteArchive = remoteArchive,
                entries = iconPaths
            )
            adb.pull(device, remoteArchive, localArchive.absolutePath, throwOnError = true)
            extractTarArchive(localArchive, extractedDirectory)
            createMetadataApk(extractedDirectory, metadataApk)
            readApkMetadata(metadataApk, packageName, includeIcon = true).copy(detailsLoaded = true)
        } catch (_: Exception) {
            AppInfo(packageName = packageName, detailsLoaded = includeIcon)
        } finally {
            withContext(NonCancellable + Dispatchers.IO) {
                localRoot.deleteRecursively()
                runCatching {
                    adb.shell(
                        device,
                        "rm", "-rf", "--", shellQuote(remoteDirectory), shellQuote(remoteArchive),
                        throwOnError = false
                    )
                }
            }
        }
    }

    private suspend fun extractAppMetadataOnDevice(
        device: AdbDevice,
        apkPath: String,
        remoteDirectory: String,
        remoteArchive: String,
        entries: List<String>
    ) {
        val args = buildList {
            addAll(listOf("mkdir", "-p", shellQuote(remoteDirectory), ";"))
            addAll(listOf("unzip", "-o", shellQuote(apkPath)))
            entries.forEach { add(shellQuote(it)) }
            addAll(listOf("-d", shellQuote(remoteDirectory), ">/dev/null", "2>&1", ";"))
            addAll(
                listOf(
                    "tar", "czf", shellQuote(remoteArchive),
                    "-C", shellQuote(remoteDirectory), "."
                )
            )
        }
        adb.shell(device, *args.toTypedArray(), throwOnError = true)
    }

    private suspend fun extractTarArchive(archive: File, destination: File) = withContext(Dispatchers.IO) {
        destination.mkdirs()
        val process = ProcessBuilder(
            "tar", "xzf", archive.absolutePath,
            "-C", destination.absolutePath
        ).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        check(process.waitFor() == 0) { "Unable to extract app metadata: $output" }
    }

    private suspend fun createMetadataApk(source: File, destination: File) = withContext(Dispatchers.IO) {
        ZipOutputStream(destination.outputStream().buffered()).use { zip ->
            Files.walk(source.toPath()).use { paths ->
                paths.filter { Files.isRegularFile(it) }.forEach { path ->
                    val name = source.toPath().relativize(path).toString().replace(File.separatorChar, '/')
                    zip.putNextEntry(ZipEntry(name))
                    Files.copy(path, zip)
                    zip.closeEntry()
                }
            }
        }
    }

    private suspend fun readApkMetadata(
        apkFile: File,
        packageName: String,
        includeIcon: Boolean
    ): AppInfo = withContext(Dispatchers.IO) {
        ApkFile(apkFile).use { apk ->
            apk.preferredLocale = Locale.getDefault()
            val label = runCatching { apk.apkMeta.label }
                .getOrNull()?.trim()?.takeIf { it.isNotEmpty() } ?: packageName
            val iconData = if (includeIcon) {
                runCatching {
                    apk.iconFiles
                        .filter { icon ->
                            val extension = icon.path.substringAfterLast('.', "").lowercase()
                            icon.data?.isNotEmpty() == true && extension in APP_ICON_EXTENSIONS
                        }
                        .maxByOrNull { it.density }
                        ?.data
                }.getOrNull()
            } else {
                null
            }
            AppInfo(packageName = packageName, label = label, iconData = iconData)
        }
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

    // ── Advanced App Actions ────────────────────────────────────────

    suspend fun launchApp(packageName: String) {
        require(packageName.isNotBlank()) { "Package name must not be blank" }
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        adb.shell(device, "monkey", "-p", shellQuote(packageName), "-c", "android.intent.category.LAUNCHER", "1", throwOnError = false)
    }

    suspend fun forceStopApp(packageName: String) {
        require(packageName.isNotBlank()) { "Package name must not be blank" }
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        adb.shell(device, "am", "force-stop", shellQuote(packageName), throwOnError = true)
    }

    suspend fun clearAppData(packageName: String) {
        require(packageName.isNotBlank()) { "Package name must not be blank" }
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        adb.shell(device, "pm", "clear", shellQuote(packageName), throwOnError = true)
    }

    suspend fun setAppEnabled(packageName: String, enabled: Boolean) {
        require(packageName.isNotBlank()) { "Package name must not be blank" }
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        val action = if (enabled) "enable" else "disable-user"
        adb.shell(device, "pm", action, shellQuote(packageName), throwOnError = true)
    }

    // ── Device Info & Reboot ────────────────────────────────────────

    suspend fun rebootDevice(mode: model.RebootMode) {
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        if (mode.commandArg != null) {
            adb.shell(device, "reboot", mode.commandArg, throwOnError = false)
        } else {
            adb.shell(device, "reboot", throwOnError = false)
        }
    }

    suspend fun getDeviceDetailedInfo(): model.DeviceDetailedInfo {
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()

        // 1. Get properties
        val propLines = adb.shell(device, "getprop", throwOnError = false)
        val props = mutableMapOf<String, String>()
        val propRegex = Regex("""\[(.*?)\]:\s*\[(.*?)\]""")
        propLines.forEach { line ->
            propRegex.find(line)?.let { match ->
                props[match.groupValues[1]] = match.groupValues[2]
            }
        }

        val modelName = props["ro.product.model"] ?: ""
        val manufacturer = props["ro.product.manufacturer"] ?: ""
        val brand = props["ro.product.brand"] ?: ""
        val deviceName = props["ro.product.name"] ?: ""
        val androidVersion = props["ro.build.version.release"] ?: ""
        val sdkInt = props["ro.build.version.sdk"]?.toIntOrNull() ?: 0
        val abi = props["ro.product.cpu.abi"] ?: ""

        // 2. Screen size & density
        val sizeLines = adb.shell(device, "wm", "size", throwOnError = false)
        val screenSize = sizeLines.firstOrNull()?.substringAfter("Physical size:")?.trim()
            ?: sizeLines.firstOrNull()?.substringAfter("size:")?.trim() ?: ""
        val densityLines = adb.shell(device, "wm", "density", throwOnError = false)
        val density = densityLines.firstOrNull()?.substringAfter("Physical density:")?.trim()
            ?: densityLines.firstOrNull()?.substringAfter("density:")?.trim() ?: ""

        // 3. Battery info
        val batteryLines = adb.shell(device, "dumpsys", "battery", throwOnError = false)
        var level = -1
        var scale = 100
        var tempRaw = 0
        var voltage = 0
        var statusStr = "未充电"
        var powerSource = "电池供电"
        var healthStr = "良好"

        batteryLines.forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("level:") -> level = line.substringAfter("level:").trim().toIntOrNull() ?: -1
                line.startsWith("scale:") -> scale = line.substringAfter("scale:").trim().toIntOrNull() ?: 100
                line.startsWith("temperature:") -> tempRaw = line.substringAfter("temperature:").trim().toIntOrNull() ?: 0
                line.startsWith("voltage:") -> voltage = line.substringAfter("voltage:").trim().toIntOrNull() ?: 0
                line.startsWith("AC powered: true") -> powerSource = "交流电源 (AC)"
                line.startsWith("USB powered: true") -> powerSource = "USB 供电"
                line.startsWith("Wireless powered: true") -> powerSource = "无线充电"
                line.startsWith("status:") -> {
                    statusStr = when (line.substringAfter("status:").trim()) {
                        "2" -> "充电中"
                        "3" -> "放电中"
                        "4" -> "未充电"
                        "5" -> "已充满"
                        else -> "正常"
                    }
                }
                line.startsWith("health:") -> {
                    healthStr = when (line.substringAfter("health:").trim()) {
                        "2" -> "良好 (Good)"
                        "3" -> "过热 (Overheat)"
                        "4" -> "损坏 (Dead)"
                        "5" -> "过压 (Over voltage)"
                        "6" -> "故障 (Failure)"
                        "7" -> "过冷 (Cold)"
                        else -> "正常"
                    }
                }
            }
        }
        val batteryInfo = model.BatteryInfo(
            level = level,
            scale = scale,
            temperature = tempRaw / 10f,
            voltage = voltage,
            health = healthStr,
            status = statusStr,
            powerSource = powerSource
        )

        // 4. Storage info from df /data
        val dfLines = adb.shell(device, "df", "-k", "/data", throwOnError = false)
        var totalBytes = 0L
        var usedBytes = 0L
        var freeBytes = 0L
        if (dfLines.size >= 2) {
            val dataLine = dfLines[1].trim().split(Regex("""\s+"""))
            if (dataLine.size >= 4) {
                val totalKb = dataLine[1].toLongOrNull() ?: 0L
                val usedKb = dataLine[2].toLongOrNull() ?: 0L
                val freeKb = dataLine[3].toLongOrNull() ?: 0L
                totalBytes = totalKb * 1024L
                usedBytes = usedKb * 1024L
                freeBytes = freeKb * 1024L
            }
        }
        val storageInfo = model.StorageInfo(totalBytes = totalBytes, usedBytes = usedBytes, freeBytes = freeBytes)

        // 5. Root check
        val suCheck = adb.shell(device, "which", "su", throwOnError = false)
        val isRooted = suCheck.any { it.contains("/su") }

        return model.DeviceDetailedInfo(
            model = modelName,
            manufacturer = manufacturer,
            brand = brand,
            deviceName = deviceName,
            androidVersion = androidVersion,
            sdkInt = sdkInt,
            abi = abi,
            screenSize = screenSize,
            density = density,
            battery = batteryInfo,
            internalStorage = storageInfo,
            isRooted = isRooted
        )
    }

    // ── Screenshot ──────────────────────────────────────────────────

    suspend fun takeScreenshot(destinationPath: String) {
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
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

    /**
     * Writes the Android system clipboard through a tiny app_process helper running as shell UID.
     * Android's `cmd clipboard` service is commonly listed but has no shell implementation.
     */
    suspend fun pushClipboard(text: String) {
        require(text.isNotBlank()) { "Clipboard text must not be blank" }
        val device = currentDeviceProvider() ?: throw AdbDeviceNotFoundException()
        val id = UUID.randomUUID().toString()
        val localHelper = File.createTempFile("afm-clipboard-helper-", ".dex")
        val localText = File.createTempFile("afm-clipboard-text-", ".txt")
        val remoteHelper = "/data/local/tmp/adb-file-manager-clipboard-$id.dex"
        val remoteText = "/data/local/tmp/adb-file-manager-clipboard-$id.txt"

        try {
            localHelper.writeBytes(clipboardHelperLoader())
            localText.writeText(text, StandardCharsets.UTF_8)
            adb.push(device, localHelper.absolutePath, remoteHelper, throwOnError = true)
            adb.push(device, localText.absolutePath, remoteText, throwOnError = true)

            val userId = adb.shell(device, "am", "get-current-user", throwOnError = true)
                .firstNotNullOfOrNull { it.trim().toIntOrNull() }
                ?: 0
            val output = adb.shell(
                device,
                "CLASSPATH=$remoteHelper",
                "app_process",
                "/system/bin",
                CLIPBOARD_HELPER_CLASS,
                remoteText,
                userId.toString(),
                throwOnError = true
            )
            if (output.none { it.trim() == CLIPBOARD_SUCCESS_MARKER }) {
                throw AdbException("设备未确认剪切板写入成功")
            }
        } finally {
            localHelper.delete()
            localText.delete()
            runCatching {
                adb.shell(device, "rm", "-f", remoteHelper, remoteText, throwOnError = false)
            }
        }
    }

    companion object {
        private const val LOCAL_STAGE_PREFIX = "afm_push_"
        private const val REMOTE_STAGE_DIRECTORY = "/data/local/tmp"
        private const val REMOTE_APP_METADATA_DIRECTORY = "/data/local/tmp"
        private val APP_ICON_EXTENSIONS = setOf("png", "webp", "jpg", "jpeg")
        private const val REMOTE_STAGE_PREFIX = "adb-file-manager-"
        private const val CLIPBOARD_HELPER_CLASS = "top.wkbin.clipboard.ClipboardHelper"
        private const val CLIPBOARD_SUCCESS_MARKER = "CLIPBOARD_SET_OK"

        @OptIn(ExperimentalResourceApi::class)
        private suspend fun loadClipboardHelper(): ByteArray {
            val encoded = Res.readBytes("files/android/clipboard-helper.dex.b64")
                .toString(StandardCharsets.US_ASCII)
                .trim()
            return Base64.getDecoder().decode(encoded)
        }
    }
}
