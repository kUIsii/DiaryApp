package com.diary.app.ui.timeline

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Schedule
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
import java.time.Month
import java.util.Locale

@Composable
fun TimelineScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToEditor: (Long?) -> Unit,
    viewModel: TimelineViewModel = viewModel()
) {
    val haptic = rememberHapticFeedback()
    val entries by viewModel.entries.collectAsState()
    val tagsMap by viewModel.tagsMap.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val listState = rememberLazyListState()

    // Group entries by date
    val groupedEntries = remember(entries) {
        entries.groupBy { entry ->
            Instant.ofEpochMilli(entry.createdAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }.toSortedMap(compareByDescending { it })
    }

    // Group entries by month for magazine-style layout
    val monthlyGroups = remember(groupedEntries) {
        groupedEntries.entries.groupBy { (date, _) ->
            YearMonth(date.year, date.monthValue)
        }.toSortedMap(compareByDescending { it })
    }

    GradientBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Page header - magazine style
                item {
                    MagazineHeader(totalEntries = entries.size)
                }

                // Monthly groups
                monthlyGroups.forEach { (yearMonth, dateEntries) ->
                    // Month header - elegant divider
                    item {
                        MonthHeader(yearMonth = yearMonth)
                    }

                    // Entries for each date in this month
                    dateEntries.forEachIndexed { dateIndex, (date, dayEntries) ->
                        // Date section with entries
                        item {
                            DateSection(
                                date = date,
                                entries = dayEntries,
                                tagsMap = tagsMap,
                                onNavigateToDetail = { entryId ->
                                    haptic.click()
                                    onNavigateToDetail(entryId)
                                }
                            )
                        }
                    }

                    // Spacer between months
                    item {
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }

                // Bottom padding
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }

            // FAB
            FAB(onClick = { onNavigateToEditor(null) })
        }
    }
}

// Data class for year-month grouping
private data class YearMonth(val year: Int, val month: Int) : Comparable<YearMonth> {
    override fun compareTo(other: YearMonth): Int {
        return if (this.year != other.year) {
            other.year - this.year
        } else {
            other.month - this.month
        }
    }
}

@Composable
private fun MagazineHeader(totalEntries: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        // Title - large, magazine style
        Text(
            text = "回忆",
            fontSize = 42.sp,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle with count
        Text(
            text = "共 $totalEntries 篇故事",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Elegant divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
        )
    }
}

@Composable
private fun MonthHeader(yearMonth: YearMonth) {
    val monthName = when (yearMonth.month) {
        1 -> "一月"
        2 -> "二月"
        3 -> "三月"
        4 -> "四月"
        5 -> "五月"
        6 -> "六月"
        7 -> "七月"
        8 -> "八月"
        9 -> "九月"
        10 -> "十月"
        11 -> "十一月"
        12 -> "十二月"
        else -> ""
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Year
            Text(
                text = "${yearMonth.year}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Month - larger, bolder
            Text(
                text = monthName,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Subtle divider line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
        )
    }
}

@Composable
private fun DateSection(
    date: LocalDate,
    entries: List<DiaryEntry>,
    tagsMap: Map<Long, List<TagInfo>>,
    onNavigateToDetail: (Long) -> Unit
) {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    val dateText = when (date) {
        today -> "今天"
        yesterday -> "昨天"
        else -> {
            val dayOfWeek = when (date.dayOfWeek) {
                DayOfWeek.MONDAY -> "周一"
                DayOfWeek.TUESDAY -> "周二"
                DayOfWeek.WEDNESDAY -> "周三"
                DayOfWeek.THURSDAY -> "周四"
                DayOfWeek.FRIDAY -> "周五"
                DayOfWeek.SATURDAY -> "周六"
                DayOfWeek.SUNDAY -> "周日"
            }
            dayOfWeek
        }
    }

    val dayNumber = date.dayOfMonth.toString()
    val isSpecialDate = date == today || date == yesterday

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        // Date row - elegant layout
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            // Day number - large, prominent
            Text(
                text = dayNumber,
                fontSize = if (isSpecialDate) 48.sp else 42.sp,
                fontWeight = FontWeight.Light,
                color = if (isSpecialDate) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                },
                lineHeight = 1.sp
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Day of week and entry count
            Column(
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = dateText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (entries.size > 1) {
                    Text(
                        text = "${entries.size} 篇",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Entries for this date
        entries.forEachIndexed { index, entry ->
            EntryCard(
                entry = entry,
                tags = tagsMap[entry.id] ?: emptyList(),
                onClick = { onNavigateToDetail(entry.id) }
            )

            if (index < entries.size - 1) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Subtle separator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
        )
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
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "entryCardScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
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
        // Time - subtle
        Text(
            text = formatEntryTime(entry.createdAt),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Title - clean, readable
        if (entry.title.isNotBlank()) {
            Text(
                text = entry.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 26.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Content preview - magazine style
        if (entry.plainText.isNotBlank()) {
            Text(
                text = entry.plainText,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 24.sp
            )
        }

        // Bottom info - mood, weather, tags - minimal
        if (entry.moodLevel != null || entry.weather != null || tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mood - subtle indicator
                if (entry.moodLevel != null) {
                    val (_, moodTint) = moodIconForLevel(entry.moodLevel)
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(moodTint.copy(alpha = 0.6f))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Weather - minimal icon
                if (entry.weather != null) {
                    val (weatherIcon, weatherTint) = weatherIconFor(entry.weather)
                    Icon(
                        imageVector = weatherIcon,
                        contentDescription = "天气",
                        tint = weatherTint.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Spacer(modifier = Modifier.weight(1f))

                // Tags - very minimal
                if (tags.isNotEmpty()) {
                    Text(
                        text = tags.first().name,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    if (tags.size > 1) {
                        Text(
                            text = " +${tags.size - 1}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
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

@Composable
private fun FAB(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(end = 24.dp, bottom = 32.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
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
