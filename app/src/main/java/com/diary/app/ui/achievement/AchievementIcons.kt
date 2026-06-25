package com.diary.app.ui.achievement

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.diary.app.data.AchievementCategory
import com.diary.app.data.AchievementTier
import kotlin.math.absoluteValue

private val WritingColor = Color(0xFF5C6BC0)
private val HabitColor = Color(0xFFFF7043)
private val TimeColor = Color(0xFF42A5F5)
private val MoodColor = Color(0xFFAB47BC)
private val WeatherColor = Color(0xFF26A69A)
private val ExplorerColor = Color(0xFF66BB6A)
private val CollectorColor = Color(0xFFEC407A)
private val LegendaryColor = Color(0xFFFFC107)

data class AchievementArtworkPalette(
    val start: Color,
    val end: Color,
    val detail: Color,
    val border: Color,
    val glow: Color
)

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
fun AchievementArtwork(
    achievementKey: String,
    category: AchievementCategory,
    tier: AchievementTier,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 24
) {
    val palette = achievementPalette(category = category, tier = tier, isUnlocked = isUnlocked)
    val shape = RoundedCornerShape(cornerRadius.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(palette.start, palette.end)), shape)
            .border(1.dp, palette.border, shape)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val seed = achievementKey.hashCode().absoluteValue
            drawArtworkBackground(seed = seed, palette = palette, isUnlocked = isUnlocked)
        }

        if (isUnlocked) {
            AchievementIcon(
                achievementKey = achievementKey,
                category = category,
                tier = tier,
                isUnlocked = true,
                modifier = Modifier.fillMaxSize(0.46f)
            )
        } else {
            LockIcon(modifier = Modifier.fillMaxSize(0.42f))
        }
    }
}

private fun DrawScope.drawArtworkBackground(
    seed: Int,
    palette: AchievementArtworkPalette,
    isUnlocked: Boolean
) {
    val width = size.width
    val height = size.height

    if (isUnlocked) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(palette.glow, Color.Transparent),
                center = Offset(width * 0.5f, height * 0.45f),
                radius = width * 0.62f
            ),
            radius = width * 0.62f,
            center = Offset(width * 0.5f, height * 0.45f)
        )
    }

    drawRoundRect(
        color = Color.White.copy(alpha = if (isUnlocked) 0.12f else 0.08f),
        topLeft = Offset(width * 0.08f, height * 0.1f),
        size = Size(width * 0.84f, height * 0.28f),
        cornerRadius = CornerRadius(width * 0.22f)
    )

    val orbitVariant = seed % 4
    when (orbitVariant) {
        0 -> {
            drawCircle(
                color = palette.detail.copy(alpha = 0.22f),
                radius = width * 0.26f,
                center = Offset(width * 0.76f, height * 0.28f),
                style = Stroke(width = width * 0.04f)
            )
            drawCircle(
                color = palette.detail.copy(alpha = 0.3f),
                radius = width * 0.05f,
                center = Offset(width * 0.76f, height * 0.28f)
            )
        }
        1 -> {
            repeat(3) { index ->
                drawLine(
                    color = palette.detail.copy(alpha = 0.3f - index * 0.06f),
                    start = Offset(width * (0.2f + index * 0.14f), height * 0.2f),
                    end = Offset(width * (0.55f + index * 0.1f), height * 0.82f),
                    strokeWidth = width * 0.03f,
                    cap = StrokeCap.Round
                )
            }
        }
        2 -> {
            val path = Path().apply {
                moveTo(width * 0.16f, height * 0.78f)
                cubicTo(width * 0.28f, height * 0.44f, width * 0.64f, height * 0.62f, width * 0.84f, height * 0.62f)
            }
            drawPath(
                path = path,
                color = palette.detail.copy(alpha = 0.28f),
                style = Stroke(width = width * 0.05f, cap = StrokeCap.Round)
            )
        }
        else -> {
            repeat(5) { index ->
                val x = width * (0.2f + index * 0.15f)
                val y = if (index % 2 == 0) height * 0.25f else height * 0.72f
                drawCircle(
                    color = palette.detail.copy(alpha = 0.25f + index * 0.03f),
                    radius = width * (0.025f + (index % 2) * 0.008f),
                    center = Offset(x, y)
                )
            }
        }
    }

    val sparkleCount = 3 + seed % 3
    repeat(sparkleCount) { index ->
        val x = width * (0.18f + ((seed shr index) and 0x7) * 0.09f).coerceAtMost(0.86f)
        val y = height * (0.18f + ((seed shr (index + 3)) and 0x7) * 0.08f).coerceAtMost(0.84f)
        drawSparkle(
            center = Offset(x, y),
            radius = width * (0.028f + (index % 2) * 0.01f),
            color = palette.detail.copy(alpha = 0.35f)
        )
    }
}

