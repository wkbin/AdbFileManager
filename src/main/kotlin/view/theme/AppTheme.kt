package view.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 主色调
private val md_theme_light_primary = Color(0xFF0061A4)
private val md_theme_light_onPrimary = Color(0xFFFFFFFF)
private val md_theme_light_primaryContainer = Color(0xFFD1E4FF)
private val md_theme_light_onPrimaryContainer = Color(0xFF001D36)

// 次要色调 - 活力蓝紫色调
private val md_theme_light_secondary = Color(0xFF535F70)
private val md_theme_light_onSecondary = Color(0xFFFFFFFF)
private val md_theme_light_secondaryContainer = Color(0xFFD7E3F8)
private val md_theme_light_onSecondaryContainer = Color(0xFF101C2B)

// 第三色调 - 薰衣草紫色
private val md_theme_light_tertiary = Color(0xFF6B5778)
private val md_theme_light_onTertiary = Color(0xFFFFFFFF)
private val md_theme_light_tertiaryContainer = Color(0xFFF2DAFF)
private val md_theme_light_onTertiaryContainer = Color(0xFF251431)

// 错误色调
private val md_theme_light_error = Color(0xFFBA1A1A)
private val md_theme_light_onError = Color(0xFFFFFFFF)
private val md_theme_light_errorContainer = Color(0xFFFFDAD6)
private val md_theme_light_onErrorContainer = Color(0xFF410002)

// 中性色调
private val md_theme_light_background = Color(0xFFFDFCFF)
private val md_theme_light_onBackground = Color(0xFF1A1C1E)
private val md_theme_light_surface = Color(0xFFFAF9FD)
private val md_theme_light_onSurface = Color(0xFF1A1C1E)
private val md_theme_light_surfaceVariant = Color(0xFFDFE2EB)
private val md_theme_light_onSurfaceVariant = Color(0xFF43474E)
private val md_theme_light_outline = Color(0xFF73777F)
private val md_theme_light_outlineVariant = Color(0xFFC3C7CF)
private val md_theme_light_scrim = Color(0xFF000000)
private val md_theme_light_inverseSurface = Color(0xFF2F3033)
private val md_theme_light_inverseOnSurface = Color(0xFFF1F0F4)
private val md_theme_light_inversePrimary = Color(0xFF9FCAFF)
private val md_theme_light_surfaceTint = Color(0xFF0061A4)

// 暗色模式色调
private val md_theme_dark_primary = Color(0xFF9FCAFF)
private val md_theme_dark_onPrimary = Color(0xFF003258)
private val md_theme_dark_primaryContainer = Color(0xFF00497D)
private val md_theme_dark_onPrimaryContainer = Color(0xFFD1E4FF)

private val md_theme_dark_secondary = Color(0xFFBBC7DB)
private val md_theme_dark_onSecondary = Color(0xFF253141)
private val md_theme_dark_secondaryContainer = Color(0xFF3B4858)
private val md_theme_dark_onSecondaryContainer = Color(0xFFD7E3F8)

private val md_theme_dark_tertiary = Color(0xFFD6BEE4)
private val md_theme_dark_onTertiary = Color(0xFF3A2948)
private val md_theme_dark_tertiaryContainer = Color(0xFF523F5F)
private val md_theme_dark_onTertiaryContainer = Color(0xFFF2DAFF)

private val md_theme_dark_error = Color(0xFFFFB4AB)
private val md_theme_dark_onError = Color(0xFF690005)
private val md_theme_dark_errorContainer = Color(0xFF93000A)
private val md_theme_dark_onErrorContainer = Color(0xFFFFB4AB)

private val md_theme_dark_background = Color(0xFF1A1C1E)
private val md_theme_dark_onBackground = Color(0xFFE2E2E6)
private val md_theme_dark_surface = Color(0xFF121316)
private val md_theme_dark_onSurface = Color(0xFFE2E2E6)
private val md_theme_dark_surfaceVariant = Color(0xFF43474E)
private val md_theme_dark_onSurfaceVariant = Color(0xFFC3C7CF)
private val md_theme_dark_outline = Color(0xFF8D9199)
private val md_theme_dark_outlineVariant = Color(0xFF43474E)
private val md_theme_dark_scrim = Color(0xFF000000)
private val md_theme_dark_inverseSurface = Color(0xFFE2E2E6)
private val md_theme_dark_inverseOnSurface = Color(0xFF2F3033)
private val md_theme_dark_inversePrimary = Color(0xFF0061A4)
private val md_theme_dark_surfaceTint = Color(0xFF9FCAFF)

// 定义浅色主题颜色
private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    outlineVariant = md_theme_light_outlineVariant,
    scrim = md_theme_light_scrim,
    inverseSurface = md_theme_light_inverseSurface,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint
)

// 定义深色主题颜色
private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    outlineVariant = md_theme_dark_outlineVariant,
    scrim = md_theme_dark_scrim,
    inverseSurface = md_theme_dark_inverseSurface,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint
)

// Material 3主题应用
@Composable
fun AdbFileManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // 动态颜色在桌面端暂不支持，这里预留接口
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// 定义排版
private val Typography = Typography(
    // 正文大号
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    // 标题大号
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    // 标签中号
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    // 正文中号
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    // 标题中号
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    )
) 