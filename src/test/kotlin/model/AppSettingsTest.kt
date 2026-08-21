package model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppSettingsTest {

    @Test
    fun `getEffectiveAdbPath resolves bundled adb path by default`() {
        AppSettings.adbMode = AdbMode.BUNDLED
        val bundledPath = "C:\\path\\to\\bundled\\adb.exe"
        val effective = AppSettings.getEffectiveAdbPath(bundledPath)
        assertEquals(bundledPath, effective)
    }

    @Test
    fun `getEffectiveAdbPath resolves system adb path when system mode is selected`() {
        AppSettings.adbMode = AdbMode.SYSTEM
        val bundledPath = "C:\\path\\to\\bundled\\adb.exe"
        val effective = AppSettings.getEffectiveAdbPath(bundledPath)
        assertTrue(effective == "adb.exe" || effective == "adb")
    }

    @Test
    fun `resetToDefaults resets all configuration values`() {
        AppSettings.adbMode = AdbMode.CUSTOM
        AppSettings.customAdbPath = "/custom/path/adb"
        AppSettings.showHiddenFiles = false

        AppSettings.resetToDefaults()

        assertEquals(AdbMode.BUNDLED, AppSettings.adbMode)
        assertEquals("", AppSettings.customAdbPath)
        assertTrue(AppSettings.showHiddenFiles)
        assertEquals(SortType.TYPE_ASC, AppSettings.defaultSortType)
    }
}