private fun DrawScope.drawSparkle(center: Offset, radius: Float, color: Color) {
    drawLine(
        color = color,
        start = Offset(center.x - radius, center.y),
        end = Offset(center.x + radius, center.y),
        strokeWidth = radius * 0.45f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = Offset(center.x, center.y - radius),
        end = Offset(center.x, center.y + radius),
        strokeWidth = radius * 0.45f,
        cap = StrokeCap.Round
    )
}

private fun achievementPalette(
    category: AchievementCategory,
    tier: AchievementTier,
    isUnlocked: Boolean
): AchievementArtworkPalette {
    val base = categoryColor(category)
    val tierTint = when (tier) {
        AchievementTier.COMMON -> Color(0xFFECE8E2)
        AchievementTier.RARE -> Color(0xFFD8E8FF)
        AchievementTier.EPIC -> Color(0xFFE7DAFF)
        AchievementTier.LEGENDARY -> Color(0xFFFFE7B8)
    }

    return if (isUnlocked) {
        AchievementArtworkPalette(
            start = tierTint.copy(alpha = 0.92f),
            end = base.copy(alpha = 0.78f),
            detail = Color.White,
            border = Color.White.copy(alpha = 0.42f),
            glow = base.copy(alpha = 0.3f)
        )
    } else {
        AchievementArtworkPalette(
            start = Color(0xFFF1EEE8),
            end = Color(0xFFE3DDD4),
            detail = Color(0xFF8C867D),
            border = Color.White.copy(alpha = 0.28f),
            glow = Color.Transparent
        )
    }
}

@Composable
fun AchievementIcon(
    achievementKey: String,
    category: AchievementCategory,
    tier: AchievementTier,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier
) {
    val size = 28.dp
    val color = if (isUnlocked) categoryColor(category) else Color.Gray.copy(alpha = 0.42f)
    val accentColor = if (isUnlocked) tierAccentColor(tier) else Color(0xFFA59F96)
    val glowColor = if (isUnlocked) color.copy(alpha = 0.26f) else Color.Transparent
    val seed = achievementKey.hashCode().absoluteValue

    Canvas(modifier = modifier.size(size)) {
        if (isUnlocked) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    center = center,
                    radius = this.size.width * 0.58f
                ),
                radius = this.size.width * 0.58f
            )
        }

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

        drawKeyAccent(seed = seed, color = accentColor)
    }
}

private fun DrawScope.drawKeyAccent(seed: Int, color: Color) {
    val width = size.width
    val height = size.height

    when (seed % 3) {
        0 -> {
            drawLine(
                color = color.copy(alpha = 0.75f),
                start = Offset(width * 0.18f, height * 0.82f),
                end = Offset(width * 0.82f, height * 0.18f),
                strokeWidth = width * 0.06f,
                cap = StrokeCap.Round
            )
        }
        1 -> {
            drawCircle(
                color = color.copy(alpha = 0.18f),
                radius = width * 0.18f,
                center = Offset(width * 0.75f, height * 0.28f),
                style = Stroke(width = width * 0.05f)
            )
        }
        else -> {
            drawCircle(
                color = color.copy(alpha = 0.75f),
                radius = width * 0.07f,
                center = Offset(width * 0.24f, height * 0.26f)
            )
            drawCircle(
                color = color.copy(alpha = 0.55f),
                radius = width * 0.05f,
                center = Offset(width * 0.76f, height * 0.76f)
            )
        }
    }
}

