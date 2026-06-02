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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.filled.CalendarViewMonth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    var currentWeekStart by remember { mutableStateOf(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))) }
    val today = remember { LocalDate.now() }

    val onBackground = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary

    // 浅色模式下添加灰色边框
    val borderModifier = if (!isDark) {
        Modifier.border(
            width = 1.dp,
            color = Color.Gray.copy(alpha = 0.2f),
            shape = RoundedCornerShape(16.dp)
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.9f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .then(borderModifier)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Navigation row with mode toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous button
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
                        Icons.Default.ChevronLeft,
                        contentDescription = "上一个",
                        tint = onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Title
                Text(
                    text = if (calendarMode == CalendarMode.MONTH) {
                        "${currentMonth.year}年${currentMonth.monthValue}月"
                    } else {
                        "${currentWeekStart.monthValue}月${currentWeekStart.dayOfMonth}日 - ${currentWeekStart.plusDays(6).monthValue}月${currentWeekStart.plusDays(6).dayOfMonth}日"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = onBackground
                )

                // Next button
                val isAtCurrent = if (calendarMode == CalendarMode.MONTH) {
                    currentMonth.year == today.year && currentMonth.monthValue == today.monthValue
                } else {
                    currentWeekStart.plusDays(6) >= today
                }
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
                        Icons.Default.ChevronRight,
                        contentDescription = "下一个",
                        tint = onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mode toggle buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModeToggleButton(
                    icon = Icons.Default.ViewWeek,
                    label = "周",
                    isSelected = calendarMode == CalendarMode.WEEK,
                    onClick = { onModeChange(CalendarMode.WEEK) }
                )
                Spacer(modifier = Modifier.width(12.dp))
                ModeToggleButton(
                    icon = Icons.Default.CalendarViewMonth,
                    label = "月",
                    isSelected = calendarMode == CalendarMode.MONTH,
                    onClick = { onModeChange(CalendarMode.MONTH) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Day of week headers
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("日", "一", "二", "三", "四", "五", "六").forEachIndexed { _, day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        fontSize = 12.sp,
                        color = onSurfaceVariant.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Date grid with animation
            AnimatedContent(
                targetState = calendarMode,
                transitionSpec = {
                    if (targetState == CalendarMode.WEEK) {
                        // 切换到周视图：向上滑入
                        (slideInVertically(animationSpec = tween(300)) { height -> height } +
                                fadeIn(animationSpec = tween(300))) togetherWith
                                (slideOutVertically(animationSpec = tween(300)) { height -> -height } +
                                        fadeOut(animationSpec = tween(300)))
                    } else {
                        // 切换到月视图：向下滑入
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
    val startOffset = firstDayOfMonth.dayOfWeek.value % 7
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
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(16.dp)
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

    val textColor = when {
        isSelected -> Color.White
        isToday -> primary
        else -> onBackground.copy(alpha = 0.8f)
    }

    val bgColor = when {
        isSelected -> primary.copy(alpha = 0.8f)
        isToday -> primary.copy(alpha = 0.08f)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .scale(animatedScale)
            .clip(CircleShape)
            .background(bgColor)
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
                val moodLevel = dayInfo?.moodLevel
                val dotColor = if (isSelected) {
                    Color.White.copy(alpha = 0.8f)
                } else if (moodLevel != null) {
                    moodColorForLevel(moodLevel).copy(alpha = 0.8f)
                } else {
                    // White-blue gradient dot for entries without mood
                    primary.copy(alpha = 0.6f)
                }
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }
    }
}
