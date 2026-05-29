package com.diary.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.ui.stats.DailyWordCount
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Composable
fun WritingHeatmap(
    dailyWordCounts: List<DailyWordCount>,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val startDate = today.minusDays(364)
    val alignedStart = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    // Create a map of date -> word count
    val wordCountMap = remember(dailyWordCounts) {
        dailyWordCounts.associate { it.date to it.wordCount }
    }

    val maxWordCount = remember(dailyWordCounts) {
        dailyWordCounts.maxOfOrNull { it.wordCount }?.coerceAtLeast(1) ?: 1
    }

    // Animation
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "alpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.95f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "scale"
    )

    LaunchedEffect(Unit) { visible = true }

    val accentColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.06f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val cellSize = 12.dp
    val cellGap = 3.dp
    val weekdays = listOf("一", "三", "五", "日")
    val monthLabels = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月")

    Column(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            scaleX = scale
            scaleY = scale
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "写作热力图",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "最近一年",
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Heatmap grid
        Row {
            // Weekday labels
            Column(
                modifier = Modifier.padding(end = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                content = {
                    weekdays.forEach { day ->
                        Box(
                            modifier = Modifier
                                .height(cellSize)
                                .padding(bottom = cellGap),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                color = textColor
                            )
                        }
                    }
                }
            )

            // Month labels and grid
            Column {
                // Month labels
                Row(modifier = Modifier.padding(bottom = 4.dp)) {
                    var currentMonth = -1
                    for (week in 0 until 53) {
                        val date = alignedStart.plusDays((week * 7).toLong())
                        val month = date.monthValue
                        if (month != currentMonth) {
                            currentMonth = month
                            Text(
                                text = monthLabels[month - 1],
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                color = textColor,
                                modifier = Modifier.width(cellSize + cellGap)
                            )
                        } else {
                            Spacer(modifier = Modifier.width(cellSize + cellGap))
                        }
                    }
                }

                // Grid
                Row {
                    for (week in 0 until 53) {
                        Column(verticalArrangement = Arrangement.spacedBy(cellGap)) {
                            for (dayOfWeek in 0 until 7) {
                                val date = alignedStart.plusDays((week * 7 + dayOfWeek).toLong())
                                val wordCount = wordCountMap[date] ?: 0
                                val isFuture = date.isAfter(today)

                                val intensity = when {
                                    isFuture -> 0f
                                    wordCount == 0 -> 0f
                                    wordCount <= maxWordCount * 0.25f -> 0.25f
                                    wordCount <= maxWordCount * 0.5f -> 0.5f
                                    wordCount <= maxWordCount * 0.75f -> 0.75f
                                    else -> 1f
                                }

                                Box(
                                    modifier = Modifier
                                        .size(cellSize)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            when {
                                                isFuture -> emptyColor.copy(alpha = 0.03f)
                                                intensity > 0f -> accentColor.copy(alpha = intensity * 0.8f)
                                                else -> emptyColor
                                            }
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Intensity legend
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "少",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                color = textColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { intensity ->
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (intensity == 0f) emptyColor
                            else accentColor.copy(alpha = intensity * 0.8f)
                        )
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "多",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                color = textColor
            )
        }

        // Summary stats
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = "总字数",
                value = formatTotalWords(dailyWordCounts.sumOf { it.wordCount })
            )
            StatItem(
                label = "写作天数",
                value = "${dailyWordCounts.count { it.wordCount > 0 }}"
            )
            StatItem(
                label = "日均字数",
                value = if (dailyWordCounts.isNotEmpty()) {
                    "${dailyWordCounts.sumOf { it.wordCount } / dailyWordCounts.size}"
                } else "0"
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
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

private fun formatTotalWords(count: Int): String {
    return when {
        count >= 10000 -> String.format("%.1f万", count / 10000.0)
        count >= 1000 -> String.format("%.1fk", count / 1000.0)
        else -> "$count"
    }
}
