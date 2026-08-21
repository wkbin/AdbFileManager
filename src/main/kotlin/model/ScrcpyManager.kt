package model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.skiko.hostOs
import utils.ZipUtils
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Comparator

/** Downloads, caches and launches scrcpy without bundling its binaries. */
object ScrcpyManager {
    private const val GITHUB_API_URL =
        "https://api.github.com/repos/Genymobile/scrcpy/releases/latest"

    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient by lazy { OkHttpClient() }

    internal enum class Platform { WINDOWS, MACOS, LINUX }

    internal fun currentPlatform(): Platform = when {
        hostOs.isWindows -> Platform.WINDOWS
        hostOs.isMacOS -> Platform.MACOS
        else -> Platform.LINUX
    }

    internal fun normalizedArchitecture(value: String = System.getProperty("os.arch")): String =
        when (value.lowercase()) {
            "aarch64", "arm64" -> "aarch64"
            "amd64", "x86_64", "x64" -> "x86_64"
            else -> value.lowercase()
        }

    internal fun cacheRoot(userHome: String = System.getProperty("user.home")): File =
        File(userHome, ".adbfilemanager/scrcpy")

    internal fun versionDirectory(
        version: String,
        userHome: String = System.getProperty("user.home")
    ): File = File(cacheRoot(userHome), version.removePrefix("v"))

    @Serializable
    data class GitHubRelease(
        val tag_name: String,
        val assets: List<GitHubAsset> = emptyList()
    )

    @Serializable
    data class GitHubAsset(
        val name: String,
        val browser_download_url: String,
        val size: Long = 0L
    )

    data class ScrcpyStatus(
        val version: String,
        val executablePath: String?,
        val assetName: String,
        val downloadUrl: String,
        val downloadSizeBytes: Long
    )

    data class DownloadProgress(
        val fraction: Float,
        val downloadedBytes: Long,
        val totalBytes: Long
    )

    internal fun parseRelease(value: String): GitHubRelease = json.decodeFromString(value)

    /** Selects the official prebuilt package for the current OS and CPU. */
    internal fun selectAsset(
        assets: List<GitHubAsset>,
        platform: Platform,
        architecture: String
    ): GitHubAsset? {
        val arch = normalizedArchitecture(architecture)
        val pattern = when (platform) {
            Platform.WINDOWS -> Regex("^scrcpy-win64-.*\\.zip$", RegexOption.IGNORE_CASE)
            Platform.MACOS -> when (arch) {
                "aarch64" -> Regex("^scrcpy-macos-aarch64-.*\\.tar\\.gz$", RegexOption.IGNORE_CASE)
                "x86_64" -> Regex("^scrcpy-macos-x86_64-.*\\.tar\\.gz$", RegexOption.IGNORE_CASE)
                else -> return null
            }
            Platform.LINUX -> when (arch) {
                "x86_64" -> Regex("^scrcpy-linux-x86_64-.*\\.tar\\.gz$", RegexOption.IGNORE_CASE)
                else -> return null
            }
        }
        return assets.firstOrNull { pattern.matches(it.name) }
    }

    /** Returns an installed cached executable without touching the network. */
    fun findCachedExecutable(): String? =
        findCachedExecutable(cacheRoot(), currentPlatform())?.absolutePath

