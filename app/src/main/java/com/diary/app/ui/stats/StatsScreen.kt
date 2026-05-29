package com.diary.app.ui.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.moodIconForLevel
import com.diary.app.ui.components.weatherIconForType
import kotlin.math.roundToInt

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    GradientBackground {
        if (state.totalEntries == 0) {
            EmptyState()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
            ) {
                // Header
                item {
                    Text(
                        text = "统计",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // Overview cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OverviewCard(
                            label = "总日记",
                            value = state.totalEntries,
                            icon = Icons.Default.Edit,
                            gradientColors = listOf(Color(0xFF667eea), Color(0xFF764ba2)),
                            modifier = Modifier.weight(1f)
                        )
                        OverviewCard(
                            label = "连续天数",
                            value = state.currentStreak,
                            icon = Icons.Default.LocalFireDepartment,
                            gradientColors = listOf(Color(0xFFf093fb), Color(0xFFf5576c)),
                            modifier = Modifier.weight(1f)
                        )
                        OverviewCard(
                            label = "本月",
                            value = state.thisMonthEntries,
                            icon = Icons.Default.CalendarMonth,
                            gradientColors = listOf(Color(0xFF4facfe), Color(0xFF00f2fe)),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Writing calendar heatmap
                item {
                    WritingCalendarHeatmap(moodPoints = state.moodTrendPoints)
                }

                // Word count stats
                state.wordStats?.let { wordStats ->
                    item {
                        SectionTitle(text = "文字统计")
                        Spacer(modifier = Modifier.height(8.dp))
                        GlassCard {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    WordStatItem(
                                        icon = Icons.Default.TextSnippet,
                                        label = "总字数",
                                        value = formatWordCount(wordStats.totalWords)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(40.dp)
                                            .background(
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                                            )
                                    )
                                    WordStatItem(
                                        icon = Icons.Default.Edit,
                                        label = "篇均字数",
                                        value = "${wordStats.avgWordsPerEntry}"
                                    )
                                }
                                if (state.moodTrendPoints.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TextSnippet,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "最近30天写了 ${state.moodTrendPoints.size} 篇日记",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Mood trend
                state.moodTrend?.let { moodTrend ->
                    item {
                        SectionTitle(text = "心情趋势")
                        Spacer(modifier = Modifier.height(8.dp))
                        GlassCard {
                            MoodTrendRow(moodTrend)
                        }
                    }
                }

                // Mood line chart
                if (state.moodTrendPoints.size >= 2) {
                    item {
                        SectionTitle(text = "心情曲线")
                        Spacer(modifier = Modifier.height(8.dp))
                        GlassCard {
                            MoodLineChart(points = state.moodTrendPoints)
                        }
                    }
                }

                // Monthly trend
                item {
                    SectionTitle(text = "近6个月趋势")
                    Spacer(modifier = Modifier.height(8.dp))
                    GlassCard {
                        MonthlyTrendChart(state.monthlyTrend)
                    }
                }

                // Writing habit
                state.writingHabit?.let { habit ->
                    item {
                        SectionTitle(text = "写作习惯")
                        Spacer(modifier = Modifier.height(8.dp))
                        WritingHabitSection(habit)
                    }
                }

                // Mood distribution
                item {
                    SectionTitle(text = "心情分布")
                    Spacer(modifier = Modifier.height(8.dp))
                    GlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            state.moodDistribution.forEach { mood ->
                                MoodBar(
                                    label = mood.label,
                                    count = mood.count,
                                    color = mood.color,
                                    maxCount = state.moodDistribution.maxOfOrNull { it.count } ?: 1,
                                    level = mood.level
                                )
                            }
                        }
                    }
                }

                // Weather statistics
                if (state.weatherDistribution.isNotEmpty()) {
                    item {
                        SectionTitle(text = "天气统计")
                        Spacer(modifier = Modifier.height(8.dp))
                        GlassCard {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                state.weatherDistribution.forEach { weather ->
                                    WeatherRow(
                                        type = weather.type,
                                        count = weather.count,
                                        icon = weatherIconForType(weather.type)
                                    )
                                }
                            }
                        }
                    }
                }

                // Tag statistics
                if (state.tagUsage.isNotEmpty()) {
                    item {
                        SectionTitle(text = "标签统计")
                        Spacer(modifier = Modifier.height(8.dp))
                        GlassCard {
                            val maxTagCount = state.tagUsage.maxOfOrNull { it.count } ?: 1
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                state.tagUsage.take(12).sortedByDescending { it.count }.forEach { tag ->
                                    TagRow(
                                        name = tag.name,
                                        color = Color(tag.color),
                                        count = tag.count,
                                        maxCount = maxTagCount
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom spacer
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.SelfImprovement,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "还没有日记",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "开始记录你的生活吧",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}

// ── Animated number counter ──

@Composable
private fun AnimatedCounter(
    targetValue: Int,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineLarge,
    fontWeight: FontWeight = FontWeight.Bold,
    color: Color = Color.White
) {
    var currentValue by remember { mutableIntStateOf(0) }
    LaunchedEffect(targetValue) {
        if (targetValue == 0) {
            currentValue = 0
            return@LaunchedEffect
        }
        val steps = 20
        val stepDelay = 30L
        for (i in 0..steps) {
            currentValue = (targetValue * i / steps)
            delay(stepDelay)
        }
        currentValue = targetValue
    }
    Text(
        text = "$currentValue",
        style = style,
        fontWeight = fontWeight,
        color = color,
        modifier = modifier
    )
}

// ── Overview card with gradient ──

@Composable
private fun OverviewCard(
    label: String,
    value: Int,
    icon: ImageVector,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(gradientColors))
            .padding(14.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = Color.White.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            AnimatedCounter(
                targetValue = value,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp),
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

// ── Writing calendar heatmap ──

@Composable
private fun WritingCalendarHeatmap(moodPoints: List<MoodPoint>) {
    val today = LocalDate.now()
    val weeksToShow = 12
    val daysToShow = weeksToShow * 7
    val startDate = today.minusDays((daysToShow - 1).toLong())
    val alignedStart = startDate.with(DayOfWeek.MONDAY)

    val entryDates = moodPoints.map { it.date }.toSet()

    val cellSize = 16.dp
    val cellGap = 3.dp
    val weekdays = listOf("一", "二", "三", "四", "五", "六", "日")
    val accentColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)

    GlassCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "写作日历",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "最近12周",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row {
                Column(
                    modifier = Modifier.padding(end = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(cellGap)
                ) {
                    weekdays.forEach { day ->
                        Box(
                            modifier = Modifier.size(cellSize),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                for (week in 0 until weeksToShow) {
                    Column(verticalArrangement = Arrangement.spacedBy(cellGap)) {
                        for (dayOfWeek in 0 until 7) {
                            val date = alignedStart.plusDays((week * 7 + dayOfWeek).toLong())
                            val hasEntry = entryDates.contains(date)
                            val isFuture = date.isAfter(today)
                            Box(
                                modifier = Modifier
                                    .size(cellSize)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        when {
                                            isFuture -> emptyColor.copy(alpha = 0.04f)
                                            hasEntry -> accentColor
                                            else -> emptyColor
                                        }
                                    )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(cellGap))
                }
            }
        }
    }
}

// ── Word count ──

@Composable
private fun WordStatItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatWordCount(count: Int): String {
    return when {
        count >= 10000 -> String.format("%.1f万", count / 10000.0)
        count >= 1000 -> String.format("%.1fk", count / 1000.0)
        else -> "$count"
    }
}

// ── Mood trend ──

@Composable
private fun MoodTrendRow(trend: MoodTrend) {
    val (icon, description) = when (trend.direction) {
        TrendDirection.UP -> Icons.Default.TrendingUp to "最近心情不错"
        TrendDirection.DOWN -> Icons.Default.TrendingDown to "最近心情低落"
        TrendDirection.FLAT -> Icons.Default.TrendingFlat to "心情平稳"
    }
    val iconTint = when (trend.direction) {
        TrendDirection.UP -> Color(0xFF66BB6A)
        TrendDirection.DOWN -> Color(0xFFE74C3C)
        TrendDirection.FLAT -> MaterialTheme.colorScheme.primary
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = iconTint
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            trend.recent30Avg?.let { avg ->
                Text(
                    text = "近30天平均心情 ${String.format("%.1f", avg)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Mood line chart with gradient fill ──

@Composable
private fun MoodLineChart(points: List<MoodPoint>) {
    if (points.size < 2) return

    val labelWidth = 36.dp
    val chartHeight = 160.dp
    val levelLabels = listOf("沮丧", "低落", "平静", "开心", "愉快", "兴奋")
    val dateFmt = DateTimeFormatter.ofPattern("M/d")

    val levelColorMap = mapOf(
        1 to Color(0xFFE74C3C),
        2 to Color(0xFFE67E22),
        3 to Color(0xFFF39C12),
        4 to Color(0xFF9CCC65),
        5 to Color(0xFF66BB6A),
        6 to Color(0xFF2E7D32),
    )

    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textAlpha = textColor.alpha
    val primaryColor = MaterialTheme.colorScheme.primary

    Row {
        Column(
            modifier = Modifier
                .width(labelWidth)
                .height(chartHeight),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            levelLabels.reversed().forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor,
                    fontSize = 9.sp
                )
            }
        }

        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(chartHeight)
        ) {
            val w = size.width
            val h = size.height
            val padTop = 4.dp.toPx()
            val padBottom = 18.dp.toPx()
            val chartH = h - padTop - padBottom

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
            val totalDays = ChronoUnit.DAYS.between(startDate, endDate)
                .coerceAtLeast(1)

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

            val avgLevel = points.map { it.level }.average().roundToInt().coerceIn(1, 6)
            val fillColor = levelColorMap[avgLevel] ?: primaryColor
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        fillColor.copy(alpha = 0.3f),
                        fillColor.copy(alpha = 0.05f)
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
                val segColor = lerpColor(c1, c2, 0.5f)

                drawPath(
                    path = segPath,
                    color = segColor,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // Data point dots
            pxPoints.forEach { p ->
                val dotColor = levelColorMap[p.level] ?: Color.Gray
                drawCircle(
                    color = Color.White,
                    radius = 5.5.dp.toPx(),
                    center = Offset(p.x, p.y)
                )
                drawCircle(
                    color = dotColor,
                    radius = 5.dp.toPx(),
                    center = Offset(p.x, p.y)
                )
            }

            // X-axis labels
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(
                    (textAlpha * 255).toInt(),
                    (textColor.red * 255).toInt(),
                    (textColor.green * 255).toInt(),
                    (textColor.blue * 255).toInt()
                )
                textSize = 9.sp.toPx()
                isAntiAlias = true
            }

            points.forEachIndexed { index, point ->
                if (index % 5 == 0 || index == points.lastIndex) {
                    val dayOffset = ChronoUnit.DAYS.between(startDate, point.date)
                    val x = if (totalDays > 0) dayOffset.toFloat() / totalDays * w else w / 2f
                    val label = point.date.format(dateFmt)
                    val textW = textPaint.measureText(label)
                    val drawX = (x - textW / 2f).coerceIn(0f, w - textW)
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        drawX,
                        h - 2.dp.toPx(),
                        textPaint
                    )
                }
            }
        }
    }
}

private fun lerpColor(c1: Color, c2: Color, fraction: Float): Color {
    val r = c1.red + (c2.red - c1.red) * fraction
    val g = c1.green + (c2.green - c1.green) * fraction
    val b = c1.blue + (c2.blue - c1.blue) * fraction
    val a = c1.alpha + (c2.alpha - c1.alpha) * fraction
    return Color(r, g, b, a)
}

// ── Monthly trend chart with rounded bars ──

@Composable
private fun MonthlyTrendChart(data: List<MonthTrend>) {
    if (data.isEmpty()) return
    val maxCount = data.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEachIndexed { index, month ->
                val fraction = month.count.toFloat() / maxCount
                val animatedFraction by animateFloatAsState(
                    targetValue = fraction,
                    animationSpec = tween(durationMillis = 600, delayMillis = index * 80),
                    label = "monthBar"
                )
                val isCurrentMonth = index == data.lastIndex
                val barColor = if (isCurrentMonth)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    if (month.count > 0) {
                        Text(
                            text = "${month.count}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .fillMaxHeight(animatedFraction.coerceAtLeast(0.02f))
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(barColor.copy(alpha = 0.5f), barColor)
                                )
                            )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.forEach { month ->
                Text(
                    text = month.month,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── Writing habit with individual cards ──

@Composable
private fun WritingHabitSection(habit: WritingHabit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HabitCard(
            icon = Icons.Default.Edit,
            label = "每周写作",
            value = "${String.format("%.1f", habit.avgPerWeek)} 篇",
            modifier = Modifier.weight(1f)
        )
        HabitCard(
            icon = Icons.Default.Weekend,
            label = "最活跃日",
            value = habit.mostActiveDay,
            modifier = Modifier.weight(1f)
        )
        HabitCard(
            icon = Icons.Default.Schedule,
            label = "活跃时段",
            value = habit.mostActiveTime,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HabitCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    GlassCard(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(primaryColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = primaryColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Mood bar with icon and gradient ──

@Composable
private fun MoodBar(
    label: String,
    count: Int,
    color: Color,
    maxCount: Int,
    level: Int
) {
    val fraction = if (maxCount > 0) count.toFloat() / maxCount else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 600),
        label = "moodBar"
    )

    val iconData = moodIconForLevel(level)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = iconData.icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = iconData.tint
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(32.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(color.copy(alpha = 0.6f), color)
                        )
                    )
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(28.dp)
        )
    }
}

@Composable
private fun WeatherRow(
    type: String,
    count: Int,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = type,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$count 篇",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Tag row with progress bar ──

@Composable
private fun TagRow(
    name: String,
    color: Color,
    count: Int,
    maxCount: Int
) {
    val fraction = if (maxCount > 0) count.toFloat() / maxCount else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 500),
        label = "tagBar"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(60.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color.copy(alpha = 0.6f))
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp),
            textAlign = TextAlign.End
        )
    }
}
