package model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skiko.hostOs
import java.io.File
import java.util.prefs.Preferences

enum class AdbMode(val displayName: String) {
    BUNDLED("内置 ADB (推荐)"),
    CUSTOM("自定义 ADB 路径"),
    SYSTEM("系统 PATH 环境变量")
}

object AppSettings {
    private const val KEY_ADB_MODE = "adbMode"
    private const val KEY_CUSTOM_ADB_PATH = "customAdbPath"
    private const val KEY_DEFAULT_EXPORT_DIR = "defaultExportDir"
    private const val KEY_SHOW_HIDDEN_FILES = "showHiddenFiles"
    private const val KEY_DEFAULT_SORT_TYPE = "defaultSortType"
    // Scrcpy settings
    private const val KEY_SCRCPY_MAX_FPS = "scrcpyMaxFps"
    private const val KEY_SCRCPY_BITRATE = "scrcpyBitrate"
    private const val KEY_SCRCPY_TURN_SCREEN_OFF = "scrcpyTurnScreenOff"
    private const val KEY_SCRCPY_STAY_AWAKE = "scrcpyStayAwake"
    private const val KEY_SCRCPY_CUSTOM_PATH = "scrcpyCustomPath"

    private val preferences by lazy {
        Preferences.userNodeForPackage(AppSettings::class.java)
    }

    var adbMode: AdbMode
        get() {
            val name = runCatching { preferences.get(KEY_ADB_MODE, AdbMode.BUNDLED.name) }.getOrDefault(AdbMode.BUNDLED.name)
            return try {
                AdbMode.valueOf(name)
            } catch (_: Exception) {
                AdbMode.BUNDLED
            }
        }
        set(value) {
            runCatching { preferences.put(KEY_ADB_MODE, value.name) }
        }

    var customAdbPath: String
        get() = runCatching { preferences.get(KEY_CUSTOM_ADB_PATH, "") }.getOrDefault("")
        set(value) {
            runCatching { preferences.put(KEY_CUSTOM_ADB_PATH, value) }
        }

    var defaultExportDir: String
        get() {
            val defaultPath = File(System.getProperty("user.home"), "Downloads").absolutePath
            return runCatching { preferences.get(KEY_DEFAULT_EXPORT_DIR, defaultPath) }.getOrDefault(defaultPath)
        }
        set(value) {
            runCatching { preferences.put(KEY_DEFAULT_EXPORT_DIR, value) }
        }

    var showHiddenFiles: Boolean
        get() = runCatching { preferences.getBoolean(KEY_SHOW_HIDDEN_FILES, true) }.getOrDefault(true)
        set(value) {
            runCatching { preferences.putBoolean(KEY_SHOW_HIDDEN_FILES, value) }
        }

    var defaultSortType: SortType
        get() {
            val name = runCatching { preferences.get(KEY_DEFAULT_SORT_TYPE, SortType.TYPE_ASC.name) }.getOrDefault(SortType.TYPE_ASC.name)
            return try {
                SortType.valueOf(name)
            } catch (_: Exception) {
                SortType.TYPE_ASC
            }
        }
        set(value) {
            runCatching { preferences.put(KEY_DEFAULT_SORT_TYPE, value.name) }
        }

    fun getEffectiveAdbPath(bundledPath: String): String {
        return when (adbMode) {
            AdbMode.BUNDLED -> bundledPath
            AdbMode.CUSTOM -> {
                val custom = customAdbPath.trim()
                if (custom.isNotEmpty() && File(custom).exists()) custom else bundledPath
            }
            AdbMode.SYSTEM -> if (hostOs.isWindows) "adb.exe" else "adb"
        }
    }

    suspend fun testAdbVersion(executablePath: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder(executablePath, "version")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()
            if (exitCode == 0 && output.contains("Android Debug Bridge", ignoreCase = true)) {
                true to output
            } else {
                false to (if (output.isNotEmpty()) output else "执行失败，返回码: $exitCode")
            }
        } catch (e: Exception) {
            false to "无法执行: ${e.message ?: "未知错误"}"
        }
    }

    fun resetToDefaults() {
        adbMode = AdbMode.BUNDLED
        customAdbPath = ""
        defaultExportDir = File(System.getProperty("user.home"), "Downloads").absolutePath
        showHiddenFiles = true
        defaultSortType = SortType.TYPE_ASC
        scrcpyMaxFps = 60
        scrcpyBitrate = 0
        scrcpyTurnScreenOff = false
        scrcpyStayAwake = false
        scrcpyCustomPath = ""
    }

    // ── Scrcpy 投屏设置 ─────────────────────────────────────────────

    /** 最大帧率 (1-120)，0 表示不限制 */
    var scrcpyMaxFps: Int
        get() = runCatching { preferences.getInt(KEY_SCRCPY_MAX_FPS, 60) }.getOrDefault(60)
        set(value) { runCatching { preferences.putInt(KEY_SCRCPY_MAX_FPS, value) } }

    /** 视频码率（Mbps），0 表示使用 scrcpy 默认 */
    var scrcpyBitrate: Int
        get() = runCatching { preferences.getInt(KEY_SCRCPY_BITRATE, 0) }.getOrDefault(0)
        set(value) { runCatching { preferences.putInt(KEY_SCRCPY_BITRATE, value) } }

    /** 投屏时关闭手机屏幕 */
    var scrcpyTurnScreenOff: Boolean
        get() = runCatching { preferences.getBoolean(KEY_SCRCPY_TURN_SCREEN_OFF, false) }.getOrDefault(false)
        set(value) { runCatching { preferences.putBoolean(KEY_SCRCPY_TURN_SCREEN_OFF, value) } }

    /** 投屏时保持手机屏幕常亮 */
    var scrcpyStayAwake: Boolean
        get() = runCatching { preferences.getBoolean(KEY_SCRCPY_STAY_AWAKE, false) }.getOrDefault(false)
        set(value) { runCatching { preferences.putBoolean(KEY_SCRCPY_STAY_AWAKE, value) } }

    /** 手动指定 scrcpy 可执行文件路径（留空则使用自动下载的版本） */
    var scrcpyCustomPath: String
        get() = runCatching { preferences.get(KEY_SCRCPY_CUSTOM_PATH, "") }.getOrDefault("")
        set(value) { runCatching { preferences.put(KEY_SCRCPY_CUSTOM_PATH, value) } }
}
