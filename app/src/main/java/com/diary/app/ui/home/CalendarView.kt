package com.diary.app.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.ui.components.GlassCard
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

enum class CalendarMode(val label: String) {
    MONTH("月"),
    WEEK("周")
}

@Composable
fun CalendarView(
    entryDates: Set<LocalDate>,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var calendarMode by remember { mutableStateOf(CalendarMode.MONTH) }
    val today = remember { LocalDate.now() }

    val onBackground = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    // Gradient brush for selected date
    val selectedGradient = Brush.linearGradient(
        colors = listOf(primary, secondary)
    )

    // Track animation direction: 1 = forward, -1 = backward
    var slideDirection by remember { mutableStateOf(1) }
    val animatedOffset by animateFloatAsState(
        targetValue = 0f,
        animationSpec = tween(durationMillis = 300),
        label = "monthSlide"
    )

    // For week mode, track the week start date
    var weekStart by remember {
        mutableStateOf(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
    }

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column {
            // Navigation row with mode toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left arrow with circular tap background
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            slideDirection = -1
                            if (calendarMode == CalendarMode.MONTH) {
                                currentMonth = currentMonth.minusMonths(1)
                            } else {
                                weekStart = weekStart.minusWeeks(1)
                                currentMonth = YearMonth.from(weekStart)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "上一个月",
                        tint = onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Month title
                    Text(
                        text = if (calendarMode == CalendarMode.MONTH)
                            "${currentMonth.year}年${currentMonth.monthValue}月"
                        else {
                            val weekEnd = weekStart.plusDays(6)
                            if (weekStart.monthValue == weekEnd.monthValue)
                                "${weekStart.monthValue}月${weekStart.dayOfMonth}-${weekEnd.dayOfMonth}日"
                            else
                                "${weekStart.monthValue}月${weekStart.dayOfMonth}-${weekEnd.monthValue}月${weekEnd.dayOfMonth}日"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = onBackground
                    )

                    // Mode toggle - capsule shape
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(onSurfaceVariant.copy(alpha = 0.08f))
                            .padding(2.dp)
                    ) {
                        CalendarMode.entries.forEach { mode ->
                            val isActive = calendarMode == mode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .then(
                                        if (isActive) {
                                            Modifier.background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(primary, secondary)
                                                )
                                            )
                                        } else {
                                            Modifier.background(Color.Transparent)
                                        }
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        calendarMode = mode
                                        if (mode == CalendarMode.WEEK) {
                                            weekStart = (selectedDate ?: today)
                                                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isActive) Color.White else onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Right arrow with circular tap background
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            slideDirection = 1
                            if (calendarMode == CalendarMode.MONTH) {
                                currentMonth = currentMonth.plusMonths(1)
                            } else {
                                weekStart = weekStart.plusWeeks(1)
                                currentMonth = YearMonth.from(weekStart)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "下一个月",
                        tint = onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Subtle separator line
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(onSurfaceVariant.copy(alpha = 0.12f))
            )

            // Day of week headers
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("日", "一", "二", "三", "四", "五", "六").forEachIndexed { index, day ->
                    val isWeekend = index == 0 || index == 6
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isWeekend) Color(0xFFE57373) else onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Gap between header and date grid
            Spacer(modifier = Modifier.height(8.dp))

            if (calendarMode == CalendarMode.MONTH) {
                // Month grid with crossfade
                Crossfade(
                    targetState = currentMonth,
                    animationSpec = tween(durationMillis = 300),
                    label = "monthCrossfade"
                ) { month ->
                    val firstDayOfMonth = month.atDay(1)
                    val daysInMonth = month.lengthOfMonth()
                    val startOffset = firstDayOfMonth.dayOfWeek.value % 7
                    val totalCells = startOffset + daysInMonth
                    val rows = (totalCells + 6) / 7

                    Column(
                        modifier = Modifier.offset(
                            x = (animatedOffset * 30 * slideDirection).dp
                        )
                    ) {
                        for (row in 0 until rows) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                for (col in 0..6) {
                                    val cellIndex = row * 7 + col
                                    val dayNum = cellIndex - startOffset + 1

                                    if (dayNum in 1..daysInMonth) {
                                        val date = month.atDay(dayNum)
                                        CalendarDay(
                                            date = date,
                                            hasEntry = date in entryDates,
                                            isSelected = date == selectedDate,
                                            isToday = date == today,
                                            isCurrentMonth = true,
                                            onDateSelected = onDateSelected,
                                            selectedGradient = selectedGradient,
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
            } else {
                // Week grid - taller rows with more prominent selection
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val date = weekStart.plusDays(col.toLong())
                        CalendarDay(
                            date = date,
                            hasEntry = date in entryDates,
                            isSelected = date == selectedDate,
                            isToday = date == today,
                            isCurrentMonth = true,
                            onDateSelected = onDateSelected,
                            selectedGradient = selectedGradient,
                            primary = primary,
                            isWeekView = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate,
    hasEntry: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    isCurrentMonth: Boolean,
    onDateSelected: (LocalDate) -> Unit,
    selectedGradient: Brush,
    primary: Color,
    modifier: Modifier = Modifier,
    isWeekView: Boolean = false
) {
    val onBackground = MaterialTheme.colorScheme.onBackground

    // Scale animation for selection
    val targetScale = if (isSelected) 1f else 0.9f
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 200),
        label = "dayScale"
    )

    // Determine text color
    val textColor = when {
        isSelected -> Color.White
        !isCurrentMonth -> onBackground.copy(alpha = 0.2f)
        isToday -> primary
        else -> onBackground
    }

    Box(
        modifier = modifier
            .then(
                if (isWeekView) Modifier.height(56.dp) else Modifier.aspectRatio(1f)
            )
            .padding(3.dp)
            .scale(animatedScale)
            .clip(CircleShape)
            .then(
                when {
                    isSelected -> Modifier.background(selectedGradient)
                    isToday -> Modifier.background(primary.copy(alpha = 0.12f))
                    else -> Modifier
                }
            )
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
                fontSize = 14.sp,
                color = textColor,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
            if (hasEntry) {
                // Entry indicator: small horizontal line instead of dot
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .width(12.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(
                            if (isSelected) Color.White.copy(alpha = 0.8f)
                            else primary.copy(alpha = 0.6f)
                        )
                )
            }
        }
    }
}