private fun tierAccentColor(tier: AchievementTier): Color = when (tier) {
    AchievementTier.COMMON -> Color(0xFF8A7966)
    AchievementTier.RARE -> Color(0xFF2F6DBA)
    AchievementTier.EPIC -> Color(0xFF7B46B8)
    AchievementTier.LEGENDARY -> Color(0xFFD98F00)
}

private fun DrawScope.drawQuillIcon(color: Color) {
    val width = size.width
    val height = size.height
    val strokeWidth = width * 0.08f

    val featherPath = Path().apply {
        moveTo(width * 0.7f, height * 0.1f)
        cubicTo(width * 0.8f, height * 0.15f, width * 0.85f, height * 0.3f, width * 0.6f, height * 0.6f)
        lineTo(width * 0.55f, height * 0.65f)
        cubicTo(width * 0.5f, height * 0.55f, width * 0.4f, height * 0.4f, width * 0.3f, height * 0.2f)
        cubicTo(width * 0.35f, height * 0.12f, width * 0.5f, height * 0.08f, width * 0.7f, height * 0.1f)
        close()
    }
    drawPath(featherPath, color)

    val tipPath = Path().apply {
        moveTo(width * 0.55f, height * 0.65f)
        lineTo(width * 0.35f, height * 0.9f)
        lineTo(width * 0.45f, height * 0.7f)
        close()
    }
    drawPath(tipPath, color.copy(alpha = 0.84f))

    drawLine(
        color = color.copy(alpha = 0.5f),
        start = Offset(width * 0.5f, height * 0.15f),
        end = Offset(width * 0.45f, height * 0.7f),
        strokeWidth = strokeWidth * 0.5f
    )
}

private fun DrawScope.drawFlameIcon(color: Color) {
    val width = size.width
    val height = size.height

    val outerFlame = Path().apply {
        moveTo(width * 0.5f, height * 0.1f)
        cubicTo(width * 0.65f, height * 0.25f, width * 0.8f, height * 0.45f, width * 0.7f, height * 0.65f)
        cubicTo(width * 0.65f, height * 0.75f, width * 0.55f, height * 0.85f, width * 0.5f, height * 0.9f)
        cubicTo(width * 0.45f, height * 0.85f, width * 0.35f, height * 0.75f, width * 0.3f, height * 0.65f)
        cubicTo(width * 0.2f, height * 0.45f, width * 0.35f, height * 0.25f, width * 0.5f, height * 0.1f)
        close()
    }
    drawPath(outerFlame, color)

    val innerFlame = Path().apply {
        moveTo(width * 0.5f, height * 0.35f)
        cubicTo(width * 0.58f, height * 0.42f, width * 0.62f, height * 0.52f, width * 0.58f, height * 0.62f)
        cubicTo(width * 0.55f, height * 0.7f, width * 0.52f, height * 0.78f, width * 0.5f, height * 0.82f)
        cubicTo(width * 0.48f, height * 0.78f, width * 0.45f, height * 0.7f, width * 0.42f, height * 0.62f)
        cubicTo(width * 0.38f, height * 0.52f, width * 0.42f, height * 0.42f, width * 0.5f, height * 0.35f)
        close()
    }
    drawPath(innerFlame, color.copy(alpha = 0.62f))
}

private fun DrawScope.drawClockIcon(color: Color) {
    val width = size.width
    val height = size.height
    val centerX = width * 0.5f
    val centerY = height * 0.5f
    val radius = width * 0.4f

    drawCircle(
        color = color.copy(alpha = 0.15f),
        radius = radius,
        center = Offset(centerX, centerY)
    )
    drawCircle(
        color = color,
        radius = radius,
        center = Offset(centerX, centerY),
        style = Stroke(width = width * 0.06f)
    )

    drawLine(
        color = color,
        start = Offset(centerX, centerY),
        end = Offset(centerX, centerY - radius * 0.5f),
        strokeWidth = width * 0.08f,
        cap = StrokeCap.Round
    )

    drawLine(
        color = color,
        start = Offset(centerX, centerY),
        end = Offset(centerX + radius * 0.6f, centerY),
        strokeWidth = width * 0.05f,
        cap = StrokeCap.Round
    )

    drawCircle(
        color = color,
        radius = width * 0.05f,
        center = Offset(centerX, centerY)
    )
}

