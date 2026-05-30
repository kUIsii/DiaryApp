package com.diary.app.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
//  Primary palette — Blue theme
//  Clean, modern, calming
// ============================================================

val PrimaryBlue = Color(0xFF4A90D9)            // Main brand blue
val PrimaryBlueVariant = Color(0xFF6BA8E8)     // Lighter blue variant
val SecondaryBlue = Color(0xFF70B8D8)          // Sky blue secondary
val AccentBlue = Color(0xFF5BA0E0)             // Accent blue

// ============================================================
//  Light mode — White-blue gradient
//  Clean white with blue center fading to white edges
// ============================================================

val LightBackgroundStart = Color(0xFFF8FBFF)   // Near-white with blue tint
val LightBackgroundMid = Color(0xFFEFF5FF)     // Light blue center
val LightBackgroundEnd = Color(0xFFF8FBFF)     // Back to white
val LightSurface = Color(0xCCFFFFFF)           // rgba(255,255,255,0.8)
val LightSurfaceBorder = Color(0x99FFFFFF)     // rgba(255,255,255,0.6)
val LightTextPrimary = Color(0xFF1A2030)       // Deep blue-gray
val LightTextSecondary = Color(0xFF4A5568)     // Medium gray
val LightTextTertiary = Color(0xFF8896A8)      // Muted gray
val LightAccentStart = PrimaryBlue             // #4A90D9
val LightAccentEnd = SecondaryBlue             // #70B8D8
val LightCardBackground = Color(0xBBFFFFFF)    // rgba(255,255,255,0.73)
val LightCardBorder = Color(0x80FFFFFF)        // rgba(255,255,255,0.5)

// ============================================================
//  Dark mode — Deep navy blue
//  Sophisticated dark with blue undertones
// ============================================================

val DarkBackgroundStart = Color(0xFF0A1520)    // Deep navy
val DarkBackgroundMid = Color(0xFF0E1A28)      // Slightly lighter
val DarkBackgroundEnd = Color(0xFF0A1520)      // Cohesive depth
val DarkSurface = Color(0x14FFFFFF)            // rgba(255,255,255,0.08)
val DarkSurfaceBorder = Color(0x1AFFFFFF)      // rgba(255,255,255,0.10)
val DarkTextPrimary = Color(0xF0E8F0F8)        // Near-white with cool cast
val DarkTextSecondary = Color(0x99B8C8D8)      // Muted blue-gray
val DarkTextTertiary = Color(0x668898A8)       // Subtle, still readable
val DarkAccentStart = PrimaryBlueVariant       // #6BA8E8 — brighter on dark
val DarkAccentEnd = SecondaryBlue              // #70B8D8
val DarkCardBackground = Color(0x11FFFFFF)     // rgba(255,255,255,0.07)
val DarkCardBorder = Color(0x1AFFFFFF)         // rgba(255,255,255,0.10)

// ============================================================
//  Semantic colors — Softer, less alarming
// ============================================================

val SuccessColor = Color(0xFF6ABF8A)          // desaturated sage green
val WarningColor = Color(0xFFE8A84C)          // warm amber, not harsh yellow
val ErrorColor = Color(0xFFE07070)            // soft coral-red
val InfoColor = Color(0xFF7BA7E0)             // muted sky blue

// ============================================================
//  Mood colors — Nuanced, desaturated gradient pairs
//  6 levels: 1=depressed ... 6=excited
// ============================================================

val MoodDepressed = Color(0xFFCF7B7B) to Color(0xFFB86060)   // muted rose-red
val MoodDown = Color(0xFFD4A06A) to Color(0xFFC08A50)        // warm sand
val MoodCalm = Color(0xFF7BA7C9) to Color(0xFF6090B5)        // soft steel blue
val MoodHappy = Color(0xFF7BC9A0) to Color(0xFF5FB88A)       // muted mint
val MoodCheerful = Color(0xFF6BC0B0) to Color(0xFF50A898)    // desaturated teal
val MoodExcited = Color(0xFFA88BC9) to Color(0xFF9070B8)     // soft violet
val MoodSad = Color(0xFF818CF8) to Color(0xFFA78BFA)         // indigo-violet
val MoodAnxious = Color(0xFFD99AB8) to Color(0xFFC87EA0)     // muted mauve
val MoodTired = Color(0xFFA8A0C0) to Color(0xFFD0C8E0)       // lavender-gray
val MoodGrateful = Color(0xFF7BC9A0) to Color(0xFF6BC0B0)    // warm teal blend

// ============================================================
//  Weather colors — Natural, muted
// ============================================================

val WeatherSunny = Color(0xFFE8B84C)          // warm amber, not neon yellow
val WeatherCloudy = Color(0xFF8A9BB0)         // blue-gray slate
val WeatherOvercast = Color(0xFF758898)       // deeper slate
val WeatherRainy = Color(0xFF70A8D0)          // muted ocean blue
val WeatherStormy = Color(0xFF9878B8)         // soft purple-gray
val WeatherWindy = Color(0xFF78B8B0)          // desaturated teal

// ============================================================
//  Pure Light mode — White-blue theme
// ============================================================

val PureLightBackground = Color(0xFFF8FBFF)   // Near-white with blue tint
val PureLightSurface = Color(0xFFF0F5FA)      // Light blue surface
val PureLightCardBackground = Color(0xFFE8F0F8)
val PureLightCardBorder = Color(0xFFD0E0F0)

// ============================================================
//  Pure Dark mode — Deep navy theme
// ============================================================

val PureDarkBackground = Color(0xFF0A1520)    // Deep navy
val PureDarkSurface = Color(0xFF121E2C)       // Subtle layer
val PureDarkCardBackground = Color(0xFF1A2838) // Card depth
val PureDarkCardBorder = Color(0xFF283848)    // Visible but subtle

// ============================================================
//  Pure Light theme — Blue palette
// ============================================================

val PureLightPrimary = Color(0xFF4A90D9)           // Blue
val PureLightPrimaryVariant = Color(0xFF3A7CC0)    // Deeper blue
val PureLightSecondary = Color(0xFF70B8D8)         // Sky blue
val PureLightOnBackground = Color(0xFF1A2030)      // Deep blue-gray
val PureLightOnSurface = Color(0xFF2A3548)         // Dark blue-gray
val PureLightOnSurfaceVariant = Color(0xFF607080)  // Muted gray

// ============================================================
//  Pure Dark theme — Cool blue palette
// ============================================================

val PureDarkPrimary = Color(0xFF70B8D8)            // Sky blue
val PureDarkPrimaryVariant = Color(0xFF5A9ABF)     // Deeper sky
val PureDarkSecondary = Color(0xFF88C8E0)          // Light sky
val PureDarkOnBackground = Color(0xFFD8E8F0)       // Light blue text
val PureDarkOnSurface = Color(0xFFB8D0E0)          // Medium blue text
