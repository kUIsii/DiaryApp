package com.diary.app.ui.achievement

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.diary.app.data.AchievementCategory
import com.diary.app.data.AchievementTier

val CategoryColors = mapOf(
    AchievementCategory.TIME to Color(0xFFFFB300),
    AchievementCategory.MOOD to Color(0xFFE91E63),
    AchievementCategory.WEATHER to Color(0xFF2196F3),
    AchievementCategory.WRITING to Color(0xFF4CAF50),
    AchievementCategory.HABIT to Color(0xFF9C27B0),
    AchievementCategory.EXPLORER to Color(0xFF009688),
    AchievementCategory.COLLECTOR to Color(0xFFFF5722),
    AchievementCategory.LEGENDARY to Color(0xFFD32F2F)
)

fun categoryColor(category: AchievementCategory): Color = CategoryColors[category] ?: Color.Gray

fun achievementIcon(key: String): ImageVector = when (key) {
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
    "streak_7" -> Icons.Default.TrendingUp
    "streak_30" -> Icons.Default.DateRange
    "daily_writer" -> Icons.Default.Edit
    "hundred_days" -> Icons.Default.CalendarMonth
    "night_writer" -> Icons.Default.NightsStay
    "early_bird" -> Icons.Default.LightMode
    "night_poet" -> Icons.Default.DarkMode
    "dawn_recorder" -> Icons.Default.WbTwilight
    "morning_writer" -> Icons.Default.WbSunny
    "weekday_killer" -> Icons.Default.Weekend
    "time_capsule_master" -> Icons.Default.History
    "flash_writer" -> Icons.Default.Bolt
    "moods_5" -> Icons.Default.SentimentSatisfied
    "mood_palette" -> Icons.Default.Palette
    "optimist" -> Icons.Default.WbSunny
    "deep_thinker" -> Icons.Default.Psychology
    "calm_sea" -> Icons.Default.SelfImprovement
    "mood_rollercoaster" -> Icons.Default.TrendingUp
    "all_weather" -> Icons.Default.Thunderstorm
    "rain_collector" -> Icons.Default.WaterDrop
    "snow_writer" -> Icons.Default.AcUnit
    "storm_writer" -> Icons.Default.Storm
    "sunny_recorder" -> Icons.Default.WbSunny
    "fearless_recorder" -> Icons.Default.Shield
    "returnee" -> Icons.Default.Replay
    "deep_writer" -> Icons.Default.Create
    "twin_stars" -> Icons.Default.Looks
    "time_traveler" -> Icons.Default.Schedule
    "new_year_eve" -> Icons.Default.AutoAwesome
    "midnight_bell" -> Icons.Default.NightsStay
    "full_moon" -> Icons.Default.DarkMode
    "first_echo" -> Icons.Default.Bookmark
    "favorite_1" -> Icons.Default.FavoriteBorder
    "favorites_10" -> Icons.Default.Favorite
    "legendary_entries_500" -> Icons.Default.Whatshot
    "legendary_streak_365" -> Icons.Default.CalendarMonth
    "legendary_words_million" -> Icons.Default.AutoStories
    "legendary_all_categories" -> Icons.Default.Star
    else -> Icons.Default.AutoAwesome
}

