package model

enum class RebootMode(val commandArg: String?, val displayName: String) {
    NORMAL(null, "常规重启"),
    RECOVERY("recovery", "重启到 Recovery (恢复模式)"),
    BOOTLOADER("bootloader", "重启到 Bootloader (引导模式)")
}

data class BatteryInfo(
    val level: Int = -1,
    val scale: Int = 100,
    val temperature: Float = 0f, // in Celsius
    val voltage: Int = 0, // in mV
    val health: String = "未知",
    val status: String = "未充电",
    val powerSource: String = "电池供电"
) {
    val percentage: Int get() = if (scale > 0 && level >= 0) (level * 100) / scale else level
}

data class StorageInfo(
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val freeBytes: Long = 0L
) {
    val totalFormatted: String get() = if (totalBytes > 0) formatBytes(totalBytes) else "未知"
    val usedFormatted: String get() = if (usedBytes > 0) formatBytes(usedBytes) else "未知"
    val freeFormatted: String get() = if (freeBytes > 0) formatBytes(freeBytes) else "未知"
    val usedPercent: Float get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

data class DeviceDetailedInfo(
    val model: String = "",
    val manufacturer: String = "",
    val brand: String = "",
    val deviceName: String = "",
    val androidVersion: String = "",
    val sdkInt: Int = 0,
    val abi: String = "",
    val screenSize: String = "",
    val density: String = "",
    val battery: BatteryInfo = BatteryInfo(),
    val internalStorage: StorageInfo = StorageInfo(),
    val isRooted: Boolean = false
)

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return String.format(java.util.Locale.US, "%.1f %s", value, units[digitGroups])
}