    internal fun findCachedExecutable(root: File, platform: Platform): File? {
        if (!root.isDirectory) return null
        return root.listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith(".") }
            ?.sortedWith(compareByDescending<File> { semanticVersionParts(it.name) })
            ?.firstNotNullOfOrNull { findExecutable(it, platform) }
    }

    /** Queries the latest GitHub release and reports whether that version is cached. */
    suspend fun checkStatus(forceDownload: Boolean = false): ScrcpyStatus = withContext(Dispatchers.IO) {
        val release = fetchLatestRelease()
        val version = release.tag_name.removePrefix("v")
        val platform = currentPlatform()
        val architecture = normalizedArchitecture()
        val asset = selectAsset(release.assets, platform, architecture)
            ?: throw IllegalStateException(
                "未找到适用于 ${platform.name.lowercase()} / $architecture 的 scrcpy 安装包（版本 $version）"
            )

        val executable = if (forceDownload) null else findExecutable(versionDirectory(version), platform)
        ScrcpyStatus(
            version = version,
            executablePath = executable?.absolutePath,
            assetName = asset.name,
            downloadUrl = asset.browser_download_url,
            downloadSizeBytes = asset.size
        )
    }

    suspend fun downloadAndExtract(
        status: ScrcpyStatus,
        onProgress: (DownloadProgress) -> Unit = {},
        onStatusText: (String) -> Unit = {}
    ): String = withContext(Dispatchers.IO) {
        val root = cacheRoot().also { it.mkdirs() }
        val archiveSuffix = when {
            status.assetName.endsWith(".tar.gz", ignoreCase = true) -> ".tar.gz"
            status.assetName.endsWith(".zip", ignoreCase = true) -> ".zip"
            else -> throw IllegalArgumentException("不支持的压缩格式: ${status.assetName}")
        }
        val archive = File.createTempFile("scrcpy-${status.version}-", archiveSuffix, root)
        val staging = File(root, ".${status.version}-${System.nanoTime()}.staging")

        try {
            onStatusText("正在连接 GitHub…")
            val request = Request.Builder()
                .url(status.downloadUrl)
                .header("User-Agent", "AdbFileManager")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("下载失败，HTTP ${response.code}")
                }
                val body = response.body
                val totalBytes = body.contentLength().takeIf { it > 0 } ?: status.downloadSizeBytes
                var downloadedBytes = 0L
                onStatusText("正在下载 scrcpy v${status.version}…")
                body.byteStream().use { input ->
                    FileOutputStream(archive).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var count: Int
                        while (input.read(buffer).also { count = it } >= 0) {
                            if (count == 0) continue
                            output.write(buffer, 0, count)
                            downloadedBytes += count
                            val fraction = if (totalBytes > 0) {
                                downloadedBytes.toFloat().div(totalBytes).coerceIn(0f, 1f)
                            } else 0f
                            onProgress(DownloadProgress(fraction, downloadedBytes, totalBytes))
                        }
                    }
                }
            }

            onStatusText("正在解压…")
            staging.mkdirs()
            extractArchive(archive, staging)
            val executable = findExecutable(staging, currentPlatform())
                ?: throw IllegalStateException("解压完成但未找到 scrcpy 可执行文件")
            if (currentPlatform() != Platform.WINDOWS && !executable.setExecutable(true)) {
                throw IllegalStateException("无法设置 scrcpy 可执行权限")
            }

            val destination = versionDirectory(status.version)
            if (destination.exists()) deleteRecursively(destination)
            try {
                Files.move(staging.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE)
            } catch (_: Exception) {
                Files.move(staging.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            findExecutable(destination, currentPlatform())?.absolutePath
                ?: throw IllegalStateException("scrcpy 安装目录校验失败")
        } finally {
            archive.delete()
            if (staging.exists()) deleteRecursively(staging)
        }
    }

    /** Ensures a usable executable exists, reusing the local cache whenever possible. */
    suspend fun ensureAvailable(
        forceDownload: Boolean = false,
        onProgress: (DownloadProgress) -> Unit = {},
        onStatusText: (String) -> Unit = {}
    ): String {
        if (!forceDownload) findCachedExecutable()?.let { return it }
        onStatusText("正在检查 scrcpy 最新版本…")
        val status = checkStatus(forceDownload)
        return status.executablePath
            ?: downloadAndExtract(status, onProgress, onStatusText)
    }

    /** Downloads the latest release even when that version is already cached. */
    suspend fun forceUpdate(
        onProgress: (DownloadProgress) -> Unit = {},
        onStatusText: (String) -> Unit = {}
    ): String {
        return ensureAvailable(
            forceDownload = true,
            onProgress = onProgress,
            onStatusText = onStatusText
        )
    }

    fun launch(executablePath: String, deviceId: String): Process {
        val executable = File(executablePath)
        require(executable.isFile) { "scrcpy 路径无效: $executablePath" }
        val args = buildList {
            add(executable.absolutePath)
            add("--serial")
            add(deviceId)
            AppSettings.scrcpyMaxFps.takeIf { it in 1..120 }?.let {
                add("--max-fps")
                add(it.toString())
            }
            AppSettings.scrcpyBitrate.takeIf { it > 0 }?.let {
                add("--video-bit-rate")
                add("${it}M")
            }
            if (AppSettings.scrcpyTurnScreenOff) add("--turn-screen-off")
            if (AppSettings.scrcpyStayAwake) add("--stay-awake")
        }
        return ProcessBuilder(args)
            .directory(executable.parentFile)
            .inheritIO()
            .start()
    }

    private fun fetchLatestRelease(): GitHubRelease {
        val request = Request.Builder()
            .url(GITHUB_API_URL)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "AdbFileManager")
            .build()
        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("GitHub API 请求失败，HTTP ${response.code}")
            }
            parseRelease(response.body.string())
        }
    }

    private fun extractArchive(archive: File, destination: File) {
        when {
            archive.name.endsWith(".zip", ignoreCase = true) ->
                ZipUtils.unzip(archive.absolutePath, destination.absolutePath)
            archive.name.endsWith(".tar.gz", ignoreCase = true) -> {
                validateTarEntries(archive)
                val process = ProcessBuilder(
                    "tar", "xzf", archive.absolutePath, "-C", destination.absolutePath
                ).redirectErrorStream(true).start()
                val output = process.inputStream.bufferedReader().use { it.readText() }
                if (process.waitFor() != 0) throw IllegalStateException("tar 解压失败: $output")
            }
            else -> throw IllegalArgumentException("不支持的压缩格式: ${archive.name}")
        }
    }

    private fun validateTarEntries(archive: File) {
        val process = ProcessBuilder("tar", "tzf", archive.absolutePath)
            .redirectErrorStream(true)
            .start()
        val entries = process.inputStream.bufferedReader().use { it.readLines() }
        if (process.waitFor() != 0) throw IllegalStateException("无法读取 tar 压缩包")
        if (entries.any { entry ->
                val normalized = entry.replace('\\', '/')
                normalized.startsWith('/') || normalized.split('/').any { it == ".." }
            }) {
            throw SecurityException("scrcpy 压缩包包含不安全路径")
        }
    }

    internal fun findExecutable(versionDir: File, platform: Platform): File? {
        if (!versionDir.isDirectory) return null
        val windows = platform == Platform.WINDOWS
        val name = if (windows) "scrcpy.exe" else "scrcpy"
        return versionDir.walkTopDown().firstOrNull { file ->
            file.isFile && file.name.equals(name, ignoreCase = windows)
        }
    }

    private fun semanticVersionParts(version: String): String =
        Regex("\\d+").findAll(version).joinToString(".") { it.value.padStart(8, '0') }

    private fun deleteRecursively(target: File) {
        if (!target.exists()) return
        Files.walk(target.toPath()).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }
}
