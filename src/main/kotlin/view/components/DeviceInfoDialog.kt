package view.components

import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.ArrowClockwise
import io.github.composefluent.icons.regular.ArrowSync
import io.github.composefluent.icons.regular.BatteryCharge
import io.github.composefluent.icons.regular.Dismiss
import io.github.composefluent.icons.regular.Flash
import io.github.composefluent.icons.regular.Phone
import io.github.composefluent.icons.regular.Power
import io.github.composefluent.icons.regular.Storage
import io.github.composefluent.icons.regular.Warning
import io.github.composefluent.icons.regular.Wrench

import io.github.composefluent.FluentTheme
import io.github.composefluent.component.Text
import io.github.composefluent.component.Icon
import view.fluent.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import model.BatteryInfo
import model.DeviceDetailedInfo
import model.RebootMode
import model.StorageInfo

@Composable
fun DeviceInfoDialog(
    visible: Boolean,
    deviceInfo: DeviceDetailedInfo?,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onReboot: (RebootMode) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    var confirmRebootMode by remember { mutableStateOf<RebootMode?>(null) }
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .fillMaxHeight(0.88f),
            shape = FluentTheme.shapes.overlay,

            color = FluentTheme.colors.background.solid.tertiary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Regular.Phone,
                        contentDescription = null,
                        tint = FluentTheme.colors.text.accent.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "设备状态与信息仪表盘",
                            style = FluentTheme.typography.subtitle,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (deviceInfo != null && deviceInfo.model.isNotEmpty())
                                "${deviceInfo.manufacturer} ${deviceInfo.model} (${deviceInfo.deviceName})"
                            else "Android 设备",
                            style = FluentTheme.typography.caption,
                            color = FluentTheme.colors.text.text.secondary
                        )
                    }
                    IconButton(onClick = onRefresh, enabled = !isLoading) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Regular.ArrowClockwise, contentDescription = "刷新")
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Regular.Dismiss, contentDescription = "关闭")
                    }
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = FluentTheme.colors.stroke.divider.default.copy(alpha = 0.5f))
                Spacer(Modifier.height(14.dp))

                if (isLoading && deviceInfo == null) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text("正在读取设备硬件与状态信息...", style = FluentTheme.typography.body)
                        }
                    }
                } else if (deviceInfo != null) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Basic Device Specs Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = FluentTheme.shapes.overlay,
                            colors = CardDefaults.cardColors(containerColor = FluentTheme.colors.subtleFill.secondary)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Regular.Phone, contentDescription = null, tint = FluentTheme.colors.text.accent.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("系统与硬件规格", style = FluentTheme.typography.bodyStrong, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.weight(1f))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (deviceInfo.isRooted) FluentTheme.colors.system.successBackground else FluentTheme.colors.subtleFill.secondary
                                    ) {
                                        Text(
                                            text = if (deviceInfo.isRooted) "已 Root" else "未 Root (标准)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (deviceInfo.isRooted) Color.White else FluentTheme.colors.text.text.secondary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    InfoItem(label = "设备型号", value = deviceInfo.model.ifEmpty { "未知" }, modifier = Modifier.weight(1f))
                                    InfoItem(label = "制造商/品牌", value = "${deviceInfo.manufacturer} / ${deviceInfo.brand}".trim('/'), modifier = Modifier.weight(1f))
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    InfoItem(label = "Android 系统", value = "Android ${deviceInfo.androidVersion} (API ${deviceInfo.sdkInt})", modifier = Modifier.weight(1f))
                                    InfoItem(label = "CPU 架构", value = deviceInfo.abi.ifEmpty { "未知" }, modifier = Modifier.weight(1f))
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    InfoItem(label = "屏幕分辨率", value = deviceInfo.screenSize.ifEmpty { "未知" }, modifier = Modifier.weight(1f))
                                    InfoItem(label = "屏幕密度 (DPI)", value = deviceInfo.density.ifEmpty { "未知" }, modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        // 2. Battery & Health Card
                        BatteryCard(deviceInfo.battery)

                        // 3. Storage Card
                        StorageCard(deviceInfo.internalStorage)

                        // 4. Quick Power Controls
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = FluentTheme.shapes.overlay,
                            colors = CardDefaults.cardColors(containerColor = FluentTheme.colors.subtleFill.secondary)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Regular.Power, contentDescription = null, tint = FluentTheme.colors.text.accent.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("电源与重启控制", style = FluentTheme.typography.bodyStrong, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { confirmRebootMode = RebootMode.NORMAL },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Regular.ArrowSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("常规重启")
                                    }
                                    OutlinedButton(
                                        onClick = { confirmRebootMode = RebootMode.RECOVERY },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Regular.Wrench, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("重启 Recovery")
                                    }
                                    OutlinedButton(
                                        onClick = { confirmRebootMode = RebootMode.BOOTLOADER },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Regular.Flash, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("重启 Bootloader")
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
                        Text("关闭")
                    }
                }
            }
        }
    }

    // Confirmation dialog for reboot
    confirmRebootMode?.let { mode ->
        AlertDialog(
            onDismissRequest = { confirmRebootMode = null },
            icon = { Icon(Icons.Regular.Warning, contentDescription = null, tint = FluentTheme.colors.system.critical) },
            title = { Text("确认执行${mode.displayName}？") },
            text = { Text("重启设备将导致 ADB 连接断开。请确保已保存所有未完成的工作。") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = FluentTheme.colors.system.critical),
                    onClick = {
                        val selected = confirmRebootMode
                        confirmRebootMode = null
                        if (selected != null) {
                            onReboot(selected)
                            onDismiss()
                        }
                    }
                ) {
                    Text("立即重启")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRebootMode = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun InfoItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(end = 8.dp)) {
        Text(label, style = FluentTheme.typography.caption, color = FluentTheme.colors.text.text.secondary)
        Text(value, style = FluentTheme.typography.body, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BatteryCard(battery: BatteryInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FluentTheme.shapes.overlay,
        colors = CardDefaults.cardColors(containerColor = FluentTheme.colors.subtleFill.secondary)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Regular.BatteryCharge, contentDescription = null, tint = FluentTheme.colors.text.accent.primary)
                Spacer(Modifier.width(8.dp))
                Text("电池状态与健康", style = FluentTheme.typography.bodyStrong, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (battery.level >= 0) "${battery.percentage}%" else "未知",
                    style = FluentTheme.typography.subtitle,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        battery.percentage > 50 -> FluentTheme.colors.system.success
                        battery.percentage > 20 -> FluentTheme.colors.system.caution
                        else -> FluentTheme.colors.system.critical
                    }
                )
            }

            if (battery.level >= 0) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (battery.percentage / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = when {
                        battery.percentage > 50 -> FluentTheme.colors.system.success
                        battery.percentage > 20 -> FluentTheme.colors.system.caution
                        else -> FluentTheme.colors.system.critical
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                InfoItem(label = "充电状态", value = battery.status, modifier = Modifier.weight(1f))
                InfoItem(label = "供电来源", value = battery.powerSource, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoItem(label = "电池温度", value = if (battery.temperature > 0) "${battery.temperature} °C" else "未知", modifier = Modifier.weight(1f))
                InfoItem(label = "电压 / 健康度", value = "${if (battery.voltage > 0) "${battery.voltage} mV" else "未知"} / ${battery.health}", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StorageCard(storage: StorageInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FluentTheme.shapes.overlay,
        colors = CardDefaults.cardColors(containerColor = FluentTheme.colors.subtleFill.secondary)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Regular.Storage, contentDescription = null, tint = FluentTheme.colors.text.accent.primary)
                Spacer(Modifier.width(8.dp))
                Text("内部存储容量 (/data)", style = FluentTheme.typography.bodyStrong, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${(storage.usedPercent * 100).toInt()}% 已使用",
                    style = FluentTheme.typography.body,
                    fontWeight = FontWeight.SemiBold,
                    color = if (storage.usedPercent > 0.9f) FluentTheme.colors.system.critical else FluentTheme.colors.text.accent.primary
                )
            }

            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { storage.usedPercent },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = if (storage.usedPercent > 0.9f) FluentTheme.colors.system.critical else FluentTheme.colors.text.accent.primary
            )

            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoItem(label = "已使用空间", value = storage.usedFormatted, modifier = Modifier.weight(1f))
                InfoItem(label = "剩余可用空间", value = storage.freeFormatted, modifier = Modifier.weight(1f))
                InfoItem(label = "存储总容量", value = storage.totalFormatted, modifier = Modifier.weight(1f))
            }
        }
    }
}