@Composable
fun AchievementBadge(
    achievementKey: String,
    category: AchievementCategory,
    tier: AchievementTier,
    unlocked: Boolean,
    modifier: Modifier = Modifier,
    size: Int = 80
) {
    val primaryColor = categoryColor(category)
    val animProgress by animateFloatAsState(
        targetValue = if (unlocked) 1f else 0f,
        animationSpec = tween(500),
        label = "badge"
    )
    val iconSize = when (tier) {
        AchievementTier.LEGENDARY -> size * 0.45f
        AchievementTier.EPIC -> size * 0.40f
        else -> size * 0.38f
    }

    Box(
        modifier = modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size.dp)) {
            val center = Offset(size / 2f, size / 2f)
            val radius = size / 2f
            val bgBrush = Brush.sweepGradient(
                center = center,
                colors = listOf(
                    primaryColor.copy(alpha = 0.9f),
                    primaryColor.copy(alpha = 0.6f),
                    primaryColor.copy(alpha = 0.9f)
                )
            )

            drawCircle(brush = bgBrush, radius = radius)

            drawCircle(
                color = primaryColor.copy(alpha = 0.3f),
                radius = radius * 0.88f,
                style = Stroke(width = size * 0.03f)
            )

            when (tier) {
                AchievementTier.COMMON -> {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.2f),
                        radius = radius * 0.3f,
                        center = Offset(center.x + radius * 0.1f, center.y - radius * 0.1f)
                    )
                }
                AchievementTier.RARE -> {
                    drawStar(center, radius * 0.65f, primaryColor.copy(alpha = 0.35f))
                }
                AchievementTier.EPIC -> {
                    drawGlow(center, radius, primaryColor)
                }
                AchievementTier.LEGENDARY -> {
                    drawFourPointStar(center, radius * 0.8f, primaryColor.copy(alpha = 0.4f))
                    drawFourPointStar(center, radius * 0.55f, primaryColor.copy(alpha = 0.25f))
                }
            }

            if (!unlocked) {
                drawCircle(color = Color.Black.copy(alpha = 0.45f), radius = radius)
            }
        }

        Icon(
            imageVector = if (unlocked) achievementIcon(achievementKey) else Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(iconSize.dp),
            tint = if (unlocked) Color.White else Color.White.copy(alpha = 0.4f)
        )
    }
}

private fun DrawScope.drawStar(center: Offset, radius: Float, color: Color) {
    val points = (0..4).map { i ->
        val angle = Math.toRadians((i * 72 - 90).toDouble())
        Offset(
            x = center.x + radius * Math.cos(angle).toFloat(),
            y = center.y + radius * Math.sin(angle).toFloat()
        )
    }
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 0..4) {
            val innerAngle = Math.toRadians((i * 72 - 90 + 36).toDouble())
            val innerRadius = radius * 0.4f
            val ix = center.x + innerRadius * Math.cos(innerAngle).toFloat()
            val iy = center.y + innerRadius * Math.sin(innerAngle).toFloat()
            lineTo(ix, iy)
            val next = points[(i + 1) % 5]
            lineTo(next.x, next.y)
        }
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawGlow(center: Offset, radius: Float, color: Color) {
    val glowColor = color.copy(alpha = 0.15f)
    for (i in 1..4) {
        val r = radius * (0.5f + i * 0.1f)
        drawCircle(color = glowColor.copy(alpha = 0.12f - i * 0.02f), radius = r)
    }
    drawCircle(color = color.copy(alpha = 0.3f), radius = radius * 0.4f)
    for (angle in 0..360 step 30) {
        val rad = Math.toRadians(angle.toDouble())
        val endX = center.x + radius * 0.75f * Math.cos(rad).toFloat()
        val endY = center.y + radius * 0.75f * Math.sin(rad).toFloat()
        drawLine(
            color = color.copy(alpha = 0.2f),
            start = center,
            end = Offset(endX, endY),
            strokeWidth = size.width * 0.01f
        )
    }
}

private fun DrawScope.drawFourPointStar(center: Offset, radius: Float, color: Color) {
    val path = androidx.compose.ui.graphics.Path().apply {
        val outer = (0..3).map { i ->
            val angle = Math.toRadians((i * 90).toDouble())
            Offset(
                x = center.x + radius * Math.cos(angle).toFloat(),
                y = center.y + radius * Math.sin(angle).toFloat()
            )
        }
        val inner = (0..3).map { i ->
            val angle = Math.toRadians((i * 90 + 45).toDouble())
            Offset(
                x = center.x + radius * 0.35f * Math.cos(angle).toFloat(),
                y = center.y + radius * 0.35f * Math.sin(angle).toFloat()
            )
        }
        moveTo(outer[0].x, outer[0].y)
        for (i in 0..3) {
            lineTo(inner[i].x, inner[i].y)
            lineTo(outer[(i + 1) % 4].x, outer[(i + 1) % 4].y)
        }
        close()
    }
    drawPath(path, color)
}

@Composable
fun AchievementArtwork(
    achievementKey: String,
    category: AchievementCategory,
    tier: AchievementTier,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 24
) {
    AchievementBadge(
        achievementKey = achievementKey,
        category = category,
        tier = tier,
        unlocked = isUnlocked,
        modifier = modifier,
        size = 80
    )
}
