package com.diary.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
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
    val sizeKey = com.diary.app.ui.editor.appFontSizeKey(prefs)
    return when (sizeKey) {
        "tiny" -> 0.85f
        "smaller" -> 0.92f
        "small" -> 1.0f
        "medium_small" -> 1.07f
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

private val OceanLightColorScheme = lightColorScheme(
    primary = OceanLightAccentStart,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9CEBEF),
    onPrimaryContainer = Color(0xFF114B53),
    secondary = OceanLightAccentEnd,
    onSecondary = Color.White,
    tertiary = OceanLightAccent3,
    onTertiary = Color.White,
    error = ErrorColor,
    background = OceanLightBackgroundStart,
    onBackground = OceanLightTextPrimary,
    surface = OceanLightSurface,
    onSurface = OceanLightTextPrimary,
    surfaceVariant = OceanLightCardBackground,
    onSurfaceVariant = OceanLightTextSecondary,
)

private val OceanDarkColorScheme = darkColorScheme(
    primary = OceanDarkAccentStart,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF235D64),
    onPrimaryContainer = Color.White,
    secondary = OceanDarkAccentEnd,
    onSecondary = Color.White,
    tertiary = OceanDarkAccent3,
    onTertiary = Color.White,
    error = ErrorColor,
    background = OceanDarkBackgroundStart,
    onBackground = OceanDarkTextPrimary,
    surface = OceanDarkSurface,
    onSurface = OceanDarkTextPrimary,
    surfaceVariant = OceanDarkCardBackground,
    onSurfaceVariant = OceanDarkTextSecondary,
)

private val PetalLightColorScheme = lightColorScheme(
    primary = PetalLightAccentStart,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF1B5AE),
    onPrimaryContainer = Color(0xFF5A2621),
    secondary = PetalLightAccentEnd,
    onSecondary = Color.White,
    tertiary = PetalLightAccent3,
    onTertiary = Color.White,
    error = ErrorColor,
    background = PetalLightBackgroundStart,
    onBackground = PetalLightTextPrimary,
    surface = PetalLightSurface,
    onSurface = PetalLightTextPrimary,
    surfaceVariant = PetalLightCardBackground,
    onSurfaceVariant = PetalLightTextSecondary,
)

private val PetalDarkColorScheme = darkColorScheme(
    primary = PetalDarkAccentStart,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF7D4A47),
    onPrimaryContainer = Color.White,
    secondary = PetalDarkAccentEnd,
    onSecondary = Color.White,
    tertiary = PetalDarkAccent3,
    onTertiary = Color.White,
    error = ErrorColor,
    background = PetalDarkBackgroundStart,
    onBackground = PetalDarkTextPrimary,
    surface = PetalDarkSurface,
    onSurface = PetalDarkTextPrimary,
    surfaceVariant = PetalDarkCardBackground,
    onSurfaceVariant = PetalDarkTextSecondary,
)

private val SandLightColorScheme = lightColorScheme(
    primary = SandLightAccentStart,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF0C788),
    onPrimaryContainer = Color(0xFF553B11),
    secondary = SandLightAccentEnd,
    onSecondary = Color.White,
    tertiary = SandLightAccent3,
    onTertiary = Color.White,
    error = ErrorColor,
    background = SandLightBackgroundStart,
    onBackground = SandLightTextPrimary,
    surface = SandLightSurface,
    onSurface = SandLightTextPrimary,
    surfaceVariant = SandLightCardBackground,
    onSurfaceVariant = SandLightTextSecondary,
)

private val SandDarkColorScheme = darkColorScheme(
    primary = SandDarkAccentStart,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF8C652A),
    onPrimaryContainer = Color.White,
    secondary = SandDarkAccentEnd,
    onSecondary = Color.White,
    tertiary = SandDarkAccent3,
    onTertiary = Color.White,
    error = ErrorColor,
    background = SandDarkBackgroundStart,
    onBackground = SandDarkTextPrimary,
    surface = SandDarkSurface,
    onSurface = SandDarkTextPrimary,
    surfaceVariant = SandDarkCardBackground,
    onSurfaceVariant = SandDarkTextSecondary,
)

