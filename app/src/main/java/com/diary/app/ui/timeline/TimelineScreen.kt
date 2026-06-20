package com.diary.app.ui.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.diary.app.data.DiaryPreview
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.cleanPreviewText
import com.diary.app.ui.components.moodColorForLevel
import com.diary.app.ui.components.moodIconForLevel
import com.diary.app.ui.components.moodLabelForLevel
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
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

// Timeline axis dimensions
private val AXIS_WIDTH = 58.dp
private val DOT_SIZE = 10.dp
private val LINE_WIDTH = 2.5.dp
// Image thumbnail size
private val IMAGE_SIZE = 90.dp
// Minimum card height for uniform sizing
private val MIN_CARD_HEIGHT = 100.dp
// Pre-compiled date title pattern
private val DATE_TITLE_REGEX = Regex("\\d{4}年\\d{1,2}月\\d{1,2}日")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimelineScreen(
    onNavigateToDetail: (Long) -> Unit,
    onMainScreenSwipe: ((Float) -> Unit)? = null,
    viewModel: TimelineViewModel = viewModel()
) {
    val haptic = rememberHapticFeedback()
    val entries by viewModel.entries.collectAsState()
    val tagsMap by viewModel.tagsMap.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val imageMap by viewModel.imageMap.collectAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var isFilterExpanded by remember { mutableStateOf(false) }

    // Show scroll-to-top button when scrolled down
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 2 }
    }

    // Search bar collapse: auto-collapse when scrolled past header
    val isScrolledPastHeader by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }
    var isSearchManuallyExpanded by remember { mutableStateOf(false) }
    val isSearchExpanded = !isScrolledPastHeader || isSearchManuallyExpanded
    // Reset manual expand when scrolling back to top
    LaunchedEffect(isScrolledPastHeader) {
        if (!isScrolledPastHeader) {
            isSearchManuallyExpanded = false
        }
    }

    // Group entries by month and date
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

    // Build flat list of items for LazyColumn with stable keys
    data class TimelineItem(val key: String, val type: String, val month: YearMonth? = null, val date: LocalDate? = null, val entry: DiaryPreview? = null)

    // Don't wrap in remember — expandedMonths is a SnapshotStateMap (reference-stable),
    // so remember(key) won't recompute on content changes. Direct computation lets
    // Compose snapshot tracking detect reads from expandedMonths and recompose correctly.
    val timelineItems = buildList<TimelineItem> {
        monthGroups.forEach { (month, monthEntries) ->
            val isExpanded = expandedMonths[month] ?: false
            if (!isExpanded) {
                add(TimelineItem(key = "collapsed_$month", type = "collapsed_month", month = month))
            } else {
                add(TimelineItem(key = "expanded_$month", type = "expanded_month", month = month))
                val monthDateGroups = monthEntries.groupBy { entry ->
                    Instant.ofEpochMilli(entry.createdAt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                }.toSortedMap(compareByDescending { it })
                monthDateGroups.forEach { (date, dayEntries) ->
                    add(TimelineItem(key = "day_$date", type = "day_header", date = date))
                    dayEntries.forEachIndexed { _, entry ->
                        add(TimelineItem(
                            key = "entry_${entry.id}",
                            type = "entry",
                            entry = entry,
                            date = date
                        ))
                    }
                }
            }
        }
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
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Page header
                item(key = "header") {
                    Column(modifier = Modifier
                        .padding(horizontal = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "时间线",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "${entries.size} 篇日记",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Integrated search + filter bar (collapsible)
                item(key = "search_bar") {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        if (isSearchExpanded) {
                            IntegratedSearchBar(
                                query = searchQuery,
                                onQueryChange = { viewModel.setSearchQuery(it) },
                                hasActiveFilters = viewModel.hasActiveFilters(),
                                onFilterClick = { isFilterExpanded = !isFilterExpanded }
                            )
                        } else {
                            CompactSearchRow(
                                query = searchQuery,
                                hasActiveFilters = viewModel.hasActiveFilters(),
                                onExpand = { isSearchManuallyExpanded = true }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Filter panel
                item(key = "filter_panel") {
                    AnimatedVisibility(
                        visible = isFilterExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            CompactFilterPanel(
                                filterState = filterState,
                                allTags = allTags,
                                onMoodToggle = { viewModel.toggleMoodFilter(it) },
                                onWeatherToggle = { viewModel.toggleWeatherFilter(it) },
                                onTagToggle = { viewModel.toggleTagFilter(it) },
                                onClearFilters = { viewModel.clearFilters() }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                // Active filters display
                if (viewModel.hasActiveFilters()) {
                    item(key = "active_filters") {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            ActiveFiltersRow(
                                filterState = filterState,
                                allTags = allTags,
                                onMoodRemove = { viewModel.toggleMoodFilter(it) },
                                onWeatherRemove = { viewModel.toggleWeatherFilter(it) },
                                onTagRemove = { viewModel.toggleTagFilter(it) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                // Month selector removed — use folding/unfolding directly

                // Empty state
                if (timelineItems.isEmpty()) {
                    item(key = "empty") {
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
                }

                // Timeline content - continuous axis on the left
                itemsIndexed(
                    items = timelineItems,
                    key = { _, item -> item.key }
                ) { index, item ->
                    val nextItem = timelineItems.getOrNull(index + 1)
                    val isLastOfDay = nextItem == null || nextItem.type == "day_header" || nextItem.type == "collapsed_month" || nextItem.type == "expanded_month"

                    when (item.type) {
                        "collapsed_month" -> {
                            item.month?.let { month ->
                                CollapsedMonthHeader(
                                    month = month,
                                    entryCount = monthGroups[month]?.size ?: 0,
                                    onClick = { expandedMonths[month] = true }
                                )
                            }
                        }
                        "expanded_month" -> {
                            item.month?.let { month ->
                                ExpandedMonthHeader(
                                    month = month,
                                    entryCount = monthGroups[month]?.size ?: 0,
                                    onCollapse = { expandedMonths[month] = false }
                                )
                            }
                        }
                        "day_header" -> {
                            item.date?.let { date ->
                                DayHeaderWithAxis(date = date)
                            }
                        }
                        "entry" -> {
                            item.entry?.let { entry ->
                                val tags = tagsMap[entry.id] ?: emptyList()
                                val imagePath = imageMap[entry.id]

                                TimelineEntryWithAxis(
                                    entry = entry,
                                    tags = tags,
                                    imagePath = imagePath,
                                    isLastInDay = isLastOfDay,
                                    onEntryClick = {
                                        haptic.click()
                                        onNavigateToDetail(entry.id)
                                    }
                                )
                            }
                        }
                    }
                }

                // Bottom padding
                item(key = "bottom_spacer") { Spacer(modifier = Modifier.height(80.dp)) }
            }

            // Scroll to top button
            if (showScrollToTop) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                        .clickable {
                            coroutineScope.launch {
                                listState.animateScrollToItem(0)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "回到顶部",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ========== Integrated Search Bar ==========

@Composable
private fun IntegratedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    hasActiveFilters: Boolean,
    onFilterClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
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
            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (hasActiveFilters) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        } else {
                            Color.Transparent
                        }
                    )
                    .clickable(onClick = onFilterClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = "筛选",
                    tint = if (hasActiveFilters) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ========== Compact Search Row (collapsed state) ==========

@Composable
private fun CompactSearchRow(
    query: String,
    hasActiveFilters: Boolean,
    onExpand: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 12.dp,
        innerPadding = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onExpand)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )

            Text(
                text = if (query.isNotBlank()) query else "搜索日记...",
                fontSize = 13.sp,
                color = if (query.isNotBlank()) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (hasActiveFilters) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "有筛选条件",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

// ========== Filter Panel ==========

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompactFilterPanel(
    filterState: FilterState,
    allTags: List<TagInfo>,
    onMoodToggle: (Int) -> Unit,
    onWeatherToggle: (String) -> Unit,
    onTagToggle: (Long) -> Unit,
    onClearFilters: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 10.dp,
        innerPadding = 8.dp
    ) {
        Column {
            // Mood section
            Text(
                text = "心情",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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

            // Weather section
            Text(
                text = "天气",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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

            // Tags section
            if (allTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "标签",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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

            // Clear button (only when filters are active)
            if (filterState.selectedMoods.isNotEmpty() || filterState.selectedWeathers.isNotEmpty() || filterState.selectedTagIds.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "清除筛选",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onClearFilters)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
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

// ========== Active Filters ==========

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

// ========== Month Headers ==========

@Composable
private fun CollapsedMonthHeader(
    month: YearMonth,
    entryCount: Int,
    onClick: () -> Unit
) {
    val now = YearMonth.now()
    val isCurrentMonth = month == now
    val monthText = if (isCurrentMonth) "本月" else "${month.year}年${month.monthValue}月"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = monthText,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$entryCount",
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Icon(
            imageVector = Icons.Default.ExpandMore,
            contentDescription = "展开",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ExpandedMonthHeader(
    month: YearMonth,
    entryCount: Int,
    onCollapse: () -> Unit
) {
    val now = YearMonth.now()
    val isCurrentMonth = month == now
    val monthText = if (isCurrentMonth) "本月" else "${month.year}年${month.monthValue}月"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCollapse)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = monthText,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$entryCount",
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Icon(
            imageVector = Icons.Default.ExpandLess,
            contentDescription = "折叠",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}

// ========== Day Header with Axis ==========

@Composable
private fun DayHeaderWithAxis(date: LocalDate) {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    val isToday = date == today
    val isYesterday = date == yesterday

    val dateLabel = when {
        isToday -> "今天"
        isYesterday -> "昨天"
        else -> date.format(DateTimeFormatter.ofPattern("M.d"))
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Axis column - date capsule centered, line broken around it
        Box(
            modifier = Modifier
                .width(AXIS_WIDTH)
                .height(56.dp),
        ) {
            // Top line segment (above capsule)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .height(10.dp)
                    .width(LINE_WIDTH)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            )
            // Date capsule centered on the axis
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isToday) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                        }
                    )
                    .border(
                        width = if (isToday) 1.dp else 0.5.dp,
                        color = if (isToday) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = dateLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isToday) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        }
                    )
                    Text(
                        text = weekdayText,
                        fontSize = 10.sp,
                        color = if (isToday) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        }
                    )
                }
            }
            // Bottom line segment (below capsule)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .height(10.dp)
                    .width(LINE_WIDTH)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            )
        }

        // No redundant text on the right - all info is in the capsule
    }
}

// ========== Timeline Entry with Axis ==========

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimelineEntryWithAxis(
    entry: DiaryPreview,
    tags: List<TagInfo>,
    imagePath: String?,
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

    val hasImage = !imagePath.isNullOrBlank()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 0.dp)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Axis column: dot + vertical line, fills full row height for continuity
        Box(
            modifier = Modifier
                .width(AXIS_WIDTH)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            // Vertical line behind the dot
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    .width(LINE_WIDTH)
                    .fillMaxHeight()
            )
            // Dot centered
            Box(
                modifier = Modifier
                    .size(DOT_SIZE)
                    .clip(CircleShape)
                    .background(moodColor)
            )
        }

        // Card
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLastInDay) 4.dp else 6.dp, top = 4.dp)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                cornerRadius = 12.dp,
                innerPadding = 12.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = MIN_CARD_HEIGHT - 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Content
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        // Title
                        val isDateTitle = entry.title.matches(DATE_TITLE_REGEX)
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
                                Spacer(modifier = Modifier.height(3.dp))
                            }
                            Text(
                                text = cleanPreviewText(entry.plainText),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (hasImage) 2 else 3,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 18.sp
                            )
                        }

                        // Location
                        if (!entry.location.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))
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

                        // Mood + Weather + Tags in one row
                        val hasMoodWeather = entry.moodLevel != null || entry.weather != null
                        val hasTags = tags.isNotEmpty()
                        if (hasMoodWeather || hasTags) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                            modifier = Modifier.size(13.dp)
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
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = weatherLabelFor(entry.weather),
                                            fontSize = 11.sp,
                                            color = weatherTint,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
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
                                }
                            }
                        }
                    }

                    // Image thumbnail
                    if (hasImage) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .size(IMAGE_SIZE)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imagePath)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "日记图片",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
    }
}
