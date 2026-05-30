package com.diary.app.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
//  Primary palette — Refined Indigo-to-Violet
//  Soft, modern, calming. Replaces the harsh purple system.
// ============================================================

val PrimaryIndigo = Color(0xFF6366F1)        // Indigo 500 — main brand
val PrimaryIndigoVariant = Color(0xFF818CF8)  // Indigo 400 — lighter variant
val SecondaryViolet = Color(0xFFA78BFA)       // Violet 400 — secondary accent
val AccentPink = Color(0xFFF472B6)            // Pink 400 — highlight only

// ============================================================
//  Light mode — Soft Dawn
//  Warm undertone, layered surfaces, gentle transparency
// ============================================================

val LightBackgroundStart = Color(0xFFF5F3FF)  // faint violet tint
val LightBackgroundMid = Color(0xFFF0EEFA)    // slightly deeper
val LightBackgroundEnd = Color(0xFFF8F6FF)    // lift at bottom
val LightSurface = Color(0xCCFFFFFF)          // rgba(255,255,255,0.8)
val LightSurfaceBorder = Color(0x99FFFFFF)    // rgba(255,255,255,0.6)
val LightTextPrimary = Color(0xFF1C1930)      // deep warm gray, high contrast
val LightTextSecondary = Color(0xFF55526B)    // medium contrast
val LightTextTertiary = Color(0xFF9895AD)     // still readable
val LightAccentStart = PrimaryIndigo          // #6366F1
val LightAccentEnd = SecondaryViolet          // #A78BFA
val LightCardBackground = Color(0xBBFFFFFF)   // rgba(255,255,255,0.73)
val LightCardBorder = Color(0x80FFFFFF)       // rgba(255,255,255,0.5)

// ============================================================
//  Dark mode — Deep Indigo Night
//  Cool blue-indigo undertone, sophisticated layering
// ============================================================

val DarkBackgroundStart = Color(0xFF0B0A1A)   // near-black with indigo cast
val DarkBackgroundMid = Color(0xFF100F24)     // slightly lighter
val DarkBackgroundEnd = Color(0xFF0E0D20)     // cohesive depth
val DarkSurface = Color(0x14FFFFFF)           // rgba(255,255,255,0.08)
val DarkSurfaceBorder = Color(0x1AFFFFFF)     // rgba(255,255,255,0.10)
val DarkTextPrimary = Color(0xF0F0F2FF)       // near-white with cool cast
val DarkTextSecondary = Color(0x99CBC8E0)     // muted lavender-gray
val DarkTextTertiary = Color(0x669E9BB8)      // subtle, still readable
val DarkAccentStart = PrimaryIndigoVariant    // #818CF8 — brighter on dark
val DarkAccentEnd = SecondaryViolet           // #A78BFA
val DarkCardBackground = Color(0x11FFFFFF)    // rgba(255,255,255,0.07)
val DarkCardBorder = Color(0x1AFFFFFF)        // rgba(255,255,255,0.10)

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
val MoodSad = Color(0xFF818CF8) to Color(0xFFA78BFA)         // indigo-violet, ties to primary
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
//  Gradient presets — Sophisticated, cohesive
// ============================================================

val GradientPurple = listOf(PrimaryIndigo, SecondaryViolet)                    // indigo -> violet
val GradientSunset = listOf(Color(0xFFF0908A), Color(0xFFF0C8A8))             // coral -> peach
val GradientOcean = listOf(Color(0xFF6AB8A8), Color(0xFF70A8D0))              // teal -> steel blue
val GradientBerry = listOf(Color(0xFF0E0D20), Color(0xFF1A1840))              // dark indigo depth
val GradientPeach = listOf(Color(0xFFF0A8A0), Color(0xFFF0D0C0))             // soft rose -> cream

// ============================================================
//  Pure Light mode — Clean, minimal
// ============================================================

val PureLightBackground = Color(0xFFFCFCFF)   // faintest indigo tint
val PureLightSurface = Color(0xFFF5F4FA)      // subtle cool undertone
val PureLightCardBackground = Color(0xFFEFEEF6)
val PureLightCardBorder = Color(0xFFE0DDE8)

// ============================================================
//  Pure Dark mode — True black with cool cast
// ============================================================