private val OceanLightExtendedColors = ExtendedColors(
    success = SuccessColor,
    warning = WarningColor,
    info = InfoColor,
    gradientStart = OceanLightAccentStart,
    gradientEnd = OceanLightAccentEnd
)

private val OceanDarkExtendedColors = ExtendedColors(
    success = SuccessColor,
    warning = WarningColor,
    info = InfoColor,
    gradientStart = OceanDarkAccentStart,
    gradientEnd = OceanDarkAccentEnd
)

private val PetalLightExtendedColors = ExtendedColors(
    success = SuccessColor,
    warning = WarningColor,
    info = InfoColor,
    gradientStart = PetalLightAccentStart,
    gradientEnd = PetalLightAccentEnd
)

private val PetalDarkExtendedColors = ExtendedColors(
    success = SuccessColor,
    warning = WarningColor,
    info = InfoColor,
    gradientStart = PetalDarkAccentStart,
    gradientEnd = PetalDarkAccentEnd
)

private val SandLightExtendedColors = ExtendedColors(
    success = SuccessColor,
    warning = WarningColor,
    info = InfoColor,
    gradientStart = SandLightAccentStart,
    gradientEnd = SandLightAccentEnd
)

private val SandDarkExtendedColors = ExtendedColors(
    success = SuccessColor,
    warning = WarningColor,
    info = InfoColor,
    gradientStart = SandDarkAccentStart,
    gradientEnd = SandDarkAccentEnd
)

private val ClayLightColorScheme = lightColorScheme(
    primary = ClayLightAccentStart,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6B2A3),
    onPrimaryContainer = Color(0xFF5E3225),
    secondary = ClayLightAccentEnd,
    onSecondary = Color.White,
    tertiary = ClayLightAccent3,
    onTertiary = Color.White,
    error = ErrorColor,
    background = ClayLightBackgroundStart,
    onBackground = ClayLightTextPrimary,
    surface = ClayLightSurface,
    onSurface = ClayLightTextPrimary,
    surfaceVariant = ClayLightCardBackground,
    onSurfaceVariant = ClayLightTextSecondary,
)

private val ClayDarkColorScheme = darkColorScheme(
    primary = ClayDarkAccentStart,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF8A5A4A),
    onPrimaryContainer = Color.White,
    secondary = ClayDarkAccentEnd,
    onSecondary = Color.White,
    tertiary = ClayDarkAccent3,
    onTertiary = Color.White,
    error = ErrorColor,
    background = ClayDarkBackgroundStart,
    onBackground = ClayDarkTextPrimary,
    surface = ClayDarkSurface,
    onSurface = ClayDarkTextPrimary,
    surfaceVariant = ClayDarkCardBackground,
    onSurfaceVariant = ClayDarkTextSecondary,
)

private val InkLightColorScheme = lightColorScheme(
    primary = InkLightAccentStart,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8C7EA),
    onPrimaryContainer = Color(0xFF263858),
    secondary = InkLightAccentEnd,
    onSecondary = Color.White,
    tertiary = InkLightAccent3,
    onTertiary = Color.White,
    error = ErrorColor,
    background = InkLightBackgroundStart,
    onBackground = InkLightTextPrimary,
    surface = InkLightSurface,
    onSurface = InkLightTextPrimary,
    surfaceVariant = InkLightCardBackground,
    onSurfaceVariant = InkLightTextSecondary,
)

private val InkDarkColorScheme = darkColorScheme(
    primary = InkDarkAccentStart,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF344B78),
    onPrimaryContainer = Color.White,
    secondary = InkDarkAccentEnd,
    onSecondary = Color.White,
    tertiary = InkDarkAccent3,
    onTertiary = Color.White,
    error = ErrorColor,
    background = InkDarkBackgroundStart,
    onBackground = InkDarkTextPrimary,
    surface = InkDarkSurface,
    onSurface = InkDarkTextPrimary,
    surfaceVariant = InkDarkCardBackground,
    onSurfaceVariant = InkDarkTextSecondary,
)

