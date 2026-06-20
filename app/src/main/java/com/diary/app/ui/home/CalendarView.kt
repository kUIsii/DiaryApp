package com.diary.app.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.ui.components.moodColorForLevel
import com.diary.app.ui.theme.isDark
import com.diary.app.ui.theme.themeMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

enum class CalendarMode { WEEK, MONTH }

@Composable
fun CalendarView(
    entryDates: Set<LocalDate>,
    dayInfoMap: Map<LocalDate, DayInfo>,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    calendarMode: CalendarMode = CalendarMode.MONTH,
    onModeChange: (CalendarMode) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = themeMode().isDark()
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var currentWeekStart by remember {
        mutableStateOf(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
    }
    val today = remember { LocalDate.now() }

    val onBackground = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary

    val borderModifier = if (!isDark) {
        Modifier.border(
            width = 1.dp,
            color = Color.Gray.copy(alpha = 0.18f),
            shape = RoundedCornerShape(16.dp)
        )
    } else {
        Modifier
    }

    val isAtCurrent = if (calendarMode == CalendarMode.MONTH) {
        currentMonth.year == today.year && currentMonth.monthValue == today.monthValue
    } else {
        currentWeekStart.plusDays(6) >= today
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)
            )
            .then(borderModifier)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (calendarMode == CalendarMode.MONTH) {
                        "${currentMonth.year}年${currentMonth.monthValue}月"
                    } else {
                        "${currentWeekStart.monthValue}月${currentWeekStart.dayOfMonth}日 - ${currentWeekStart.plusDays(6).monthValue}月${currentWeekStart.plusDays(6).dayOfMonth}日"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = onBackground
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModeToggleButton(
                        icon = Icons.Default.ViewWeek,
                        label = "周",
                        isSelected = calendarMode == CalendarMode.WEEK,
                        onClick = { onModeChange(CalendarMode.WEEK) }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    ModeToggleButton(
                        icon = Icons.Default.CalendarViewMonth,
                        label = "月",
                        isSelected = calendarMode == CalendarMode.MONTH,
                        onClick = { onModeChange(CalendarMode.MONTH) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        fontSize = 12.sp,
                        color = onSurfaceVariant.copy(alpha = 0.62f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(calendarMode) {
                        var totalDragX = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalDragX = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                totalDragX += dragAmount
                                change.consume()
                            },
                            onDragEnd = {
                                val threshold = 40f
                                if (totalDragX < -threshold) {
                                    if (!isAtCurrent) {
                                        if (calendarMode == CalendarMode.MONTH) {
                                            currentMonth = currentMonth.plusMonths(1)
                                        } else {
                                            currentWeekStart = currentWeekStart.plusWeeks(1)
                                        }
                                    }
                                } else if (totalDragX > threshold) {
                                    if (calendarMode == CalendarMode.MONTH) {
                                        currentMonth = currentMonth.minusMonths(1)
                                    } else {
                                        currentWeekStart = currentWeekStart.minusWeeks(1)
                                    }
                                }
                            }
                        )
                    }
            ) {
                AnimatedContent(
                    targetState = calendarMode,
                    transitionSpec = {
                        if (targetState == CalendarMode.WEEK) {
                            (slideInVertically(animationSpec = tween(300)) { height -> height } +
                                fadeIn(animationSpec = tween(300))) togetherWith
                                (slideOutVertically(animationSpec = tween(300)) { height -> -height } +
                                    fadeOut(animationSpec = tween(300)))
                        } else {
                            (slideInVertically(animationSpec = tween(300)) { height -> -height } +
                                fadeIn(animationSpec = tween(300))) togetherWith
                                (slideOutVertically(animationSpec = tween(300)) { height -> height } +
                                    fadeOut(animationSpec = tween(300)))
                        }
                    },
                    label = "calendarMode"
                ) { mode ->
                    if (mode == CalendarMode.MONTH) {
                        MonthView(
                            currentMonth = currentMonth,
                            entryDates = entryDates,
                            dayInfoMap = dayInfoMap,
                            selectedDate = selectedDate,
                            today = today,
                            onDateSelected = onDateSelected,
                            primary = primary
                        )
                    } else {
                        WeekView(
                            weekStart = currentWeekStart,
                            entryDates = entryDates,
                            dayInfoMap = dayInfoMap,
                            selectedDate = selectedDate,
                            today = today,
                            onDateSelected = onDateSelected,
                            primary = primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (calendarMode == CalendarMode.MONTH) {
                                currentMonth = currentMonth.minusMonths(1)
                            } else {
                                currentWeekStart = currentWeekStart.minusWeeks(1)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "上一页",
                        tint = onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = if (calendarMode == CalendarMode.MONTH) "左右滑动切换月份" else "左右滑动切换周",
                    fontSize = 11.sp,
                    color = onSurfaceVariant.copy(alpha = 0.72f)
                )

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = !isAtCurrent
                        ) {
                            if (!isAtCurrent) {
                                if (calendarMode == CalendarMode.MONTH) {
                                    currentMonth = currentMonth.plusMonths(1)
                                } else {
                                    currentWeekStart = currentWeekStart.plusWeeks(1)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "下一页",
                        tint = onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
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
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
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
                Text(
                    text = "${dayInfo?.entryCount ?: 1}",
                    fontSize = 8.sp,
                    color = countColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
