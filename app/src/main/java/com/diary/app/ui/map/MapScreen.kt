package com.diary.app.ui.map

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MoodBad
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.data.DiaryEntry
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.home.TagInfo
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MapScreen(
    onNavigateToEditor: (Long?) -> Unit = {},
    viewModel: TimelineViewModel = viewModel()
) {
    val monthGroups by viewModel.monthGroups.collectAsState()
    val tagsMap by viewModel.tagsMap.collectAsState()

    LaunchedEffect(monthGroups) {
        val allEntries = monthGroups.flatMap { it.entries }
        viewModel.loadTagsForEntries(allEntries)
    }

    GradientBackground {
        if (monthGroups.isEmpty()) {
            TimelineEmptyState()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Header
                item {
                    Text(
                        text = "时间线",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "回顾你的日记足迹",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                monthGroups.forEach { group ->
                    // Month header
                    item {
                        MonthHeader(
                            yearMonth = group.yearMonth,
                            entryCount = group.entries.size
                        )
                    }

                    // Timeline items
                    items(group.entries) { entry ->
                        TimelineItem(
                            entry = entry,
                            tags = tagsMap[entry.id] ?: emptyList(),
                            onClick = { onNavigateToEditor(entry.id) }
                        )
                    }

                    // Spacer between month groups
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Bottom padding
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun MonthHeader(yearMonth: YearMonth, entryCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${yearMonth.year}年${yearMonth.monthValue}月",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${entryCount}篇",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TimelineItem(
    entry: DiaryEntry,
    tags: List<TagInfo>,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "timelineItemScale"
    )

    val accentColor = MaterialTheme.colorScheme.primary
    val dotColor = if (entry.moodLevel != null) {
        moodColorForLevel(entry.moodLevel)
    } else {
        accentColor
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        // Timeline line + dot column
        Box(
            modifier = Modifier.width(32.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // Vertical line
            Canvas(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxSize()
            ) {
                drawLine(
                    color = accentColor.copy(alpha = 0.15f),
                    start = Offset(size.width / 2, 0f),
                    end = Offset(size.width / 2, size.height),
                    strokeWidth = size.width
                )
            }
            // Dot
            Box(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Content card
        GlassCard(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
        ) {
            Column {
                // Date and time row
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTimelineDate(entry.createdAt),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = formatTimelineTime(entry.createdAt),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                // Text preview
                if (entry.plainText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = entry.plainText,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                }

                // Bottom info row: mood + weather + tags
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mood icon
                    if (entry.moodLevel != null) {
                        val (moodIcon, moodTint) = moodIconForLevel(entry.moodLevel)
                        Icon(
                            imageVector = moodIcon,
                            contentDescription = "心情",
                            tint = moodTint,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Weather icon
                    if (entry.weather != null) {
                        val (weatherIcon, weatherTint) = weatherIconFor(entry.weather)
                        Icon(
                            imageVector = weatherIcon,
                            contentDescription = "天气",
                            tint = weatherTint,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Tags
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        tags.take(2).forEach { tag ->
                            TimelineTagChip(name = tag.name, color = tag.color)
                        }
                        if (tags.size > 2) {
                            Text(
                                text = "+${tags.size - 2}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineTagChip(name: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = name,
            fontSize = 11.sp,
            color = color,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun TimelineEmptyState() {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Timeline,
                contentDescription = null,
                tint = onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "时间线为空",
                fontSize = 18.sp,
                color = onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "写下第一篇日记，开始你的时间线",
                fontSize = 14.sp,
                color = onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

private data class IconWithTint(val icon: ImageVector, val tint: Color)

private fun moodIconForLevel(level: Int): IconWithTint {
    return when (level.coerceIn(1, 6)) {
        1 -> IconWithTint(Icons.Default.MoodBad, Color(0xFFE57373))
        2 -> IconWithTint(Icons.Default.SentimentDissatisfied, Color(0xFFFFB74D))
        3 -> IconWithTint(Icons.Default.SentimentNeutral, Color(0xFFFFF176))
        4 -> IconWithTint(Icons.Default.Mood, Color(0xFFAED581))
        5 -> IconWithTint(Icons.Default.SentimentSatisfied, Color(0xFF81C784))
        6 -> IconWithTint(Icons.Default.SentimentVerySatisfied, Color(0xFF4FC3F7))
        else -> IconWithTint(Icons.Default.SentimentNeutral, Color(0xFFFFF176))
    }
}

private fun moodColorForLevel(level: Int): Color {
    return when (level.coerceIn(1, 6)) {
        1 -> Color(0xFFE57373)
        2 -> Color(0xFFFFB74D)
        3 -> Color(0xFFFFF176)
        4 -> Color(0xFFAED581)
        5 -> Color(0xFF81C784)
        6 -> Color(0xFF4FC3F7)
        else -> Color(0xFFFFF176)
    }
}

private fun weatherIconFor(weather: String?): IconWithTint {
    return when (weather) {
        "晴", "晴天" -> IconWithTint(Icons.Default.WbSunny, Color(0xFFFFCA28))
        "多云" -> IconWithTint(Icons.Default.Cloud, Color(0xFF90A4AE))
        "阴", "阴天" -> IconWithTint(Icons.Default.CloudQueue, Color(0xFF78909C))
        "雨", "雨天" -> IconWithTint(Icons.Default.Umbrella, Color(0xFF64B5F6))
        "雷", "雷暴" -> IconWithTint(Icons.Default.Thunderstorm, Color(0xFFBA68C8))
        "风", "大风" -> IconWithTint(Icons.Default.Air, Color(0xFF80CBC4))
        else -> IconWithTint(Icons.Default.WbSunny, Color(0xFFFFCA28))
    }
}

private fun formatTimelineDate(timestamp: Long): String {
    val entryDate = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    return when (entryDate) {
        today -> "今天"
        yesterday -> "昨天"
        else -> {
            val formatter = DateTimeFormatter.ofPattern("M月d日", Locale.getDefault())
            entryDate.format(formatter)
        }
    }
}

private fun formatTimelineTime(timestamp: Long): String {
    val localDateTime = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    return localDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
}
