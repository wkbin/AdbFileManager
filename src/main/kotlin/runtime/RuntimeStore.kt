package runtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skiko.hostOs
import utils.ZipUtils
import java.io.ByteArrayInputStream
import java.io.File



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



class AdbStore(runtimeDir: File) : Runtime() {
    val adbHostFile by lazy {
        File(runtimeDir, "adb").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    val resourceName by lazy {
        when {
            hostOs.isWindows -> R.raw.adbWindows
            hostOs.isMacOS -> R.raw.adbMacOs
            else -> R.raw.adbLinux
        }
    }
}