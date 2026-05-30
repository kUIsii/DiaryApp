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
        gradientStart = LightAccentStart,
        gradientEnd = LightAccentEnd
    )
}

// ---- Light mode color schemes ----

private val GradientLightColorScheme = lightColorScheme(
    primary = LightAccentStart,
    onPrimary = Color.White,
    primaryContainer = LightAccentStart,
    onPrimaryContainer = Color.White,
    secondary = LightAccentEnd,
    onSecondary = Color.White,
    error = ErrorColor,
    background = LightBackgroundStart,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightCardBackground,
    onSurfaceVariant = LightTextSecondary,
)

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

// ---- Dark mode color schemes (3-color gradient background, improved contrast) ----

private val GradientDarkColorScheme = darkColorScheme(
    primary = DarkAccentStart,
    onPrimary = Color.White,
    primaryContainer = DarkAccentStart,
    onPrimaryContainer = Color.White,
    secondary = DarkAccentEnd,
    onSecondary = Color.White,
    error = ErrorColor,
    background = DarkBackgroundStart,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkCardBackground,
    onSurfaceVariant = DarkTextSecondary,
)

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

// ---- Warm Rose color schemes ----

private val WarmRoseLightColorScheme = lightColorScheme(
    primary = WarmRosePrimary,
    onPrimary = Color.White,
    primaryContainer = WarmRosePrimaryVariant,
    onPrimaryContainer = Color.White,
    secondary = WarmRoseSecondary,
    onSecondary = Color.White,
    error = ErrorColor,
    background = WarmRoseBackground,
    onBackground = WarmRoseOnBackground,
    surface = WarmRoseSurface,
    onSurface = WarmRoseOnSurface,
    surfaceVariant = WarmRoseSurfaceVariant,
    onSurfaceVariant = WarmRoseOnSurfaceVariant,
)

private val WarmRoseDarkColorScheme = darkColorScheme(
    primary = WarmRoseDarkPrimary,
    onPrimary = Color.White,
    primaryContainer = WarmRoseDarkPrimary,
    onPrimaryContainer = Color.White,
    secondary = WarmRoseSecondary,
    onSecondary = Color.White,
    error = ErrorColor,
    background = WarmRoseDarkBackground,
    onBackground = WarmRoseDarkOnBackground,
    surface = WarmRoseDarkSurface,
    onSurface = WarmRoseDarkOnBackground,
    surfaceVariant = WarmRoseDarkSurfaceVariant,
    onSurfaceVariant = Color(0xCCD8B8B0), // muted warm rose text ~0.8 alpha
)

// ---- Ocean Blue color schemes ----

private val OceanBlueLightColorScheme = lightColorScheme(
    primary = OceanBluePrimary,
    onPrimary = Color.White,
    primaryContainer = OceanBluePrimaryVariant,
    onPrimaryContainer = Color.White,
    secondary = OceanBlueSecondary,
    onSecondary = Color.White,
    error = ErrorColor,
    background = OceanBlueBackground,
    onBackground = OceanBlueOnBackground,
    surface = OceanBlueSurface,
    onSurface = OceanBlueOnSurface,
    surfaceVariant = OceanBlueSurfaceVariant,
    onSurfaceVariant = OceanBlueOnSurfaceVariant,
)

private val OceanBlueDarkColorScheme = darkColorScheme(
    primary = OceanBlueDarkPrimary,
    onPrimary = Color.White,
    primaryContainer = OceanBlueDarkPrimary,
    onPrimaryContainer = Color.White,
    secondary = OceanBlueSecondary,
    onSecondary = Color.White,
    error = ErrorColor,
    background = OceanBlueDarkBackground,
    onBackground = OceanBlueDarkOnBackground,
    surface = OceanBlueDarkSurface,
    onSurface = OceanBlueDarkOnBackground,
    surfaceVariant = OceanBlueDarkSurfaceVariant,
    onSurfaceVariant = Color(0xCCC0D0E0), // muted blue text ~0.8 alpha
)

// ---- Extended color presets per theme variant ----

private val GradientLightExtendedColors = ExtendedColors(
    success = SuccessColor,
    warning = WarningColor,
    info = InfoColor,
    gradientStart = LightAccentStart,
    gradientEnd = LightAccentEnd
)

private val GradientDarkExtendedColors = ExtendedColors(
    success = SuccessColor,
    warning = WarningColor,
    info = InfoColor,
    gradientStart = DarkAccentStart,
    gradientEnd = DarkAccentEnd
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

private val WarmRoseLightExtendedColors = ExtendedColors(
    success = SuccessColor,
    warning = WarningColor,
    info = InfoColor,
    gradientStart = WarmRosePrimary,
    gradientEnd = WarmRoseSecondary
)

private val WarmRoseDarkExtendedColors = ExtendedColors(
    success = SuccessColor,
    warning = WarningColor,
    info = InfoColor,
    gradientStart = WarmRoseDarkPrimary,
    gradientEnd = WarmRoseSecondary
)

private val OceanBlueLightExtendedColors = ExtendedColors(
    success = SuccessColor,
    warning = WarningColor,
    info = InfoColor,
    gradientStart = OceanBluePrimary,
    gradientEnd = OceanBlueSecondary
)

private val OceanBlueDarkExtendedColors = ExtendedColors(
    success = SuccessColor,
    warning = WarningColor,
    info = InfoColor,
    gradientStart = OceanBlueDarkPrimary,
    gradientEnd = OceanBlueSecondary
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
        ThemeMode.WARM_ROSE -> if (isDark) WarmRoseDarkColorScheme else WarmRoseLightColorScheme
        ThemeMode.OCEAN_BLUE -> if (isDark) OceanBlueDarkColorScheme else OceanBlueLightColorScheme
    }

    val extendedColors = when (themeMode) {
        ThemeMode.PURE_LIGHT -> PureLightExtendedColors
        ThemeMode.PURE_DARK -> PureDarkExtendedColors
        ThemeMode.GRADIENT -> if (isDark) GradientDarkExtendedColors else GradientLightExtendedColors
        ThemeMode.SYSTEM -> if (isDark) GradientDarkExtendedColors else GradientLightExtendedColors
        ThemeMode.WARM_ROSE -> if (isDark) WarmRoseDarkExtendedColors else WarmRoseLightExtendedColors
        ThemeMode.OCEAN_BLUE -> if (isDark) OceanBlueDarkExtendedColors else OceanBlueLightExtendedColors
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
