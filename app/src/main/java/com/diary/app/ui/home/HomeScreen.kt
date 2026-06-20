package com.diary.app.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.diary.app.ai.AiInsight
import com.diary.app.data.DiaryPreview
import com.diary.app.ui.components.EmptyState
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.cleanPreviewText
import com.diary.app.ui.components.formatEntryTime
import com.diary.app.ui.components.moodIconForLevel
import com.diary.app.ui.components.moodLabelForLevel
import com.diary.app.ui.components.rememberHapticFeedback
import com.diary.app.ui.components.weatherIconFor
import com.diary.app.ui.components.weatherLabelFor
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToEditor: (Long?) -> Unit,
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToTrash: () -> Unit = {},
    onNavigateToTimeline: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToAiAssistant: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val haptic = rememberHapticFeedback()
    val entryDates by viewModel.entryDates.collectAsState()
    val dayInfoMap by viewModel.dayInfoMap.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedEntries by viewModel.selectedEntries.collectAsState()
    val tagsMap by viewModel.tagsMap.collectAsState()
    val unreadCount by viewModel.unreadNotificationCount.collectAsState()
    val aiInsight by viewModel.aiInsight.collectAsState()
    val imageMap by viewModel.imageMap.collectAsState()
    val stats by viewModel.stats.collectAsState()

    var calendarMode by remember { mutableStateOf(CalendarMode.WEEK) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var multiSelectState by remember { mutableStateOf(HomeMultiSelectState()) }

    LaunchedEffect(Unit) {
        if (selectedDate == null) {
            viewModel.selectDate(LocalDate.now())
        }
        viewModel.loadInsight()
    }

    LaunchedEffect(selectedDate) {
        multiSelectState = HomeMultiSelectState()
        showDeleteConfirm = false
    }

    GradientBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    HomeHeroSection(
                        selectedDate = selectedDate ?: LocalDate.now(),
                        stats = stats,
                        unreadCount = unreadCount,
                        aiInsight = aiInsight,
                        onNotificationsClick = {
                            haptic.click()
                            onNavigateToNotifications()
                        },
                        onAiClick = {
                            haptic.click()
                            onNavigateToAiAssistant()
                        }
                    )
                }

                item {
                    CalendarSection(
                        entryDates = entryDates,
                        dayInfoMap = dayInfoMap,
                        selectedDate = selectedDate,
                        calendarMode = calendarMode,
                        onModeChange = { calendarMode = it },
                        onDateSelected = { date ->
                            haptic.click()
                            viewModel.selectDate(date)
                        }
                    )
                }

                selectedDate?.let { currentDate ->
                    item {
                        SelectedDateHeader(
                            date = currentDate,
                            entryCount = selectedEntries.size,
                            multiSelectState = multiSelectState,
                            onFavoriteSelected = {
                                if (multiSelectState.selectedIds.isNotEmpty()) {
                                    haptic.click()
                                    viewModel.favoriteEntries(multiSelectState.selectedIds)
                                    multiSelectState = multiSelectState.clearSelection()
                                }
                            },
                            onDeleteSelected = {
                                if (multiSelectState.selectedIds.isNotEmpty()) {
                                    haptic.click()
                                    showDeleteConfirm = true
                                }
                            },
                            onCancelMultiSelect = {
                                haptic.click()
                                multiSelectState = multiSelectState.clearSelection()
                            }
                        )
                    }
                }

                if (selectedDate != null && selectedEntries.isEmpty()) {
                    item { NoEntriesForDate() }
                } else {
                    items(items = selectedEntries, key = { entry -> entry.id }) { entry ->
                        HomeEntryCard(
                            entry = entry,
                            tags = tagsMap[entry.id] ?: emptyList(),
                            imagePath = imageMap[entry.id],
                            isSelected = entry.id in multiSelectState.selectedIds,
                            onClick = {
                                haptic.click()
                                if (multiSelectState.isEnabled) {
                                    multiSelectState = multiSelectState.toggleSelection(entry.id)
                                } else {
                                    onNavigateToDetail(entry.id)
                                }
                            },
                            onLongClick = {
                                haptic.click()
                                multiSelectState = if (multiSelectState.isEnabled) {
                                    multiSelectState.toggleSelection(entry.id)
                                } else {
                                    HomeMultiSelectState.startSelection(entry.id)
                                }
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(84.dp)) }
            }

            if (!multiSelectState.isEnabled) {
                HomeFab(onClick = { onNavigateToEditor(null) })
            }

            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = { Text("删除选中的日记？") },
                    text = { Text("这些日记会被移入回收站。") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteEntries(multiSelectState.selectedIds)
                                multiSelectState = multiSelectState.clearSelection()
                                showDeleteConfirm = false
                            }
                        ) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = false }) {
                            Text("取消")
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeHeroSection(
    selectedDate: LocalDate,
    stats: HomeStats,
    unreadCount: Int,
    aiInsight: AiInsight?,
    onNotificationsClick: () -> Unit,
    onAiClick: () -> Unit
) {
    val today = LocalDate.now()
    val title = when (selectedDate) {
        today -> "今天"
        today.minusDays(1) -> "昨天"
        else -> selectedDate.format(DateTimeFormatter.ofPattern("M 月 d 日"))
    }
    val subtitle = selectedDate.format(DateTimeFormatter.ofPattern("yyyy 年 M 月 d 日 · EEEE"))

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HomeHeaderAction(
                    icon = Icons.Default.Notifications,
                    contentDescription = "通知",
                    badgeCount = unreadCount,
                    onClick = onNotificationsClick
                )
                HomeHeaderAction(
                    icon = Icons.Default.AutoAwesome,
                    contentDescription = "AI 助手",
                    onClick = onAiClick
                )
            }
        }

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
            innerPadding = 16.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OverviewMetricCard(
                        value = stats.total.toString(),
                        label = "累计日记",
                        modifier = Modifier.weight(1f)
                    )
                    OverviewMetricCard(
                        value = "${stats.streak} 天",
                        label = "连续记录",
                        modifier = Modifier.weight(1f)
                    )
                    OverviewMetricCard(
                        value = stats.thisMonth.toString(),
                        label = "本月有内容",
                        modifier = Modifier.weight(1f)
                    )
                }

                if (aiInsight != null) {
                    HomeInsightCard(insight = aiInsight, compact = true)
                } else {
                    HomeQuietPrompt()
                }
            }
        }
    }
}

