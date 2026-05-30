package com.diary.app.ui.stats

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.MoodChart
import com.diary.app.ui.components.WordCloud
import com.diary.app.ui.components.WritingHeatmap
import com.diary.app.ui.components.moodIconForLevel
import com.diary.app.ui.components.weatherIconForType
import androidx.compose.ui.res.stringResource
import com.diary.app.R
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
                        text = stringResource(R.string.stats_title),
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
                            label = stringResource(R.string.stat_total_diaries),
                            value = state.totalEntries,
                            icon = Icons.Default.Edit,
                            gradientColors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.weight(1f)
                        )
                        OverviewCard(
                            label = stringResource(R.string.stat_streak),
                            value = state.currentStreak,
                            icon = Icons.Default.LocalFireDepartment,
                            gradientColors = listOf(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.error.copy(alpha = 0.7f)),
                            modifier = Modifier.weight(1f)
                        )
                        OverviewCard(
                            label = stringResource(R.string.stat_this_month),
                            value = state.thisMonthEntries,
                            icon = Icons.Default.CalendarMonth,
                            gradientColors = listOf(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)),
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
                        SectionTitle(text = stringResource(R.string.stats_total_words))
                        Spacer(modifier = Modifier.height(8.dp))
                        GlassCard {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    WordStatItem(
                                        icon = Icons.Default.TextSnippet,
                                        label = stringResource(R.string.stats_total_words),
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
                                        label = stringResource(R.string.stats_avg_words),
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
                                            contentDescription = "文字统计",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.stats_recent_entries, state.moodTrendPoints.size),
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
                        SectionTitle(text = stringResource(R.string.stats_mood_trend))
                        Spacer(modifier = Modifier.height(8.dp))
                        GlassCard {
                            MoodTrendRow(moodTrend)
                        }
                    }
                }

                // Mood line chart
                if (state.moodTrendPoints.size >= 2) {
                    item {
                        SectionTitle(text = stringResource(R.string.stats_mood_chart))
                        Spacer(modifier = Modifier.height(8.dp))
                        GlassCard {
                            MoodLineChart(points = state.moodTrendPoints)
                        }
                    }
                }

                // Enhanced Mood Chart with interaction
                if (state.moodTrendPoints.size >= 2) {
                    item {
                        GlassCard {
                            MoodChart(points = state.moodTrendPoints)
                        }
                    }
                }

                // Writing Heatmap (365 days)
                if (state.dailyWordCounts.isNotEmpty()) {
                    item {
                        GlassCard {
                            WritingHeatmap(dailyWordCounts = state.dailyWordCounts)
                        }
                    }
                }

                // Word Cloud
                if (state.wordFrequency.isNotEmpty()) {
                    item {
                        GlassCard {
                            WordCloud(
                                words = state.wordFrequency,
                                onWordClick = { _ ->
                                    // Navigate to search with the word
                                    // For now, we can just show the word
                                }
                            )
                        }
                    }
                }

                // Monthly trend
                item {
                    SectionTitle(text = stringResource(R.string.stats_6month_trend))
                    Spacer(modifier = Modifier.height(8.dp))
                    GlassCard {
                        MonthlyTrendChart(state.monthlyTrend)
                    }
                }

                // Writing habit
                state.writingHabit?.let { habit ->
                    item {
                        SectionTitle(text = stringResource(R.string.stats_writing_habit))
                        Spacer(modifier = Modifier.height(8.dp))
                        WritingHabitSection(habit)
                    }
                }

                // Mood distribution
                item {
                    SectionTitle(text = stringResource(R.string.stats_mood_distribution))
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
                        SectionTitle(text = stringResource(R.string.stats_weather))
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
                        SectionTitle(text = stringResource(R.string.stats_tags))
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
                contentDescription = "还没有日记",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.stats_no_entries),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.stats_start_recording),
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
    color: Color = Color.Unspecified
) {
    var animatedValue by remember { mutableFloatStateOf(0f) }
    val animatedFloat by animateFloatAsState(
        targetValue = animatedValue,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "counter"
    )

    LaunchedEffect(targetValue) {
        animatedValue = targetValue.toFloat()
    }

    Text(
        text = "${animatedFloat.toInt()}",
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
    // Shimmer animation
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )
    // Subtle glow pulse
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(gradientColors))
            .drawBehind {
                // Subtle glow at top
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            onPrimaryColor.copy(alpha = glowAlpha),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.3f, size.height * 0.2f),
                        radius = size.width * 0.6f
                    ),
                    size = size
                )
                // Shimmer stripe
                val shimmerWidth = size.width * 0.4f
                val shimmerX = shimmerOffset * (size.width + shimmerWidth) - shimmerWidth
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            onPrimaryColor.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        startX = shimmerX,
                        endX = shimmerX + shimmerWidth
                    ),
                    size = size
                )
            }
            .padding(14.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = onPrimaryColor.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            AnimatedCounter(
                targetValue = value,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp),
                fontWeight = FontWeight.Bold,
                color = onPrimaryColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = onPrimaryColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                    )
                )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Writing calendar heatmap ──

