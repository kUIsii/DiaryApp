package com.diary.app.ui.home

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MoodBad
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.data.DiaryEntry
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.theme.DarkAccentEnd
import com.diary.app.ui.theme.DarkAccentStart
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
    viewModel: HomeViewModel = viewModel()
) {
    val entries by viewModel.entries.collectAsState()
    val entryDates by viewModel.entryDates.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val tagsMap by viewModel.tagsMap.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val selectedTagFilter by viewModel.selectedTagFilter.collectAsState()
    val allTags by viewModel.allTags.collectAsState()

    val isSearchActive = searchQuery.isNotBlank()
    val isTagFilterActive = selectedTagFilter != null

    LaunchedEffect(entries) {
        viewModel.loadTagsForEntries(entries)
    }

    var entryToDelete by remember { mutableStateOf<DiaryEntry?>(null) }

    // Delete confirmation dialog
    if (entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除这篇日记吗？删除后无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteEntry(entryToDelete!!)
                    entryToDelete = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("取消")
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
                        onQueryChange = { viewModel.setSearchQuery(it) }
                    )
                }

                // Tag filter chips
                if (allTags.isNotEmpty()) {
                    item {
                        TagFilterRow(
                            tags = allTags,
                            selectedTagId = selectedTagFilter,
                            onTagSelected = { viewModel.setTagFilter(it) }
                        )
                    }
                }

                // Stats card
                item {
                    StatsCard(stats = stats)
                }

                // Calendar view (hidden when search or tag filter is active)
                if (!isSearchActive && !isTagFilterActive) {
                    item {
                        CalendarView(
                            entryDates = entryDates,
                            selectedDate = selectedDate,
                            onDateSelected = { date ->
                                viewModel.selectDate(if (date == selectedDate) null else date)
                            }
                        )
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
                                text = "${selectedDate!!.monthValue}月${selectedDate!!.dayOfMonth}日的日记",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "查看全部",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable { viewModel.selectDate(null) }
                            )
                        }
                    }
                }

                if (entries.isEmpty()) {
                    item { EmptyState() }
                } else {
                    itemsIndexed(entries, key = { _, entry -> entry.id }) { index, entry ->
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) { visible = true }

                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(300, delayMillis = index * 50)) +
                                    slideInVertically(tween(300, delayMillis = index * 50)) { it / 4 }
                        ) {
                            DiaryCardWithContextMenu(
                                entry = entry,
                                tags = tagsMap[entry.id] ?: emptyList(),
                                onClick = { onNavigateToDetail(entry.id) },
                                onEdit = { onNavigateToEditor(entry.id) },
                                onDelete = { entryToDelete = entry },
                                onToggleFavorite = { viewModel.toggleFavorite(entry) }
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }

            // FAB
            FAB(onClick = { onNavigateToEditor(null) })
        }
    }
}

@Composable
private fun GreetingHeader() {
    val now = LocalTime.now()
    val greeting = when {
        now.hour < 12 -> "早安"
        now.hour < 18 -> "下午好"
        else -> "晚上好"
    }
    val today = LocalDate.now()
    val dateFormatter = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINESE)
    val dateText = today.format(dateFormatter)

    Column(modifier = Modifier.padding(bottom = 4.dp)) {
        Text(
            text = greeting,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = dateText,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    "搜索日记...",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "清除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(20.dp)
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
    }
}

@Composable
private fun StatsCard(stats: HomeStats) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(value = stats.total, label = "总日记")
            StatItem(value = stats.streak, label = "连续天数")
            StatItem(value = stats.thisMonth, label = "本月日记")
        }
    }
}

