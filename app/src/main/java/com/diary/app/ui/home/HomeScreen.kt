package com.diary.app.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.DiaryApplication
import com.diary.app.ai.AiInsight
import com.diary.app.data.DiaryPreview
import com.diary.app.ui.components.FunctionMenu
import com.diary.app.ui.components.FunctionMenuItem
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.cleanPreviewText
import com.diary.app.ui.components.formatEntryTime
import com.diary.app.ui.components.moodIconForLevel
import com.diary.app.ui.components.moodLabelForLevel
import com.diary.app.ui.components.rememberHapticFeedback
import com.diary.app.ui.components.weatherIconFor
import com.diary.app.ui.components.weatherLabelFor
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
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
    onNavigateToMediaLibrary: () -> Unit = {},
    onNavigateToExperimentalFeatures: () -> Unit = {},
    onNavigateToTimeCapsule: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToAiAssistant: () -> Unit = {},
    onNavigateToHealth: () -> Unit = {},
    onNavigateToDiaryMap: () -> Unit = {},
    onNavigateToBiography: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val haptic = rememberHapticFeedback()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val app = context.applicationContext as? DiaryApplication ?: return
    val features by app.experimentalFeatures.collectAsState()
    val entryDates by viewModel.entryDates.collectAsState()
    val dayInfoMap by viewModel.dayInfoMap.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedEntries by viewModel.selectedEntries.collectAsState()
    val tagsMap by viewModel.tagsMap.collectAsState()
    val unreadCount by viewModel.unreadNotificationCount.collectAsState()
    val aiInsight by viewModel.aiInsight.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadInsight()
    }

    var calendarMode by remember { mutableStateOf(CalendarMode.WEEK) }
    var showFunctionMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var multiSelectState by remember { mutableStateOf(HomeMultiSelectState()) }

    LaunchedEffect(Unit) {
        if (selectedDate == null) {
            viewModel.selectDate(LocalDate.now())
        }
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
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
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

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .clickable { onNavigateToNotifications() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "消息",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                if (unreadCount > 0) {
                                    Badge(
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Text(
                                            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                            if (features.aiAssistantEnabled) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                        .clickable { onNavigateToAiAssistant() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChatBubbleOutline,
                                        contentDescription = "小墨",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
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
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                aiInsight?.let { insight ->
                    item {
                        InsightCard(insight = insight)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

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
                    Spacer(modifier = Modifier.height(12.dp))
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
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                if (selectedDate != null && selectedEntries.isEmpty()) {
                    item {
                        NoEntriesForDate()
                    }
                } else {
                    itemsIndexed(
                        items = selectedEntries,
                        key = { _, entry -> entry.id }
                    ) { _, entry ->
                        EntryCard(
                            entry = entry,
                            tags = tagsMap[entry.id] ?: emptyList(),
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

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }

            if (!multiSelectState.isEnabled) {
                FAB(onClick = { onNavigateToEditor(null) })
            }

            val menuItems = remember(features) {
                buildList {
                    add(FunctionMenuItem(
                        id = "random",
                        title = "随机回顾",
                        icon = Icons.Default.Shuffle,
                        onClick = {
                            showFunctionMenu = false
                            scope.launch {
                                val randomId = viewModel.getRandomEntryId()
                                if (randomId != null) {
                                    onNavigateToDetail(randomId)
                                }
                            }
                        }
                    ))
                    add(FunctionMenuItem(
                        id = "experimental",
                        title = "实验功能",
                        icon = Icons.Default.AutoAwesome,
                        onClick = {
                            showFunctionMenu = false
                            onNavigateToExperimentalFeatures()
                        }
                    ))
                    add(FunctionMenuItem(
                        id = "media",
                        title = "媒体库",
                        icon = Icons.Default.Collections,
                        onClick = {
                            showFunctionMenu = false
                            onNavigateToMediaLibrary()
                        }
                    ))
                    add(FunctionMenuItem(
                        id = "stats",
                        title = "统计",
                        icon = Icons.Default.BarChart,
                        onClick = {
                            showFunctionMenu = false
                            onNavigateToStats()
                        }
                    ))
                    add(FunctionMenuItem(
                        id = "countdown",
                        title = "倒数日",
                        icon = Icons.Default.Timer,
                        onClick = {
                            showFunctionMenu = false
                            onNavigateToCountDown()
                        }
                    ))
                    add(FunctionMenuItem(
                        id = "capsule",
                        title = "时间胶囊",
                        icon = Icons.Default.MarkEmailUnread,
                        onClick = {
                            showFunctionMenu = false
                            onNavigateToTimeCapsule()
                        }
                    ))
                    if (features.healthDataEnabled) {
                        add(FunctionMenuItem(
                            id = "health",
                            title = "健康数据",
                            icon = Icons.Default.Favorite,
                            onClick = {
                                showFunctionMenu = false
                                onNavigateToHealth()
                            }
                        ))
                    }
                    if (features.diaryMapEnabled) {
                        add(FunctionMenuItem(
                            id = "map",
                            title = "日记地图",
                            icon = Icons.Default.LocationOn,
                            onClick = {
                                showFunctionMenu = false
                                onNavigateToDiaryMap()
                            }
                        ))
                    }
                    if (features.aiBiographyEnabled) {
                        add(FunctionMenuItem(
                            id = "biography",
                            title = "AI 传记",
                            icon = Icons.Default.AutoAwesome,
                            onClick = {
                                showFunctionMenu = false
                                onNavigateToBiography()
                            }
                        ))
                    }
                }
            }
            FunctionMenu(
                expanded = showFunctionMenu,
                onDismiss = { showFunctionMenu = false },
                items = menuItems
            )

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
    val dateText = when (date) {
        today -> "今天"
        today.minusDays(1) -> "昨天"
        else -> date.format(DateTimeFormatter.ofPattern("M月d日 EEEE"))
    }

    if (multiSelectState.isEnabled) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "已选择 ${multiSelectState.selectedCount} 篇",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "可批量收藏或删除",
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
            TextButton(onClick = onCancelMultiSelect) {
                Text("取消")
            }
        }
    } else {
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
}

@Composable
private fun HeaderActionButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (enabled) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(containerColor),
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
                text = "这一天还没有日记",
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
    tags: List<TagInfo>,
    isSelected: Boolean,
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(16.dp)
                    )
                } else {
                    Modifier
                }
            )
            .clip(RoundedCornerShape(16.dp))
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
            cornerRadius = 16.dp,
            innerPadding = 12.dp
        ) {
            Column {
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
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 标题
                if (entry.title.isNotBlank()) {
                    Text(
                        text = entry.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 内容预览
                if (entry.plainText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = cleanPreviewText(entry.plainText),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 地点（单独一行，可能较长）
                if (!entry.location.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = entry.location,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 心情/天气/标签/时间
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = moodLabelForLevel(entry.moodLevel),
                                fontSize = 11.sp,
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
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = weatherLabelFor(entry.weather),
                                fontSize = 11.sp,
                                color = weatherTint,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (tags.isNotEmpty()) {
                        tags.take(2).forEach { tag ->
                            Text(
                                text = tag.name,
                                fontSize = 10.sp,
                                color = tag.color,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(tag.color.copy(alpha = 0.1f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (tags.size > 2) {
                            Text(
                                text = "+${tags.size - 2}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Text(
                        text = formatEntryTime(entry.createdAt),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightCard(insight: AiInsight) {
    val icon = when (insight.type) {
        "mood" -> Icons.Default.Favorite
        "encourage" -> Icons.Default.AutoAwesome
        "pattern" -> Icons.Default.BarChart
        else -> Icons.Default.Schedule
    }
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = insight.text,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp
            )
        }
    }
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

@Composable
private fun QuickAccessRow(
    onStats: () -> Unit,
    onMedia: () -> Unit,
    onCountDown: () -> Unit,
    onCapsule: () -> Unit,
    onRandom: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickAccessItem(Icons.Default.BarChart, "统计", onStats)
        QuickAccessItem(Icons.Default.Collections, "媒体库", onMedia)
        QuickAccessItem(Icons.Default.Timer, "倒数日", onCountDown)
        QuickAccessItem(Icons.Default.MarkEmailUnread, "胶囊", onCapsule)
        QuickAccessItem(Icons.Default.Shuffle, "随机", onRandom)
    }
}

@Composable
private fun QuickAccessItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
