package com.diary.app.ui.achievement

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.diary.app.data.AchievementCategory
import com.diary.app.data.AchievementTier

// Category colors
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

@Composable
fun AchievementIcon(
    category: AchievementCategory,
    tier: AchievementTier,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier
) {
    val size = 28.dp
    val color = if (isUnlocked) categoryColor(category) else Color.Gray.copy(alpha = 0.4f)
    val glowColor = if (isUnlocked) categoryColor(category).copy(alpha = 0.3f) else Color.Transparent

    Canvas(modifier = modifier.size(size)) {
        // Draw glow effect for unlocked achievements
        if (isUnlocked) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    center = center,
                    radius = this.size.width * 0.6f
                ),
                radius = this.size.width * 0.6f
            )
        }

        // Draw the icon based on category
        when (category) {
            AchievementCategory.WRITING -> drawQuillIcon(color)
            AchievementCategory.HABIT -> drawFlameIcon(color)
            AchievementCategory.TIME -> drawClockIcon(color)
            AchievementCategory.MOOD -> drawPaletteIcon(color)
            AchievementCategory.WEATHER -> drawCloudIcon(color)
            AchievementCategory.EXPLORER -> drawCompassIcon(color)
            AchievementCategory.COLLECTOR -> drawGemIcon(color)
            AchievementCategory.LEGENDARY -> drawCrownIcon(color)
        }
    }
}

// Quill pen icon for WRITING
private fun DrawScope.drawQuillIcon(color: Color) {
    val w = size.width
    val h = size.height
    val strokeWidth = w * 0.08f

    // Feather body
    val featherPath = Path().apply {
        moveTo(w * 0.7f, h * 0.1f)
        cubicTo(w * 0.8f, h * 0.15f, w * 0.85f, h * 0.3f, w * 0.6f, h * 0.6f)
        lineTo(w * 0.55f, h * 0.65f)
        cubicTo(w * 0.5f, h * 0.55f, w * 0.4f, h * 0.4f, w * 0.3f, h * 0.2f)
        cubicTo(w * 0.35f, h * 0.12f, w * 0.5f, h * 0.08f, w * 0.7f, h * 0.1f)
        close()
    }
    drawPath(featherPath, color)

    // Quill tip
    val tipPath = Path().apply {
        moveTo(w * 0.55f, h * 0.65f)
        lineTo(w * 0.35f, h * 0.9f)
        lineTo(w * 0.45f, h * 0.7f)
        close()
    }
    drawPath(tipPath, color.copy(alpha = 0.8f))

    // Center line
    drawLine(
        color = color.copy(alpha = 0.5f),
        start = Offset(w * 0.5f, h * 0.15f),
        end = Offset(w * 0.45f, h * 0.7f),
        strokeWidth = strokeWidth * 0.5f
    )
}

// Flame icon for HABIT
private fun DrawScope.drawFlameIcon(color: Color) {
    val w = size.width
    val h = size.height

    // Outer flame
    val outerFlame = Path().apply {
        moveTo(w * 0.5f, h * 0.1f)
        cubicTo(w * 0.65f, h * 0.25f, w * 0.8f, h * 0.45f, w * 0.7f, h * 0.65f)
        cubicTo(w * 0.65f, h * 0.75f, w * 0.55f, h * 0.85f, w * 0.5f, h * 0.9f)
        cubicTo(w * 0.45f, h * 0.85f, w * 0.35f, h * 0.75f, w * 0.3f, h * 0.65f)
        cubicTo(w * 0.2f, h * 0.45f, w * 0.35f, h * 0.25f, w * 0.5f, h * 0.1f)
        close()
    }
    drawPath(outerFlame, color)

    // Inner flame (lighter)
    val innerFlame = Path().apply {
        moveTo(w * 0.5f, h * 0.35f)
        cubicTo(w * 0.58f, h * 0.42f, w * 0.62f, h * 0.52f, w * 0.58f, h * 0.62f)
        cubicTo(w * 0.55f, h * 0.7f, w * 0.52f, h * 0.78f, w * 0.5f, h * 0.82f)
        cubicTo(w * 0.48f, h * 0.78f, w * 0.45f, h * 0.7f, w * 0.42f, h * 0.62f)
        cubicTo(w * 0.38f, h * 0.52f, w * 0.42f, h * 0.42f, w * 0.5f, h * 0.35f)
        close()
    }
    drawPath(innerFlame, color.copy(alpha = 0.6f))
}

