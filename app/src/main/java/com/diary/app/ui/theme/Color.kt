package com.diary.app.ui.theme

import androidx.compose.ui.graphics.Color

// Light mode - Morning Glow (softer, reduced saturation)
val LightBackgroundStart = Color(0xFFF0F1F5)
val LightBackgroundMid = Color(0xFFEAECF3)
val LightBackgroundEnd = Color(0xFFF2F0F5)
val LightSurface = Color(0x99FFFFFF) // rgba(255,255,255,0.6)
val LightSurfaceBorder = Color(0x80FFFFFF) // rgba(255,255,255,0.5)
val LightTextPrimary = Color(0xFF1A1A2E)
val LightTextSecondary = Color(0xFF4A4A68)
val LightTextTertiary = Color(0xFF9999AA)
val LightAccentStart = Color(0xFF667EEA)
val LightAccentEnd = Color(0xFF764BA2)
val LightCardBackground = Color(0x99FFFFFF) // rgba(255,255,255,0.6)
val LightCardBorder = Color(0x80FFFFFF)

// Dark mode - Deep Galaxy (blue-tinted, modern)
val DarkBackgroundStart = Color(0xFF080810)
val DarkBackgroundMid = Color(0xFF0C0C1E)
val DarkBackgroundEnd = Color(0xFF10102C)
val DarkSurface = Color(0x0FFFFFFF) // rgba(255,255,255,0.06)
val DarkSurfaceBorder = Color(0x14FFFFFF) // rgba(255,255,255,0.08)
val DarkTextPrimary = Color(0xE6FFFFFF) // rgba(255,255,255,0.9)
val DarkTextSecondary = Color(0x80FFFFFF) // rgba(255,255,255,0.5)
val DarkTextTertiary = Color(0x66FFFFFF) // rgba(255,255,255,0.4)
val DarkAccentStart = Color(0xFF667EEA)
val DarkAccentEnd = Color(0xFF764BA2)
val DarkCardBackground = Color(0x0FFFFFFF)
val DarkCardBorder = Color(0x14FFFFFF)

// Semantic colors
val SuccessColor = Color(0xFF2ECC71)
val WarningColor = Color(0xFFF39C12)
val ErrorColor = Color(0xFFE74C3C)
val InfoColor = Color(0xFF3498DB)

// Mood colors (gradient pairs)
val MoodDepressed = Color(0xFFE74C3C) to Color(0xFFC0392B)
val MoodDown = Color(0xFFE67E22) to Color(0xFFD35400)
val MoodCalm = Color(0xFF3498DB) to Color(0xFF2980B9)
val MoodHappy = Color(0xFF2ECC71) to Color(0xFF27AE60)
val MoodCheerful = Color(0xFF1ABC9C) to Color(0xFF16A085)
val MoodExcited = Color(0xFF9B59B6) to Color(0xFF8E44AD)
val MoodSad = Color(0xFF667EEA) to Color(0xFF764BA2)
val MoodAnxious = Color(0xFFF093FB) to Color(0xFFF5576C)
val MoodTired = Color(0xFFA18CD1) to Color(0xFFDBC2EF)
val MoodGrateful = Color(0xFF667EEA) to Color(0xFF764BA2)

// Weather colors
val WeatherSunny = Color(0xFFFFCA28)
val WeatherCloudy = Color(0xFF90A4AE)
val WeatherOvercast = Color(0xFF78909C)
val WeatherRainy = Color(0xFF64B5F6)
val WeatherStormy = Color(0xFFBA68C8)
val WeatherWindy = Color(0xFF80CBC4)

// Gradient color collections
val GradientPurple = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
val GradientSunset = listOf(Color(0xFFFF9A9E), Color(0xFFFECFEF))
val GradientOcean = listOf(Color(0xFF43E97B), Color(0xFF38F9D7))
val GradientBerry = listOf(Color(0xFF0D0D0D), Color(0xFF1A1A3E))
val GradientPeach = listOf(Color(0xFFFF9A9E), Color(0xFFFAD0C4))

// Pure Light mode
val PureLightBackground = Color(0xFFFFFFFF)
val PureLightSurface = Color(0xFFF5F5F5)
val PureLightCardBackground = Color(0xFFF0F0F0)
val PureLightCardBorder = Color(0xFFE0E0E0)

// Pure Dark mode
val PureDarkBackground = Color(0xFF000000)
val PureDarkSurface = Color(0xFF111111)
val PureDarkCardBackground = Color(0xFF1A1A1A)
val PureDarkCardBorder = Color(0xFF2A2A2A)

// Warm Rose theme - light variant
val WarmRoseBackground = Color(0xFFFDF5F3)
val WarmRoseSurface = Color(0xFFFFF0EB)
val WarmRoseSurfaceVariant = Color(0xFFF5E6E0)
val WarmRosePrimary = Color(0xFFBF7B6B)
val WarmRosePrimaryVariant = Color(0xFFA66353)
val WarmRoseSecondary = Color(0xFFC49B8A)
val WarmRoseOnBackground = Color(0xFF3D2B26)
val WarmRoseOnSurface = Color(0xFF4A3530)
val WarmRoseOnSurfaceVariant = Color(0xFF8A7068)

// Warm Rose theme - dark variant
val WarmRoseDarkBackground = Color(0xFF1A1312)
val WarmRoseDarkSurface = Color(0xFF2A1F1D)
val WarmRoseDarkSurfaceVariant = Color(0xFF3A2D2A)
val WarmRoseDarkPrimary = Color(0xFFD4978A)
val WarmRoseDarkOnBackground = Color(0xFFF0E0DC)
