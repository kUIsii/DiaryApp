package com.diary.app.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToEditor: (Long?) -> Unit,
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToTrash: () -> Unit = {},
    onNavigateToTimeline: (String?) -> Unit = { _ -> },
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

    // Random review state
    var randomEntry by remember { mutableStateOf<DiaryPreview?>(null) }

    LaunchedEffect(Unit) {
        val id = viewModel.getRandomEntryId()
        randomEntry = if (id != null) viewModel.getEntryPreview(id) else null
    }

    // On this day state
    var onThisDayEntries by remember { mutableStateOf<List<DiaryPreview>>(emptyList()) }
    LaunchedEffect(Unit) {
        onThisDayEntries = viewModel.getOnThisDayPreviews()
    }

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
                        randomEntry = if (searchQuery.isBlank()) randomEntry else null,
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
                            randomEntry?.let { onNavigateToDetail(it.id) }
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
                        SearchResultCard(
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

                // On this day card (hidden when searching)
                if (searchQuery.isBlank() && onThisDayEntries.isNotEmpty()) {
                    item {
                        OnThisDayCard(
                            entries = onThisDayEntries,
                            onClick = { entry -> onNavigateToDetail(entry.id) }
                        )
                    }
                }

                // Quick shortcuts (above calendar)
                item {
                    QuickShortcutsSection(
                        onNavigate = { route ->
                            when (route) {
                                "stats" -> onNavigateToStats()
                                "countdown" -> onNavigateToCountDown()
                                "ai_assistant" -> onNavigateToAiAssistant()
                                "favorites" -> onNavigateToFavorites()
                                "time_capsule" -> onNavigateToTimeCapsule()
                                "media_library" -> onNavigateToMediaLibrary()
                                "diary_map" -> onNavigateToDiaryMap()
                                "biography" -> onNavigateToBiography()
                                "achievements" -> onNavigateToAchievements()
                                "timeline" -> onNavigateToTimeline(null)
                                "notifications" -> onNavigateToNotifications()
                                "backup" -> onNavigateToBackup()
                                "tag_management" -> onNavigateToTagManagement()
                                "storage" -> onNavigateToStorage()
                                "todo" -> onNavigateToTimeline(null)
                            }
                        }
                    )
                }

                item {
                    CalendarSection(
                        entryDates = entryDates,
                        dayInfoMap = dayInfoMap,
                        selectedDate = selectedDate,
                        calendarMode = calendarMode,
                        onModeChange = { newMode ->
                            calendarMode = newMode
                            // When switching modes, sync with currentWeekStart/currentMonth
                            if (newMode == CalendarMode.MONTH) {
                                // Switching to month: use selectedDate to determine month
                                val refDate = selectedDate ?: LocalDate.now()
                                val ym = java.time.YearMonth.from(refDate)
                                if (ym != currentMonth) currentMonth = ym
                            } else {
                                // Switching to week: use selectedDate to determine week start
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

                // Day content - display only, no swipe
                item(key = "day-pager") {
                    val currentDate = selectedDate ?: LocalDate.now()
                    val currentEntries = entriesByDate[currentDate] ?: emptyList()

                    Column {
                        SelectedDateHeader(
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
                                NoEntriesForDate()
                            } else {
                                currentEntries.forEach { entry ->
                                    HomeEntryCard(
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
private fun CalendarSection(
    entryDates: Set<LocalDate>,
    dayInfoMap: Map<LocalDate, DayInfo>,
    selectedDate: LocalDate?,
    calendarMode: CalendarMode,
    onModeChange: (CalendarMode) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    currentMonth: java.time.YearMonth,
    onCurrentMonthChange: (java.time.YearMonth) -> Unit,
    currentWeekStart: LocalDate,
    onCurrentWeekStartChange: (LocalDate) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        innerPadding = 12.dp
    ) {
        CalendarView(
            entryDates = entryDates,
            dayInfoMap = dayInfoMap,
            selectedDate = selectedDate,
            onDateSelected = onDateSelected,
            calendarMode = calendarMode,
            onModeChange = onModeChange,
            currentMonth = currentMonth,
            onCurrentMonthChange = onCurrentMonthChange,
            currentWeekStart = currentWeekStart,
            onCurrentWeekStartChange = onCurrentWeekStartChange
        )
    }
}

@Composable
private fun QuickShortcutsSection(
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    var shortcutRoutes by remember { mutableStateOf(QuickShortcutStore.getShortcuts(context)) }
    var showPicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
            shortcutRoutes.forEach { route ->
                val option = QuickShortcutStore.getOption(route) ?: return@forEach
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onNavigate(route) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = option.label,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // Edit button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { showPicker = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "编辑",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "编辑",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

    if (showPicker) {
        QuickShortcutPickerSheet(
            currentRoutes = shortcutRoutes,
            onDismiss = { showPicker = false },
            onConfirm = { newRoutes ->
                QuickShortcutStore.setShortcuts(context, newRoutes)
                shortcutRoutes = newRoutes
                showPicker = false
            }
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
        else -> date.format(DateTimeFormatter.ofPattern("M月d日 · EEEE"))
    }

    if (multiSelectState.isEnabled) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 18.dp,
            innerPadding = 12.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
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
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
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
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
        Text(text = label, fontSize = 11.sp, color = contentColor)
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
    imagePaths: List<String>,
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
    val hasImage = imagePaths.isNotEmpty()

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
            innerPadding = 0.dp
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Background: image full coverage or light mood color
                if (hasImage) {
                    if (imagePaths.size > 1) {
                        val pagerState = rememberPagerState { imagePaths.size }
                        Box {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(18.dp))
                            ) { page ->
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(File(imagePaths[page]))
                                        .crossfade(true)
                                        .size(400)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            // Dark overlay
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.35f),
                                                Color.Black.copy(alpha = 0.55f)
                                            )
                                        )
                                    )
                            )
                            // Dot indicators
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                repeat(imagePaths.size) { index ->
                                    Box(
                                        modifier = Modifier
                                            .size(if (pagerState.currentPage == index) 6.dp else 5.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (pagerState.currentPage == index) Color.White
                                                else Color.White.copy(alpha = 0.45f)
                                            )
                                    )
                                }
                            }
                        }
                    } else {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(File(imagePaths[0]))
                                .crossfade(true)
                                .size(400)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(18.dp))
                        )
                        // Dark overlay for text readability
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.35f),
                                            Color.Black.copy(alpha = 0.55f)
                                        )
                                    )
                                )
                        )
                    }
                } else if (moodData != null) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(moodData.tint.copy(alpha = 0.18f))
                    )
                }

                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                            color = if (hasImage) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = entry.title.ifBlank { "未命名日记" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (hasImage) Color.White else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (entry.plainText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = cleanPreviewText(entry.plainText),
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = if (hasImage) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (!entry.location.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = if (hasImage) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = entry.location,
                            fontSize = 11.sp,
                            color = if (hasImage) Color.White.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
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
                        if (imagePaths.size > 1) {
                            MetaChip(
                                icon = Icons.Default.Image,
                                label = "${imagePaths.size} 张图片",
                                tint = if (hasImage) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        moodData?.let { mood ->
                            MetaChip(icon = mood.icon, label = moodLabelForLevel(entry.moodLevel), tint = if (hasImage) Color.White.copy(alpha = 0.8f) else mood.tint)
                        }
                        weatherData?.let { weather ->
                            MetaChip(icon = weather.icon, label = weatherLabelFor(entry.weather), tint = if (hasImage) Color.White.copy(alpha = 0.7f) else weather.tint)
                        }
                        tags.take(2).forEach { tag ->
                            ColorTagChip(tag = tag, lightMode = hasImage)
                        }
                        if (tags.size > 2) {
                            SubtleTextChip(text = "+${tags.size - 2}", lightMode = hasImage)
                        }
                    }
                }
            }
            } // close inner Box
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
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = tint)
    }
}

