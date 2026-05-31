package com.diary.app.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
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
import com.diary.app.ui.home.CalendarMode
import com.diary.app.ui.home.CalendarView
import com.diary.app.ui.home.TagInfo
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MapScreen(
    onNavigateToDetail: (Long) -> Unit = {},
    onNavigateToEditor: (Long?) -> Unit = {},
    viewModel: TimelineViewModel = viewModel()
) {
    val haptic = rememberHapticFeedback()
    val entryDates by viewModel.entryDates.collectAsState()
    val dayInfoMap by viewModel.dayInfoMap.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedEntries by viewModel.selectedEntries.collectAsState()
    val tagsMap by viewModel.tagsMap.collectAsState()

    var calendarMode by remember { mutableStateOf(CalendarMode.MONTH) }

    LaunchedEffect(Unit) {
        if (selectedDate == null) {
            viewModel.selectDate(LocalDate.now())
        }
    }

    GradientBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Page header
            item {
                Text(
                    text = "日历",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "共 ${entryDates.size} 天有记录",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Year overview stats
            item {
                YearOverviewCard(entryDates = entryDates)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Calendar
            item {
                CalendarView(
                    entryDates = entryDates,
                    dayInfoMap = dayInfoMap,
                    selectedDate = selectedDate,
                    onDateSelected = { date ->
                        haptic.click()
                        viewModel.selectDate(date)
                    },
                    calendarMode = calendarMode,
                    onModeChange = { calendarMode = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Selected date header
            if (selectedDate != null) {
                item {
                    SelectedDateHeader(
                        date = selectedDate!!,
                        entryCount = selectedEntries.size
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Entries for selected date
            if (selectedDate != null && selectedEntries.isEmpty()) {
                item {
                    NoEntriesForDate()
                }
            } else {
                itemsIndexed(
                    items = selectedEntries,
                    key = { _, entry -> entry.id }
                ) { index, entry ->
                    val enterDelay = (index * 60).coerceAtMost(400)
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(300, delayMillis = enterDelay)) +
                                slideInVertically(
                                    animationSpec = tween(300, delayMillis = enterDelay),
                                    initialOffsetY = { it / 5 }
                                )
                    ) {
                        EntryCard(
                            entry = entry,
                            tags = tagsMap[entry.id] ?: emptyList(),
                            onClick = {
                                haptic.click()
                                onNavigateToDetail(entry.id)
                            }
                        )
                    }
                }
            }

            // Bottom padding
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun YearOverviewCard(entryDates: Set<LocalDate>) {
    val currentYear = YearMonth.now().year
    val yearDates = entryDates.filter { it.year == currentYear }
    val currentMonth = YearMonth.now()
    val monthDates = entryDates.filter {
        YearMonth.from(it) == currentMonth
    }

    // Calculate streak
    var streak = 0
    var checkDate = LocalDate.now()
    while (checkDate in entryDates) {
        streak++
        checkDate = checkDate.minusDays(1)
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        innerPadding = 16.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${currentYear}年概览",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Icon(
                    Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "年度总天数",
                    value = "${yearDates.size}",
                    color = MaterialTheme.colorScheme.primary
                )
                StatItem(
                    label = "本月天数",
                    value = "${monthDates.size}",
                    color = MaterialTheme.colorScheme.tertiary
                )
                StatItem(
                    label = "连续天数",
                    value = "$streak",
                    color = if (streak > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun SelectedDateHeader(date: LocalDate, entryCount: Int) {
    val today = LocalDate.now()
    val dateText = when (date) {
        today -> "今天"
        today.minusDays(1) -> "昨天"
        else -> {
            val formatter = DateTimeFormatter.ofPattern("M月d日 EEEE")
            date.format(formatter)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = dateText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (entryCount > 0) "$entryCount 篇日记" else "暂无日记",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NoEntriesForDate() {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = onSurfaceVariant.copy(alpha = 0.25f),
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "这天没有日记",
                fontSize = 16.sp,
                color = onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "点击下方按钮开始记录",
                fontSize = 13.sp,
                color = onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun EntryCard(
    entry: DiaryEntry,
    tags: List<TagInfo>,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "entryCardScale"
    )

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        cornerRadius = 16.dp,
        innerPadding = 16.dp
    ) {
        Column {
            // Time row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatEntryTime(entry.createdAt),
                    fontSize = 13.sp,
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
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 22.sp
                )
            }

            // Bottom info: mood + weather + tags
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(tag.color.copy(alpha = 0.12f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(tag.color)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = tag.name,
                                fontSize = 11.sp,
                                color = tag.color,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
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

private fun formatEntryTime(timestamp: Long): String {
    val localDateTime = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    return localDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
}
