package com.diary.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.ui.stats.MoodPoint
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun MoodChart(
    points: List<MoodPoint>,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) return

    val chartHeight = 200.dp
    val dateFmt = DateTimeFormatter.ofPattern("M/d")
    val fullDateFmt = DateTimeFormatter.ofPattern("yyyy年M月d日")

    val levelColorMap = mapOf(
        1 to Color(0xFFE57373),
        2 to Color(0xFFFFB74D),
        3 to Color(0xFFFFF176),
        4 to Color(0xFFAED581),
        5 to Color(0xFF81C784),
        6 to Color(0xFF4FC3F7)
    )

    val levelLabels = listOf("沮丧", "低落", "平静", "开心", "愉快", "兴奋")
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Animation state
    var animationPlayed by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "progress"
    )

    // Selected point state
    var selectedIndex by remember { mutableIntStateOf(-1) }

    // Get primary color before Canvas (not composable context)
    val primaryColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(Unit) { animationPlayed = true }

    Column(modifier = modifier) {
        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "心情趋势",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "最近30天",
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Chart with labels
        Row {
            // Y-axis labels
            Column(
                modifier = Modifier
                    .width(36.dp)
                    .height(chartHeight),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                levelLabels.reversed().forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = textColor
                    )
                }
            }

            // Canvas chart
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(chartHeight)
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            // Find nearest point
                            val startDate = points.first().date
                            val endDate = points.last().date
                            val totalDays = ChronoUnit.DAYS.between(startDate, endDate).coerceAtLeast(1)

                            var nearestIdx = -1
                            var nearestDist = Float.MAX_VALUE

                            points.forEachIndexed { index, point ->
                                val dayOffset = ChronoUnit.DAYS.between(startDate, point.date)
                                val x = dayOffset.toFloat() / totalDays * size.width
                                val y = size.height * (1f - (point.level - 1) / 5f)
                                val dist = (offset - Offset(x, y)).getDistance()
                                if (dist < nearestDist && dist < 40f) {
                                    nearestDist = dist
                                    nearestIdx = index
                                }
                            }

                            selectedIndex = if (selectedIndex == nearestIdx) -1 else nearestIdx
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                val padTop = 8.dp.toPx()
                val padBottom = 20.dp.toPx()
                val chartH = h - padTop - padBottom

                // Grid lines
                for (level in 1..6) {
                    val y = padTop + chartH * (1f - (level - 1) / 5f)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val startDate = points.first().date
                val endDate = points.last().date
                val totalDays = ChronoUnit.DAYS.between(startDate, endDate).coerceAtLeast(1)

                data class PxPoint(val x: Float, val y: Float, val level: Int)

                val pxPoints = points.map { p ->
                    val dayOffset = ChronoUnit.DAYS.between(startDate, p.date)
                    val x = if (totalDays > 0) dayOffset.toFloat() / totalDays * w else w / 2f
                    val y = padTop + chartH * (1f - (p.level - 1) / 5f)
                    PxPoint(x, y, p.level)
                }

                // Gradient fill under curve
                val fillPath = Path().apply {
                    moveTo(pxPoints.first().x, h - padBottom)
                    for (i in 0 until pxPoints.size - 1) {
                        val p0 = pxPoints[(i - 1).coerceAtLeast(0)]
                        val p1 = pxPoints[i]
                        val p2 = pxPoints[i + 1]
                        val p3 = pxPoints[(i + 2).coerceAtMost(pxPoints.lastIndex)]

                        val cp1x = p1.x + (p2.x - p0.x) / 6f
                        val cp1y = p1.y + (p2.y - p0.y) / 6f
                        val cp2x = p2.x - (p3.x - p1.x) / 6f
                        val cp2y = p2.y - (p3.y - p1.y) / 6f

                        if (i == 0) lineTo(p1.x, p1.y)
                        cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
                    }
                    lineTo(pxPoints.last().x, h - padBottom)
                    close()
                }

                val avgLevel = points.map { it.level }.average().toInt().coerceIn(1, 6)
                val fillColor = levelColorMap[avgLevel] ?: primaryColor

                // Draw fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            fillColor.copy(alpha = 0.25f * progress),
                            fillColor.copy(alpha = 0.02f * progress)
                        ),
                        startY = padTop,
                        endY = h - padBottom
                    )
                )

                // Curve segments
                for (i in 0 until pxPoints.size - 1) {
                    val p0 = pxPoints[(i - 1).coerceAtLeast(0)]
                    val p1 = pxPoints[i]
                    val p2 = pxPoints[i + 1]
                    val p3 = pxPoints[(i + 2).coerceAtMost(pxPoints.lastIndex)]

                    val cp1x = p1.x + (p2.x - p0.x) / 6f
                    val cp1y = p1.y + (p2.y - p0.y) / 6f
                    val cp2x = p2.x - (p3.x - p1.x) / 6f
                    val cp2y = p2.y - (p3.y - p1.y) / 6f

                    val segPath = Path().apply {
                        moveTo(p1.x, p1.y)
                        cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
                    }

                    val c1 = levelColorMap[p1.level] ?: Color.Gray
                    val c2 = levelColorMap[p2.level] ?: Color.Gray
                    val segColor = lerpMoodColor(c1, c2, 0.5f)

                    drawPath(
                        path = segPath,
                        color = segColor.copy(alpha = progress),
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // Data point dots
                if (progress > 0.5f) {
                    val dotAlpha = ((progress - 0.5f) * 2f).coerceIn(0f, 1f)
                    pxPoints.forEachIndexed { index, p ->
                        val dotColor = levelColorMap[p.level] ?: Color.Gray
                        val isSelected = index == selectedIndex

                        // Glow for selected
                        if (isSelected) {
                            drawCircle(
                                color = dotColor.copy(alpha = 0.3f),
                                radius = 14.dp.toPx(),
                                center = Offset(p.x, p.y)
                            )
                        }

                        // Outer glow
                        drawCircle(
                            color = dotColor.copy(alpha = 0.2f * dotAlpha),
                            radius = if (isSelected) 10.dp.toPx() else 8.dp.toPx(),
                            center = Offset(p.x, p.y)
                        )

                        // White background
                        drawCircle(
                            color = Color.White.copy(alpha = dotAlpha),
                            radius = if (isSelected) 7.dp.toPx() else 5.dp.toPx(),
                            center = Offset(p.x, p.y)
                        )

                        // Colored center
                        drawCircle(
                            color = dotColor.copy(alpha = dotAlpha),
                            radius = if (isSelected) 5.5.dp.toPx() else 3.5.dp.toPx(),
                            center = Offset(p.x, p.y)
                        )
                    }
                }

                // X-axis labels
                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(
                        (textColor.alpha * 255).toInt(),
                        (textColor.red * 255).toInt(),
                        (textColor.green * 255).toInt(),
                        (textColor.blue * 255).toInt()
                    )
                    textSize = 9.sp.toPx()
                    isAntiAlias = true
                }

                points.forEachIndexed { index, point ->
                    if (index % 7 == 0 || index == points.lastIndex) {
                        val dayOffset = ChronoUnit.DAYS.between(startDate, point.date)
                        val x = dayOffset.toFloat() / totalDays * w
                        val label = point.date.format(dateFmt)
                        val textW = textPaint.measureText(label)
                        val drawX = (x - textW / 2f).coerceIn(0f, w - textW)
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            drawX,
                            h - 4.dp.toPx(),
                            textPaint
                        )
                    }
                }
            }
        }

        // Selected point info
        if (selectedIndex in points.indices) {
            val point = points[selectedIndex]
            val color = levelColorMap[point.level] ?: MaterialTheme.colorScheme.primary

            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.08f))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = point.date.format(fullDateFmt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "心情等级: ${levelLabels[point.level - 1]}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Mood indicator
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${point.level}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                }
            }
        }

        // Mood level legend
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            levelLabels.forEachIndexed { index, label ->
                val color = levelColorMap[index + 1] ?: Color.Gray
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun lerpMoodColor(c1: Color, c2: Color, fraction: Float): Color {
    val r = c1.red + (c2.red - c1.red) * fraction
    val g = c1.green + (c2.green - c1.green) * fraction
    val b = c1.blue + (c2.blue - c1.blue) * fraction
    val a = c1.alpha + (c2.alpha - c1.alpha) * fraction
    return Color(r, g, b, a)
}
