package com.diary.app.ui.stats

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.EmptyState
import com.diary.app.ui.components.moodIconForLevel
import com.diary.app.ui.components.weatherIconFor
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
            EmptyState(
                icon = Icons.Default.SelfImprovement,
                title = stringResource(R.string.stats_no_entries),
                subtitle = stringResource(R.string.stats_start_recording),
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
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

                // Overview cards - 2x2 grid
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OverviewCard(
                                label = stringResource(R.string.stat_this_month),
                                value = state.thisMonthEntries,
                                icon = Icons.Default.CalendarMonth,
                                gradientColors = listOf(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)),
                                modifier = Modifier.weight(1f)
                            )
                            OverviewCard(
                                label = "总字数",
                                value = state.wordStats?.totalWords ?: 0,
                                icon = Icons.Default.TextSnippet,
                                gradientColors = listOf(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f), MaterialTheme.colorScheme.primary),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Heatmap
                if (state.heatmapData.isNotEmpty()) {
                    item {
                        SectionTitle(text = "记录热力图")
                        Spacer(modifier = Modifier.height(8.dp))
                        GlassCard {
                            DiaryHeatmap(data = state.heatmapData)
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
                                    val (wIcon, wTint) = weatherIconFor(weather.type)
                                    WeatherRow(
                                        type = weather.type,
                                        count = weather.count,
                                        icon = wIcon,
                                        tint = wTint
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
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(gradientColors))
            .drawBehind {
                // Subtle static highlight at top
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            onPrimaryColor.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.3f, size.height * 0.2f),
                        radius = size.width * 0.6f
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
                modifier = Modifier.size(20.dp),
                tint = onPrimaryColor.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            AnimatedCounter(
                targetValue = value,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 24.sp),
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
    icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = type,
            modifier = Modifier.size(20.dp),
            tint = tint
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
private fun DiaryHeatmap(data: List<HeatmapDay>) {
    val cellSize = 14.dp
    val cellGap = 3.dp
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    // Group data by calendar week (Monday-based columns)
    val weeks = remember(data) {
        if (data.isEmpty()) return@remember emptyList()
        // Pad start to align with Monday (DayOfWeek.MONDAY = 1)
        val firstDayOfWeek = data.first().date.dayOfWeek.value // 1=Mon, 7=Sun
        val paddedStart = if (firstDayOfWeek > 1) {
            List(firstDayOfWeek - 1) { null } + data.map { it }
        } else {
            data.map { it }
        }
        // Chunk into weeks of 7
        paddedStart.chunked(7).map { week ->
            week.map { it }
        }
    }

    Column {
        // Heatmap grid - horizontal scroll
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(cellGap)
            ) {
                weeks.forEach { week ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(cellGap)
                    ) {
                        for (day in week) {
                            if (day != null) {
                                Box(
                                    modifier = Modifier
                                        .size(cellSize)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            if (day.count > 0) primaryColor.copy(alpha = 0.75f)
                                            else surfaceVariant.copy(alpha = 0.4f)
                                        )
                                )
                            } else {
                                // Empty cell for padding
                                Box(modifier = Modifier.size(cellSize))
                            }
                        }
                        // Fill remaining cells if week is incomplete
                        repeat(7 - week.size) {
                            Box(modifier = Modifier.size(cellSize))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "少",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            repeat(4) { level ->
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (level == 0) surfaceVariant.copy(alpha = 0.4f)
                            else primaryColor.copy(alpha = 0.2f + level * 0.2f)
                        )
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "多",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Stats summary
        val activeDays = data.count { it.count > 0 }
        val totalDays = data.size
        val percentage = if (totalDays > 0) (activeDays * 100 / totalDays) else 0
        Text(
            text = "过去一年有 $activeDays 天写了日记，记录率 $percentage%",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

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
