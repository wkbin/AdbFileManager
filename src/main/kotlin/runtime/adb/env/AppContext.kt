package runtime.adb.env

import runtime.adb.AdbDevice

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