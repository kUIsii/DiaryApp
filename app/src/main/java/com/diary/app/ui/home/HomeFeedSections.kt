package com.diary.app.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.diary.app.data.DiaryPreview
import com.diary.app.ui.components.EmptyState
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.cleanPreviewText
import com.diary.app.ui.components.formatEntryTime
import com.diary.app.ui.components.moodIconForLevel
import com.diary.app.ui.components.moodLabelForLevel
import com.diary.app.ui.components.weatherIconFor
import com.diary.app.ui.components.weatherLabelFor
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun HomeCalendarSectionCard(
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
internal fun HomeQuickShortcutsSection(
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
internal fun HomeSelectedDateHeader(
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

                HomeHeaderActionButton(
                    icon = Icons.Default.Favorite,
                    label = "收藏",
                    enabled = multiSelectState.selectedIds.isNotEmpty(),
                    onClick = onFavoriteSelected
                )
                Spacer(modifier = Modifier.width(8.dp))
                HomeHeaderActionButton(
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
internal fun HomeHeaderActionButton(
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
internal fun HomeNoEntriesForDate(modifier: Modifier = Modifier) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
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
internal fun HomeEntryFeedCard(
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
                if (hasImage) {
                    if (imagePaths.size > 1) {
                        val pagerState = rememberPagerState { imagePaths.size }
                        Box {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .fillMaxWidth()
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
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
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
                                HomeMetaChip(
                                    icon = Icons.Default.Image,
                                    label = "${imagePaths.size} 张图片",
                                    tint = if (hasImage) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            moodData?.let { mood ->
                                HomeMetaChip(
                                    icon = mood.icon,
                                    label = moodLabelForLevel(entry.moodLevel),
                                    tint = if (hasImage) Color.White.copy(alpha = 0.8f) else mood.tint
                                )
                            }
                            weatherData?.let { weather ->
                                HomeMetaChip(
                                    icon = weather.icon,
                                    label = weatherLabelFor(entry.weather),
                                    tint = if (hasImage) Color.White.copy(alpha = 0.7f) else weather.tint
                                )
                            }
                            tags.take(2).forEach { tag ->
                                HomeColorTagChip(tag = tag, lightMode = hasImage)
                            }
                            if (tags.size > 2) {
                                HomeSubtleTextChip(text = "+${tags.size - 2}", lightMode = hasImage)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun HomeMetaChip(
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
internal fun HomeColorTagChip(tag: TagInfo, lightMode: Boolean = false) {
    val chipColor = if (lightMode) Color.White else tag.color
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(chipColor.copy(alpha = if (lightMode) 0.18f else 0.10f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = tag.name,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = if (lightMode) Color.White.copy(alpha = 0.85f) else tag.color
        )
    }
}

@Composable
internal fun HomeSubtleTextChip(text: String, lightMode: Boolean = false) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (lightMode) Color.White.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            color = if (lightMode) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun HomeSearchResultCard(
    entry: DiaryPreview,
    imageMap: Map<Long, String>,
    onClick: () -> Unit
) {
    val entryDate = java.time.Instant.ofEpochMilli(entry.createdAt)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
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
                    modifier = Modifier
                        .size(18.dp)
                        .padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
internal fun HomeOnThisDayCard(
    entries: List<DiaryPreview>,
    onClick: (DiaryPreview) -> Unit
) {
    val today = LocalDate.now()
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
                            modifier = Modifier
                                .size(16.dp)
                                .padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
