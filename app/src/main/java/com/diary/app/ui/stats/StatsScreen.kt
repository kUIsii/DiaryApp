package com.diary.app.ui.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MoodBad
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
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
                            value = "${state.totalEntries}",
                            modifier = Modifier.weight(1f)
                        )
                        OverviewCard(
                            label = "连续天数",
                            value = "${state.currentStreak}",
                            modifier = Modifier.weight(1f)
                        )
                        OverviewCard(
                            label = "本月",
                            value = "${state.thisMonthEntries}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Word count stats
                state.wordStats?.let { wordStats ->
                    item {
                        SectionTitle(text = "文字统计")
                        Spacer(modifier = Modifier.height(8.dp))
                        GlassCard {
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
                        GlassCard {
                            WritingHabitSection(habit)
                        }
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
                                    icon = moodIconForLevel(mood.level)
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
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                state.tagUsage.take(12).forEach { tag ->
                                    TagChip(
                                        name = tag.name,
                                        color = Color(tag.color),
                                        count = tag.count
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

@Composable
private fun OverviewCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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

// ── Monthly trend chart ──

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
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    if (month.count > 0) {
                        Text(
                            text = "${month.count}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .fillMaxHeight(animatedFraction.coerceAtLeast(0.02f))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(barColor.copy(alpha = 0.6f), barColor)
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
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

// ── Writing habit ──

@Composable
private fun WritingHabitSection(habit: WritingHabit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HabitRow(
            icon = Icons.Default.Edit,
            label = "平均每周写作",
            value = "${String.format("%.1f", habit.avgPerWeek)} 篇"
        )
        HabitRow(
            icon = Icons.Default.Weekend,
            label = "最活跃的一天",
            value = habit.mostActiveDay
        )
        HabitRow(
            icon = Icons.Default.Schedule,
            label = "最活跃的时段",
            value = habit.mostActiveTime
        )
    }
}

@Composable
private fun HabitRow(
    icon: ImageVector,
    label: String,
    value: String
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
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Existing composables ──

@Composable
private fun MoodBar(
    label: String,
    count: Int,
    color: Color,
    maxCount: Int,
    icon: ImageVector
) {
    val fraction = if (maxCount > 0) count.toFloat() / maxCount else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 600),
        label = "moodBar"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = color
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
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.12f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(color.copy(alpha = 0.7f), color)
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

@Composable
private fun TagChip(
    name: String,
    color: Color,
    count: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun moodIconForLevel(level: Int): ImageVector = when (level) {
    1 -> Icons.Default.MoodBad
    2 -> Icons.Default.SentimentDissatisfied
    3 -> Icons.Default.SentimentNeutral
    4 -> Icons.Default.Mood
    5 -> Icons.Default.SentimentSatisfied
    6 -> Icons.Default.SentimentVerySatisfied
    else -> Icons.Default.SentimentNeutral
}

private fun weatherIconForType(type: String): ImageVector = when (type) {
    "晴" -> Icons.Default.WbSunny
    "多云" -> Icons.Default.Cloud
    "阴" -> Icons.Default.CloudQueue
    "雨" -> Icons.Default.Umbrella
    "雪" -> Icons.Default.AcUnit
    "风" -> Icons.Default.Air
    "雷雨" -> Icons.Default.Thunderstorm
    "炎热" -> Icons.Default.LocalFireDepartment
    else -> Icons.Default.WbSunny
}
