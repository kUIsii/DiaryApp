package com.diary.app.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.data.DiaryEntry
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.moodColorForLevel
import com.diary.app.ui.components.moodIconForLevel
import com.diary.app.ui.components.moodLabelForLevel
import com.diary.app.ui.components.rememberHapticFeedback
import com.diary.app.ui.components.weatherIconFor
import com.diary.app.ui.home.TagInfo
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MapScreen(
    onNavigateToDetail: (Long) -> Unit = {},
    onNavigateToEditor: (Long?) -> Unit = {},
    viewModel: TimelineViewModel = viewModel()
) {
    val haptic = rememberHapticFeedback()
    val monthGroups by viewModel.monthGroups.collectAsState()
    val tagsMap by viewModel.tagsMap.collectAsState()
    val totalEntries = remember(monthGroups) { monthGroups.sumOf { it.entries.size } }

    GradientBackground {
        if (monthGroups.isEmpty()) {
            TimelineEmptyState(onNavigateToEditor = onNavigateToEditor)
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
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "共 ${totalEntries} 篇日记",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                monthGroups.forEachIndexed { monthIndex, group ->
                    // Month header card
                    item {
                        MonthHeader(
                            yearMonth = group.yearMonth,
                            entryCount = group.entries.size
                        )
                    }

                    // Timeline items with stagger animation
                    itemsIndexed(group.entries) { entryIndex, entry ->
                        AnimatedTimelineItem(
                            index = entryIndex,
                            entry = entry,
                            tags = tagsMap[entry.id] ?: emptyList(),
                            onClick = {
                                haptic.click()
                                onNavigateToDetail(entry.id)
                            }
                        )
                    }

                    // Month divider between groups
                    if (monthIndex < monthGroups.lastIndex) {
                        item { MonthDivider() }
                    }

                    // Spacer between month groups
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                // Bottom padding
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun MonthHeader(yearMonth: YearMonth, entryCount: Int) {
    val accentColor = MaterialTheme.colorScheme.primary
    GlassCard(
        cornerRadius = 20.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with subtle background
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "${yearMonth.year}年${yearMonth.monthValue}月",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "$entryCount 篇日记",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            // Entry count badge with gradient
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$entryCount",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
        }
    }
}

@Composable
private fun MonthDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left dashed line
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
        ) {
            drawLine(
                color = Color.Gray.copy(alpha = 0.25f),
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
            )
        }
        // Center diamond marker
        Box(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .size(8.dp)
                .rotate(45f)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    RoundedCornerShape(2.dp)
                )
        )
        // Right dashed line
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
        ) {
            drawLine(
                color = Color.Gray.copy(alpha = 0.25f),
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
            )
        }
    }
}

@Composable
private fun AnimatedTimelineItem(
    index: Int,
    entry: DiaryEntry,
    tags: List<TagInfo>,
    onClick: () -> Unit
) {
    var appeared by remember { mutableStateOf(false) }
    val delayMs = (index * 60L).coerceAtMost(600L)

    LaunchedEffect(Unit) {
        delay(delayMs)
        appeared = true
    }

    AnimatedVisibility(
        visible = appeared,
        enter = fadeIn(animationSpec = tween(400)) +
                slideInVertically(
                    animationSpec = tween(400),
                    initialOffsetY = { it / 5 }
                )
    ) {
        TimelineItem(
            entry = entry,
            tags = tags,
            onClick = onClick
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
            .padding(bottom = 14.dp)
    ) {
        // Timeline line + dot column
        Box(
            modifier = Modifier.width(32.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // Gradient dashed vertical line
            Canvas(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxSize()
            ) {
                drawLine(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.35f),
                            accentColor.copy(alpha = 0.05f)
                        )
                    ),
                    start = Offset(size.width / 2, 0f),
                    end = Offset(size.width / 2, size.height),
                    strokeWidth = size.width,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 5f), 0f)
                )
            }
            // Glow halo + mood dot
            Box(
                modifier = Modifier.padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer glow halo
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(dotColor.copy(alpha = 0.18f))
                )
                // Inner mood dot
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Content card with shadow
        Box(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    shadowElevation = 8.dp.toPx()
                    shape = RoundedCornerShape(20.dp)
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
        ) {
            GlassCard(cornerRadius = 20.dp) {
                Column {
                    // Date and time row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatTimelineDate(entry.createdAt),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
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
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = entry.plainText,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 22.sp
                        )
                    }

                    // Bottom info row: mood + weather + tags
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mood icon + label
                        if (entry.moodLevel != null) {
                            val (moodIcon, moodTint) = moodIconForLevel(entry.moodLevel)
                            Icon(
                                imageVector = moodIcon,
                                contentDescription = "心情",
                                tint = moodTint,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = moodLabelForLevel(entry.moodLevel),
                                fontSize = 12.sp,
                                color = moodTint,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(10.dp))
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
private fun TimelineEmptyState(onNavigateToEditor: (Long?) -> Unit = {}) {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    // Pulsing animation for hint text
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Timeline,
                contentDescription = null,
                tint = onSurfaceVariant.copy(alpha = 0.25f),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "时间线为空",
                fontSize = 22.sp,
                color = onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "写下第一篇日记，开始你的时间线",
                fontSize = 14.sp,
                color = onSurfaceVariant.copy(alpha = pulseAlpha),
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = { onNavigateToEditor(null) },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "写第一篇日记",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
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
