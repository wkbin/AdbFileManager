package data.remote.adb.env

import data.remote.adb.AdbDevice

/**
 * Global application context for sharing state across components
 */
object AppContext {
    /**
     * Current ADB device
     */
    @Volatile
    var adbDevice: AdbDevice? = null

    /**
     * Debug mode flag. Set to true to enable verbose logging.
     * Controlled by system property "adbFileManager.debug" or environment variable "ADBFM_DEBUG".
     */
    val isDebug: Boolean by lazy {
        System.getProperty("adbFileManager.debug")?.toBoolean() == true
                || System.getenv("ADBFM_DEBUG")?.toBoolean() == true
    }
} 