val PureDarkBackground = Color(0xFF08080E)    // near-black, cool undertone
val PureDarkSurface = Color(0xFF12121C)       // subtle layer
val PureDarkCardBackground = Color(0xFF1A1A28) // card depth
val PureDarkCardBorder = Color(0xFF282838)    // visible but subtle

// ============================================================
//  Warm Rose theme — Light variant (refined, elegant)
//  Warmer, more sophisticated terracotta-and-blush palette
// ============================================================

val WarmRoseBackground = Color(0xFFFBF5F3)       // soft warm white
val WarmRoseSurface = Color(0xFFF8EFEB)          // blush surface
val WarmRoseSurfaceVariant = Color(0xFFF0E4DE)   // deeper blush
val WarmRosePrimary = Color(0xFFB76E79)           // terracotta rose
val WarmRosePrimaryVariant = Color(0xFF9E5A65)    // deeper terracotta
val WarmRoseSecondary = Color(0xFFC89098)         // muted rose
val WarmRoseOnBackground = Color(0xFF382422)      // deep warm brown
val WarmRoseOnSurface = Color(0xFF44302C)         // dark warm
val WarmRoseOnSurfaceVariant = Color(0xFF8C706A)  // muted warm gray

// ============================================================
//  Warm Rose theme — Dark variant
// ============================================================

val WarmRoseDarkBackground = Color(0xFF161010)    // deep warm black
val WarmRoseDarkSurface = Color(0xFF241A18)       // warm dark layer
val WarmRoseDarkSurfaceVariant = Color(0xFF322824) // card depth
val WarmRoseDarkPrimary = Color(0xFFD49898)       // soft rose for dark
val WarmRoseDarkOnBackground = Color(0xFFF0DCD8)  // warm light text

// ============================================================
//  Ocean Blue theme — Light variant (fresh, calming)
//  Sky blue to seafoam gradient, airy and clean
// ============================================================

val OceanBlueBackground = Color(0xFFF0F7FB)       // soft sky white
val OceanBlueSurface = Color(0xFFE8F2F8)          // light blue surface
val OceanBlueSurfaceVariant = Color(0xFFDCE8F0)   // deeper blue
val OceanBluePrimary = Color(0xFF4A90B8)          // ocean blue
val OceanBluePrimaryVariant = Color(0xFF3A7CA0)   // deeper ocean
val OceanBlueSecondary = Color(0xFF6AB0D0)        // sky blue
val OceanBlueOnBackground = Color(0xFF1A3040)     // deep navy
val OceanBlueOnSurface = Color(0xFF2A4050)        // dark blue
val OceanBlueOnSurfaceVariant = Color(0xFF608090) // muted blue-gray

// ============================================================
//  Ocean Blue theme — Dark variant
// ============================================================

val OceanBlueDarkBackground = Color(0xFF0A1520)    // deep navy black
val OceanBlueDarkSurface = Color(0xFF142030)       // dark blue layer
val OceanBlueDarkSurfaceVariant = Color(0xFF1E2E40) // card depth
val OceanBlueDarkPrimary = Color(0xFF70B8D8)       // bright sky for dark
val OceanBlueDarkOnBackground = Color(0xFFD8E8F0)  // light blue text

// ============================================================
//  Pure Light theme — Refined neutral palette
//  Warm gray with subtle sage accents
// ============================================================

val PureLightPrimary = Color(0xFF6B8F7B)           // sage green
val PureLightPrimaryVariant = Color(0xFF5A7A6A)    // deeper sage
val PureLightSecondary = Color(0xFF8FB8A0)         // light sage
val PureLightOnBackground = Color(0xFF2C3028)      // deep warm gray
val PureLightOnSurface = Color(0xFF3C4038)         // dark warm gray
val PureLightOnSurfaceVariant = Color(0xFF707870)  // muted gray

// ============================================================
//  Pure Dark theme — Refined cool palette
//  Cool gray with subtle blue accents
// ============================================================

val PureDarkPrimary = Color(0xFF8090A0)            // cool gray-blue
val PureDarkPrimaryVariant = Color(0xFF6A7A8A)     // deeper cool
val PureDarkSecondary = Color(0xFFA0B0C0)          // light cool
val PureDarkOnBackground = Color(0xFFD8DCE0)       // light cool text
val PureDarkOnSurface = Color(0xFFC0C8D0)          // medium cool text
