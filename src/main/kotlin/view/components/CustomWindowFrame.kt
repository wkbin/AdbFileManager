package view.components

import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.Checkmark
import io.github.composefluent.icons.regular.Code
import io.github.composefluent.icons.regular.DarkTheme
import io.github.composefluent.icons.regular.Dismiss
import io.github.composefluent.icons.regular.Phone
import io.github.composefluent.icons.regular.PlugConnected
import io.github.composefluent.icons.regular.Settings
import io.github.composefluent.icons.regular.Subtract
import io.github.composefluent.icons.regular.Translate
import io.github.composefluent.icons.regular.WeatherMoon
import io.github.composefluent.icons.regular.WeatherSunny

import io.github.composefluent.FluentTheme
import io.github.composefluent.component.Text
import io.github.composefluent.component.Icon
import view.fluent.*

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
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
    onTerminalClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onCloseRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    // 关于对话框状态
    var showAboutDialog by remember { mutableStateOf(false) }
    val strings by StringsManager.strings.collectAsState()

    Surface(
        modifier = Modifier
            .fillMaxSize(),

        color = FluentTheme.colors.background.mica.base
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 自定义标题栏 - 可拖动
            Surface(
                color = FluentTheme.colors.background.mica.base,

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
                            imageVector = Icons.Regular.Phone,
                            contentDescription = null,
                            tint = FluentTheme.colors.text.accent.primary,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // 应用标题
                        Text(
                            text = "$title - ${currentDevice?.deviceId ?: strings.deviceNotConnected}",
                            style = FluentTheme.typography.bodyStrong.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = FluentTheme.colors.text.text.primary
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        ThemeMenuView()

                        Spacer(modifier = Modifier.width(4.dp))

                        LanguageSwitchButton()

                        Spacer(modifier = Modifier.width(4.dp))

                        DeviceMenuView(currentDevice, devices, onConnect)

                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onTerminalClick,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Regular.Code,
                                contentDescription = strings.terminalOpen,
                                tint = FluentTheme.colors.text.accent.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))
                        // 设置按钮
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Regular.Settings,
                                contentDescription = "设置",
                                tint = FluentTheme.colors.text.accent.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))
                        // GitHub 图标按钮
                        IconButton(
                            onClick = { showAboutDialog = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Regular.Code,
                                contentDescription = strings.aboutTitle,
                                tint = FluentTheme.colors.text.accent.primary
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
                                    imageVector = Icons.Regular.Subtract,
                                    contentDescription = strings.windowMinimize,
                                    tint = FluentTheme.colors.text.text.secondary
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // 关闭按钮
                            Surface(
                                color = Color.Transparent,
                                shape = FluentTheme.shapes.control,
                                modifier = Modifier.size(40.dp)
                            ) {
                                IconButton(
                                    onClick = onCloseRequest
                                ) {
                                    Icon(
                                        imageVector = Icons.Regular.Dismiss,
                                        contentDescription = strings.windowClose,
                                        tint = FluentTheme.colors.system.critical
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
        isDarkValue == null -> Icons.Regular.DarkTheme // 跟随系统
        isDark -> Icons.Regular.WeatherMoon // 暗色模式
        else -> Icons.Regular.WeatherSunny // 亮色模式
    }
    Box {
        IconButton(
            onClick = { showThemeMenu = true }
        ) {
            Icon(
                imageVector = themeIcon,
                contentDescription = strings.themeToggle,
                tint = FluentTheme.colors.text.accent.primary
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
                        imageVector = Icons.Regular.DarkTheme,
                        contentDescription = null
                    )
                },
                trailingIcon = if (isDarkValue == null) {
                    {
                        Icon(
                            imageVector = Icons.Regular.Checkmark,
                            contentDescription = null,
                            tint = FluentTheme.colors.text.accent.primary
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
                        imageVector = Icons.Regular.WeatherSunny,
                        contentDescription = null
                    )
                },
                trailingIcon = if (isDarkValue == false) {
                    {
                        Icon(
                            imageVector = Icons.Regular.Checkmark,
                            contentDescription = null,
                            tint = FluentTheme.colors.text.accent.primary
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
                        imageVector = Icons.Regular.WeatherMoon,
                        contentDescription = null
                    )
                },
                trailingIcon = if (isDarkValue == true) {
                    {
                        Icon(
                            imageVector = Icons.Regular.Checkmark,
                            contentDescription = null,
                            tint = FluentTheme.colors.text.accent.primary
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
                imageVector = Icons.Regular.Translate,
                contentDescription = strings.languageToggle,
                tint = FluentTheme.colors.text.accent.primary
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
                            imageVector = Icons.Regular.Checkmark,
                            contentDescription = null,
                            tint = FluentTheme.colors.text.accent.primary
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
                            imageVector = Icons.Regular.Checkmark,
                            contentDescription = null,
                            tint = FluentTheme.colors.text.accent.primary
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
                imageVector = Icons.Regular.PlugConnected,
                contentDescription = strings.deviceToggle,
                tint = FluentTheme.colors.text.accent.primary
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
                            imageVector = Icons.Regular.Phone,
                            contentDescription = null
                        )
                    },
                    trailingIcon = if (device.deviceId == currentDevice?.deviceId) {
                        {
                            Icon(
                                imageVector = Icons.Regular.Checkmark,
                                contentDescription = null,
                                tint = FluentTheme.colors.text.accent.primary
                            )
                        }
                    } else null
                )
            }
        }
    }
}
