package com.diary.app.ui.timeline

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.data.DiaryPreview
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.moodColorForLevel
import com.diary.app.ui.components.moodIconForLevel
import com.diary.app.ui.components.moodLabelForLevel
import com.diary.app.ui.theme.themeMode
import com.diary.app.ui.theme.isDark
import com.diary.app.ui.components.rememberHapticFeedback
import com.diary.app.ui.components.weatherIconFor
import com.diary.app.ui.components.weatherLabelFor
import com.diary.app.ui.home.TagInfo
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.DayOfWeek
import androidx.compose.runtime.mutableStateMapOf

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimelineScreen(
    onNavigateToDetail: (Long) -> Unit,
    viewModel: TimelineViewModel = viewModel()
) {
    val haptic = rememberHapticFeedback()
    val entries by viewModel.entries.collectAsState()
    val tagsMap by viewModel.tagsMap.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterState by viewModel.filterState.collectAsState()

    val listState = rememberLazyListState()
    var isSearchExpanded by remember { mutableStateOf(false) }
    var isFilterExpanded by remember { mutableStateOf(false) }

    // Group entries by date
    val dateGroups = remember(entries) {
        entries.groupBy { entry ->
            Instant.ofEpochMilli(entry.createdAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }.toSortedMap(compareByDescending { it })
    }

    // Group by month for month selector
    val monthGroups = remember(entries) {
        entries.groupBy { entry ->
            val date = Instant.ofEpochMilli(entry.createdAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            YearMonth.from(date)
        }.toSortedMap(compareByDescending { it })
    }

    // Track expanded/collapsed state for each month
    val expandedMonths = remember { mutableStateMapOf<YearMonth, Boolean>() }
    val currentMonth = YearMonth.now()
    if (!expandedMonths.containsKey(currentMonth)) {
        expandedMonths[currentMonth] = true
    }

    // Selected month for month selector
    var selectedMonth by remember { mutableStateOf(currentMonth) }

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
                        Column {
                            Text(
                                text = "时间线",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${entries.size} 篇日记",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Search button
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable {
                                        isSearchExpanded = !isSearchExpanded
                                        if (!isSearchExpanded) {
                                            viewModel.setSearchQuery("")
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                                    contentDescription = if (isSearchExpanded) "关闭搜索" else "搜索",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Filter button
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (viewModel.hasActiveFilters()) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        }
                                    )
                                    .clickable { isFilterExpanded = !isFilterExpanded },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = "筛选",
                                    tint = if (viewModel.hasActiveFilters()) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Search bar (compact)
                if (isSearchExpanded) {
                    item {
                        CompactSearchBar(
                            query = searchQuery,
                            onQueryChange = { viewModel.setSearchQuery(it) },
                            onDismiss = {
                                isSearchExpanded = false
                                viewModel.setSearchQuery("")
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Filter panel (compact)
                if (isFilterExpanded) {
                    item {
                        CompactFilterPanel(
                            filterState = filterState,
                            allTags = allTags,
                            onMoodToggle = { viewModel.toggleMoodFilter(it) },
                            onWeatherToggle = { viewModel.toggleWeatherFilter(it) },
                            onTagToggle = { viewModel.toggleTagFilter(it) },
                            onClearFilters = { viewModel.clearFilters() },
                            onDismiss = { isFilterExpanded = false }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Active filters display
                if (viewModel.hasActiveFilters()) {
                    item {
                        ActiveFiltersRow(
                            filterState = filterState,
                            allTags = allTags,
                            onMoodRemove = { viewModel.toggleMoodFilter(it) },
                            onWeatherRemove = { viewModel.toggleWeatherFilter(it) },
                            onTagRemove = { viewModel.toggleTagFilter(it) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Month selector
                item {
                    MonthSelector(
                        months = monthGroups.keys.toList(),
                        selectedMonth = selectedMonth,
                        onMonthClick = { month ->
                            selectedMonth = month
                            expandedMonths[month] = true
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Timeline entries
                if (dateGroups.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank() || viewModel.hasActiveFilters()) {
                                        "没有找到匹配的日记"
                                    } else {
                                        "还没有日记"
                                    },
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    // Display entries grouped by month
                    monthGroups.forEach { (month, monthEntries) ->
                        val isExpanded = expandedMonths[month] ?: false
                        val monthTotal = monthEntries.size

                        // Month header
                        if (!isExpanded) {
                            item(key = "month_$month") {
                                CollapsedMonthHeader(
                                    month = month,
                                    entryCount = monthTotal,
                                    onClick = {
                                        expandedMonths[month] = true
                                    }
                                )
                            }
                        }

                        // Entries for this month (only when expanded)
                        if (isExpanded) {
                            val monthDateGroups = monthEntries.groupBy { entry ->
                                Instant.ofEpochMilli(entry.createdAt)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            }.toSortedMap(compareByDescending { it })

                            monthDateGroups.forEach { (date, dayEntries) ->
                                // Day header
                                item(key = "dayheader_${date}") {
                                    DayHeader(
                                        date = date,
                                        entryCount = dayEntries.size,
                                        isCurrentMonth = month == currentMonth,
                                        onCollapse = {
                                            expandedMonths[month] = false
                                        }
                                    )
                                }

                                // Entries for this day
                                dayEntries.forEachIndexed { index, entry ->
                                    item(key = "entry_${entry.id}") {
                                        val tags = tagsMap[entry.id] ?: emptyList()
                                        val isFirst = index == 0
                                        val isLast = index == dayEntries.size - 1

                                        TimelineEntry(
                                            entry = entry,
                                            tags = tags,
                                            isFirstInDay = isFirst,
                                            isLastInDay = isLast,
                                            onEntryClick = {
                                                haptic.click()
                                                onNavigateToDetail(entry.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom padding
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun MonthSelector(
    months: List<YearMonth>,
    selectedMonth: YearMonth,
    onMonthClick: (YearMonth) -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        months.forEach { month ->
            val isSelected = month == selectedMonth
            val monthText = if (month == YearMonth.now()) {
                "本月"
            } else {
                "${month.monthValue}月"
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    )
                    .clickable { onMonthClick(month) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = monthText,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun CompactSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 10.dp,
        innerPadding = 6.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 8.dp).size(18.dp)
            )
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "搜索...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompactFilterPanel(
    filterState: FilterState,
    allTags: List<TagInfo>,
    onMoodToggle: (Int) -> Unit,
    onWeatherToggle: (String) -> Unit,
    onTagToggle: (Long) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 10.dp,
        innerPadding = 10.dp
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "筛选",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row {
                    TextButton(onClick = onClearFilters) {
                        Text(
                            text = "清除",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Mood filters
            Text(
                text = "心情",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (1..6).forEach { level ->
                    val isSelected = level in filterState.selectedMoods
                    val (icon, tint) = moodIconForLevel(level)
                    val label = moodLabelForLevel(level)

                    CompactFilterChip(
                        label = label,
                        icon = icon,
                        isSelected = isSelected,
                        color = tint,
                        onClick = { onMoodToggle(level) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Weather filters
            Text(
                text = "天气",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("晴天", "多云", "阴天", "雨天", "大风", "雷暴").forEach { weather ->
                    val isSelected = weather in filterState.selectedWeathers
                    val (icon, tint) = weatherIconFor(weather)

                    CompactFilterChip(
                        label = weather,
                        icon = icon,
                        isSelected = isSelected,
                        color = tint,
                        onClick = { onWeatherToggle(weather) }
                    )
                }
            }

            // Tag filters
            if (allTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "标签",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allTags.forEach { tag ->
                        val isSelected = tag.id in filterState.selectedTagIds

                        CompactFilterChip(
                            label = tag.name,
                            isSelected = isSelected,
                            color = tag.color,
                            onClick = { onTagToggle(tag.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactFilterChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isSelected) color.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = label,
                fontSize = 11.sp,
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveFiltersRow(
    filterState: FilterState,
    allTags: List<TagInfo>,
    onMoodRemove: (Int) -> Unit,
    onWeatherRemove: (String) -> Unit,
    onTagRemove: (Long) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        filterState.selectedMoods.forEach { level ->
            val label = moodLabelForLevel(level)
            val (_, tint) = moodIconForLevel(level)
            ActiveFilterChip(
                label = label,
                color = tint,
                onRemove = { onMoodRemove(level) }
            )
        }

        filterState.selectedWeathers.forEach { weather ->
            val (_, tint) = weatherIconFor(weather)
            ActiveFilterChip(
                label = weather,
                color = tint,
                onRemove = { onWeatherRemove(weather) }
            )
        }

        filterState.selectedTagIds.forEach { tagId ->
            val tag = allTags.find { it.id == tagId }
            if (tag != null) {
                ActiveFilterChip(
                    label = tag.name,
                    color = tag.color,
                    onRemove = { onTagRemove(tagId) }
                )
            }
        }
    }
}

@Composable
private fun ActiveFilterChip(
    label: String,
    color: Color,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = color
        )
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "移除",
            tint = color.copy(alpha = 0.7f),
            modifier = Modifier
                .size(12.dp)
                .clickable(onClick = onRemove)
        )
    }
}

@Composable
private fun CollapsedMonthHeader(
    month: YearMonth,
    entryCount: Int,
    onClick: () -> Unit
) {
    val now = YearMonth.now()
    val isCurrentMonth = month == now

    val monthText = if (isCurrentMonth) {
        "本月"
    } else {
        "${month.year}年${month.monthValue}月"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Collapsed indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        )

        Text(
            text = monthText,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "$entryCount",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = "篇日记",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "展开",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun DayHeader(
    date: LocalDate,
    entryCount: Int,
    isCurrentMonth: Boolean,
    onCollapse: () -> Unit
) {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    val isToday = date == today
    val isYesterday = date == yesterday

    val dateLabel = when {
        isToday -> "今天"
        isYesterday -> "昨天"
        else -> {
            val monthDay = date.format(DateTimeFormatter.ofPattern("M.d"))
            monthDay
        }
    }

    val weekdayText = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "周一"
        DayOfWeek.TUESDAY -> "周二"
        DayOfWeek.WEDNESDAY -> "周三"
        DayOfWeek.THURSDAY -> "周四"
        DayOfWeek.FRIDAY -> "周五"
        DayOfWeek.SATURDAY -> "周六"
        DayOfWeek.SUNDAY -> "周日"
    }

    val fullDateText = date.format(DateTimeFormatter.ofPattern("M月d日")) + " · $weekdayText"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Date capsule
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (isToday) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    }
                )
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dateLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isToday) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = fullDateText,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.weight(1f))

        // Entry count and collapse button
        Text(
            text = "$entryCount 篇日记",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimelineEntry(
    entry: DiaryPreview,
    tags: List<TagInfo>,
    isFirstInDay: Boolean,
    isLastInDay: Boolean,
    onEntryClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "entryScale"
    )

    val moodColor = entry.moodLevel?.let { moodColorForLevel(it) }
        ?: MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (isLastInDay) 0.dp else 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Timeline axis
        Column(
            modifier = Modifier.width(44.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Time text
            Text(
                text = formatEntryTime(entry.createdAt),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Dot
            Box(
                modifier = Modifier
                    .size(if (isFirstInDay) 10.dp else 6.dp)
                    .clip(CircleShape)
                    .background(moodColor)
            )

            // Vertical line (not for last entry)
            if (!isLastInDay) {
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .height(80.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                )
            }
        }

        // Card
        Box(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onEntryClick
                )
        ) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 12.dp,
                innerPadding = 12.dp
            ) {
                Column {
                    // Title
                    val isDateTitle = entry.title.matches(Regex("\\d{4}年\\d{1,2}月\\d{1,2}日"))
                    if (entry.title.isNotBlank() && !isDateTitle) {
                        Text(
                            text = entry.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Content preview
                    if (entry.plainText.isNotBlank()) {
                        if (entry.title.isNotBlank() && !isDateTitle) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text(
                            text = cleanPreviewText(entry.plainText),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 18.sp
                        )
                    }

                    // Location
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

                    // Mood + Weather + Tags
                    val hasMoodWeather = entry.moodLevel != null || entry.weather != null
                    val hasTags = tags.isNotEmpty()
                    if (hasMoodWeather || hasTags) {
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

                            if (hasTags) {
                                tags.take(1).forEach { tag ->
                                    Text(
                                        text = tag.name,
                                        fontSize = 10.sp,
                                        color = tag.color,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(tag.color.copy(alpha = 0.1f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                if (tags.size > 1) {
                                    Text(
                                        text = "+${tags.size - 1}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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

private fun cleanPreviewText(text: String): String {
    return text
        .replace("\\n", "\n")
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .replace(Regex("[☐☑✓✔✕✖✗✘❎✅❌]"), "")
        .replace(Regex("^[•·‣⁃]\\s*", RegexOption.MULTILINE), "")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}

private fun formatEntryTime(timestamp: Long): String {
    val localDateTime = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    return localDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
}
