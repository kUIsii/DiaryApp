package com.diary.app.ui.achievement

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Looks
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShortText
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storm
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.diary.app.data.AchievementCategory
import com.diary.app.data.AchievementTier

// ── Category colors ────────────────────────────────────────────

private val WritingColor = Color(0xFF5C6BC0)
private val HabitColor = Color(0xFFFF7043)
private val TimeColor = Color(0xFF42A5F5)
private val MoodColor = Color(0xFFAB47BC)
private val WeatherColor = Color(0xFF26A69A)
private val ExplorerColor = Color(0xFF66BB6A)
private val CollectorColor = Color(0xFFEC407A)
private val LegendaryColor = Color(0xFFFFC107)

fun categoryColor(category: AchievementCategory): Color = when (category) {
    AchievementCategory.WRITING -> WritingColor
    AchievementCategory.HABIT -> HabitColor
    AchievementCategory.TIME -> TimeColor
    AchievementCategory.MOOD -> MoodColor
    AchievementCategory.WEATHER -> WeatherColor
    AchievementCategory.EXPLORER -> ExplorerColor
    AchievementCategory.COLLECTOR -> CollectorColor
    AchievementCategory.LEGENDARY -> LegendaryColor
}

// ── Per-achievement icon mapping ───────────────────────────────

fun achievementIcon(key: String): ImageVector = when (key) {
    // Writing
    "first_entry" -> Icons.Default.AutoFixHigh
    "entries_10" -> Icons.Default.HistoryEdu
    "entries_50" -> Icons.Default.MilitaryTech
    "entries_100" -> Icons.Default.Whatshot
    "words_10000" -> Icons.Default.TextSnippet
    "words_100000" -> Icons.Default.AutoStories
    "fifty_thousand_words" -> Icons.Default.MenuBook
    "thousand_words" -> Icons.Default.ShortText
    "brief_master" -> Icons.Default.FormatQuote
    "tags_5" -> Icons.Default.AutoAwesome
    "images_10" -> Icons.Default.PhotoLibrary
    "photo_diary" -> Icons.Default.CameraAlt
    "collector" -> Icons.Default.Star

    // Habit
    "streak_7" -> Icons.Default.TrendingUp
    "streak_30" -> Icons.Default.DateRange
    "daily_writer" -> Icons.Default.Edit
    "hundred_days" -> Icons.Default.CalendarMonth

    // Time
    "night_writer" -> Icons.Default.NightsStay
    "early_bird" -> Icons.Default.LightMode
    "night_poet" -> Icons.Default.DarkMode
    "dawn_recorder" -> Icons.Default.WbTwilight
    "morning_writer" -> Icons.Default.WbSunny
    "weekday_killer" -> Icons.Default.Weekend
    "time_capsule_master" -> Icons.Default.History
    "flash_writer" -> Icons.Default.Bolt

    // Mood
    "moods_5" -> Icons.Default.SentimentSatisfied
    "mood_palette" -> Icons.Default.Palette
    "optimist" -> Icons.Default.WbSunny
    "deep_thinker" -> Icons.Default.Psychology
    "calm_sea" -> Icons.Default.SelfImprovement
    "mood_rollercoaster" -> Icons.Default.TrendingUp

    // Weather
    "all_weather" -> Icons.Default.Thunderstorm
    "rain_collector" -> Icons.Default.WaterDrop
    "snow_writer" -> Icons.Default.AcUnit
    "storm_writer" -> Icons.Default.Storm
    "sunny_recorder" -> Icons.Default.WbSunny
    "fearless_recorder" -> Icons.Default.Shield

    // Explorer
    "returnee" -> Icons.Default.Replay
    "deep_writer" -> Icons.Default.Create
    "twin_stars" -> Icons.Default.Looks
    "time_traveler" -> Icons.Default.Schedule
    "new_year_eve" -> Icons.Default.AutoAwesome
    "midnight_bell" -> Icons.Default.NightsStay
    "full_moon" -> Icons.Default.DarkMode
    "first_echo" -> Icons.Default.Bookmark

    // Collector
    "favorite_1" -> Icons.Default.FavoriteBorder
    "favorites_10" -> Icons.Default.Favorite

    // Legendary
    "legendary_entries_500" -> Icons.Default.Whatshot
    "legendary_streak_365" -> Icons.Default.CalendarMonth
    "legendary_words_million" -> Icons.Default.AutoStories
    "legendary_all_categories" -> Icons.Default.Star

    // Fallback
    else -> Icons.Default.AutoAwesome
}

// ── Main composable ────────────────────────────────────────────

@Composable
fun AchievementArtwork(
    achievementKey: String,
    category: AchievementCategory,
    tier: AchievementTier,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 24
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    val baseColor = categoryColor(category)

    // Tier affects saturation/brightness
    val (bgStart, bgEnd, iconAlpha) = when {
        !isUnlocked -> Triple(
            Color(0xFFF0EDE8),
            Color(0xFFE2DDD5),
            0.3f
        )
        tier == AchievementTier.LEGENDARY -> Triple(
            baseColor.copy(alpha = 0.22f),
            baseColor.copy(alpha = 0.38f),
            1f
        )
        tier == AchievementTier.EPIC -> Triple(
            baseColor.copy(alpha = 0.15f),
            baseColor.copy(alpha = 0.30f),
            0.95f
        )
        tier == AchievementTier.RARE -> Triple(
            baseColor.copy(alpha = 0.10f),
            baseColor.copy(alpha = 0.22f),
            0.9f
        )
        else -> Triple(
            baseColor.copy(alpha = 0.07f),
            baseColor.copy(alpha = 0.16f),
            0.85f
        )
    }

    val borderColor = if (isUnlocked) {
        baseColor.copy(alpha = 0.25f)
    } else {
        Color(0xFFD6D0C8).copy(alpha = 0.4f)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(listOf(bgStart, bgEnd)),
                shape
            )
            .border(1.dp, borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        // Soft circular glow behind icon (unlocked only)
        if (isUnlocked) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(baseColor.copy(alpha = 0.10f))
            )
        }

        // Icon
        Icon(
            imageVector = if (isUnlocked) {
                achievementIcon(achievementKey)
            } else {
                Icons.Default.Lock
            },
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = if (isUnlocked) {
                baseColor.copy(alpha = iconAlpha)
            } else {
                Color(0xFFB0A99E)
            }
        )
    }
}
