package runtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skiko.hostOs
import utils.ZipUtils
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Paths


abstract class Runtime {
    suspend fun installRuntime(byteArray: ByteArray, installPath: String) {
        withContext(Dispatchers.IO) {
            if (hostOs.isWindows) {
                ZipUtils.unzip(ByteArrayInputStream(byteArray), installPath)
                return@withContext
            }
            try {
                val adbOutputFile = File(installPath, "adb")
                if (!adbOutputFile.exists()) {
                    adbOutputFile.createNewFile()
                }
                ByteArrayInputStream(byteArray).use { inputStream ->
                    adbOutputFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 添加具体错误处理
            }
        }
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