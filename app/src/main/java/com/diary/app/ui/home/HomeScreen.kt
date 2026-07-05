package com.diary.app.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import java.time.temporal.ChronoUnit

internal enum class HomeShortcutDestination {
    TIMELINE,
    TODO,
    STATS,
    COUNTDOWN,
    AI_ASSISTANT,
    FAVORITES,
    TIME_CAPSULE,
    MEDIA_LIBRARY,
    DIARY_MAP,
    BIOGRAPHY,
    ACHIEVEMENTS,
    NOTIFICATIONS,
    BACKUP,
    TAG_MANAGEMENT,
    STORAGE
}

internal fun resolveHomeShortcutDestination(route: String): HomeShortcutDestination? {
    return when (route) {
        "stats" -> HomeShortcutDestination.STATS
        "countdown" -> HomeShortcutDestination.COUNTDOWN
        "ai_assistant" -> HomeShortcutDestination.AI_ASSISTANT
        "favorites" -> HomeShortcutDestination.FAVORITES
        "time_capsule" -> HomeShortcutDestination.TIME_CAPSULE
        "media_library" -> HomeShortcutDestination.MEDIA_LIBRARY
        "diary_map" -> HomeShortcutDestination.DIARY_MAP
        "biography" -> HomeShortcutDestination.BIOGRAPHY
        "achievements" -> HomeShortcutDestination.ACHIEVEMENTS
        "timeline" -> HomeShortcutDestination.TIMELINE
        "notifications" -> HomeShortcutDestination.NOTIFICATIONS
        "backup" -> HomeShortcutDestination.BACKUP
        "tag_management" -> HomeShortcutDestination.TAG_MANAGEMENT
        "storage" -> HomeShortcutDestination.STORAGE
        "todo" -> HomeShortcutDestination.TODO
        else -> null
    }
}

internal fun shouldShowSearchEmptyState(
    query: String,
    results: List<DiaryPreview>
): Boolean {
    return query.isNotBlank() && results.isEmpty()
}