@Composable
private fun WritingCalendarHeatmap(moodPoints: List<MoodPoint>) {
    val today = LocalDate.now()
    val weeksToShow = 12
    val daysToShow = weeksToShow * 7
    val startDate = today.minusDays((daysToShow - 1).toLong())
    val alignedStart = startDate.with(DayOfWeek.MONDAY)

    // Count entries per date for intensity levels
    val entryDateCounts = moodPoints.groupBy { it.date }.mapValues { it.value.size }
    val maxCount = entryDateCounts.values.maxOrNull()?.coerceAtLeast(1) ?: 1

    val cellSize = 16.dp
    val cellGap = 3.dp
    val weekdays = listOf("一", "二", "三", "四", "五", "六", "日")
    val accentColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)

    // Fade-in animation
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "heatmapAlpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 12f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "heatmapOffset"
    )
    LaunchedEffect(Unit) { visible = true }

    GlassCard(
        modifier = Modifier
            .graphicsLayer {
                this.alpha = alpha
                translationY = offsetY
            }
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.stats_writing_calendar),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.stats_recent_12_weeks),
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
                            val count = entryDateCounts[date] ?: 0
                            val isFuture = date.isAfter(today)
                            val intensity = when {
                                isFuture -> 0f
                                count == 0 -> 0f
                                count == 1 -> 0.3f
                                count == 2 -> 0.6f
                                else -> 1f
                            }
                            Box(
                                modifier = Modifier
                                    .size(cellSize)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        when {
                                            isFuture -> emptyColor.copy(alpha = 0.04f)
                                            intensity > 0f -> accentColor.copy(alpha = intensity)
                                            else -> emptyColor
                                        }
                                    )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(cellGap))
                }
            }
            // Intensity legend
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.stats_less),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                listOf(0f, 0.3f, 0.6f, 1f).forEach { intensity ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (intensity == 0f) emptyColor
                                else accentColor.copy(alpha = intensity)
                            )
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = stringResource(R.string.stats_more),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
            contentDescription = label,
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
        TrendDirection.UP -> Icons.Default.TrendingUp to stringResource(R.string.stats_mood_up)
        TrendDirection.DOWN -> Icons.Default.TrendingDown to stringResource(R.string.stats_mood_down)
        TrendDirection.FLAT -> Icons.Default.TrendingFlat to stringResource(R.string.stats_mood_flat)
    }
    val iconTint = when (trend.direction) {
        TrendDirection.UP -> com.diary.app.ui.theme.SuccessColor
        TrendDirection.DOWN -> com.diary.app.ui.theme.ErrorColor
        TrendDirection.FLAT -> MaterialTheme.colorScheme.primary
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
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
                    text = stringResource(R.string.stats_mood_avg, String.format("%.1f", avg)),
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
        1 to com.diary.app.ui.theme.MoodDepressed.first,
        2 to com.diary.app.ui.theme.MoodDown.first,
        3 to com.diary.app.ui.theme.MoodCalm.first,
        4 to com.diary.app.ui.theme.MoodHappy.first,
        5 to com.diary.app.ui.theme.MoodCheerful.first,
        6 to com.diary.app.ui.theme.MoodExcited.first,
    )

    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textAlpha = textColor.alpha
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val fallbackColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Progressive draw animation
    var drawProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = drawProgress,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "lineProgress"
    )
    LaunchedEffect(Unit) { drawProgress = 1f }

    // Dot fade-in
    val dotAlpha by animateFloatAsState(
        targetValue = if (drawProgress > 0.9f) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "dotAlpha"
    )

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

            // Progressive clip: only draw up to animatedProgress
            val clipX = w * animatedProgress

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

            // Clip fill to progress
            clipRect(left = 0f, top = 0f, right = clipX, bottom = h) {
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
            }

            // Curve segments (clipped to progress)
            clipRect(left = 0f, top = 0f, right = clipX, bottom = h) {
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

                    val c1 = levelColorMap[p1.level] ?: fallbackColor
                    val c2 = levelColorMap[p2.level] ?: fallbackColor
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
            }

            // Data point dots with glow (faded in after line completes)
            if (dotAlpha > 0f) {
                pxPoints.forEach { p ->
                    val dotColor = levelColorMap[p.level] ?: fallbackColor
                    // Outer glow
                    drawCircle(
                        color = dotColor.copy(alpha = 0.25f * dotAlpha),
                        radius = 9.dp.toPx(),
                        center = Offset(p.x, p.y)
                    )
                    // Background
                    drawCircle(
                        color = surfaceColor.copy(alpha = dotAlpha),
                        radius = 5.5.dp.toPx(),
                        center = Offset(p.x, p.y)
                    )
                    // Colored center
                    drawCircle(
                        color = dotColor.copy(alpha = dotAlpha),
                        radius = 4.5.dp.toPx(),
                        center = Offset(p.x, p.y)
                    )
                }
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
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            // Background grid lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridColor = gridLineColor
                for (i in 0..3) {
                    val y = size.height * i / 3f
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEachIndexed { index, month ->
                    val fraction = month.count.toFloat() / maxCount
                    val animatedFraction by animateFloatAsState(
                        targetValue = fraction,
                        animationSpec = tween(
                            durationMillis = 700,
                            delayMillis = index * 100,
                            easing = FastOutSlowInEasing
                        ),
                        label = "monthBar"
                    )
                    val isCurrentMonth = index == data.lastIndex

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
                                    if (isCurrentMonth) {
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                primaryColor.copy(alpha = 0.6f),
                                                primaryColor
                                            )
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                primaryColor.copy(alpha = 0.12f),
                                                primaryColor.copy(alpha = 0.28f)
                                            )
                                        )
                                    }
                                )
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.forEachIndexed { index, month ->
                val isCurrentMonth = index == data.lastIndex
                Text(
                    text = month.month,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isCurrentMonth) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isCurrentMonth) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
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
            label = stringResource(R.string.stats_weekly_writing),
            value = "${String.format("%.1f", habit.avgPerWeek)} 篇",
            modifier = Modifier.weight(1f)
        )
        HabitCard(
            icon = Icons.Default.Weekend,
            label = stringResource(R.string.stats_most_active_day),
            value = habit.mostActiveDay,
            modifier = Modifier.weight(1f)
        )
        HabitCard(
            icon = Icons.Default.Schedule,
            label = stringResource(R.string.stats_active_time),
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
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.15f),
                                primaryColor.copy(alpha = 0.06f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
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
    val totalCount = remember(maxCount) { maxCount }
    val fraction = if (totalCount > 0) count.toFloat() / totalCount else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "moodBar"
    )

    val iconData = moodIconForLevel(level)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = iconData.icon,
            contentDescription = label,
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
                .background(color.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(color.copy(alpha = 0.5f), color)
                        )
                    )
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.width(40.dp)
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${(animatedFraction * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
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
            contentDescription = type,
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
            text = stringResource(R.string.stats_entries_count, count),
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
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "tagBar"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    Brush.linearGradient(listOf(color, color.copy(alpha = 0.7f)))
                )
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
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(color.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(color.copy(alpha = 0.4f), color)
                        )
                    )
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
