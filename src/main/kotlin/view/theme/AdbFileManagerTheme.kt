package view.theme

import androidx.compose.foundation.isSystemInDarkTheme
import io.github.composefluent.FluentTheme
import io.github.composefluent.darkColors
import io.github.composefluent.lightColors
import androidx.compose.runtime.*

// 应用主题状态，用于在整个应用中共享暗色模式设置
object ThemeState {
    // 默认使用系统主题
    val isDarkMode = mutableStateOf<Boolean?>(null)
    
    // 获取当前是否为暗色主题
    @Composable
    fun isDark(): Boolean {
        return isDarkMode.value ?: isSystemInDarkTheme()
    }
    
    // 切换主题
    fun toggleTheme() {
        isDarkMode.value = !(isDarkMode.value ?: false)
    }
    
    // 设置为跟随系统
    fun useSystemTheme() {
        isDarkMode.value = null
    }
}

@Composable
fun AdbFileManagerTheme(content: @Composable () -> Unit) {
    val darkTheme = ThemeState.isDark()
    val colors = remember(darkTheme) { if (darkTheme) darkColors() else lightColors() }
    FluentTheme(colors = colors, content = content)
}