internal fun shouldShowBrowseSections(query: String): Boolean {
    return query.isBlank()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToEditor: (Long?) -> Unit,
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToTrash: () -> Unit = {},
    onNavigateToTimeline: (String?) -> Unit = { _ -> },
    onNavigateToTodo: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToAiAssistant: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToCountDown: () -> Unit = {},
    onNavigateToTimeCapsule: () -> Unit = {},
    onNavigateToMediaLibrary: () -> Unit = {},
    onNavigateToDiaryMap: () -> Unit = {},
    onNavigateToBiography: () -> Unit = {},
    onNavigateToAchievements: () -> Unit = {},
    onNavigateToTagManagement: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToStorage: () -> Unit = {},
    onNavigateToWeatherDetail: () -> Unit = {},
    onMainScreenSwipe: ((Float) -> Unit)? = null,
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
    val currentWeather by viewModel.currentWeather.collectAsState()
    val imageMap by viewModel.imageMap.collectAsState()
    val allImagesMap by viewModel.allImagesMap.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val entriesByDate by viewModel.entriesByDate.collectAsState()
    val homeHighlights by viewModel.homeHighlights.collectAsState()

    var calendarMode by remember { mutableStateOf(CalendarMode.WEEK) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Calendar view state (managed here for pager sync)
    val todayForCalendar = remember { LocalDate.now() }
    var currentMonth by remember { mutableStateOf(java.time.YearMonth.now()) }
    var currentWeekStart by remember {
        mutableStateOf(todayForCalendar.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)))
    }

    // Sync calendar state when selectedDate changes (from pager swipe)
    LaunchedEffect(selectedDate) {
        selectedDate?.let { date ->
            if (calendarMode == CalendarMode.MONTH) {
                val ym = java.time.YearMonth.from(date)
                if (ym != currentMonth) currentMonth = ym
            } else {
                val weekStart = date.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                if (weekStart != currentWeekStart) currentWeekStart = weekStart
            }
        }
    }
    var multiSelectState by remember { mutableStateOf(HomeMultiSelectState()) }

    // Weather state
    val isWeatherEnabled by viewModel.isWeatherEnabled.collectAsState()

    // Weather permission handling
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            viewModel.loadWeather()
        }
    }

    LaunchedEffect(Unit) {
        if (selectedDate == null) {
            viewModel.selectDate(LocalDate.now())
        }
        viewModel.loadInsight()
        viewModel.autoLoadWeather()
        viewModel.refreshHomeHighlights()
    }

    LaunchedEffect(selectedDate) {
        multiSelectState = HomeMultiSelectState()
        showDeleteConfirm = false
    }


    GradientBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(onMainScreenSwipe) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            totalDrag += dragAmount
                            change.consume()
                        },
                        onDragEnd = {
                            onMainScreenSwipe?.invoke(totalDrag)
                        }
                    )
                }
        ) {
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
                        currentWeather = if (isWeatherEnabled) currentWeather else null,
                        randomEntry = if (searchQuery.isBlank()) homeHighlights.randomEntry else null,
                        isWeatherEnabled = isWeatherEnabled,
                        onWeatherToggle = { enabled ->
                            if (enabled) {
                                viewModel.enableWeather(
                                    onRequestPermission = {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                )
                            } else {
                                viewModel.disableWeather()
                            }
                        },
                        onWeatherClick = {
                            haptic.click()
                            onNavigateToWeatherDetail()
                        },
                        onRandomClick = {
                            haptic.click()
                            homeHighlights.randomEntry?.let { onNavigateToDetail(it.id) }
                        },
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

                // Search bar
                item {
                    HomeSearchBar(
                        query = searchQuery,
                        onQueryChange = { viewModel.setSearchQuery(it) }
                    )
                }

                // Search results (when query is active)
                if (searchQuery.isNotBlank() && searchResults.isNotEmpty()) {
                    item {
                        Text(
                            text = "找到 ${searchResults.size} 条结果",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }
                    items(searchResults.take(10), key = { it.id }) { entry ->
                        HomeSearchResultCard(
                            entry = entry,
                            imageMap = imageMap,
                            onClick = { onNavigateToDetail(entry.id) }
                        )
                    }
                    if (searchResults.size > 10) {
                        item {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onNavigateToTimeline(searchQuery) },
                                cornerRadius = 12.dp,
                                innerPadding = 12.dp
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "查看全部 ${searchResults.size} 条结果",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (shouldShowSearchEmptyState(searchQuery, searchResults)) {
                    item {
                        EmptyState(
                            icon = Icons.Default.Search,
                            title = "没有找到相关日记",
                            subtitle = "换个关键词试试，或者去时间线看看更完整的内容",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (shouldShowBrowseSections(searchQuery) && homeHighlights.onThisDayEntries.isNotEmpty()) {
                    item {
                        HomeOnThisDayCard(
                            entries = homeHighlights.onThisDayEntries,
                            onClick = { entry -> onNavigateToDetail(entry.id) }
                        )
                    }
                }

                if (shouldShowBrowseSections(searchQuery)) {
                    item {
                        HomeQuickShortcutsSection(
                            onNavigate = { route ->
                                when (resolveHomeShortcutDestination(route)) {
                                    HomeShortcutDestination.STATS -> onNavigateToStats()
                                    HomeShortcutDestination.COUNTDOWN -> onNavigateToCountDown()
                                    HomeShortcutDestination.AI_ASSISTANT -> onNavigateToAiAssistant()
                                    HomeShortcutDestination.FAVORITES -> onNavigateToFavorites()
                                    HomeShortcutDestination.TIME_CAPSULE -> onNavigateToTimeCapsule()
                                    HomeShortcutDestination.MEDIA_LIBRARY -> onNavigateToMediaLibrary()
                                    HomeShortcutDestination.DIARY_MAP -> onNavigateToDiaryMap()
                                    HomeShortcutDestination.BIOGRAPHY -> onNavigateToBiography()
                                    HomeShortcutDestination.ACHIEVEMENTS -> onNavigateToAchievements()
                                    HomeShortcutDestination.TIMELINE -> onNavigateToTimeline(null)
                                    HomeShortcutDestination.NOTIFICATIONS -> onNavigateToNotifications()
                                    HomeShortcutDestination.BACKUP -> onNavigateToBackup()
                                    HomeShortcutDestination.TAG_MANAGEMENT -> onNavigateToTagManagement()
                                    HomeShortcutDestination.STORAGE -> onNavigateToStorage()
                                    HomeShortcutDestination.TODO -> onNavigateToTodo()
                                    null -> Unit
                                }
                            }
                        )
                    }

                    item {
                        HomeCalendarSectionCard(
                            entryDates = entryDates,
                            dayInfoMap = dayInfoMap,
                            selectedDate = selectedDate,
                            calendarMode = calendarMode,
                            onModeChange = { newMode ->
                                calendarMode = newMode
                                if (newMode == CalendarMode.MONTH) {
                                    val refDate = selectedDate ?: LocalDate.now()
                                    val ym = java.time.YearMonth.from(refDate)
                                    if (ym != currentMonth) currentMonth = ym
                                } else {
                                    val refDate = selectedDate ?: LocalDate.now()
                                    val ws = refDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                                    if (ws != currentWeekStart) currentWeekStart = ws
                                }
                            },
                            onDateSelected = { date ->
                                haptic.click()
                                viewModel.selectDate(date)
                            },
                            currentMonth = currentMonth,
                            onCurrentMonthChange = { currentMonth = it },
                            currentWeekStart = currentWeekStart,
                            onCurrentWeekStartChange = { currentWeekStart = it }
                        )
                    }
                }

                // Day content - display only, no swipe
                item(key = "day-pager") {
                    val currentDate = selectedDate ?: LocalDate.now()
                    val currentEntries = entriesByDate[currentDate] ?: emptyList()

                    Column {
                        HomeSelectedDateHeader(
                            date = currentDate,
                            entryCount = currentEntries.size,
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

                        Spacer(modifier = Modifier.height(4.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (currentEntries.isEmpty()) {
                                HomeNoEntriesForDate()
                            } else {
                                currentEntries.forEach { entry ->
                                    HomeEntryFeedCard(
                                        entry = entry,
                                        tags = tagsMap[entry.id] ?: emptyList(),
                                        imagePaths = allImagesMap[entry.id] ?: emptyList(),
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
                        }
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

@Composable
private fun HomeHeroSection(
    selectedDate: LocalDate,
    stats: HomeStats,
    unreadCount: Int,
    aiInsight: AiInsight?,
    currentWeather: com.diary.app.weather.CurrentWeather?,
    randomEntry: DiaryPreview?,
    isWeatherEnabled: Boolean = false,
    onWeatherToggle: (Boolean) -> Unit = {},
    onWeatherClick: () -> Unit = {},
    onRandomClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onAiClick: () -> Unit
) {
    val hour = java.time.LocalTime.now().hour
    val greeting = when {
        hour < 6 -> "夜深了"
        hour < 9 -> "早上好"
        hour < 12 -> "上午好"
        hour < 14 -> "中午好"
        hour < 18 -> "下午好"
        hour < 22 -> "晚上好"
        else -> "夜深了"
    }
    val today = LocalDate.now()
    val dateStr = today.format(DateTimeFormatter.ofPattern("M月d日 EEEE", java.util.Locale.CHINA))

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = greeting,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$dateStr · 共 ${stats.total} 篇日记",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (randomEntry != null) {
                    HomeHeaderAction(
                        icon = Icons.Default.Shuffle,
                        contentDescription = "随机回顾",
                        onClick = onRandomClick
                    )
                }
                HomeHeaderAction(
                    icon = Icons.Default.Notifications,
                    contentDescription = "通知",
                    badgeCount = unreadCount,
                    onClick = onNotificationsClick
                )
                HomeHeaderAction(
                    icon = Icons.Default.ChatBubbleOutline,
                    contentDescription = "AI 助手",
                    onClick = onAiClick
                )
            }
        }

        // Weather row (separate, stable layout)
        if (currentWeather != null && currentWeather.weather.isNotBlank()) {
            var alertExpanded by remember { mutableStateOf(false) }
            val hasAlerts = currentWeather.alerts.isNotEmpty()
            val firstAlert = if (hasAlerts) currentWeather.alerts.first() else null

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        .clickable { onWeatherClick() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = com.diary.app.ui.components.weatherIconForType(
                            com.diary.app.weather.WeatherManager.mapAmapWeatherToType(currentWeather.weather)
                        ),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${currentWeather.city} · ${currentWeather.weather} ${currentWeather.temperature}°C",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (hasAlerts && firstAlert != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                                .clickable { alertExpanded = !alertExpanded }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(MaterialTheme.colorScheme.error, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${firstAlert.level}预警 · ${firstAlert.type}",
                                fontSize = 11.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = alertExpanded && firstAlert != null,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    if (firstAlert != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.06f))
                                .clickable { alertExpanded = false }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(40.dp)
                                    .background(MaterialTheme.colorScheme.error, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = firstAlert.text,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else if (!isWeatherEnabled) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f))
                    .clickable { onWeatherToggle(true) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "查看天气",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        // AI insight only (streak/month stats removed)
        if (aiInsight != null) {
            CompactStatChip(
                icon = Icons.Default.AutoAwesome,
                text = aiInsight.text.take(20),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CompactStatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
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
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                    shape = CircleShape
                )
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
                Text(text = badgeCount.coerceAtMost(99).toString(), fontSize = 10.sp)
            }
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

@Composable
private fun HomeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    GlassCard(
        cornerRadius = 12.dp,
        innerPadding = 4.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 10.dp, end = 6.dp).size(18.dp)
            )
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "搜索日记...",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
            if (query.isNotBlank()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "清除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