// Clock icon for TIME
private fun DrawScope.drawClockIcon(color: Color) {
    val w = size.width
    val h = size.height
    val centerX = w * 0.5f
    val centerY = h * 0.5f
    val radius = w * 0.4f

    // Clock face
    drawCircle(
        color = color.copy(alpha = 0.15f),
        radius = radius,
        center = Offset(centerX, centerY)
    )
    drawCircle(
        color = color,
        radius = radius,
        center = Offset(centerX, centerY),
        style = Stroke(width = w * 0.06f)
    )

    // Hour hand
    drawLine(
        color = color,
        start = Offset(centerX, centerY),
        end = Offset(centerX, centerY - radius * 0.5f),
        strokeWidth = w * 0.08f,
        cap = androidx.compose.ui.graphics.StrokeCap.Round
    )

    // Minute hand
    drawLine(
        color = color,
        start = Offset(centerX, centerY),
        end = Offset(centerX + radius * 0.6f, centerY),
        strokeWidth = w * 0.05f,
        cap = androidx.compose.ui.graphics.StrokeCap.Round
    )

    // Center dot
    drawCircle(
        color = color,
        radius = w * 0.05f,
        center = Offset(centerX, centerY)
    )
}

// Palette icon for MOOD
private fun DrawScope.drawPaletteIcon(color: Color) {
    val w = size.width
    val h = size.height

    // Palette shape
    val palettePath = Path().apply {
        moveTo(w * 0.5f, h * 0.15f)
        cubicTo(w * 0.75f, h * 0.1f, w * 0.9f, h * 0.3f, w * 0.85f, h * 0.55f)
        cubicTo(w * 0.8f, h * 0.75f, w * 0.6f, h * 0.9f, w * 0.4f, h * 0.85f)
        cubicTo(w * 0.2f, h * 0.8f, w * 0.1f, h * 0.6f, w * 0.15f, h * 0.4f)
        cubicTo(w * 0.2f, h * 0.2f, w * 0.35f, h * 0.12f, w * 0.5f, h * 0.15f)
        close()
    }
    drawPath(palettePath, color.copy(alpha = 0.2f))
    drawPath(palettePath, color, style = Stroke(width = w * 0.04f))

    // Paint dots
    val dotRadius = w * 0.08f
    drawCircle(color = Color(0xFFEF5350), radius = dotRadius, center = Offset(w * 0.35f, h * 0.35f))
    drawCircle(color = Color(0xFF42A5F5), radius = dotRadius, center = Offset(w * 0.55f, h * 0.3f))
    drawCircle(color = Color(0xFF66BB6A), radius = dotRadius, center = Offset(w * 0.65f, h * 0.45f))
    drawCircle(color = Color(0xFFFFC107), radius = dotRadius, center = Offset(w * 0.6f, h * 0.6f))

    // Thumb hole
    drawCircle(
        color = Color.Transparent,
        radius = w * 0.12f,
        center = Offset(w * 0.3f, h * 0.55f)
    )
}

