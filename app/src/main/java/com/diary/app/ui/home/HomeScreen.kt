package com.diary.app.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.R
import com.diary.app.data.DiaryEntry
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.moodColorForLevel
import com.diary.app.ui.components.moodIconForLevel
import com.diary.app.ui.components.staggeredListItem
import com.diary.app.ui.components.rememberHapticFeedback
import com.diary.app.ui.theme.DesignTokens
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToEditor: (Long?) -> Unit,
    onNavigateToReview: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val haptic = rememberHapticFeedback()
    val entries by viewModel.entries.collectAsState()
    val entryDates by viewModel.entryDates.collectAsState()
    val dayInfoMap by viewModel.dayInfoMap.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchResultCount by viewModel.searchResultCount.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val selectedTagFilter by viewModel.selectedTagFilter.collectAsState()

    val isSearchActive = searchQuery.isNotBlank()

    // First launch welcome
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("diary_prefs", android.content.Context.MODE_PRIVATE) }
    var showWelcome by remember { mutableStateOf(!prefs.getBoolean("has_seen_welcome", false)) }
    LaunchedEffect(showWelcome) {
        if (!showWelcome) {
            prefs.edit().putBoolean("has_seen_welcome", true).apply()
        }
    }
    var showCalendar by remember { mutableStateOf(true) }  // Calendar expanded by default
    var calendarMode by remember {
        mutableStateOf(
            if (prefs.getString("calendar_mode", "MONTH") == "WEEK") CalendarMode.WEEK else CalendarMode.MONTH
        )
    }

    // Multi-select state
    var selectedEntries by remember { mutableStateOf(setOf<Long>()) }
    val isMultiSelectMode = selectedEntries.isNotEmpty()

    var entryToDelete by remember { mutableStateOf<DiaryEntry?>(null) }
    var showMultiSelectDeleteDialog by remember { mutableStateOf(false) }

    // Multi-select delete confirmation
    if (showMultiSelectDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showMultiSelectDeleteDialog = false },
            title = { Text("删除 ${selectedEntries.size} 条日记") },
            text = { Text("确定要删除选中的 ${selectedEntries.size} 条日记吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    haptic.warning()
                    selectedEntries.forEach { id ->
                        viewModel.deleteEntryById(id)
                    }
                    selectedEntries = emptySet()
                    showMultiSelectDeleteDialog = false
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMultiSelectDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Single delete confirmation
    if (entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text(stringResource(R.string.confirm_delete)) },
            text = { Text(stringResource(R.string.confirm_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    haptic.warning()
                    entryToDelete?.let { viewModel.deleteEntry(it) }
                    entryToDelete = null
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    GradientBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            // Multi-select top bar
            if (isMultiSelectMode) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Close button
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "取消多选",
                            modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    haptic.click()
                                    selectedEntries = emptySet()
                                },
                            tint = MaterialTheme.colorScheme.onSurface
                        )

                        // Selected count
                        Text(
                            text = "已选 ${selectedEntries.size} 条",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Select all
                            Text(
                                text = "全选",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    haptic.click()
                                    selectedEntries = if (selectedEntries.size == entries.size) {
                                        emptySet()
                                    } else {
                                        entries.map { it.id }.toSet()
                                    }
                                }
                            )

                            // Delete
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除",
                                modifier = Modifier
                                    .size(22.dp)
                                    .clickable {
                                        if (selectedEntries.isNotEmpty()) {
                                            haptic.warning()
                                            showMultiSelectDeleteDialog = true
                                        }
                                    },
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Greeting header
                item {
                    GreetingHeader()
                }

                // Welcome message for first-time users
                if (showWelcome && entries.isEmpty() && !isLoading) {
                    item {
                        WelcomeCard(onDismiss = { showWelcome = false })
                    }
                }

                // Search bar
                item {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { viewModel.setSearchQuery(it) },
                        onCommitSearch = { viewModel.commitSearch(it) },
                        resultCount = if (isSearchActive) searchResultCount else -1
                    )
                }

                // Recent searches
                if (searchQuery.isBlank() && recentSearches.isNotEmpty()) {
                    item {
                        RecentSearchesRow(
                            searches = recentSearches,
                            onSelect = { viewModel.setSearchQuery(it) },
                            onClear = { viewModel.clearSearchHistory() }
                        )
                    }
                }

                // Tag filter chips
                if (!isSearchActive && allTags.isNotEmpty()) {
                    item {
                        TagFilterRow(
                            tags = allTags,
                            selectedTagId = selectedTagFilter,
                            onTagSelected = { viewModel.setTagFilter(it) }
                        )
                    }
                }

                // Compact stats + calendar toggle in one row
                if (!isSearchActive) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CompactStatsRow(
                                stats = stats,
                                onNavigateToReview = onNavigateToReview,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            CalendarToggleButton(
                                isExpanded = showCalendar,
                                onToggle = { showCalendar = !showCalendar },
                                selectedDate = selectedDate,
                                onClearDate = { viewModel.selectDate(null) }
                            )
                        }
                    }
                    if (showCalendar) {
                        item {
                            CalendarView(
                                entryDates = entryDates,
                                dayInfoMap = dayInfoMap,
                                selectedDate = selectedDate,
                                onDateSelected = { date ->
                                    viewModel.selectDate(if (date == selectedDate) null else date)
                                },
                                calendarMode = calendarMode,
                                onModeChange = { mode ->
                                    calendarMode = mode
                                    prefs.edit().putString("calendar_mode", mode.name).apply()
                                }
                            )
                        }
                    }
                }

                // Selected date indicator
                if (selectedDate != null && !isSearchActive) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedDate!!.monthValue}月${selectedDate!!.dayOfMonth}日的日记",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "查看全部",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable { viewModel.selectDate(null) }
                            )
                        }
                    }
                }

                if (isLoading) {
                    items(3) { ShimmerDiaryCard() }
                } else if (entries.isEmpty()) {
                    item { EmptyState() }
                } else {
                    itemsIndexed(entries, key = { _, entry -> entry.id }) { index, entry ->
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) { visible = true }

                        Box(
                            modifier = Modifier.staggeredListItem(
                                index = index,
                                visible = visible,
                                initialDelayMs = 30,
                                itemDelayMs = 30
                            )
                        ) {
                            DiaryCardWithContextMenu(
                                entry = entry,
                                searchQuery = searchQuery,
                                isSelected = entry.id in selectedEntries,
                                isMultiSelectMode = isMultiSelectMode,
                                onClick = {
                                    if (isMultiSelectMode) {
                                        // Toggle selection
                                        haptic.click()
                                        selectedEntries = if (entry.id in selectedEntries) {
                                            selectedEntries - entry.id
                                        } else {
                                            selectedEntries + entry.id
                                        }
                                    } else {
                                        haptic.click()
                                        onNavigateToDetail(entry.id)
                                    }
                                },
                                onLongClick = {
                                    haptic.click()
                                    if (!isMultiSelectMode) {
                                        // Enter multi-select mode
                                        selectedEntries = setOf(entry.id)
                                    }
                                },
                                onDelete = { entryToDelete = entry }
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }

            // FAB
            FAB(onClick = { onNavigateToEditor(null) }, isEmpty = entries.isEmpty() && !isLoading)
        }
    }
}