private fun DrawScope.drawPaletteIcon(color: Color) {
    val width = size.width
    val height = size.height

    val palettePath = Path().apply {
        moveTo(width * 0.5f, height * 0.15f)
        cubicTo(width * 0.75f, height * 0.1f, width * 0.9f, height * 0.3f, width * 0.85f, height * 0.55f)
        cubicTo(width * 0.8f, height * 0.75f, width * 0.6f, height * 0.9f, width * 0.4f, height * 0.85f)
        cubicTo(width * 0.2f, height * 0.8f, width * 0.1f, height * 0.6f, width * 0.15f, height * 0.4f)
        cubicTo(width * 0.2f, height * 0.2f, width * 0.35f, height * 0.12f, width * 0.5f, height * 0.15f)
        close()
    }
    drawPath(palettePath, color.copy(alpha = 0.2f))
    drawPath(palettePath, color, style = Stroke(width = width * 0.04f))

    val dotRadius = width * 0.08f
    drawCircle(color = Color(0xFFEF5350), radius = dotRadius, center = Offset(width * 0.35f, height * 0.35f))
    drawCircle(color = Color(0xFF42A5F5), radius = dotRadius, center = Offset(width * 0.55f, height * 0.3f))
    drawCircle(color = Color(0xFF66BB6A), radius = dotRadius, center = Offset(width * 0.65f, height * 0.45f))
    drawCircle(color = Color(0xFFFFC107), radius = dotRadius, center = Offset(width * 0.6f, height * 0.6f))
}

