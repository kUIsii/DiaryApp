package com.diary.app.ui.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
import com.diary.app.ui.components.moodEmojiForLevel
import com.diary.app.ui.components.moodIconForLevel
import com.diary.app.ui.components.moodLabelForLevel
import com.diary.app.ui.components.rememberHapticFeedback
import com.diary.app.ui.components.weatherIconFor
import com.diary.app.ui.home.TagInfo
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.DayOfWeek

@Composable
fun TimelineScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToEditor: (Long?) -> Unit,
    viewModel: TimelineViewModel = viewModel()
) {
    val haptic = rememberHapticFeedback()
    val entries by viewModel.entries.collectAsState()
    val tagsMap by viewModel.tagsMap.collectAsState()

    val listState = rememberLazyListState()

    // Group entries by date
    val groupedEntries = remember(entries) {
        entries.groupBy { entry ->
            Instant.ofEpochMilli(entry.createdAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }.toSortedMap(compareByDescending { it })
    }

    GradientBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Page header
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "时间线",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${entries.size} 篇",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Timeline entries grouped by date
                groupedEntries.forEach { (date, dayEntries) ->
                    item {
                        DateGroupHeader(
                            date = date,
                            entryCount = dayEntries.size
                        )
                    }

                    itemsIndexed(
                        items = dayEntries,
                        key = { _, entry -> entry.id }
                    ) { index, entry ->
                        TimelineEntryCard(
                            entry = entry,
                            tags = tagsMap[entry.id] ?: emptyList(),
                            isLast = index == dayEntries.size - 1,
                            onClick = {
                                haptic.click()
                                onNavigateToDetail(entry.id)
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Bottom padding
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }

            // FAB
            FAB(onClick = { onNavigateToEditor(null) })
        }
    }
}

@Composable
private fun DateGroupHeader(date: LocalDate, entryCount: Int) {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    val dateText = when (date) {
        today -> "今天"
        yesterday -> "昨天"
        else -> {
            val formatter = DateTimeFormatter.ofPattern("M月d日")
            val dayOfWeek = when (date.dayOfWeek) {
                DayOfWeek.MONDAY -> "周一"
                DayOfWeek.TUESDAY -> "周二"
                DayOfWeek.WEDNESDAY -> "周三"
                DayOfWeek.THURSDAY -> "周四"
                DayOfWeek.FRIDAY -> "周五"
                DayOfWeek.SATURDAY -> "周六"
                DayOfWeek.SUNDAY -> "周日"
            }
            "${date.format(formatter)} $dayOfWeek"
        }
    }

    val isSpecial = date == today || date == yesterday

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Date label
        Text(
            text = dateText,
            fontSize = if (isSpecial) 18.sp else 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSpecial) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onBackground
            }
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Entry count badge
        if (entryCount > 1) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$entryCount",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Subtle divider
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
        )
    }
}

@Composable
private fun TimelineEntryCard(
    entry: DiaryEntry,
    tags: List<TagInfo>,
    isLast: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "cardScale"
    )

    // Get mood color for left accent
    val moodColor = entry.moodLevel?.let { moodColorForLevel(it) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Left accent bar with mood color
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(20.dp)
        ) {
            // Mood indicator dot
            if (moodColor != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(moodColor)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                )
            }

            // Connecting line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(60.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Entry card with shadow for depth
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 8.dp)
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = Color.Black.copy(alpha = 0.05f),
                    spotColor = Color.Black.copy(alpha = 0.05f)
                )
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
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
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                // Top row: time + mood emoji
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Time
                    Text(
                        text = formatEntryTime(entry.createdAt),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )

                    // Mood emoji if present
                    if (entry.moodLevel != null) {
                        val emoji = moodEmojiForLevel(entry.moodLevel)
                        Text(
                            text = emoji,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                if (entry.title.isNotBlank()) {
                    Text(
                        text = entry.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Content preview
                if (entry.plainText.isNotBlank()) {
                    Text(
                        text = entry.plainText,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                }

                // Bottom: weather + tags
                if (entry.weather != null || tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Weather
                        if (entry.weather != null) {
                            val (weatherIcon, weatherTint) = weatherIconFor(entry.weather)
                            Icon(
                                imageVector = weatherIcon,
                                contentDescription = "天气",
                                tint = weatherTint.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Tags - compact chips
                        tags.take(2).forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(tag.color.copy(alpha = 0.1f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = tag.name,
                                    fontSize = 10.sp,
                                    color = tag.color.copy(alpha = 0.8f),
                                    maxLines = 1
                                )
                            }
                        }

                        if (tags.size > 2) {
                            Text(
                                text = "+${tags.size - 2}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatEntryTime(timestamp: Long): String {
    val localDateTime = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    return localDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
}

@Composable
private fun FAB(onClick: () -> Unit) {
    val fabColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(end = 20.dp, bottom = 16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(fabColor)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "新建日记",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