@Composable
private fun StatItem(value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FAB(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(end = 20.dp, bottom = 16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.horizontalGradient(listOf(DarkAccentStart, DarkAccentEnd)))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "新建日记",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun EmptyState() {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "还没有日记",
                fontSize = 16.sp,
                color = onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "点击右下角按钮开始记录",
                fontSize = 13.sp,
                color = onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagFilterRow(
    tags: List<com.diary.app.data.Tag>,
    selectedTagId: Long?,
    onTagSelected: (Long?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEach { tag ->
            val isSelected = tag.id == selectedTagId
            val tagColor = Color(tag.color)
            FilterChip(
                selected = isSelected,
                onClick = { onTagSelected(tag.id) },
                label = {
                    Text(
                        text = tag.name,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else tagColor)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = tagColor.copy(alpha = 0.1f),
                    labelColor = tagColor
                )
            )
        }
    }
}

@Composable
private fun DiaryCardWithContextMenu(
    entry: DiaryEntry,
    tags: List<TagInfo>,
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
            tags = tags,
            onClick = onClick,
            onLongClick = { showContextMenu = true },
            onDelete = onDelete,
            onToggleFavorite = onToggleFavorite
        )

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            DropdownMenuItem(
                text = { Text("编辑") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    showContextMenu = false
                    onEdit()
                }
            )
            DropdownMenuItem(
                text = { Text(if (entry.isFavorite) "取消收藏" else "收藏") },
                leadingIcon = {
                    Icon(
                        if (entry.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = if (entry.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    showContextMenu = false
                    onToggleFavorite()
                }
            )
            DropdownMenuItem(
                text = { Text("分享") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
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
                    Text("删除", color = MaterialTheme.colorScheme.error)
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
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
    tags: List<TagInfo>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val dismissState = rememberDismissState(
        confirmValueChange = { value ->
            if (value == DismissValue.DismissedToStart) {
                onDelete()
                false // Don't dismiss, let the dialog handle it
            } else {
                false
            }
        }
    )

    SwipeToDismiss(
        state = dismissState,
        background = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFE74C3C))
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        dismissContent = {
            DiaryCard(
                entry = entry,
                tags = tags,
                onClick = onClick,
                onLongClick = onLongClick,
                onToggleFavorite = onToggleFavorite
            )
        },
        directions = setOf(DismissDirection.EndToStart)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiaryCard(
    entry: DiaryEntry,
    tags: List<TagInfo>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "cardScale"
    )

    val onBackground = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

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
            )
    ) {
        Column {
            // Date row with favorite star
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatCardDate(entry.createdAt),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = onBackground
                )
                Icon(
                    imageVector = if (entry.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (entry.isFavorite) "取消收藏" else "收藏",
                    tint = if (entry.isFavorite) MaterialTheme.colorScheme.primary else onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onToggleFavorite() }
                )
            }

            // Text preview
            if (entry.plainText.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = entry.plainText,
                    fontSize = 14.sp,
                    color = onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }

            // Bottom info row
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mood icon
                if (entry.moodLevel != null) {
                    val (moodIcon, moodTint) = moodIconForLevel(entry.moodLevel)
                    Icon(
                        imageVector = moodIcon,
                        contentDescription = "心情",
                        tint = moodTint,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }

                // Weather icon
                if (entry.weather != null) {
                    val (weatherIcon, weatherTint) = weatherIconFor(entry.weather)
                    Icon(
                        imageVector = weatherIcon,
                        contentDescription = "天气",
                        tint = weatherTint,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }

                // Spacer to push tags right
                Spacer(modifier = Modifier.weight(1f))

                // Tag chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.take(3).forEach { tag ->
                        TagChip(name = tag.name, color = tag.color)
                    }
                    if (tags.size > 3) {
                        Text(
                            text = "+${tags.size - 3}",
                            fontSize = 11.sp,
                            color = onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TagChip(name: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = name,
            fontSize = 11.sp,
            color = color,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

private data class IconWithTint(val icon: ImageVector, val tint: Color)

private fun moodIconForLevel(level: Int): IconWithTint {
    return when (level.coerceIn(1, 6)) {
        1 -> IconWithTint(Icons.Default.MoodBad, Color(0xFFE57373))
        2 -> IconWithTint(Icons.Default.SentimentDissatisfied, Color(0xFFFFB74D))
        3 -> IconWithTint(Icons.Default.SentimentNeutral, Color(0xFFFFF176))
        4 -> IconWithTint(Icons.Default.Mood, Color(0xFFAED581))
        5 -> IconWithTint(Icons.Default.SentimentSatisfied, Color(0xFF81C784))
        6 -> IconWithTint(Icons.Default.SentimentVerySatisfied, Color(0xFF4FC3F7))
        else -> IconWithTint(Icons.Default.SentimentNeutral, Color(0xFFFFF176))
    }
}

private fun weatherIconFor(weather: String?): IconWithTint {
    return when (weather) {
        "晴" -> IconWithTint(Icons.Default.WbSunny, Color(0xFFFFCA28))
        "多云" -> IconWithTint(Icons.Default.Cloud, Color(0xFF90A4AE))
        "阴" -> IconWithTint(Icons.Default.CloudQueue, Color(0xFF78909C))
        "雨" -> IconWithTint(Icons.Default.Umbrella, Color(0xFF64B5F6))
        "雷" -> IconWithTint(Icons.Default.Thunderstorm, Color(0xFFBA68C8))
        "风" -> IconWithTint(Icons.Default.Air, Color(0xFF80CBC4))
        else -> IconWithTint(Icons.Default.WbSunny, Color(0xFFFFCA28))
    }
}

private fun formatCardDate(timestamp: Long): String {
    val entryDate = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    return when (entryDate) {
        today -> "今天"
        yesterday -> "昨天"
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

    val moodLabel = entry.moodLevel?.let { level ->
        when (level.coerceIn(1, 6)) {
            1 -> "沮丧"
            2 -> "低落"
            3 -> "平静"
            4 -> "开心"
            5 -> "愉快"
            6 -> "兴奋"
            else -> "平静"
        }
    }

    val weatherLabel = entry.weather?.let { weather ->
        when (weather) {
            "晴", "晴天" -> "晴天"
            "多云" -> "多云"
            "阴", "阴天" -> "阴天"
            "雨", "雨天" -> "雨天"
            "雷", "雷暴" -> "雷暴"
            "风", "大风" -> "大风"
            else -> weather
        }
    }

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