private fun DrawScope.drawCloudIcon(color: Color) {
    val width = size.width
    val height = size.height

    val cloudPath = Path().apply {
        moveTo(width * 0.25f, height * 0.55f)
        cubicTo(width * 0.15f, height * 0.55f, width * 0.1f, height * 0.45f, width * 0.2f, height * 0.4f)
        cubicTo(width * 0.15f, height * 0.3f, width * 0.25f, height * 0.2f, width * 0.35f, height * 0.25f)
        cubicTo(width * 0.4f, height * 0.15f, width * 0.6f, height * 0.15f, width * 0.65f, height * 0.25f)
        cubicTo(width * 0.75f, height * 0.2f, width * 0.85f, height * 0.3f, width * 0.8f, height * 0.4f)
        cubicTo(width * 0.9f, height * 0.45f, width * 0.85f, height * 0.55f, width * 0.75f, height * 0.55f)
        close()
    }
    drawPath(cloudPath, color.copy(alpha = 0.3f))
    drawPath(cloudPath, color, style = Stroke(width = width * 0.04f))

    val dropColor = color.copy(alpha = 0.62f)
    drawLine(
        color = dropColor,
        start = Offset(width * 0.35f, height * 0.65f),
        end = Offset(width * 0.35f, height * 0.8f),
        strokeWidth = width * 0.04f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = dropColor,
        start = Offset(width * 0.5f, height * 0.7f),
        end = Offset(width * 0.5f, height * 0.85f),
        strokeWidth = width * 0.04f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = dropColor,
        start = Offset(width * 0.65f, height * 0.65f),
        end = Offset(width * 0.65f, height * 0.8f),
        strokeWidth = width * 0.04f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawCompassIcon(color: Color) {
    val width = size.width
    val height = size.height
    val centerX = width * 0.5f
    val centerY = height * 0.5f
    val radius = width * 0.4f

    drawCircle(
        color = color.copy(alpha = 0.15f),
        radius = radius,
        center = Offset(centerX, centerY)
    )
    drawCircle(
        color = color,
        radius = radius,
        center = Offset(centerX, centerY),
        style = Stroke(width = width * 0.04f)
    )

    val northPath = Path().apply {
        moveTo(centerX, centerY - radius * 0.7f)
        lineTo(centerX - radius * 0.15f, centerY)
        lineTo(centerX, centerY - radius * 0.2f)
        close()
    }
    drawPath(northPath, Color(0xFFEF5350))

    val southPath = Path().apply {
        moveTo(centerX, centerY + radius * 0.7f)
        lineTo(centerX + radius * 0.15f, centerY)
        lineTo(centerX, centerY + radius * 0.2f)
        close()
    }
    drawPath(southPath, color.copy(alpha = 0.52f))

    drawCircle(
        color = color,
        radius = width * 0.04f,
        center = Offset(centerX, centerY)
    )
}

private fun DrawScope.drawGemIcon(color: Color) {
    val width = size.width
    val height = size.height

    val topPath = Path().apply {
        moveTo(width * 0.5f, height * 0.1f)
        lineTo(width * 0.25f, height * 0.35f)
        lineTo(width * 0.5f, height * 0.45f)
        lineTo(width * 0.75f, height * 0.35f)
        close()
    }
    drawPath(topPath, color.copy(alpha = 0.4f))

    val bottomPath = Path().apply {
        moveTo(width * 0.25f, height * 0.35f)
        lineTo(width * 0.5f, height * 0.9f)
        lineTo(width * 0.75f, height * 0.35f)
        lineTo(width * 0.5f, height * 0.45f)
        close()
    }
    drawPath(bottomPath, color.copy(alpha = 0.62f))

    val outlinePath = Path().apply {
        moveTo(width * 0.5f, height * 0.1f)
        lineTo(width * 0.25f, height * 0.35f)
        lineTo(width * 0.5f, height * 0.9f)
        lineTo(width * 0.75f, height * 0.35f)
        close()
    }
    drawPath(outlinePath, color, style = Stroke(width = width * 0.04f))
}

private fun DrawScope.drawCrownIcon(color: Color) {
    val width = size.width
    val height = size.height

    val crownPath = Path().apply {
        moveTo(width * 0.1f, height * 0.65f)
        lineTo(width * 0.2f, height * 0.3f)
        lineTo(width * 0.35f, height * 0.5f)
        lineTo(width * 0.5f, height * 0.2f)
        lineTo(width * 0.65f, height * 0.5f)
        lineTo(width * 0.8f, height * 0.3f)
        lineTo(width * 0.9f, height * 0.65f)
        close()
    }
    drawPath(crownPath, color.copy(alpha = 0.3f))
    drawPath(crownPath, color, style = Stroke(width = width * 0.04f))

    drawRoundRect(
        color = color.copy(alpha = 0.42f),
        topLeft = Offset(width * 0.1f, height * 0.65f),
        size = Size(width * 0.8f, height * 0.15f),
        cornerRadius = CornerRadius(width * 0.06f)
    )
}

@Composable
fun LockIcon(modifier: Modifier = Modifier) {
    val size = 28.dp
    val color = Color(0xFF9A9389)

    Canvas(modifier = modifier.size(size)) {
        val width = this.size.width
        val height = this.size.height

        drawRoundRect(
            color = color.copy(alpha = 0.22f),
            topLeft = Offset(width * 0.2f, height * 0.45f),
            size = Size(width * 0.6f, height * 0.45f),
            cornerRadius = CornerRadius(width * 0.08f)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(width * 0.2f, height * 0.45f),
            size = Size(width * 0.6f, height * 0.45f),
            cornerRadius = CornerRadius(width * 0.08f),
            style = Stroke(width = width * 0.04f)
        )

        val shacklePath = Path().apply {
            moveTo(width * 0.35f, height * 0.45f)
            lineTo(width * 0.35f, height * 0.3f)
            cubicTo(width * 0.35f, height * 0.15f, width * 0.65f, height * 0.15f, width * 0.65f, height * 0.3f)
            lineTo(width * 0.65f, height * 0.45f)
        }
        drawPath(
            shacklePath,
            color,
            style = Stroke(width = width * 0.06f, cap = StrokeCap.Round)
        )

        drawCircle(
            color = color.copy(alpha = 0.55f),
            radius = width * 0.06f,
            center = Offset(width * 0.5f, height * 0.6f)
        )
    }
}
