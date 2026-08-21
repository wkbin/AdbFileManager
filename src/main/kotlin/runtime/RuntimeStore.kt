package runtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skiko.hostOs
import utils.ZipUtils
import java.io.ByteArrayInputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileLock
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.file.Paths


abstract class Runtime {
    suspend fun installRuntime(byteArray: ByteArray, installPath: String) {
        withContext(Dispatchers.IO) {
            synchronized(INSTALL_MONITOR) {
                val installDirectory = File(installPath).also { it.mkdirs() }
                val lockFile = File(installDirectory.parentFile ?: installDirectory, ".adb-runtime-install.lock")
                RandomAccessFile(lockFile, "rw").channel.use { channel ->
                    val lock: FileLock = channel.lock()
                    try {
                        if (hostOs.isWindows) {
                            installWindowsRuntime(byteArray, installDirectory)
                            return@synchronized
                        }

                        val adbOutputFile = File(installDirectory, "adb")
                        if (!adbOutputFile.exists() || !contentMatches(adbOutputFile, byteArray)) {
                            adbOutputFile.outputStream().use { outputStream ->
                                outputStream.write(byteArray)
                            }
                        }
                    } finally {
                        lock.release()
                    }
                }
            }
        }
    }

    private fun installWindowsRuntime(byteArray: ByteArray, installDirectory: File) {
        if (archiveMatches(byteArray, installDirectory)) return

        val existingAdb = File(installDirectory, "adb.exe")
        if (existingAdb.isFile && existingAdb.length() > 0 && isFileInUse(existingAdb)) {
            // Another app instance or adb command is using this executable. The
            // existing complete runtime remains usable; retry the update next start.
            println("ADB runtime update deferred because ${existingAdb.absolutePath} is in use")
            return
        }

        val parentDir = installDirectory.parentFile ?: installDirectory
        parentDir.mkdirs()
        installDirectory.mkdirs()

        val stagingDirectory = Files.createTempDirectory(
            parentDir.toPath(),
            ".adb-runtime-staging-"
        ).toFile()
        try {
            ByteArrayInputStream(byteArray).use { ZipUtils.unzip(it, stagingDirectory.absolutePath) }

            stagingDirectory.walkTopDown().forEach { stagedFile ->
                val relativePath = stagedFile.relativeTo(stagingDirectory).path
                if (relativePath.isNotEmpty()) {
                    val targetFile = File(installDirectory, relativePath)
                    if (stagedFile.isDirectory) {
                        targetFile.mkdirs()
                    } else if (stagedFile.isFile) {
                        targetFile.parentFile?.mkdirs()
                        Files.move(
                            stagedFile.toPath(),
                            targetFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                        )
                    }
                }
            }
        } finally {
            stagingDirectory.deleteRecursively()
        }
    }

    private fun archiveMatches(byteArray: ByteArray, installDirectory: File): Boolean =
        ByteArrayInputStream(byteArray).use {
            ZipUtils.archiveMatches(it, installDirectory.absolutePath)
        }

    private fun isFileInUse(file: File): Boolean = try {
        Files.newByteChannel(file.toPath(), StandardOpenOption.WRITE).use { }
        false
    } catch (_: Exception) {
        true
    }

    private fun contentMatches(file: File, bytes: ByteArray): Boolean {
        if (!file.isFile || file.length() != bytes.size.toLong()) return false
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var offset = 0
            while (true) {
                val count = input.read(buffer)
                if (count < 0) return offset == bytes.size
                for (index in 0 until count) {
                    if (buffer[index] != bytes[offset + index]) return false
                }
                offset += count
            }
        }
    }

    companion object {
        private val INSTALL_MONITOR = Any()
    }
}


class ContextStore {
    private val rootPath: String = ".fileManager"

    //缓存目录
    private val rootDir by lazy {
        val path = when {
            hostOs.isWindows -> System.getenv("LOCALAPPDATA") ?: Paths.get(
                System.getProperty("user.home"),
                "AppData",
                "Local"
            ).toString()

            hostOs.isMacOS -> Paths.get(System.getProperty("user.home"), "Library", "Caches")
                .toString()

            else -> Paths.get(System.getProperty("user.home"), ".cache").toString()
        }
        val file = File(path, rootPath)
        if (!file.exists()) file.mkdirs()
        file
    }

    val fileDir by lazy {
        val file = File(rootDir, "files")
        if (!file.exists()) {
            file.mkdirs()
        }
        file
    }
}


class AdbStore(runtimeDir: File) : Runtime() {
    val adbHostFile = runtimeDir

    val resourceName by lazy {
        val hostName = when {
            hostOs.isWindows -> "windows"
            hostOs.isMacOS -> "macos"
            else -> "linux"
        }
        val adb = if (hostOs.isWindows) "adb.zip" else "adb"
        "files/$hostName/$adb"
    }
}