private val ClayLightExtendedColors = ExtendedColors(
    success = SuccessColor,
    warning = WarningColor,
    info = InfoColor,
    gradientStart = ClayLightAccentStart,
    gradientEnd = ClayLightAccentEnd
)

private val ClayDarkExtendedColors = ExtendedColors(
    success = SuccessColor,
    warning = WarningColor,
    info = InfoColor,
    gradientStart = ClayDarkAccentStart,
    gradientEnd = ClayDarkAccentEnd
)

private val InkLightExtendedColors = ExtendedColors(
    success = SuccessColor,
    warning = WarningColor,
    info = InfoColor,
    gradientStart = InkLightAccentStart,
    gradientEnd = InkLightAccentEnd
)

private val InkDarkExtendedColors = ExtendedColors(
    success = SuccessColor,
    warning = WarningColor,
    info = InfoColor,
    gradientStart = InkDarkAccentStart,
    gradientEnd = InkDarkAccentEnd
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
    fontScaleOverride: Float? = null,
    content: @Composable () -> Unit
) {
    val isDark = themeMode.isDark()
    val colorScheme = when (themeMode) {
        ThemeMode.PURE_LIGHT -> PureLightColorScheme
        ThemeMode.PURE_DARK -> PureDarkColorScheme
        ThemeMode.MOSS_GREEN_LIGHT -> MossGreenLightColorScheme
        ThemeMode.MOSS_GREEN_DARK -> MossGreenDarkColorScheme
        ThemeMode.OCEAN_LIGHT -> OceanLightColorScheme
        ThemeMode.OCEAN_DARK -> OceanDarkColorScheme
        ThemeMode.PETAL_LIGHT -> PetalLightColorScheme
        ThemeMode.PETAL_DARK -> PetalDarkColorScheme
        ThemeMode.SAND_LIGHT -> SandLightColorScheme
        ThemeMode.SAND_DARK -> SandDarkColorScheme
        ThemeMode.CLAY_LIGHT -> ClayLightColorScheme
        ThemeMode.CLAY_DARK -> ClayDarkColorScheme
        ThemeMode.INK_LIGHT -> InkLightColorScheme
        ThemeMode.INK_DARK -> InkDarkColorScheme
    }

    val extendedColors = when (themeMode) {
        ThemeMode.PURE_LIGHT -> PureLightExtendedColors
        ThemeMode.PURE_DARK -> PureDarkExtendedColors
        ThemeMode.MOSS_GREEN_LIGHT -> MossGreenLightExtendedColors
        ThemeMode.MOSS_GREEN_DARK -> MossGreenDarkExtendedColors
        ThemeMode.OCEAN_LIGHT -> OceanLightExtendedColors
        ThemeMode.OCEAN_DARK -> OceanDarkExtendedColors
        ThemeMode.PETAL_LIGHT -> PetalLightExtendedColors
        ThemeMode.PETAL_DARK -> PetalDarkExtendedColors
        ThemeMode.SAND_LIGHT -> SandLightExtendedColors
        ThemeMode.SAND_DARK -> SandDarkExtendedColors
        ThemeMode.CLAY_LIGHT -> ClayLightExtendedColors
        ThemeMode.CLAY_DARK -> ClayDarkExtendedColors
        ThemeMode.INK_LIGHT -> InkLightExtendedColors
        ThemeMode.INK_DARK -> InkDarkExtendedColors
    }

    val context = LocalContext.current
    val fontScale = fontScaleOverride ?: getFontScale(context)

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

    val typography = remember(fontScale) { scaledTypography(fontScale) }

    val baseDensity = LocalDensity.current
    val scaledDensity = remember(fontScale, baseDensity) {
        androidx.compose.ui.unit.Density(baseDensity.density, baseDensity.fontScale * fontScale)
    }

    CompositionLocalProvider(
        LocalThemeMode provides themeMode,
        LocalExtendedColors provides extendedColors,
        LocalFontScale provides fontScale,
        LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography
        ) {
            content()
        }
    }
}
