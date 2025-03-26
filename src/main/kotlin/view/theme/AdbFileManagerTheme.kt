package view.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

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

// 浅色主题颜色
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1E88E5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD0E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    
    secondary = Color(0xFF00695C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFAEEADE),
    onSecondaryContainer = Color(0xFF00201C),
    
    tertiary = Color(0xFF6A3AB2),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEDDCFF),
    onTertiaryContainer = Color(0xFF25005A),
    
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1D1B1A),
    surface = Color(0xFFFEFEFE),
    onSurface = Color(0xFF1D1B1A),
    
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF7C7C7C),
    outlineVariant = Color(0xFFCAC4D0)
)

// 深色主题颜色
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF93CCFF),
    onPrimary = Color(0xFF003259),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD0E4FF),
    
    secondary = Color(0xFF8BD0C4),
    onSecondary = Color(0xFF003831),
    secondaryContainer = Color(0xFF005046),
    onSecondaryContainer = Color(0xFFAEEADE),
    
    tertiary = Color(0xFFD0BCFF),
    onTertiary = Color(0xFF371E73),
    tertiaryContainer = Color(0xFF4F378B),
    onTertiaryContainer = Color(0xFFEDDCFF),
    
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE3E2E6),
    
    surfaceVariant = Color(0xFF323232),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

@Composable
fun AdbFileManagerTheme(
    content: @Composable () -> Unit
) {
    // 获取当前是否使用暗色主题
    val darkTheme = ThemeState.isDark()
    
    // 应用相应的颜色主题
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// 自定义文字排版风格
private val Typography = Typography(
    // 标题大
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp
    ),
    // 标题中
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    // 标题小
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    // 正文大
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    // 正文中
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    // 正文小
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    // 标签中
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    ),
    // 标签小
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp
    )
) 