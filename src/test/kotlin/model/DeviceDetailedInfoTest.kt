package model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeviceDetailedInfoTest {

    @Test
    fun `battery percentage calculates accurately`() {
        val battery = BatteryInfo(level = 85, scale = 100)
        assertEquals(85, battery.percentage)

        val customScaleBattery = BatteryInfo(level = 450, scale = 500)
        assertEquals(90, customScaleBattery.percentage)
    }

    @Test
    fun `storage info calculates used percent and formatters`() {
        val storage = StorageInfo(
            totalBytes = 1000L * 1024L * 1024L * 1024L, // 1000 GB
            usedBytes = 250L * 1024L * 1024L * 1024L,   // 250 GB
            freeBytes = 750L * 1024L * 1024L * 1024L    // 750 GB
        )

        assertEquals(0.25f, storage.usedPercent, 0.01f)
        assertTrue(storage.totalFormatted.contains("GB") || storage.totalFormatted.contains("TB"))
        assertTrue(storage.usedFormatted.contains("GB"))
        assertTrue(storage.freeFormatted.contains("GB"))
    }
}