// Cloud icon for WEATHER
private fun DrawScope.drawCloudIcon(color: Color) {
    val w = size.width
    val h = size.height

    // Main cloud
    val cloudPath = Path().apply {
        moveTo(w * 0.25f, h * 0.55f)
        cubicTo(w * 0.15f, h * 0.55f, w * 0.1f, h * 0.45f, w * 0.2f, h * 0.4f)
        cubicTo(w * 0.15f, h * 0.3f, w * 0.25f, h * 0.2f, w * 0.35f, h * 0.25f)
        cubicTo(w * 0.4f, h * 0.15f, w * 0.6f, h * 0.15f, w * 0.65f, h * 0.25f)
        cubicTo(w * 0.75f, h * 0.2f, w * 0.85f, h * 0.3f, w * 0.8f, h * 0.4f)
        cubicTo(w * 0.9f, h * 0.45f, w * 0.85f, h * 0.55f, w * 0.75f, h * 0.55f)
        close()
    }
    drawPath(cloudPath, color.copy(alpha = 0.3f))
    drawPath(cloudPath, color, style = Stroke(width = w * 0.04f))

    // Rain drops
    val dropColor = color.copy(alpha = 0.6f)
    drawLine(
        color = dropColor,
        start = Offset(w * 0.35f, h * 0.65f),
        end = Offset(w * 0.35f, h * 0.8f),
        strokeWidth = w * 0.04f,
        cap = androidx.compose.ui.graphics.StrokeCap.Round
    )
    drawLine(
        color = dropColor,
        start = Offset(w * 0.5f, h * 0.7f),
        end = Offset(w * 0.5f, h * 0.85f),
        strokeWidth = w * 0.04f,
        cap = androidx.compose.ui.graphics.StrokeCap.Round
    )
    drawLine(
        color = dropColor,
        start = Offset(w * 0.65f, h * 0.65f),
        end = Offset(w * 0.65f, h * 0.8f),
        strokeWidth = w * 0.04f,
        cap = androidx.compose.ui.graphics.StrokeCap.Round
    )
}

// Compass icon for EXPLORER
private fun DrawScope.drawCompassIcon(color: Color) {
    val w = size.width
    val h = size.height
    val centerX = w * 0.5f
    val centerY = h * 0.5f
    val radius = w * 0.4f

    // Outer circle
    drawCircle(
        color = color.copy(alpha = 0.15f),
        radius = radius,
        center = Offset(centerX, centerY)
    )
    drawCircle(
        color = color,
        radius = radius,
        center = Offset(centerX, centerY),
        style = Stroke(width = w * 0.04f)
    )

    // North pointer (red)
    val northPath = Path().apply {
        moveTo(centerX, centerY - radius * 0.7f)
        lineTo(centerX - radius * 0.15f, centerY)
        lineTo(centerX, centerY - radius * 0.2f)
        close()
    }
    drawPath(northPath, Color(0xFFEF5350))

    // South pointer (blue)
    val southPath = Path().apply {
        moveTo(centerX, centerY + radius * 0.7f)
        lineTo(centerX + radius * 0.15f, centerY)
        lineTo(centerX, centerY + radius * 0.2f)
        close()
    }
    drawPath(southPath, color.copy(alpha = 0.5f))

    // Center dot
    drawCircle(
        color = color,
        radius = w * 0.04f,
        center = Offset(centerX, centerY)
    )

    // Cardinal points
    drawCircle(color = color, radius = w * 0.03f, center = Offset(centerX, centerY - radius * 0.85f))
    drawCircle(color = color, radius = w * 0.03f, center = Offset(centerX, centerY + radius * 0.85f))
    drawCircle(color = color, radius = w * 0.03f, center = Offset(centerX - radius * 0.85f, centerY))
    drawCircle(color = color, radius = w * 0.03f, center = Offset(centerX + radius * 0.85f, centerY))
}

// Gem icon for COLLECTOR
private fun DrawScope.drawGemIcon(color: Color) {
    val w = size.width
    val h = size.height

    // Gem top facets
    val topPath = Path().apply {
        moveTo(w * 0.5f, h * 0.1f)
        lineTo(w * 0.25f, h * 0.35f)
        lineTo(w * 0.5f, h * 0.45f)
        lineTo(w * 0.75f, h * 0.35f)
        close()
    }
    drawPath(topPath, color.copy(alpha = 0.4f))

    // Gem bottom facets
    val bottomPath = Path().apply {
        moveTo(w * 0.25f, h * 0.35f)
        lineTo(w * 0.5f, h * 0.9f)
        lineTo(w * 0.75f, h * 0.35f)
        lineTo(w * 0.5f, h * 0.45f)
        close()
    }
    drawPath(bottomPath, color.copy(alpha = 0.6f))

    // Outline
    val outlinePath = Path().apply {
        moveTo(w * 0.5f, h * 0.1f)
        lineTo(w * 0.25f, h * 0.35f)
        lineTo(w * 0.5f, h * 0.9f)
        lineTo(w * 0.75f, h * 0.35f)
        close()
    }
    drawPath(outlinePath, color, style = Stroke(width = w * 0.04f))

    // Highlight
    drawLine(
        color = Color.White.copy(alpha = 0.5f),
        start = Offset(w * 0.35f, h * 0.25f),
        end = Offset(w * 0.45f, h * 0.35f),
        strokeWidth = w * 0.04f,
        cap = androidx.compose.ui.graphics.StrokeCap.Round
    )
}

