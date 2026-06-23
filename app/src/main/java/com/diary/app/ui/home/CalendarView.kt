package com.diary.app.ui.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.ui.components.moodColorForLevel
import com.diary.app.ui.components.weatherIconFor
import com.diary.app.ui.theme.themeMode
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs

enum class CalendarMode { WEEK, MONTH }

private const val CENTER_PAGE = Int.MAX_VALUE / 2

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CalendarView(
    entryDates: Set<LocalDate>,
    dayInfoMap: Map<LocalDate, DayInfo>,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    calendarMode: CalendarMode = CalendarMode.MONTH,
    onModeChange: (CalendarMode) -> Unit = {},
    currentMonth: YearMonth = YearMonth.now(),
    onCurrentMonthChange: (YearMonth) -> Unit = {},
    currentWeekStart: LocalDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
    onCurrentWeekStartChange: (LocalDate) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val onBackground = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary

    val canGoToToday = if (calendarMode == CalendarMode.MONTH) {
        !(currentMonth.year == today.year && currentMonth.monthValue == today.monthValue)
    } else {
        !(currentWeekStart <= today && currentWeekStart.plusDays(6) >= today)
    }

    var showDatePicker by remember { mutableStateOf(false) }

    // Pager: source of truth for displayed month/week
    val pagerState = rememberPagerState(initialPage = CENTER_PAGE) { Int.MAX_VALUE }

    // Flag to prevent feedback loop: state→pager sync should not trigger pager→state sync
    var suppressPagerSync by remember { mutableStateOf(false) }

    // When pager settles, update currentMonth/currentWeekStart (pager → state)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            if (!suppressPagerSync) {
                val offset = page - CENTER_PAGE
                if (calendarMode == CalendarMode.MONTH) {
                    onCurrentMonthChange(YearMonth.now().plusMonths(offset.toLong()))
                } else {
                    onCurrentWeekStartChange(
                        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                            .plusWeeks(offset.toLong())
                    )
                }
            }
            suppressPagerSync = false
        }
    }

    // When state changes from arrows/back-to-today/jump, sync pager (state → pager)
    LaunchedEffect(currentMonth, currentWeekStart, calendarMode) {
        val targetOffset = if (calendarMode == CalendarMode.MONTH) {
            ChronoUnit.MONTHS.between(YearMonth.now(), currentMonth).toInt()
        } else {
            ChronoUnit.WEEKS.between(
                today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                currentWeekStart
            ).toInt()
        }
        val targetPage = CENTER_PAGE + targetOffset
        if (pagerState.currentPage != targetPage) {
            suppressPagerSync = true
            pagerState.scrollToPage(targetPage)
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            // Row 1: [<] date range [>]
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (calendarMode == CalendarMode.MONTH) {
                                onCurrentMonthChange(currentMonth.minusMonths(1))
                            } else {
                                onCurrentWeekStartChange(currentWeekStart.minusWeeks(1))
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "上一页",
                        tint = onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = if (calendarMode == CalendarMode.MONTH) {
                        "${currentMonth.year}年${currentMonth.monthValue}月"
                    } else {
                        val weekEnd = currentWeekStart.plusDays(6)
                        if (currentWeekStart.monthValue == weekEnd.monthValue) {
                            "${currentWeekStart.monthValue}月${currentWeekStart.dayOfMonth}日 - ${weekEnd.dayOfMonth}日"
                        } else {
                            "${currentWeekStart.monthValue}月${currentWeekStart.dayOfMonth}日 - ${weekEnd.monthValue}月${weekEnd.dayOfMonth}日"
                        }
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = onBackground,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                )

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (calendarMode == CalendarMode.MONTH) {
                                if (currentMonth < YearMonth.now()) {
                                    onCurrentMonthChange(currentMonth.plusMonths(1))
                                }
                            } else {
                                if (currentWeekStart.plusDays(6) < today) {
                                    onCurrentWeekStartChange(currentWeekStart.plusWeeks(1))
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val isAtEnd = if (calendarMode == CalendarMode.MONTH) {
                        currentMonth >= YearMonth.now()
                    } else {
                        currentWeekStart.plusDays(6) >= today
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "下一页",
                        tint = if (isAtEnd) onSurfaceVariant.copy(alpha = 0.3f) else onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Row 2: mode toggle centered
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModeToggleButton(
                        icon = Icons.Default.ViewWeek,
                        label = "周",
                        isSelected = calendarMode == CalendarMode.WEEK,
                        onClick = { onModeChange(CalendarMode.WEEK) }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    ModeToggleButton(
                        icon = Icons.Default.CalendarViewMonth,
                        label = "月",
                        isSelected = calendarMode == CalendarMode.MONTH,
                        onClick = { onModeChange(CalendarMode.MONTH) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Weekday headers
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        fontSize = 11.sp,
                        color = onSurfaceVariant.copy(alpha = 0.62f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Horizontal drag detection to block LazyColumn from stealing vertical scroll
            var isHorizontalDrag by remember { mutableStateOf(false) }
            val nestedScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        return if (isHorizontalDrag) {
                            Offset(0f, available.y) // consume vertical, let horizontal pass through
                        } else {
                            Offset.Zero
                        }
                    }
                }
            }

            // Calendar pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .nestedScroll(nestedScrollConnection)
                    .pointerInput(calendarMode) {
                        detectDragGestures(
                            onDragStart = { isHorizontalDrag = false },
                            onDrag = { change, dragAmount ->
                                if (!isHorizontalDrag && abs(dragAmount.x) > abs(dragAmount.y) && abs(dragAmount.x) > 10f) {
                                    isHorizontalDrag = true
                                }
                            },
                            onDragEnd = { isHorizontalDrag = false },
                            onDragCancel = { isHorizontalDrag = false }
                        )
                    },
                key = { "${calendarMode.name}-$it" },
                beyondBoundsPageCount = 1
            ) { page ->
                val offset = page - CENTER_PAGE
                if (calendarMode == CalendarMode.MONTH) {
                    val month = YearMonth.now().plusMonths(offset.toLong())
                    MonthView(
                        currentMonth = month,
                        entryDates = entryDates,
                        dayInfoMap = dayInfoMap,
                        selectedDate = selectedDate,
                        today = today,
                        onDateSelected = onDateSelected,
                        primary = primary
                    )
                } else {
                    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        .plusWeeks(offset.toLong())
                    Box {
                        WeekView(
                            weekStart = weekStart,
                            entryDates = entryDates,
                            dayInfoMap = dayInfoMap,
                            selectedDate = selectedDate,
                            today = today,
                            onDateSelected = onDateSelected,
                            primary = primary
                        )
                        // Subtle edge shadows to show page boundaries
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .width(6.dp)
                                .matchParentSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .width(6.dp)
                                .matchParentSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                                        )
                                    )
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom bar: back to today + jump to date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canGoToToday) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(primary.copy(alpha = 0.1f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (calendarMode == CalendarMode.MONTH) {
                                    onCurrentMonthChange(YearMonth.now())
                                } else {
                                    onCurrentWeekStartChange(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
                                }
                                onDateSelected(today)
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "回到今天",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = primary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(onSurfaceVariant.copy(alpha = 0.1f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            showDatePicker = true
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditCalendar,
                            contentDescription = null,
                            tint = onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "跳转日期",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Scrollable year/month/day picker dialog
    if (showDatePicker) {
        val initDate = selectedDate ?: today
        var pickedYear by remember { mutableStateOf(initDate.year) }
        var pickedMonth by remember { mutableStateOf(initDate.monthValue) }
        var pickedDay by remember { mutableStateOf(initDate.dayOfMonth) }

        // Clamp day to valid range
        val maxDay = try {
            YearMonth.of(pickedYear, pickedMonth).lengthOfMonth()
        } catch (_: Exception) { 31 }
        val clampedDay = pickedDay.coerceIn(1, maxDay)
        if (clampedDay != pickedDay) pickedDay = clampedDay

        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            title = { Text("选择日期", fontWeight = FontWeight.SemiBold) },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Year column
                    WheelPicker(
                        value = pickedYear,
                        range = 2000..today.year + 1,
                        label = { "${it}年" },
                        onValueChange = { pickedYear = it }
                    )
                    // Month column
                    WheelPicker(
                        value = pickedMonth,
                        range = 1..12,
                        label = { "${it}月" },
                        onValueChange = { pickedMonth = it }
                    )
                    // Day column
                    WheelPicker(
                        value = clampedDay,
                        range = 1..maxDay,
                        label = { "${it}日" },
                        onValueChange = { pickedDay = it }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val date = LocalDate.of(pickedYear, pickedMonth, clampedDay)
                        if (calendarMode == CalendarMode.MONTH) {
                            onCurrentMonthChange(YearMonth.from(date))
                        } else {
                            onCurrentWeekStartChange(date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
                        }
                        onDateSelected(date)
                        showDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun MonthView(
    currentMonth: YearMonth,
    entryDates: Set<LocalDate>,
    dayInfoMap: Map<LocalDate, DayInfo>,
    selectedDate: LocalDate?,
    today: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    primary: Color
) {
    val firstDayOfMonth = currentMonth.atDay(1)
    val daysInMonth = currentMonth.lengthOfMonth()
    val startOffset = (firstDayOfMonth.dayOfWeek.value - 1 + 7) % 7
    val totalCells = startOffset + daysInMonth
    val rows = (totalCells + 6) / 7

    Column {
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    val dayNum = cellIndex - startOffset + 1

                    if (dayNum in 1..daysInMonth) {
                        val date = currentMonth.atDay(dayNum)
                        CalendarDay(
                            date = date,
                            hasEntry = date in entryDates,
                            dayInfo = dayInfoMap[date],
                            isSelected = date == selectedDate,
                            isToday = date == today,
                            onDateSelected = onDateSelected,
                            primary = primary,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekView(
    weekStart: LocalDate,
    entryDates: Set<LocalDate>,
    dayInfoMap: Map<LocalDate, DayInfo>,
    selectedDate: LocalDate?,
    today: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    primary: Color
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        for (i in 0..6) {
            val date = weekStart.plusDays(i.toLong())
            CalendarDay(
                date = date,
                hasEntry = date in entryDates,
                dayInfo = dayInfoMap[date],
                isSelected = date == selectedDate,
                isToday = date == today,
                onDateSelected = onDateSelected,
                primary = primary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ModeToggleButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    } else {
        Color.Transparent
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            color = contentColor
        )
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate,
    hasEntry: Boolean,
    dayInfo: DayInfo?,
    isSelected: Boolean,
    isToday: Boolean,
    onDateSelected: (LocalDate) -> Unit,
    primary: Color,
    modifier: Modifier = Modifier
) {
    val onBackground = MaterialTheme.colorScheme.onBackground

    val targetScale = if (isSelected) 1f else 0.95f
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dayScale"
    )

    val primaryMoodColor = dayInfo?.moodLevel?.let(::moodColorForLevel)
    val accentMoodColor = dayInfo?.accentMoodLevel?.let(::moodColorForLevel)

    val textColor = when {
        isSelected -> Color.White
        isToday -> primary
        else -> onBackground.copy(alpha = 0.8f)
    }

    val backgroundBrush = when {
        isSelected -> Brush.linearGradient(
            colors = listOf(primary.copy(alpha = 0.9f), primary.copy(alpha = 0.76f))
        )
        primaryMoodColor != null && dayInfo?.hasMixedMoods == true && accentMoodColor != null ->
            Brush.linearGradient(
                colors = listOf(
                    primaryMoodColor.copy(alpha = if (isToday) 0.24f else 0.18f),
                    accentMoodColor.copy(alpha = if (isToday) 0.18f else 0.12f)
                )
            )
        primaryMoodColor != null ->
            Brush.linearGradient(
                colors = listOf(
                    primaryMoodColor.copy(alpha = if (isToday) 0.22f else 0.16f),
                    primaryMoodColor.copy(alpha = if (isToday) 0.14f else 0.1f)
                )
            )
        isToday -> Brush.linearGradient(
            colors = listOf(
                primary.copy(alpha = 0.1f),
                primary.copy(alpha = 0.06f)
            )
        )
        else -> Brush.linearGradient(colors = listOf(Color.Transparent, Color.Transparent))
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .scale(animatedScale)
            .clip(CircleShape)
            .background(backgroundBrush)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDateSelected(date) },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${date.dayOfMonth}",
                fontSize = 13.sp,
                color = textColor,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (hasEntry) {
                val countColor = when {
                    isSelected -> Color.White.copy(alpha = 0.94f)
                    primaryMoodColor != null -> primaryMoodColor.copy(alpha = 0.92f)
                    else -> primary.copy(alpha = 0.82f)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val weather = dayInfo?.weather
                    if (!weather.isNullOrBlank()) {
                        val (weatherIcon, weatherTint) = weatherIconFor(weather)
                        Icon(
                            imageVector = weatherIcon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White.copy(alpha = 0.85f) else weatherTint.copy(alpha = 0.7f),
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    Text(
                        text = "${dayInfo?.entryCount ?: 1}",
                        fontSize = 8.sp,
                        color = countColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else if (isToday && !isSelected) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(primary)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun <T> WheelPicker(
    value: T,
    range: IntRange,
    label: (Int) -> String,
    onValueChange: (T) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val onBackground = MaterialTheme.colorScheme.onBackground

    val itemHeight = 40.dp
    val visibleItems = 5
    val pickerHeight = itemHeight * visibleItems
    val paddingItems = (visibleItems / 2)

    val items = remember(range) { range.toList() }
    val initialIndex = remember(value, range) { (value as Int) - range.first }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = maxOf(0, initialIndex - paddingItems))
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    var isUserScrolling by remember { mutableStateOf(false) }

    // Track scroll to update value
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo.minByOrNull {
                kotlin.math.abs((it.offset + it.size / 2) - viewportCenter)
            }?.index
        }.collect { index ->
            if (index != null && !isUserScrolling) {
                val newValue = range.first + index
                if (newValue in range && newValue != (value as Int)) {
                    @Suppress("UNCHECKED_CAST")
                    onValueChange(newValue as T)
                }
            }
        }
    }

    // Detect user scrolling state
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            isUserScrolling = scrolling
        }
    }

    // Scroll to value when changed externally
    LaunchedEffect(value) {
        val index = (value as Int) - range.first
        if (index >= 0 && index < items.size) {
            listState.animateScrollToItem(maxOf(0, index - paddingItems))
        }
    }

    Box(
        modifier = Modifier
            .width(80.dp)
            .height(pickerHeight)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            flingBehavior = snapBehavior,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(count = items.size + paddingItems * 2) { rawIndex ->
                val itemIndex = rawIndex - paddingItems
                val isValid = itemIndex in items.indices
                val itemValue = if (isValid) items[itemIndex] else 0
                val isSelected = isValid && itemValue == (value as Int)

                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isValid) {
                        Text(
                            text = label(itemValue),
                            fontSize = if (isSelected) 18.sp else 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) primary else onBackground.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }

        // Center highlight indicator (top and bottom lines)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeight)
                .border(
                    width = 1.dp,
                    color = primary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                )
        )
    }
}
