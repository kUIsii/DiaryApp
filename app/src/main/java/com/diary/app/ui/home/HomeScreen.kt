package com.diary.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
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
import com.diary.app.data.DiaryPreview
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.FunctionMenu
import com.diary.app.ui.components.FunctionMenuItem
import com.diary.app.ui.components.moodIconForLevel
import com.diary.app.ui.components.moodLabelForLevel
import com.diary.app.ui.components.rememberHapticFeedback
import com.diary.app.ui.components.weatherIconFor
import com.diary.app.ui.components.weatherLabelFor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToEditor: (Long?) -> Unit,
    onNavigateToReview: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToTrash: () -> Unit = {},
    onNavigateToCountDown: () -> Unit = {},
    onNavigateToTimeline: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val haptic = rememberHapticFeedback()
    val entries by viewModel.entries.collectAsState()
    val entryDates by viewModel.entryDates.collectAsState()
    val dayInfoMap by viewModel.dayInfoMap.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedEntries by viewModel.selectedEntries.collectAsState()
    val tagsMap by viewModel.tagsMap.collectAsState()

    var calendarMode by remember { mutableStateOf(CalendarMode.WEEK) }
    var showFunctionMenu by remember { mutableStateOf(false) }

    // Multi-select state
    var multiSelectMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }

    LaunchedEffect(Unit) {
        if (selectedDate == null) {
            viewModel.selectDate(LocalDate.now())
        }
    }

    GradientBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Page header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "首页",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "共 ${entryDates.size} 天有记录",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Function menu button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .clickable { showFunctionMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "功能",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
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
                val currentSelectedDate = selectedDate
                if (currentSelectedDate != null) {
                    item {
                        SelectedDateHeader(
                            date = currentSelectedDate,
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
                                },
                                onLongClick = {
                                    haptic.click()
                                    // TODO: Implement multi-select mode
                                }
                            )
                        }
                    }
                }

                // Bottom padding
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }

            // FAB
            FAB(onClick = { onNavigateToEditor(null) })

            // Function menu overlay (MUST be after LazyColumn for proper z-order)
            FunctionMenu(
                expanded = showFunctionMenu,
                onDismiss = { showFunctionMenu = false },
                items = listOf(
                    FunctionMenuItem(
                        id = "stats",
                        title = "统计",
                        icon = Icons.Default.BarChart,
                        onClick = {
                            showFunctionMenu = false
                            onNavigateToStats()
                        }
                    ),
                    FunctionMenuItem(
                        id = "countdown",
                        title = "倒数日",
                        icon = Icons.Default.Timer,
                        onClick = {
                            showFunctionMenu = false
                            onNavigateToCountDown()
                        }
                    )
                )
            )
        }
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun EntryCard(
    entry: DiaryPreview,
    tags: List<com.diary.app.ui.home.TagInfo>,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
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
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
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
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatEntryTime(entry.createdAt),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // Title (if exists)
            if (entry.title.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = entry.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Text preview (replace literal \n with line breaks)
            if (entry.plainText.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = entry.plainText
                        .replace("\\n", "\n")
                        .replace("\r\n", "\n")
                        .replace("\r", "\n"),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 22.sp
                )
            }

            // Bottom info: mood/weather on left, tags on right
            val hasMoodWeather = entry.moodLevel != null || entry.weather != null
            val hasTags = tags.isNotEmpty()
            if (hasMoodWeather || hasTags) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: mood + weather
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (entry.moodLevel != null) {
                            val (moodIcon, moodTint) = moodIconForLevel(entry.moodLevel)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = moodIcon,
                                    contentDescription = "心情",
                                    tint = moodTint,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = moodLabelForLevel(entry.moodLevel),
                                    fontSize = 12.sp,
                                    color = moodTint,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        if (entry.weather != null) {
                            val (weatherIcon, weatherTint) = weatherIconFor(entry.weather)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = weatherIcon,
                                    contentDescription = "天气",
                                    tint = weatherTint,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = weatherLabelFor(entry.weather),
                                    fontSize = 12.sp,
                                    color = weatherTint,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Spacer to push tags to the right
                    Spacer(modifier = Modifier.weight(1f))

                    // Right: tags
                    if (hasTags) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            tags.forEach { tag ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(tag.color.copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
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
                .clip(RoundedCornerShape(16.dp))
                .background(fabColor.copy(alpha = 0.9f))
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
