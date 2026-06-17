package com.diary.app.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
//  Theme A: Fog Blue-Gray — 雾蓝灰
//  Soft, calming blue-gray with warm undertones
// ============================================================

// Light mode
val FogBlueLightBg1 = Color(0xFFF5F7FA)
val FogBlueLightBg2 = Color(0xFFEFF3F8)
val FogBlueLightBg3 = Color(0xFFE8EDF4)
val FogBlueLightSurface = Color(0xFFFFFFFF)
val FogBlueLightTextPrimary = Color(0xFF2C3344)
val FogBlueLightTextSecondary = Color(0xFF5A6577)
val FogBlueLightTextTertiary = Color(0xFF8A96A8)
val FogBlueLightAccent = Color(0xFF6B8DB5)
val FogBlueLightAccent2 = Color(0xFFB5926B)
val FogBlueLightCardBg = Color(0xFFFFFFFF)
val FogBlueLightCardBorder = Color(0x2E8296B4)

// Tertiary accent
val FogBlueLightAccent3 = Color(0xFF9B8EBA)

// Dark mode
val FogBlueDarkBg1 = Color(0xFF0B0D12)
val FogBlueDarkBg2 = Color(0xFF0E1018)
val FogBlueDarkBg3 = Color(0xFF11131C)
val FogBlueDarkSurface = Color(0xFF161822)
val FogBlueDarkTextPrimary = Color(0xFFD8E0F0)
val FogBlueDarkTextSecondary = Color(0xFF9AA8C0)
val FogBlueDarkTextTertiary = Color(0xFF6A7890)
val FogBlueDarkAccent = Color(0xFF88B0D4)
val FogBlueDarkAccent2 = Color(0xFFD4A878)
val FogBlueDarkAccent3 = Color(0xFFA89AC8)
val FogBlueDarkCardBg = Color(0xFF181A26)
val FogBlueDarkCardBorder = Color(0x2D8296B4)

// ============================================================
//  Theme B: Moss Green — 苔藓绿
//  Warm paper-white with soft sage green undertones
// ============================================================

// Light mode
val MossGreenLightBg1 = Color(0xFFF6F7F4)
val MossGreenLightBg2 = Color(0xFFEFF2EB)
val MossGreenLightBg3 = Color(0xFFE5EAE0)
val MossGreenLightSurface = Color(0xFFFCFDFB)
val MossGreenLightTextPrimary = Color(0xFF2E3328)
val MossGreenLightTextSecondary = Color(0xFF5A6450)
val MossGreenLightTextTertiary = Color(0xFF8A9480)
val MossGreenLightAccent = Color(0xFF7BA06E)
val MossGreenLightAccent2 = Color(0xFFC4A06B)
val MossGreenLightCardBg = Color(0xFFFCFDFB)
val MossGreenLightCardBorder = Color(0x2E647A5A)

// Tertiary accent
val MossGreenLightAccent3 = Color(0xFFA088B0)

// Dark mode
val MossGreenDarkBg1 = Color(0xFF0B0D0A)
val MossGreenDarkBg2 = Color(0xFF0E110D)
val MossGreenDarkBg3 = Color(0xFF111410)
val MossGreenDarkSurface = Color(0xFF161A14)
val MossGreenDarkTextPrimary = Color(0xFFD8E4D0)
val MossGreenDarkTextSecondary = Color(0xFF9AAA90)
val MossGreenDarkTextTertiary = Color(0xFF6A7A60)
val MossGreenDarkAccent = Color(0xFF8BC07A)
val MossGreenDarkAccent2 = Color(0xFFD4B078)
val MossGreenDarkAccent3 = Color(0xFFB098C4)
val MossGreenDarkCardBg = Color(0xFF181C16)
val MossGreenDarkCardBorder = Color(0x24A0B890)

// ============================================================
//  Semantic colors
// ============================================================

val SuccessColor = Color(0xFF6ABF8A)
val WarningColor = Color(0xFFE8A84C)
val ErrorColor = Color(0xFFE07070)
val InfoColor = Color(0xFF7BA7E0)

// ============================================================
//  Heatmap colors (theme-aware)
// ============================================================

val HeatmapLevel0 = Color(0x00000000) // transparent, uses surfaceVariant
val HeatmapLevel1 = Color(0xFFC8E6C9)
val HeatmapLevel2 = Color(0xFF81C784)
val HeatmapLevel3 = Color(0xFF4CAF50)
val HeatmapLevel4 = Color(0xFF2E7D32)
val HeatmapLevel5 = Color(0xFF1B5E20)