@Composable
private fun ColorTagChip(tag: TagInfo, lightMode: Boolean = false) {
    val chipColor = if (lightMode) Color.White else tag.color
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(chipColor.copy(alpha = if (lightMode) 0.18f else 0.10f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = tag.name, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = if (lightMode) Color.White.copy(alpha = 0.85f) else tag.color)
    }
}

@Composable
private fun SubtleTextChip(text: String, lightMode: Boolean = false) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (lightMode) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = text, fontSize = 10.sp, color = if (lightMode) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant)
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

@Composable
private fun SearchResultCard(
    entry: DiaryPreview,
    imageMap: Map<Long, String>,
    onClick: () -> Unit
) {
    val entryDate = java.time.Instant.ofEpochMilli(entry.createdAt)
        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    val dateStr = "${entryDate.monthValue}/${entryDate.dayOfMonth}"

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        cornerRadius = 12.dp,
        innerPadding = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.title.ifBlank { "无标题" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                if (entry.plainText.isNotBlank()) {
                    Text(
                        text = cleanPreviewText(entry.plainText),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            entry.moodLevel?.let { level ->
                Icon(
                    imageVector = moodIconForLevel(level).icon,
                    contentDescription = moodLabelForLevel(level),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp).padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun OnThisDayCard(
    entries: List<DiaryPreview>,
    onClick: (DiaryPreview) -> Unit
) {
    val today = java.time.LocalDate.now()
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        innerPadding = 14.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "那年今日",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            entries.take(3).forEach { entry ->
                val entryDate = java.time.Instant.ofEpochMilli(entry.createdAt)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                val yearsAgo = today.year - entryDate.year
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClick(entry) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${yearsAgo}年前",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.width(40.dp)
                    )
                    Text(
                        text = entry.title.ifBlank { "无标题" },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    entry.moodLevel?.let { level ->
                        Icon(
                            imageVector = moodIconForLevel(level).icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp).padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
