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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import androidx.compose.ui.draw.shadow
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

                // Search bar
                if (isSearchExpanded) {
                    item {
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = { viewModel.setSearchQuery(it) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Filter panel
                if (isFilterExpanded) {
                    item {
                        FilterPanel(
                            filterState = filterState,
                            allTags = allTags,
                            onMoodToggle = { viewModel.toggleMoodFilter(it) },
                            onWeatherToggle = { viewModel.toggleWeatherFilter(it) },
                            onTagToggle = { viewModel.toggleTagFilter(it) },
                            onClearFilters = { viewModel.clearFilters() }
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
                    // Group entries by month and display
                    monthGroups.forEach { (month, monthEntries) ->
                        val isExpanded = expandedMonths[month] ?: false
                        val monthTotal = monthEntries.size

                        // Month header
                        item(key = "month_$month") {
                            MonthGroupHeader(
                                month = month,
                                entryCount = monthTotal,
                                isExpanded = isExpanded,
                                onClick = {
                                    expandedMonths[month] = !isExpanded
                                }
                            )
                        }

                        // Entries for this month (only when expanded)
                        if (isExpanded) {
                            val monthDateGroups = monthEntries.groupBy { entry ->
                                Instant.ofEpochMilli(entry.createdAt)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            }.toSortedMap(compareByDescending { it })

                            monthDateGroups.forEach { (date, dayEntries) ->
                                item(key = "date_${date}") {
                                    DayGroup(
                                        date = date,
                                        entries = dayEntries,
                                        tagsMap = tagsMap,
                                        onEntryClick = { entryId ->
                                            haptic.click()
                                            onNavigateToDetail(entryId)
                                        }
                                    )
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
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 12.dp,
        innerPadding = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "搜索日记内容...",
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "清除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterPanel(
    filterState: FilterState,
    allTags: List<TagInfo>,
    onMoodToggle: (Int) -> Unit,
    onWeatherToggle: (String) -> Unit,
    onTagToggle: (Long) -> Unit,
    onClearFilters: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 12.dp,
        innerPadding = 12.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "筛选",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onClearFilters) {
                    Text(
                        text = "清除全部",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "心情",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (1..6).forEach { level ->
                    val isSelected = level in filterState.selectedMoods
                    val (icon, tint) = moodIconForLevel(level)
                    val label = moodLabelForLevel(level)

                    FilterChip(
                        label = label,
                        icon = icon,
                        isSelected = isSelected,
                        color = tint,
                        onClick = { onMoodToggle(level) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "天气",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("晴天", "多云", "阴天", "雨天", "大风", "雷暴").forEach { weather ->
                    val isSelected = weather in filterState.selectedWeathers
                    val (icon, tint) = weatherIconFor(weather)

                    FilterChip(
                        label = weather,
                        icon = icon,
                        isSelected = isSelected,
                        color = tint,
                        onClick = { onWeatherToggle(weather) }
                    )
                }
            }

            if (allTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "标签",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allTags.forEach { tag ->
                        val isSelected = tag.id in filterState.selectedTagIds

                        FilterChip(
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
private fun FilterChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) color.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                fontSize = 12.sp,
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
                .size(14.dp)
                .clickable(onClick = onRemove)
        )
    }
}

@Composable
private fun MonthGroupHeader(
    month: YearMonth,
    entryCount: Int,
    isExpanded: Boolean,
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
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = monthText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
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
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
            contentDescription = if (isExpanded) "折叠" else "展开",
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayGroup(
    date: LocalDate,
    entries: List<DiaryPreview>,
    tagsMap: Map<Long, List<TagInfo>>,
    onEntryClick: (Long) -> Unit
) {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        // Left: date label + timeline axis
        Column(
            modifier = Modifier.width(50.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Date label (only for first entry)
            val dateText = when (date) {
                today -> "今天"
                yesterday -> "昨天"
                else -> {
                    val monthDay = date.format(DateTimeFormatter.ofPattern("M.d"))
                    val dayOfWeek = when (date.dayOfWeek) {
                        DayOfWeek.MONDAY -> "周一"
                        DayOfWeek.TUESDAY -> "周二"
                        DayOfWeek.WEDNESDAY -> "周三"
                        DayOfWeek.THURSDAY -> "周四"
                        DayOfWeek.FRIDAY -> "周五"
                        DayOfWeek.SATURDAY -> "周六"
                        DayOfWeek.SUNDAY -> "周日"
                    }
                    monthDay
                }
            }
            val weekdayText = when (date) {
                today -> ""
                yesterday -> ""
                else -> {
                    when (date.dayOfWeek) {
                        DayOfWeek.MONDAY -> "周一"
                        DayOfWeek.TUESDAY -> "周二"
                        DayOfWeek.WEDNESDAY -> "周三"
                        DayOfWeek.THURSDAY -> "周四"
                        DayOfWeek.FRIDAY -> "周五"
                        DayOfWeek.SATURDAY -> "周六"
                        DayOfWeek.SUNDAY -> "周日"
                    }
                }
            }

            Text(
                text = dateText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (weekdayText.isNotEmpty()) {
                Text(
                    text = weekdayText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Timeline dot (first entry uses large dot)
            val firstEntry = entries.first()
            val moodColor = firstEntry.moodLevel?.let { moodColorForLevel(it) }
                ?: MaterialTheme.colorScheme.primary

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(moodColor)
            )

            // Vertical line
            if (entries.size > 1) {
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .height(16.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )
            }
        }

        // Right: entry cards
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            entries.forEachIndexed { index, entry ->
                val tags = tagsMap[entry.id] ?: emptyList()
                val isFirst = index == 0

                if (!isFirst) {
                    // Small dot for subsequent entries
                    val moodColor = entry.moodLevel?.let { moodColorForLevel(it) }
                        ?: MaterialTheme.colorScheme.primary

                    Row(
                        modifier = Modifier.padding(start = 0.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(moodColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatEntryTime(entry.createdAt),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }

                TimelineEntryCard(
                    entry = entry,
                    tags = tags,
                    showTime = isFirst,
                    onClick = { onEntryClick(entry.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimelineEntryCard(
    entry: DiaryPreview,
    tags: List<TagInfo>,
    showTime: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "entryCardScale"
    )

    val isDark = themeMode().isDark()
    val moodBgColor = if (isDark) {
        when (entry.moodLevel) {
            1 -> Color(0xFF2D2218)
            2 -> Color(0xFF2D2818)
            3 -> Color(0xFF222222)
            4 -> Color(0xFF1A2D1A)
            5 -> Color(0xFF1A2235)
            6 -> Color(0xFF251A30)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        }
    } else {
        when (entry.moodLevel) {
            1 -> Color(0xFFFFF3E0)
            2 -> Color(0xFFFFF8E1)
            3 -> Color(0xFFF5F5F5)
            4 -> Color(0xFFE8F5E9)
            5 -> Color(0xFFE3F2FD)
            6 -> Color(0xFFF3E5F5)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        }
    }

    Box(
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
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 12.dp,
            innerPadding = 12.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Time (only for first entry)
                    if (showTime) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatEntryTime(entry.createdAt),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Title
                    val isDateTitle = entry.title.matches(Regex("\\d{4}年\\d{1,2}月\\d{1,2}日"))
                    if (entry.title.isNotBlank() && !isDateTitle) {
                        if (showTime) Spacer(modifier = Modifier.height(4.dp))
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
                        Spacer(modifier = Modifier.height(4.dp))
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