// Crown icon for LEGENDARY
private fun DrawScope.drawCrownIcon(color: Color) {
    val w = size.width
    val h = size.height

    // Crown body
    val crownPath = Path().apply {
        moveTo(w * 0.1f, h * 0.65f)
        lineTo(w * 0.2f, h * 0.3f)
        lineTo(w * 0.35f, h * 0.5f)
        lineTo(w * 0.5f, h * 0.2f)
        lineTo(w * 0.65f, h * 0.5f)
        lineTo(w * 0.8f, h * 0.3f)
        lineTo(w * 0.9f, h * 0.65f)
        close()
    }
    drawPath(crownPath, color.copy(alpha = 0.3f))
    drawPath(crownPath, color, style = Stroke(width = w * 0.04f))

    // Crown base
    drawRect(
        color = color.copy(alpha = 0.4f),
        topLeft = Offset(w * 0.1f, h * 0.65f),
        size = Size(w * 0.8f, h * 0.15f)
    )
    drawRect(
        color = color,
        topLeft = Offset(w * 0.1f, h * 0.65f),
        size = Size(w * 0.8f, h * 0.15f),
        style = Stroke(width = w * 0.04f)
    )

    // Jewels
    drawCircle(color = Color(0xFFEF5350), radius = w * 0.06f, center = Offset(w * 0.5f, h * 0.45f))
    drawCircle(color = Color(0xFF42A5F5), radius = w * 0.05f, center = Offset(w * 0.3f, h * 0.55f))
    drawCircle(color = Color(0xFF66BB6A), radius = w * 0.05f, center = Offset(w * 0.7f, h * 0.55f))

    // Crown tips
    drawCircle(color = color, radius = w * 0.05f, center = Offset(w * 0.2f, h * 0.28f))
    drawCircle(color = color, radius = w * 0.05f, center = Offset(w * 0.5f, h * 0.18f))
    drawCircle(color = color, radius = w * 0.05f, center = Offset(w * 0.8f, h * 0.28f))
}

// Lock icon for locked achievements
@Composable
fun LockIcon(modifier: Modifier = Modifier) {
    val size = 28.dp
    val color = Color.Gray.copy(alpha = 0.4f)

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Lock body
        drawRoundRect(
            color = color.copy(alpha = 0.3f),
            topLeft = Offset(w * 0.2f, h * 0.45f),
            size = Size(w * 0.6f, h * 0.45f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.2f, h * 0.45f),
            size = Size(w * 0.6f, h * 0.45f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f),
            style = Stroke(width = w * 0.04f)
        )

        // Lock shackle
        val shacklePath = Path().apply {
            moveTo(w * 0.35f, h * 0.45f)
            lineTo(w * 0.35f, h * 0.3f)
            cubicTo(w * 0.35f, h * 0.15f, w * 0.65f, h * 0.15f, w * 0.65f, h * 0.3f)
            lineTo(w * 0.65f, h * 0.45f)
        }
        drawPath(shacklePath, color, style = Stroke(width = w * 0.06f, cap = androidx.compose.ui.graphics.StrokeCap.Round))

        // Keyhole
        drawCircle(
            color = color.copy(alpha = 0.5f),
            radius = w * 0.06f,
            center = Offset(w * 0.5f, h * 0.6f)
        )
    }
}
