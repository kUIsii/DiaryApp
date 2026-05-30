package com.diary.app.ui.home

import android.content.Intent
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.moodColorForLevel
import com.diary.app.ui.components.moodIconForLevel
import com.diary.app.ui.components.moodLabelForLevel
import com.diary.app.ui.components.staggeredListItem
import com.diary.app.ui.components.weatherIconFor
import com.diary.app.ui.components.weatherLabelFor
import com.diary.app.ui.components.rememberHapticFeedback
import com.diary.app.ui.theme.DarkAccentEnd
import com.diary.app.ui.theme.DarkAccentStart
import com.diary.app.ui.theme.DesignTokens
import com.diary.app.ui.theme.LightAccentEnd
import com.diary.app.ui.theme.LightAccentStart
import com.diary.app.ui.theme.isDark
import com.diary.app.ui.theme.themeMode
import kotlinx.coroutines.delay
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
    val onThisDayEntries by viewModel.onThisDayEntries.collectAsState()
    val reviewEntries by viewModel.reviewEntries.collectAsState()
    val searchResultCount by viewModel.searchResultCount.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val moodTrend by viewModel.moodTrend.collectAsState()

    val isSearchActive = searchQuery.isNotBlank()
    var showCalendar by remember { mutableStateOf(false) }  // Calendar collapsed by default

    var entryToDelete by remember { mutableStateOf<DiaryEntry?>(null) }

    // Delete confirmation dialog
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Greeting header
                item {
                    GreetingHeader()
                }

                // Search bar
                item {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { viewModel.setSearchQuery(it) },
                        onCommitSearch = { viewModel.commitSearch(it) },
                        sortOrder = sortOrder,
                        onSortOrderChange = { viewModel.setSortOrder(it) },
                        resultCount = if (isSearchActive) searchResultCount else -1
                    )
                }

                // Recent searches - show when search bar is focused but query is empty
                if (searchQuery.isBlank() && recentSearches.isNotEmpty()) {
                    item {
                        RecentSearchesRow(
                            searches = recentSearches,
                            onSelect = { viewModel.setSearchQuery(it) },
                            onClear = { viewModel.clearSearchHistory() }
                        )
                    }
                }

                // On This Day card
                if (!isSearchActive && onThisDayEntries.isNotEmpty()) {
                    item {
                        OnThisDayCard(
                            entries = onThisDayEntries,
                            onEntryClick = onNavigateToDetail
                        )
                    }
                }

                // Review card
                if (!isSearchActive && reviewEntries.isNotEmpty()) {
                    item {
                        ReviewCardHome(
                            reviewEntries = reviewEntries,
                            onEntryClick = onNavigateToDetail,
                            onViewAll = onNavigateToReview
                        )
                    }
                }

                // Stats card - compact version
                if (!isSearchActive) {
                    item {
                        CompactStatsRow(stats = stats, onNavigateToReview = onNavigateToReview)
                    }
                }

                // Mood trend for the last 7 days
                if (!isSearchActive && moodTrend.any { it.moodLevel != null }) {
                    item {
                        MoodTrendRow(moodTrend = moodTrend)
                    }
                }

                // Quick mood check-in (only show if no entry today)
                if (!isSearchActive && !entryDates.contains(LocalDate.now())) {
                    item {
                        QuickMoodCheckIn(
                            onMoodSelected = { level ->
                                onNavigateToEditor(null)
                            }
                        )
                    }
                }

                // Calendar toggle button + collapsible calendar
                if (!isSearchActive) {
                    item {
                        CalendarToggleButton(
                            isExpanded = showCalendar,
                            onToggle = { showCalendar = !showCalendar },
                            selectedDate = selectedDate,
                            onClearDate = { viewModel.selectDate(null) }
                        )
                    }
                    if (showCalendar) {
                        item {
                            CalendarView(
                                entryDates = entryDates,
                                dayInfoMap = dayInfoMap,
                                selectedDate = selectedDate,
                                onDateSelected = { date ->
                                    viewModel.selectDate(if (date == selectedDate) null else date)
                                }
                            )
                        }
                    }
                }

                if (selectedDate != null && !isSearchActive) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.diary_for_date, selectedDate!!.monthValue, selectedDate!!.dayOfMonth),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.view_all),
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
                                onClick = {
                                    haptic.click()
                                    onNavigateToDetail(entry.id)
                                },
                                onEdit = { onNavigateToEditor(entry.id) },
                                onDelete = { entryToDelete = entry },
                                onToggleFavorite = {
                                    haptic.success()
                                    viewModel.toggleFavorite(entry)
                                }
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
private fun QuickMoodCheckIn(
    onMoodSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val moodColors = listOf(
        Color(0xFF6C7A89),  // 沮丧 - muted blue-grey
        Color(0xFF9B8EA8),  // 低落 - soft purple
        Color(0xFF7FB5A0),  // 平静 - sage green
        Color(0xFFF5C76E),  // 开心 - warm yellow
        Color(0xFFF2994A),  // 愉快 - orange
        Color(0xFFEB5757)   // 兴奋 - vibrant red
    )
    val moodLabels = listOf("沮", "低", "平", "喜", "悦", "奋")

    var selectedMood by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DesignTokens.CornerMedium))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
            .padding(horizontal = DesignTokens.SpacingMd, vertical = DesignTokens.SpacingSm)
    ) {
        Text(
            text = "现在的心情",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            moodColors.forEachIndexed { index, color ->
                val level = index + 1
                val isSelected = selectedMood == level
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.1f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "moodScale"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            selectedMood = level
                            onMoodSelected(level)
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) color.copy(alpha = 0.2f)
                                else color.copy(alpha = 0.08f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = moodLabels[index],
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) color else color.copy(alpha = 0.7f)
                        )
                    }
                }
            }
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
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onCommitSearch: (String) -> Unit,
    sortOrder: HomeViewModel.SortOrder,
    onSortOrderChange: (HomeViewModel.SortOrder) -> Unit,
    resultCount: Int = -1
) {
    var showSortMenu by remember { mutableStateOf(false) }

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

                Box {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { showSortMenu = true }
                    )

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        HomeViewModel.SortOrder.entries.forEach { order ->
                            val isSelected = order == sortOrder
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = order.label,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                trailingIcon = {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    onSortOrderChange(order)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }

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
private fun CompactStatsRow(stats: HomeStats, onNavigateToReview: () -> Unit) {
    val streakMessage = when (stats.streak) {
        3 -> "开始坚持了!"
        7 -> "一周不间断!"
        14 -> "两周持续记录!"
        30 -> "一个月的坚持!"
        60 -> "两个月不间断!"
        100 -> "百日如一日!"
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable { onNavigateToReview() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${stats.total}篇",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("·", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                Text(
                    "连续${stats.streak}天",
                    fontSize = 13.sp,
                    color = if (streakMessage != null) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("·", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                Text(
                    "本月${stats.thisMonth}篇",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "查看详情",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }

        // Streak celebration message
        if (streakMessage != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = streakMessage,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
        }
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
private fun MoodTrendRow(moodTrend: List<HomeViewModel.MoodDay>) {
    val dayLabels = listOf("日", "一", "二", "三", "四", "五", "六")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DesignTokens.CornerMedium))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
            .padding(horizontal = DesignTokens.SpacingMd, vertical = DesignTokens.SpacingSm)
    ) {
        Text(
            text = "近7天心情",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            moodTrend.forEach { day ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val dayOfWeek = day.date.dayOfWeek.value % 7
                    Text(
                        text = dayLabels[dayOfWeek],
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (day.moodLevel != null) {
                                    moodColorForLevel(day.moodLevel).copy(alpha = 0.15f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day.moodLevel != null) {
                            Text(
                                text = moodTextForLevel(day.moodLevel),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = moodColorForLevel(day.moodLevel).copy(alpha = 0.8f)
                            )
                        }
                    }
                    Text(
                        text = "${day.date.dayOfMonth}",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

private fun moodTextForLevel(level: Int): String {
    return when (level) {
        1 -> "沮"
        2 -> "低"
        3 -> "平"
        4 -> "喜"
        5 -> "悦"
        6 -> "奋"
        else -> "平"
    }
}

@Composable
private fun StatsCard(stats: HomeStats) {
    val dark = themeMode().isDark()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatItem(
            value = stats.total,
            label = stringResource(R.string.stat_total_diaries),
            icon = Icons.Default.Edit,
            gradientColors = if (dark) listOf(Color(0xFF667eea).copy(alpha = 0.25f), Color(0xFF764ba2).copy(alpha = 0.15f))
            else listOf(Color(0xFF667eea).copy(alpha = 0.12f), Color(0xFF764ba2).copy(alpha = 0.08f)),
            modifier = Modifier.weight(1f)
        )
        StatItem(
            value = stats.streak,
            label = stringResource(R.string.stat_streak),
            icon = Icons.Default.LocalFireDepartment,
            gradientColors = if (dark) listOf(Color(0xFFf093fb).copy(alpha = 0.25f), Color(0xFFf5576c).copy(alpha = 0.15f))
            else listOf(Color(0xFFf093fb).copy(alpha = 0.12f), Color(0xFFf5576c).copy(alpha = 0.08f)),
            modifier = Modifier.weight(1f)
        )
        StatItem(
            value = stats.thisMonth,
            label = stringResource(R.string.stat_this_month),
            icon = Icons.Default.CalendarMonth,
            gradientColors = if (dark) listOf(Color(0xFF4facfe).copy(alpha = 0.25f), Color(0xFF00f2fe).copy(alpha = 0.15f))
            else listOf(Color(0xFF4facfe).copy(alpha = 0.12f), Color(0xFF00f2fe).copy(alpha = 0.08f)),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AnimatedCounter(
    targetValue: Int,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineLarge,
    fontWeight: FontWeight = FontWeight.Bold,
    color: Color = Color.White
) {
    var currentValue by remember { mutableIntStateOf(0) }
    LaunchedEffect(targetValue) {
        if (targetValue == 0) {
            currentValue = 0
            return@LaunchedEffect
        }
        val steps = 20
        val stepDelay = 30L
        for (i in 0..steps) {
            currentValue = (targetValue * i / steps)
            delay(stepDelay)
        }
        currentValue = targetValue
    }
    Text(
        text = "$currentValue",
        style = style,
        fontWeight = fontWeight,
        color = color,
        modifier = modifier
    )
}

@Composable
private fun StatItem(
    value: Int,
    label: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(gradientColors))
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            AnimatedCounter(
                targetValue = value,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FAB(onClick: () -> Unit, isEmpty: Boolean = false) {
    val dark = themeMode().isDark()
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
                tint = Color.White,
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
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "记录生活中的点滴",
                fontSize = 14.sp,
                color = onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "每一天都值得被记住",
                fontSize = 14.sp,
                color = onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
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
            Color.LightGray.copy(alpha = 0.3f),
            Color.LightGray.copy(alpha = 0.1f),
            Color.LightGray.copy(alpha = 0.3f)
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
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val context = LocalContext.current
    var showContextMenu by remember { mutableStateOf(false) }

    Box {
        SwipeableDiaryCard(
            entry = entry,
            searchQuery = searchQuery,
            onClick = onClick,
            onLongClick = { showContextMenu = true },
            onDelete = onDelete,
            onShare = {
                val shareText = formatShareText(entry)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    putExtra(Intent.EXTRA_SUBJECT, "日记")
                }
                context.startActivity(Intent.createChooser(intent, "分享日记"))
            }
        )

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit)) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "编辑",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    showContextMenu = false
                    onEdit()
                }
            )
            DropdownMenuItem(
                text = { Text(if (entry.isFavorite) stringResource(R.string.unfavorite) else stringResource(R.string.favorite)) },
                leadingIcon = {
                    Icon(
                        if (entry.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = if (entry.isFavorite) "取消收藏" else "收藏",
                        tint = if (entry.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    showContextMenu = false
                    onToggleFavorite()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.share)) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "分享",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    showContextMenu = false
                    val shareText = formatShareText(entry)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        putExtra(Intent.EXTRA_SUBJECT, "日记")
                    }
                    context.startActivity(Intent.createChooser(intent, "分享日记"))
                }
            )
            Divider()
            DropdownMenuItem(
                text = {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    showContextMenu = false
                    onDelete()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableDiaryCard(
    entry: DiaryEntry,
    searchQuery: String = "",
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val dismissState = rememberDismissState(
        confirmValueChange = { value ->
            when (value) {
                DismissValue.DismissedToEnd -> {
                    onShare()
                    false
                }
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
            val direction = dismissState.dismissDirection
            val isSwipeRight = direction == DismissDirection.StartToEnd

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSwipeRight) Color(0xFF66BB6A) else errorColor)
                    .padding(
                        start = if (isSwipeRight) 24.dp else 0.dp,
                        end = if (isSwipeRight) 0.dp else 24.dp
                    ),
                contentAlignment = if (isSwipeRight) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = if (isSwipeRight) Icons.Default.Share else Icons.Default.Delete,
                    contentDescription = if (isSwipeRight) "分享" else "删除",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        dismissContent = {
            DiaryCard(
                entry = entry,
                searchQuery = searchQuery,
                onClick = onClick,
                onLongClick = onLongClick
            )
        },
        directions = setOf(DismissDirection.StartToEnd, DismissDirection.EndToStart)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiaryCard(
    entry: DiaryEntry,
    searchQuery: String = "",
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            .then(
                if (moodColor != null) {
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
            )
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
                val previewSource = entry.plainText.take(60)
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
private fun OnThisDayCard(
    entries: List<DiaryEntry>,
    onEntryClick: (Long) -> Unit
) {
    val onBackground = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = "那年今日",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = onBackground
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${entries.size}条回忆",
                    fontSize = 12.sp,
                    color = onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            entries.take(3).forEach { entry ->
                val entryDate = Instant.ofEpochMilli(entry.createdAt)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                val yearDiff = LocalDate.now().year - entryDate.year

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onEntryClick(entry.id) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${entryDate.year}年",
                        fontSize = 13.sp,
                        color = onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.width(56.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        if (entry.title.isNotBlank()) {
                            Text(
                                text = entry.title,
                                fontSize = 14.sp,
                                color = onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (entry.plainText.isNotBlank()) {
                            Text(
                                text = entry.plainText,
                                fontSize = 12.sp,
                                color = onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        text = "${yearDiff}年前",
                        fontSize = 11.sp,
                        color = onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            if (entries.size > 3) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "还有${entries.size - 3}条",
                    fontSize = 12.sp,
                    color = onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun ReviewCardHome(
    reviewEntries: List<ReviewEntry>,
    onEntryClick: (Long) -> Unit,
    onViewAll: () -> Unit
) {
    val onBackground = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "日记回顾",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = onBackground
                )
                Text(
                    text = "查看全部",
                    fontSize = 12.sp,
                    color = onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.clickable(onClick = onViewAll)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            reviewEntries.take(3).forEach { reviewItem ->
                val entry = reviewItem.entry
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onEntryClick(entry.id) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = reviewItem.label,
                        fontSize = 13.sp,
                        color = onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.width(64.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        if (entry.title.isNotBlank()) {
                            Text(
                                text = entry.title,
                                fontSize = 14.sp,
                                color = onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (entry.plainText.isNotBlank()) {
                            Text(
                                text = entry.plainText,
                                fontSize = 12.sp,
                                color = onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (entry.moodLevel != null) {
                        val (moodIcon, moodTint) = moodIconForLevel(entry.moodLevel)
                        Icon(
                            imageVector = moodIcon,
                            contentDescription = "心情",
                            tint = moodTint.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
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

private fun formatShareText(entry: DiaryEntry): String {
    val entryDate = Instant.ofEpochMilli(entry.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
    val entryTime = Instant.ofEpochMilli(entry.createdAt).atZone(ZoneId.systemDefault()).toLocalTime()
    val dateText = "${entryDate.year}年${entryDate.monthValue}月${entryDate.dayOfMonth}日"
    val timeText = entryTime.format(DateTimeFormatter.ofPattern("HH:mm"))

    val moodLabel = entry.moodLevel?.let { moodLabelForLevel(it) }

    val weatherLabel = entry.weather?.let { weatherLabelFor(it) }

    val sb = StringBuilder()
    sb.appendLine("$dateText $timeText")

    val metaLine = listOfNotNull(
        moodLabel?.let { "心情: $it" },
        weatherLabel?.let { "天气: $it" }
    ).joinToString(" | ")
    if (metaLine.isNotEmpty()) {
        sb.appendLine(metaLine)
    }

    sb.appendLine()

    if (entry.plainText.isNotBlank()) {
        sb.appendLine(entry.plainText)
    }

    sb.appendLine()
    sb.appendLine("---")
    sb.append("来自 日记本 App")

    return sb.toString()
}
