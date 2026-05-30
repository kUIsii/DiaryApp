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
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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
        gradientStart = PrimaryBlue,
        gradientEnd = SecondaryBlue
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
    error = ErrorColor,
    background = PureDarkBackground,
    onBackground = PureDarkOnBackground,
    surface = DarkSurface,
    onSurface = PureDarkOnSurface,
    surfaceVariant = PureDarkCardBackground,
    onSurfaceVariant = DarkTextSecondary,
)

// ---- Extended color presets per theme variant ----

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
    }

    val extendedColors = when (themeMode) {
        ThemeMode.PURE_LIGHT -> PureLightExtendedColors
        ThemeMode.PURE_DARK -> PureDarkExtendedColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    CompositionLocalProvider(
        LocalThemeMode provides themeMode,
        LocalExtendedColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
