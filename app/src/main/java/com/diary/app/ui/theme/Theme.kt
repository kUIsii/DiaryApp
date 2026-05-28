package com.diary.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val GradientLightColorScheme = lightColorScheme(
    primary = LightAccentStart,
    onPrimary = Color.White,
    primaryContainer = LightAccentStart,
    onPrimaryContainer = Color.White,
    secondary = LightAccentEnd,
    onSecondary = Color.White,
    background = LightBackgroundStart,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightCardBackground,
    onSurfaceVariant = LightTextSecondary,
)

private val GradientDarkColorScheme = darkColorScheme(
    primary = DarkAccentStart,
    onPrimary = Color.White,
    primaryContainer = DarkAccentStart,
    onPrimaryContainer = Color.White,
    secondary = DarkAccentEnd,
    onSecondary = Color.White,
    background = DarkBackgroundStart,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkCardBackground,
    onSurfaceVariant = DarkTextSecondary,
)

private val PureLightColorScheme = lightColorScheme(
    primary = LightAccentStart,
    onPrimary = Color.White,
    primaryContainer = LightAccentStart,
    onPrimaryContainer = Color.White,
    secondary = LightAccentEnd,
    onSecondary = Color.White,
    background = PureLightBackground,
    onBackground = Color(0xFF1A1A2E),
    surface = PureLightSurface,
    onSurface = Color(0xFF1A1A2E),
    surfaceVariant = PureLightCardBackground,
    onSurfaceVariant = Color(0xFF666680),
)

private val PureDarkColorScheme = darkColorScheme(
    primary = DarkAccentStart,
    onPrimary = Color.White,
    primaryContainer = DarkAccentStart,
    onPrimaryContainer = Color.White,
    secondary = DarkAccentEnd,
    onSecondary = Color.White,
    background = PureDarkBackground,
    onBackground = Color(0xFFE6E6E6),
    surface = PureDarkSurface,
    onSurface = Color(0xFFE6E6E6),
    surfaceVariant = PureDarkCardBackground,
    onSurfaceVariant = Color(0xFF888888),
)

@Composable
fun DiaryAppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = themeMode.isDark()
    val colorScheme = when (themeMode) {
        ThemeMode.PURE_LIGHT -> PureLightColorScheme
        ThemeMode.PURE_DARK -> PureDarkColorScheme
        ThemeMode.GRADIENT -> if (isDark) GradientDarkColorScheme else GradientLightColorScheme
        ThemeMode.SYSTEM -> if (isDark) GradientDarkColorScheme else GradientLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    CompositionLocalProvider(LocalThemeMode provides themeMode) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
