package runtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skiko.hostOs
import utils.ZipUtils
import java.awt.Desktop
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URI
import java.nio.file.Paths


abstract class Runtime {
    suspend fun installRuntime(byteArray: ByteArray, installPath: String) {
        val inputStream = ByteArrayInputStream(byteArray)
        try {
            ZipUtils.unzip(inputStream, installPath)
            withContext(Dispatchers.IO) {
                inputStream.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

            hostOs.isMacOS -> Paths.get(System.getProperty("user.home"), "Library", "Caches").toString()
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
    val adbHostFile by lazy {
        File(runtimeDir, "adb").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    val resourceName by lazy {
        val hostName = when {
            hostOs.isWindows -> "windows"
            hostOs.isMacOS -> "macos"
            else -> "linux"
        }
        "files/$hostName/adb.zip"
    }
}