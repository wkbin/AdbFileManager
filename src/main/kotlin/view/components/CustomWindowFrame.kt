package view.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Minimize
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ElectricalServices
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import model.Language
import model.StringsManager
import data.remote.adb.AdbDevice
import view.theme.ThemeState

/**
 * 自定义窗口框架，提供自定义标题栏和内容区域
 */
@Composable
fun FrameWindowScope.CustomWindowFrame(
    title: String,
    currentDevice: AdbDevice?,
    devices: List<AdbDevice>,
    onConnect: (device: AdbDevice) -> Unit,
    onCloseRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    // 检测当前是否为暗色模式
    val isDarkMode = ThemeState.isDark()

    // 关于对话框状态
    var showAboutDialog by remember { mutableStateOf(false) }
    val strings by StringsManager.strings.collectAsState()

    Surface(
        modifier = Modifier
            .fillMaxSize(),
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 自定义标题栏 - 可拖动
            Surface(
                color = if (isDarkMode)
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                else
                    MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 可拖动区域
                MoveableWindowArea { dragModifier ->
                    Row(
                        modifier = dragModifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 应用图标
                        Icon(
                            imageVector = Icons.Filled.PhoneAndroid,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // 应用标题
                        Text(
                            text = "$title - ${currentDevice?.deviceId ?: strings.deviceNotConnected}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        ThemeMenuView()

                        Spacer(modifier = Modifier.width(4.dp))

                        LanguageSwitchButton()

                        Spacer(modifier = Modifier.width(4.dp))

                        DeviceMenuView(currentDevice, devices, onConnect)

                        Spacer(modifier = Modifier.width(4.dp))
                        // GitHub 图标按钮
                        IconButton(
                            onClick = { showAboutDialog = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Code,
                                contentDescription = strings.aboutTitle,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // 窗口控制按钮区域
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 最小化按钮
                            IconButton(
                                onClick = { window.isMinimized = true },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Minimize,
                                    contentDescription = strings.windowMinimize,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // 关闭按钮
                            Surface(
                                color = Color.Transparent,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.size(40.dp)
                            ) {
                                IconButton(
                                    onClick = onCloseRequest
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = strings.windowClose,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 内容区域
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                content()
            }

            if (showAboutDialog) {
                // 关于对话框
                AboutDialog(
                    onDismiss = { showAboutDialog = false }
                )
            }
        }
    }
}

@Composable
private fun ThemeMenuView() {
    // 获取当前主题模式状态
    val isSystemDark = isSystemInDarkTheme()
    val isDarkValue = ThemeState.isDarkMode.value
    val isDark = isDarkValue ?: isSystemDark
    // 主题切换菜单状态
    var showThemeMenu by remember { mutableStateOf(false) }
    val strings by StringsManager.strings.collectAsState()

    // 确定主题图标
    val themeIcon = when {
        isDarkValue == null -> Icons.Outlined.Brightness6 // 跟随系统
        isDark -> Icons.Outlined.DarkMode // 暗色模式
        else -> Icons.Outlined.LightMode // 亮色模式
    }
    Box {
        IconButton(
            onClick = { showThemeMenu = true }
        ) {
            Icon(
                imageVector = themeIcon,
                contentDescription = strings.themeToggle,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        // 主题切换菜单
        DropdownMenu(
            expanded = showThemeMenu,
            onDismissRequest = { showThemeMenu = false }
        ) {
            // 跟随系统选项
            DropdownMenuItem(
                text = { Text(strings.themeFollowSystem) },
                onClick = {
                    ThemeState.useSystemTheme()
                    showThemeMenu = false
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Brightness6,
                        contentDescription = null
                    )
                },
                trailingIcon = if (isDarkValue == null) {
                    {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else null
            )

            // 亮色模式选项
            DropdownMenuItem(
                text = { Text(strings.themeLight) },
                onClick = {
                    ThemeState.isDarkMode.value = false
                    showThemeMenu = false
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.LightMode,
                        contentDescription = null
                    )
                },
                trailingIcon = if (isDarkValue == false) {
                    {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else null
            )

            // 暗色模式选项
            DropdownMenuItem(
                text = { Text(strings.themeDark) },
                onClick = {
                    ThemeState.isDarkMode.value = true
                    showThemeMenu = false
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.DarkMode,
                        contentDescription = null
                    )
                },
                trailingIcon = if (isDarkValue == true) {
                    {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else null
            )
        }
    }
}

@Composable
private fun LanguageSwitchButton() {
    val currentLanguage by StringsManager.language.collectAsState()
    var showLanguageMenu by remember { mutableStateOf(false) }
    val strings by StringsManager.strings.collectAsState()
    Box {
        IconButton(
            onClick = { showLanguageMenu = true }
        ) {
            Icon(
                imageVector = Icons.Outlined.Language,
                contentDescription = strings.languageToggle,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        DropdownMenu(
            expanded = showLanguageMenu,
            onDismissRequest = { showLanguageMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(strings.languageChinese) },
                onClick = {
                    StringsManager.setLanguage(Language.ZH)
                    showLanguageMenu = false
                },
                trailingIcon = if (currentLanguage == Language.ZH) {
                    {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else null
            )
            DropdownMenuItem(
                text = { Text(strings.languageEnglish) },
                onClick = {
                    StringsManager.setLanguage(Language.EN)
                    showLanguageMenu = false
                },
                trailingIcon = if (currentLanguage == Language.EN) {
                    {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else null
            )
        }
    }
}

@Composable
private fun DeviceMenuView(
    currentDevice: AdbDevice?,
    devices: List<AdbDevice>,
    onConnect: (device: AdbDevice) -> Unit,
) {
    val strings by StringsManager.strings.collectAsState()
    Box {
        var showDevicesMenu by remember { mutableStateOf(false) }
        IconButton(
            onClick = { showDevicesMenu = true }
        ) {
            Icon(
                imageVector = Icons.Rounded.ElectricalServices,
                contentDescription = strings.deviceToggle,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        DropdownMenu(
            expanded = showDevicesMenu,
            onDismissRequest = { showDevicesMenu = false }
        ) {
            devices.forEach { device ->
                DropdownMenuItem(
                    text = { Text(device.deviceId) },
                    onClick = {
                        onConnect.invoke(device)
                        showDevicesMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Smartphone,
                            contentDescription = null
                        )
                    },
                    trailingIcon = if (device.deviceId == currentDevice?.deviceId) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else null
                )
            }
        }
    }
}