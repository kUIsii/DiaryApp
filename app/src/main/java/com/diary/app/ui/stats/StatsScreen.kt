package com.diary.app.ui.stats

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
                                gradientColors = listOf(Color(0xFFB3D4FC), Color(0xFF81B4F8)),
                                modifier = Modifier.weight(1f)
                            )
                            OverviewCard(
                                label = stringResource(R.string.stat_streak),
                                value = state.currentStreak,
                                icon = Icons.Default.LocalFireDepartment,
                                gradientColors = listOf(Color(0xFFFFCCBC), Color(0xFFFFAB91)),
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
                                gradientColors = listOf(Color(0xFFC8E6C9), Color(0xFFA5D6A7)),
                                modifier = Modifier.weight(1f)
                            )
                            OverviewCard(
                                label = "总字数",
                                value = state.wordStats?.totalWords ?: 0,
                                icon = Icons.Default.TextSnippet,
                                gradientColors = listOf(Color(0xFFE1BEE7), Color(0xFFCE93D8)),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Heatmap
                if (state.heatmapData.isNotEmpty()) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
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
                                text = "记录热力图",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            // Range toggle
                            val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(surfaceVariant.copy(alpha = 0.3f))
                                    .padding(3.dp)
                            ) {
                                HeatmapToggleButton(
                                    text = "1个月",
                                    selected = state.heatmapRange == HeatmapRange.ONE_MONTH,
                                    onClick = { viewModel.toggleHeatmapRange() }
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                HeatmapToggleButton(
                                    text = "半年",
                                    selected = state.heatmapRange == HeatmapRange.SIX_MONTHS,
                                    onClick = { viewModel.toggleHeatmapRange() }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        GlassCard {
                            DiaryHeatmap(
                                data = state.heatmapData,
                                range = state.heatmapRange,
                            )
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

// ── Overview card with gradient ──

@Composable
private fun OverviewCard(
    label: String,
    value: Int,
    icon: ImageVector,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier
) {
    val contentColor = Color(0xFF37474F)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(gradientColors))
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(18.dp),
                    tint = contentColor.copy(alpha = 0.7f)
                )
                Text(
                    text = "$value",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp),
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.7f)
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

// ── Diary heatmap ──

@Composable
private fun DiaryHeatmap(
    data: List<HeatmapDay>,
    range: HeatmapRange,
) {
    when (range) {
        HeatmapRange.ONE_MONTH -> MonthlyHeatmap(data = data)
        HeatmapRange.SIX_MONTHS -> HalfYearHeatmap(data = data)
    }
}

@Composable
private fun MonthlyHeatmap(data: List<HeatmapDay>) {
    if (data.isEmpty()) return
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val today = LocalDate.now()

    // Group by weeks (Mon-Sun), pad start to align with Monday
    val weeks = remember(data) {
        val firstDate = data.first().date
        val daysToPad = (firstDate.dayOfWeek.value - 1 + 7) % 7
        val padded: List<HeatmapDay?> = List(daysToPad) { null } + data
        padded.chunked(7)
    }

    Column {
        // Day of week headers
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                Text(
                    text = day,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Calendar grid
        weeks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                for (i in 0 until 7) {
                    val day = week.getOrNull(i)
                    if (day != null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (day.count > 0) primaryColor
                                    else surfaceVariant.copy(alpha = 0.4f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${day.date.dayOfMonth}",
                                fontSize = 11.sp,
                                fontWeight = if (day.date == today) FontWeight.Bold else FontWeight.Normal,
                                color = if (day.count > 0) Color.White
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .padding(2.dp)
                        )
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
                text = "无记录",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(surfaceVariant.copy(alpha = 0.4f))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "有记录",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(primaryColor)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Stats summary
        val activeDays = data.count { it.count > 0 }
        val totalDays = data.size
        val percentage = if (totalDays > 0) (activeDays * 100 / totalDays) else 0
        Text(
            text = "最近30天有 $activeDays 天写了日记，记录率 $percentage%",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HalfYearHeatmap(data: List<HeatmapDay>) {
    val cellSize = 16.dp
    val cellGap = 4.dp
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val today = java.time.LocalDate.now()

    val weeks = remember(data) {
        if (data.isEmpty()) return@remember emptyList<List<HeatmapDay?>>()
        val firstDayOfWeek = data.first().date.dayOfWeek.value
        val paddedStart: List<HeatmapDay?> = if (firstDayOfWeek > 1) {
            List(firstDayOfWeek - 1) { null } + data
        } else {
            data
        }
        paddedStart.chunked(7)
    }

    // Month labels for each week column
    val monthLabels = remember(weeks) {
        val labels = mutableListOf<Pair<Int, String>>()
        var lastMonth = -1
        weeks.forEachIndexed { index, week ->
            val firstDay = week.firstOrNull { it != null }
            if (firstDay != null) {
                val month = firstDay.date.monthValue
                if (month != lastMonth) {
                    labels.add(index to "${month}月")
                    lastMonth = month
                }
            }
        }
        labels
    }

    Column {
        // Month labels row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Row {
                // Space for weekday labels
                Spacer(modifier = Modifier.width(20.dp))
                weeks.forEachIndexed { index, _ ->
                    val label = monthLabels.find { it.first == index }
                    Box(modifier = Modifier.width(cellSize + cellGap)) {
                        if (label != null) {
                            Text(
                                text = label.second,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Heatmap grid with weekday labels
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Row {
                // Weekday labels column
                Column(
                    modifier = Modifier.width(20.dp),
                    verticalArrangement = Arrangement.spacedBy(cellGap)
                ) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEachIndexed { index, label ->
                        Box(
                            modifier = Modifier.size(cellSize),
                            contentAlignment = Alignment.Center
                        ) {
                            if (index % 2 == 0) { // Show only Mon, Wed, Fri
                                Text(
                                    text = label,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                // Heatmap cells
                Row(
                    horizontalArrangement = Arrangement.spacedBy(cellGap)
                ) {
                    weeks.forEach { week ->
                        Column(verticalArrangement = Arrangement.spacedBy(cellGap)) {
                            for (day in week) {
                                if (day != null) {
                                    val isToday = day.date == today
                                    Box(
                                        modifier = Modifier
                                            .size(cellSize)
                                            .clip(RoundedCornerShape(3.dp))
                                            .then(
                                                if (isToday) Modifier.border(
                                                    1.5.dp,
                                                    primaryColor,
                                                    RoundedCornerShape(3.dp)
                                                ) else Modifier
                                            )
                                            .background(
                                                if (day.count > 0) primaryColor
                                                else surfaceVariant.copy(alpha = 0.5f)
                                            )
                                    )
                                } else {
                                    Box(modifier = Modifier.size(cellSize))
                                }
                            }
                            repeat(7 - week.size) {
                                Box(modifier = Modifier.size(cellSize))
                            }
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
                            if (level == 0) surfaceVariant.copy(alpha = 0.5f)
                            else primaryColor.copy(alpha = 0.3f + level * 0.2f)
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

        val activeDays = data.count { it.count > 0 }
        val totalDays = data.size
        val percentage = if (totalDays > 0) (activeDays * 100 / totalDays) else 0
        Text(
            text = "过去半年有 $activeDays 天写了日记，记录率 $percentage%",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HeatmapToggleButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
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