val HeatmapDarkLevel1 = Color(0xFF1B3A1B)
val HeatmapDarkLevel2 = Color(0xFF2E5A2E)
val HeatmapDarkLevel3 = Color(0xFF3E7A3E)
val HeatmapDarkLevel4 = Color(0xFF559A55)
val HeatmapDarkLevel5 = Color(0xFF70B870)

// ============================================================
//  Mood colors
// ============================================================

val MoodDepressed = Color(0xFFCF7B7B) to Color(0xFFB86060)
val MoodDown = Color(0xFFD4A06A) to Color(0xFFC08A50)
val MoodCalm = Color(0xFF7BA7C9) to Color(0xFF6090B5)
val MoodHappy = Color(0xFF7BC9A0) to Color(0xFF5FB88A)
val MoodCheerful = Color(0xFF6BC0B0) to Color(0xFF50A898)
val MoodExcited = Color(0xFFA88BC9) to Color(0xFF9070B8)
val MoodSad = Color(0xFF818CF8) to Color(0xFFA78BFA)
val MoodAnxious = Color(0xFFD99AB8) to Color(0xFFC87EA0)
val MoodTired = Color(0xFFA8A0C0) to Color(0xFFD0C8E0)
val MoodGrateful = Color(0xFF7BC9A0) to Color(0xFF6BC0B0)

// ============================================================
//  Weather colors
// ============================================================

val WeatherSunny = Color(0xFFE8B84C)
val WeatherCloudy = Color(0xFF8A9BB0)
val WeatherOvercast = Color(0xFF758898)
val WeatherRainy = Color(0xFF70A8D0)
val WeatherStormy = Color(0xFF9878B8)
val WeatherWindy = Color(0xFF78B8B0)

// ============================================================
//  Legacy aliases (used by Theme.kt, GlassCard, GradientBackground)
// ============================================================

val PrimaryBlue = FogBlueLightAccent
val SecondaryBlue = FogBlueLightAccent2
val DarkAccentStart = FogBlueDarkAccent
val DarkAccentEnd = FogBlueDarkAccent2

val LightBackgroundStart = FogBlueLightBg1
val LightBackgroundMid = FogBlueLightBg2
val LightBackgroundEnd = FogBlueLightBg3

val DarkBackgroundStart = FogBlueDarkBg1
val DarkBackgroundMid = FogBlueDarkBg2
val DarkBackgroundEnd = FogBlueDarkBg3
val DarkSurface = FogBlueDarkSurface
val DarkTextSecondary = FogBlueDarkTextSecondary

val PureLightBackground = FogBlueLightBg1
val PureLightSurface = FogBlueLightSurface
val PureLightCardBackground = FogBlueLightCardBg
val PureLightCardBorder = FogBlueLightCardBorder

val PureDarkBackground = FogBlueDarkBg1
val PureDarkCardBackground = FogBlueDarkCardBg
val PureDarkCardBorder = FogBlueDarkCardBorder

val PureLightPrimary = FogBlueLightAccent
val PureLightPrimaryVariant = Color(0xFF5A7DA5)
val PureLightSecondary = Color(0xFF9C826B)
val PureLightOnBackground = FogBlueLightTextPrimary
val PureLightOnSurface = Color(0xFF2A3548)
val PureLightOnSurfaceVariant = Color(0xFF607080)

val PureDarkPrimary = FogBlueDarkAccent
val PureDarkPrimaryVariant = Color(0xFF6A98B8)
val PureDarkSecondary = Color(0xFFBA9478)
val PureDarkOnBackground = FogBlueDarkTextPrimary
val PureDarkOnSurface = Color(0xFFB8D0E0)

val MossGreenLightBackgroundStart = MossGreenLightBg1
val MossGreenLightBackgroundMid = MossGreenLightBg2
val MossGreenLightBackgroundEnd = MossGreenLightBg3
val MossGreenLightAccentStart = MossGreenLightAccent
val MossGreenLightAccentEnd = MossGreenLightAccent2
val MossGreenLightCardBackground = MossGreenLightCardBg

val MossGreenDarkBackgroundStart = MossGreenDarkBg1
val MossGreenDarkBackgroundMid = MossGreenDarkBg2
val MossGreenDarkBackgroundEnd = MossGreenDarkBg3
val MossGreenDarkAccentStart = MossGreenDarkAccent
val MossGreenDarkAccentEnd = MossGreenDarkAccent2
val MossGreenDarkCardBackground = MossGreenDarkCardBg