@Composable
private fun OverviewMetricCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f))
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun HomeHeaderAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    Box {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                .combinedClickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        if (badgeCount > 0) {
            Badge(modifier = Modifier.align(Alignment.TopEnd)) {
                Text(
                    text = badgeCount.coerceAtMost(99).toString(),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun CalendarSection(
    entryDates: Set<LocalDate>,
    dayInfoMap: Map<LocalDate, DayInfo>,
    selectedDate: LocalDate?,
    calendarMode: CalendarMode,
    onModeChange: (CalendarMode) -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        innerPadding = 14.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "日期选择",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "先选日期，再查看当天内容",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
                        .padding(4.dp)
                ) {
                    CalendarModeChip(
                        label = "周",
                        selected = calendarMode == CalendarMode.WEEK,
                        onClick = { onModeChange(CalendarMode.WEEK) }
                    )
                    CalendarModeChip(
                        label = "月",
                        selected = calendarMode == CalendarMode.MONTH,
                        onClick = { onModeChange(CalendarMode.MONTH) }
                    )
                }
            }

            CalendarView(
                entryDates = entryDates,
                dayInfoMap = dayInfoMap,
                selectedDate = selectedDate,
                onDateSelected = onDateSelected,
                calendarMode = calendarMode,
                onModeChange = onModeChange
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalendarModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                else Color.Transparent
            )
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SelectedDateHeader(
    date: LocalDate,
    entryCount: Int,
    multiSelectState: HomeMultiSelectState,
    onFavoriteSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onCancelMultiSelect: () -> Unit
) {
    val today = LocalDate.now()
    val title = when (date) {
        today -> "今天"
        today.minusDays(1) -> "昨天"
        else -> date.format(DateTimeFormatter.ofPattern("M 月 d 日 · EEEE"))
    }

    if (multiSelectState.isEnabled) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 18.dp,
            innerPadding = 14.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "已选 ${multiSelectState.selectedCount} 篇",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "可以继续收藏或移入回收站",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HeaderActionButton(
                    icon = Icons.Default.Favorite,
                    label = "收藏",
                    enabled = multiSelectState.selectedIds.isNotEmpty(),
                    onClick = onFavoriteSelected
                )
                Spacer(modifier = Modifier.width(8.dp))
                HeaderActionButton(
                    icon = Icons.Default.Delete,
                    label = "删除",
                    enabled = multiSelectState.selectedIds.isNotEmpty(),
                    onClick = onDeleteSelected
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onCancelMultiSelect) { Text("取消") }
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (entryCount > 0) "当天共 $entryCount 篇日记" else "这一天还没有新的日记",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeaderActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (enabled) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(containerColor)
                .combinedClickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = contentColor
        )
    }
}

@Composable
private fun NoEntriesForDate() {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        EmptyState(
            icon = Icons.Default.CalendarMonth,
            title = "这一天还没有日记",
            subtitle = "点击右下角按钮，开始记录今天的内容",
            iconSize = 54.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeEntryCard(
    entry: DiaryPreview,
    tags: List<TagInfo>,
    imagePath: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "homeEntryScale"
    )

    val moodData = entry.moodLevel?.let { moodIconForLevel(it) }
    val weatherData = entry.weather?.let { weatherIconFor(it) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
                        shape = RoundedCornerShape(18.dp)
                    )
                } else {
                    Modifier
                }
            )
            .clip(RoundedCornerShape(18.dp))
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
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
            cornerRadius = 18.dp,
            innerPadding = 14.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isSelected) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = formatEntryTime(entry.createdAt),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = entry.title.ifBlank { "未命名日记" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (entry.plainText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = cleanPreviewText(entry.plainText),
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (!imagePath.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(12.dp))
                        AsyncImage(
                            model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                .data(File(imagePath))
                                .crossfade(true)
                                .size(144)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        )
                    }
                }

                if (!entry.location.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = entry.location,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        moodData?.let { mood ->
                            MetaChip(
                                icon = mood.icon,
                                label = moodLabelForLevel(entry.moodLevel),
                                tint = mood.tint
                            )
                        }
                        weatherData?.let { weather ->
                            MetaChip(
                                icon = weather.icon,
                                label = weatherLabelFor(entry.weather),
                                tint = weather.tint
                            )
                        }
                        tags.take(2).forEach { tag ->
                            ColorTagChip(tag = tag)
                        }
                        if (tags.size > 2) {
                            SubtleTextChip(text = "+${tags.size - 2}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(tint.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = tint
        )
    }
}

@Composable
private fun ColorTagChip(tag: TagInfo) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(tag.color.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = tag.name,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = tag.color
        )
    }
}

@Composable
private fun SubtleTextChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HomeInsightCard(insight: AiInsight, compact: Boolean = false) {
    val icon = when (insight.type) {
        "mood" -> Icons.Default.Favorite
        "pattern" -> Icons.Default.CalendarMonth
        else -> Icons.Default.AutoAwesome
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        innerPadding = 14.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(if (compact) 40.dp else 36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "AI 提醒",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = insight.text,
                    fontSize = if (compact) 13.sp else 14.sp,
                    lineHeight = if (compact) 20.sp else 21.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun HomeQuietPrompt() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "AI 提醒会出现在这里",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "今天先从日期和记录开始，等有新内容后再生成轻量提示。",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HomeFab(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(end = 20.dp, bottom = 16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "新建日记",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
