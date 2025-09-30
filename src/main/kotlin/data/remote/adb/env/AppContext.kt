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
} 