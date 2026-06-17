package com.diary.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// Font scale for global text size adjustment
val LocalFontScale = staticCompositionLocalOf { 1.0f }

fun scaledTypography(scale: Float) = androidx.compose.material3.Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = (28 * scale).sp,
        lineHeight = (37 * scale).sp,
        letterSpacing = 0.3.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = (22 * scale).sp,
        lineHeight = (29 * scale).sp,
        letterSpacing = 0.3.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = (18 * scale).sp,
        lineHeight = (25 * scale).sp,
        letterSpacing = 0.3.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = (20 * scale).sp,
        lineHeight = (27 * scale).sp,
        letterSpacing = 0.3.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = (16 * scale).sp,
        lineHeight = (22 * scale).sp,
        letterSpacing = 0.3.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = (16 * scale).sp,
        lineHeight = (28 * scale).sp,
        letterSpacing = 0.4.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = (14 * scale).sp,
        lineHeight = (23 * scale).sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = (12 * scale).sp,
        lineHeight = (16 * scale).sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = (12 * scale).sp,
        lineHeight = (15 * scale).sp,
        letterSpacing = 0.5.sp
    )
)

fun getFontScale(context: android.content.Context): Float {
    val prefs = context.getSharedPreferences("diary_prefs", android.content.Context.MODE_PRIVATE)
    return when (prefs.getString("editor_font_size", "small")) {
        "tiny" -> 0.85f
        "small" -> 1.0f
        "medium" -> 1.15f
        "large" -> 1.3f
        "extra_large" -> 1.5f
        else -> 1.0f
    }
}

// Extended colors for semantic & gradient access via CompositionLocal
data class ExtendedColors(
    val success: Color,
    val warning: Color,
    val info: Color,
    val gradientStart: Color,
    val gradientEnd: Color
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        success = SuccessColor,
        warning = WarningColor,
        info = InfoColor,
        gradientStart = FogBlueLightAccent,
        gradientEnd = FogBlueLightAccent2
    )
}

// ---- Light mode color scheme ----

private val PureLightColorScheme = lightColorScheme(
    primary = PureLightPrimary,
    onPrimary = Color.White,
    primaryContainer = PureLightPrimaryVariant,
    onPrimaryContainer = Color.White,
    secondary = PureLightSecondary,
    onSecondary = Color.White,
    tertiary = FogBlueLightAccent3,
    onTertiary = Color.White,
    error = ErrorColor,
    background = PureLightBackground,
    onBackground = PureLightOnBackground,
    surface = PureLightSurface,
    onSurface = PureLightOnSurface,
    surfaceVariant = PureLightCardBackground,
    onSurfaceVariant = PureLightOnSurfaceVariant,
)

// ---- Dark mode color scheme ----

private val PureDarkColorScheme = darkColorScheme(
    primary = PureDarkPrimary,
    onPrimary = Color.White,
    primaryContainer = PureDarkPrimaryVariant,
    onPrimaryContainer = Color.White,
    secondary = PureDarkSecondary,
    onSecondary = Color.White,
    tertiary = FogBlueDarkAccent3,
    onTertiary = Color.White,
    error = ErrorColor,
    background = PureDarkBackground,
    onBackground = PureDarkOnBackground,
    surface = DarkSurface,
    onSurface = PureDarkOnSurface,
    surfaceVariant = PureDarkCardBackground,
    onSurfaceVariant = DarkTextSecondary,
)

// ---- Moss Green Light mode color scheme ----

private val MossGreenLightColorScheme = lightColorScheme(
    primary = MossGreenLightAccentStart,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF6B8E5E),
    onPrimaryContainer = Color.White,
    secondary = MossGreenLightAccentEnd,
    onSecondary = Color.White,
    tertiary = MossGreenLightAccent3,
    onTertiary = Color.White,
    error = ErrorColor,
    background = MossGreenLightBackgroundStart,
    onBackground = MossGreenLightTextPrimary,
    surface = MossGreenLightSurface,
    onSurface = MossGreenLightTextPrimary,
    surfaceVariant = MossGreenLightCardBackground,
    onSurfaceVariant = MossGreenLightTextSecondary,
)

// ---- Moss Green Dark mode color scheme ----

private val MossGreenDarkColorScheme = darkColorScheme(
    primary = MossGreenDarkAccentStart,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5A8E4E),
    onPrimaryContainer = Color.White,
    secondary = MossGreenDarkAccentEnd,
    onSecondary = Color.White,
    tertiary = MossGreenDarkAccent3,
    onTertiary = Color.White,
    error = ErrorColor,
    background = MossGreenDarkBackgroundStart,
    onBackground = MossGreenDarkTextPrimary,
    surface = MossGreenDarkSurface,
    onSurface = MossGreenDarkTextPrimary,
    surfaceVariant = MossGreenDarkCardBackground,
    onSurfaceVariant = MossGreenDarkTextSecondary,
)

// ---- Extended color presets per theme variant ----

private val MossGreenLightExtendedColors = ExtendedColors(
    success = SuccessColor,
    warning = WarningColor,
    info = InfoColor,
    gradientStart = MossGreenLightAccentStart,
    gradientEnd = MossGreenLightAccentEnd
)

private val MossGreenDarkExtendedColors = ExtendedColors(
    success = SuccessColor,
    warning = WarningColor,
    info = InfoColor,
    gradientStart = MossGreenDarkAccentStart,
    gradientEnd = MossGreenDarkAccentEnd
)

private val PureLightExtendedColors = ExtendedColors(
    success = SuccessColor,
    warning = WarningColor,
    info = InfoColor,
    gradientStart = PureLightPrimary,
    gradientEnd = PureLightSecondary
)

private val PureDarkExtendedColors = ExtendedColors(
    success = SuccessColor,
    warning = WarningColor,
    info = InfoColor,
    gradientStart = PureDarkPrimary,
    gradientEnd = PureDarkSecondary
)

@Composable
fun DiaryAppTheme(
    themeMode: ThemeMode = ThemeMode.PURE_LIGHT,
    content: @Composable () -> Unit
) {
    val isDark = themeMode.isDark()
    val colorScheme = when (themeMode) {
        ThemeMode.PURE_LIGHT -> PureLightColorScheme
        ThemeMode.PURE_DARK -> PureDarkColorScheme
        ThemeMode.MOSS_GREEN_LIGHT -> MossGreenLightColorScheme
        ThemeMode.MOSS_GREEN_DARK -> MossGreenDarkColorScheme
    }

    val extendedColors = when (themeMode) {
        ThemeMode.PURE_LIGHT -> PureLightExtendedColors
        ThemeMode.PURE_DARK -> PureDarkExtendedColors
        ThemeMode.MOSS_GREEN_LIGHT -> MossGreenLightExtendedColors
        ThemeMode.MOSS_GREEN_DARK -> MossGreenDarkExtendedColors
    }

    val context = LocalContext.current
    val fontScale = getFontScale(context)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalThemeMode provides themeMode,
        LocalExtendedColors provides extendedColors,
        LocalFontScale provides fontScale
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = scaledTypography(fontScale)
        ) {
            content()
        }
    }
}