@Composable
private fun GreetingHeader() {
    val now = LocalTime.now()
    val greeting = when {
        now.hour < 6 -> "夜深了，记录今天的思绪吧"
        now.hour < 12 -> "早安，新的一天"
        now.hour < 14 -> "午安，午后时光"
        now.hour < 18 -> "下午好，阳光正好"
        now.hour < 22 -> "晚上好，记录美好"
        else -> "夜深了，写下感悟"
    }
    val today = LocalDate.now()
    val dateFormatter = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINESE)
    val dateText = today.format(dateFormatter)

    Column(modifier = Modifier.padding(bottom = 4.dp)) {
        Text(
            text = greeting,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = dateText,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun WelcomeCard(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DesignTokens.CornerMedium))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .padding(DesignTokens.SpacingLg)
    ) {
        Text(
            text = "欢迎使用日记本",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "这里是属于你的私人空间，记录生活的点滴，留住每一个值得记住的瞬间。",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "点击右下角的按钮，开始你的第一篇日记吧。",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "我知道了",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.End)
                .clickable { onDismiss() }
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onCommitSearch: (String) -> Unit,
    resultCount: Int = -1
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onCommitSearch(query) }),
                    placeholder = {
                        Text(
                            "搜索日记...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onQueryChange("") }
                            )
                        }
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                )

                Spacer(modifier = Modifier.width(12.dp))
            }
        }

        // Search result count
        if (resultCount >= 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "找到 $resultCount 条日记",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun CompactStatsRow(stats: HomeStats, onNavigateToReview: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable { onNavigateToReview() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${stats.total}篇",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text("·", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
        Text(
            "连续${stats.streak}天",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text("·", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
        Text(
            "本月${stats.thisMonth}篇",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CalendarToggleButton(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    selectedDate: LocalDate?,
    onClearDate: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DesignTokens.CornerMedium))
            .clickable { onToggle() }
            .padding(horizontal = DesignTokens.SpacingMd, vertical = DesignTokens.SpacingSm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = if (selectedDate != null) {
                    "${selectedDate.monthValue}月${selectedDate.dayOfMonth}日的日记"
                } else {
                    "日历"
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (selectedDate != null) {
                Icon(Icons.Default.Close, contentDescription = "清除选择", modifier = Modifier
                    .size(14.dp)
                    .clickable { onClearDate() },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        }
        Icon(
            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "收起" else "展开",
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun FAB(onClick: () -> Unit, isEmpty: Boolean = false) {
    val fabColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(end = 20.dp, bottom = 16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        // Pulse ring behind FAB - only create infinite transition when needed
        if (isEmpty) {
            FABPulseRing()
        }
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
private fun FABPulseRing() {
    val infiniteTransition = rememberInfiniteTransition(label = "fabPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fabPulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fabPulseAlpha"
    )
    Box(
        modifier = Modifier
            .size(72.dp)
            .graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
                alpha = pulseAlpha
            }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    )
}

@Composable
private fun EmptyState() {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val prompt = remember { com.diary.app.data.WritingPrompts.getRandomPrompt() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp, bottom = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "开始你的故事",
                fontSize = 20.sp,
                color = onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = prompt,
                fontSize = 14.sp,
                color = onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 32.dp),
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "点击右下角按钮开始写作",
                fontSize = 12.sp,
                color = onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun ShimmerDiaryCard() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "translate"
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        ),
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 0f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(16.dp)
    ) {
        Column {
            // Title placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Line 1
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Line 2
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Bottom row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(shimmerBrush)
                )
            }
        }
    }
}

@Composable
private fun DiaryCardWithContextMenu(
    entry: DiaryEntry,
    searchQuery: String = "",
    isSelected: Boolean = false,
    isMultiSelectMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    Box {
        SwipeableDiaryCard(
            entry = entry,
            searchQuery = searchQuery,
            isSelected = isSelected,
            isMultiSelectMode = isMultiSelectMode,
            onClick = onClick,
            onLongClick = onLongClick,
            onDelete = onDelete
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableDiaryCard(
    entry: DiaryEntry,
    searchQuery: String = "",
    isSelected: Boolean = false,
    isMultiSelectMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    if (isMultiSelectMode) {
        // In multi-select mode, no swipe - just tap to select
        DiaryCard(
            entry = entry,
            searchQuery = searchQuery,
            isSelected = isSelected,
            onClick = onClick,
            onLongClick = onLongClick
        )
    } else {
        val dismissState = rememberDismissState(
            confirmValueChange = { value ->
                when (value) {
                    DismissValue.DismissedToStart -> {
                        onDelete()
                        false
                    }
                    else -> false
                }
            }
        )

        val errorColor = MaterialTheme.colorScheme.error

        SwipeToDismiss(
            state = dismissState,
            background = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(errorColor)
                        .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            dismissContent = {
                DiaryCard(
                    entry = entry,
                    searchQuery = searchQuery,
                    isSelected = isSelected,
                    onClick = onClick,
                    onLongClick = onLongClick
                )
            },
            directions = setOf(DismissDirection.EndToStart)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiaryCard(
    entry: DiaryEntry,
    searchQuery: String = "",
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardScale"
    )

    val onBackground = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = MaterialTheme.colorScheme.primary

    // Mood accent color for left border
    val moodColor = entry.moodLevel?.let { moodColorForLevel(it) }

    // Selection border color
    val selectionBorder = if (isSelected) {
        Modifier.border(
            width = 2.dp,
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(16.dp)
        )
    } else if (moodColor != null) {
        Modifier.border(
            width = 0.dp,
            color = Color.Transparent,
            shape = RoundedCornerShape(16.dp)
        )
    } else {
        Modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(16.dp)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            )
            .then(selectionBorder)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row {
            // Mood accent bar on left
            if (moodColor != null) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(moodColor.copy(alpha = 0.6f))
                )
            }

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Title (if present)
                if (entry.title.isNotBlank()) {
                    Text(
                        text = entry.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Preview text (limited to 60 chars)
                val previewSource = entry.plainText
                    .replace("\\n", " ")  // Handle old entries with literal \n
                    .replace("\n", " ")   // Handle actual newlines
                    .trim()
                    .take(60)
                if (previewSource.isNotBlank()) {
                    val displayText by remember(previewSource, searchQuery, accentColor) {
                        derivedStateOf {
                            if (searchQuery.isNotBlank()) {
                                highlightText(previewSource, searchQuery, accentColor)
                            } else {
                                AnnotatedString(previewSource)
                            }
                        }
                    }
                    Text(
                        text = displayText,
                        fontSize = 14.sp,
                        color = onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                }

                // Bottom row: date + mood + favorite
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatCardDate(entry.createdAt),
                        fontSize = 12.sp,
                        color = onSurfaceVariant.copy(alpha = 0.6f)
                    )

                    if (entry.moodLevel != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val (moodIcon, moodTint) = moodIconForLevel(entry.moodLevel)
                        Icon(
                            imageVector = moodIcon,
                            contentDescription = null,
                            tint = moodTint.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (entry.isFavorite) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Highlight matching keyword in text with primary color background */
private fun highlightText(text: String, query: String, highlightColor: Color): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    val highlightBg = highlightColor.copy(alpha = 0.2f)
    val highlightFg = highlightColor

    return buildAnnotatedString {
        var start = 0
        while (start < text.length) {
            val index = lowerText.indexOf(lowerQuery, start)
            if (index == -1) {
                append(text.substring(start))
                break
            }
            append(text.substring(start, index))
            withStyle(
                SpanStyle(
                    color = highlightFg,
                    background = highlightBg,
                    fontWeight = FontWeight.SemiBold
                )
            ) {
                append(text.substring(index, index + query.length))
            }
            start = index + query.length
        }
    }
}

@Composable
private fun RecentSearchesRow(
    searches: List<String>,
    onSelect: (String) -> Unit,
    onClear: () -> Unit
) {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "最近搜索",
                    tint = onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.recent_searches),
                    fontSize = 12.sp,
                    color = onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            Text(
                text = stringResource(R.string.clear),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.clickable { onClear() }
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            searches.forEach { search ->
                Text(
                    text = search,
                    fontSize = 12.sp,
                    color = onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { onSelect(search) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun TagFilterRow(
    tags: List<com.diary.app.data.Tag>,
    selectedTagId: Long?,
    onTagSelected: (Long?) -> Unit
) {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All" chip
        Text(
            text = "全部",
            fontSize = 12.sp,
            color = if (selectedTagId == null) primary else onSurfaceVariant.copy(alpha = 0.6f),
            fontWeight = if (selectedTagId == null) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (selectedTagId == null) primary.copy(alpha = 0.1f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
                .clickable { onTagSelected(null) }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )

        tags.forEach { tag ->
            val isSelected = selectedTagId == tag.id
            Text(
                text = tag.name,
                fontSize = 12.sp,
                color = if (isSelected) primary else onSurfaceVariant.copy(alpha = 0.6f),
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) primary.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    .clickable { onTagSelected(tag.id) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

private fun formatCardDate(timestamp: Long): String {
    val entryDate = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(entryDate, today).toInt()

    return when {
        entryDate == today -> "今天"
        entryDate == yesterday -> "昨天"
        daysBetween in 2..6 -> "${daysBetween}天前"
        else -> {
            val formatter = DateTimeFormatter.ofPattern("M月d日", Locale.getDefault())
            entryDate.format(formatter)
        }
    }
}
