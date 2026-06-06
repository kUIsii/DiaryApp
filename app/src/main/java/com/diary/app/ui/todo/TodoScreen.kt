package com.diary.app.ui.todo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.data.TodoItem
import com.diary.app.ui.components.EmptyState
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.rememberHapticFeedback
import com.diary.app.ui.theme.ErrorColor
import com.diary.app.ui.theme.SuccessColor
import com.diary.app.ui.theme.WarningColor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

private enum class TodoTab(val label: String) { HABIT("打卡"), MEMO("备忘"), DEADLINE("待办") }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TodoScreen(viewModel: TodoViewModel = viewModel()) {
    val haptic = rememberHapticFeedback()
    val allTodos by viewModel.allTodos.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var habitWeekOffset by remember { mutableIntStateOf(0) }

    val currentTab = TodoTab.entries[pagerState.currentPage]
    val today = remember { LocalDate.now() }
    val habitWeekStart = remember(habitWeekOffset, today) {
        val weekField = WeekFields.of(Locale.getDefault())
        val base = today.with(weekField.dayOfWeek(), 1)
        base.plusWeeks(habitWeekOffset.toLong())
    }
    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    val habitItems = allTodos.filter { it.category == TodoItem.CATEGORY_GOAL }
    val memoItems = allTodos.filter { it.category != TodoItem.CATEGORY_GOAL && it.dueDate == null }
    val deadlineItems = allTodos.filter { it.dueDate != null }.sortedBy { it.dueDate ?: Long.MAX_VALUE }

    // Delete confirmation
    var deletingTodo by remember { mutableStateOf<TodoItem?>(null) }
    deletingTodo?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingTodo = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("删除确认") },
            text = { Text("确定要删除「${target.title}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    haptic.warning()
                    viewModel.deleteTodo(target)
                    deletingTodo = null
                }) { Text("删除", color = ErrorColor) }
            },
            dismissButton = { TextButton(onClick = { deletingTodo = null }) { Text("取消") } }
        )
    }

    if (showAddDialog) {
        when (currentTab) {
            TodoTab.HABIT -> {
                var name by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showAddDialog = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    title = { Text("新建习惯") },
                    text = {
                        TextField(
                            value = name, onValueChange = { name = it },
                            placeholder = { Text("习惯名称，如：运动、阅读...") },
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (name.isNotBlank()) {
                                viewModel.addHabit(name.trim())
                                showAddDialog = false
                            }
                        }) { Text("创建") }
                    },
                    dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("取消") } }
                )
            }
            TodoTab.MEMO -> {
                var content by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showAddDialog = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    title = { Text("新建备忘") },
                    text = {
                        TextField(
                            value = content, onValueChange = { content = it },
                            placeholder = { Text("要记住的事情...") },
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (content.isNotBlank()) {
                                viewModel.addMemo(content.trim())
                                showAddDialog = false
                            }
                        }) { Text("创建") }
                    },
                    dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("取消") } }
                )
            }
            TodoTab.DEADLINE -> {
                var content by remember { mutableStateOf("") }
                var selectedDate by remember { mutableStateOf<Long?>(null) }
                var showDatePicker by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = { showAddDialog = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    title = { Text("新建待办") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            TextField(
                                value = content, onValueChange = { content = it },
                                placeholder = { Text("要做什么事...") },
                                singleLine = true, modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { showDatePicker = true }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Today, null, tint = textSecondary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedDate?.let {
                                        val d = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                                        "${d.monthValue}月${d.dayOfMonth}日"
                                    } ?: "选择截止日期",
                                    fontSize = 14.sp,
                                    color = if (selectedDate != null) textColor else textSecondary.copy(alpha = 0.6f)
                                )
                            }
                        }

                        if (showDatePicker) {
                            val datePickerState = rememberDatePickerState()
                            DatePickerDialog(
                                onDismissRequest = { showDatePicker = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        datePickerState.selectedDateMillis?.let { millis ->
                                            selectedDate = millis
                                        }
                                        showDatePicker = false
                                    }) { Text("确定") }
                                },
                                dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } }
                            ) { DatePicker(state = datePickerState) }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (content.isNotBlank() && selectedDate != null) {
                                viewModel.addDeadline(content.trim(), selectedDate!!)
                                showAddDialog = false
                            }
                        }, enabled = content.isNotBlank() && selectedDate != null) { Text("创建") }
                    },
                    dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("取消") } }
                )
            }
        }
    }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top tabs with border
            val tabBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TodoTab.entries.forEachIndexed { idx, tab ->
                    val isActive = currentTab == tab
                    val count = when (tab) {
                        TodoTab.HABIT -> habitItems.size
                        TodoTab.MEMO -> memoItems.count { !it.isCompleted }
                        TodoTab.DEADLINE -> deadlineItems.count { !it.isCompleted }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                if (!isActive) Modifier.border(1.dp, tabBorderColor, RoundedCornerShape(12.dp))
                                else Modifier
                            )
                            .background(if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { scope.launch { pagerState.animateScrollToPage(idx) } }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = tab.label,
                                fontSize = 15.sp,
                                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isActive) MaterialTheme.colorScheme.primary else textSecondary
                            )
                            if (count > 0) {
                                Text(
                                    text = "$count 项",
                                    fontSize = 11.sp,
                                    color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else textSecondary.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            // Swipeable content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().weight(1f).clipToBounds()
            ) { page ->
                when (TodoTab.entries[page]) {
                    TodoTab.HABIT -> HabitTab(
                        habits = habitItems,
                        viewModel = viewModel,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        weekStart = habitWeekStart,
                        weekOffset = habitWeekOffset,
                        onWeekOffsetChange = { habitWeekOffset = it },
                        onAdd = { showAddDialog = true },
                        onDeleteRequest = { deletingTodo = it }
                    )
                    TodoTab.MEMO -> MemoTab(
                        items = memoItems,
                        viewModel = viewModel,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        onAdd = { showAddDialog = true },
                        onDeleteRequest = { deletingTodo = it }
                    )
                    TodoTab.DEADLINE -> DeadlineTab(
                        items = deadlineItems,
                        viewModel = viewModel,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        onAdd = { showAddDialog = true },
                        onDeleteRequest = { deletingTodo = it }
                    )
                }
            }
        }
    }
}

// ── Habit Tab ──

@Composable
private fun HabitTab(
    habits: List<TodoItem>,
    viewModel: TodoViewModel,
    textColor: Color,
    textSecondary: Color,
    weekStart: LocalDate,
    weekOffset: Int,
    onWeekOffsetChange: (Int) -> Unit,
    onAdd: () -> Unit,
    onDeleteRequest: (TodoItem) -> Unit
) {
    val today = remember { LocalDate.now() }
    val isCurrentWeek = weekOffset == 0

    if (habits.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Repeat,
            title = "还没有习惯",
            subtitle = "添加每日习惯，追踪你的坚持",
            modifier = Modifier.fillMaxSize()
        ) {
            AddButton(onClick = onAdd)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Week navigation
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onWeekOffsetChange(weekOffset - 1) }) {
                        Icon(Icons.Default.KeyboardArrowLeft, "上一周", tint = textSecondary)
                    }
                    Text(
                        text = if (isCurrentWeek) "本周" else "${weekStart.monthValue}月${weekStart.dayOfMonth}日 起",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary
                    )
                    IconButton(
                        onClick = { if (!isCurrentWeek) onWeekOffsetChange(weekOffset + 1) },
                        enabled = !isCurrentWeek
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowRight, "下一周",
                            tint = if (isCurrentWeek) textSecondary.copy(alpha = 0.3f) else textSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            items(habits, key = { it.id }) { habit ->
                HabitCard(
                    habit = habit,
                    today = today,
                    weekStart = weekStart,
                    textColor = textColor,
                    textSecondary = textSecondary,
                    onToggleDay = { dayIndex ->
                        viewModel.toggleHabitDay(habit, dayIndex, weekStart)
                    },
                    onLongPress = { onDeleteRequest(habit) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                AddButton(onClick = onAdd)
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HabitCard(
    habit: TodoItem,
    today: LocalDate,
    weekStart: LocalDate,
    textColor: Color,
    textSecondary: Color,
    onToggleDay: (Int) -> Unit,
    onLongPress: () -> Unit = {}
) {
    val weekField = WeekFields.of(Locale.getDefault())
    val currentWeek = today.get(weekField.weekOfYear())
    val targetWeek = weekStart.get(weekField.weekOfYear())

    val habitData = parseHabitData(habit.description)
    val habitWeek = habit.tags.toIntOrNull() ?: currentWeek

    val isCurrentWeek = habitWeek == targetWeek
    val weekData = if (isCurrentWeek) habitData else listOf(false, false, false, false, false, false, false)

    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                        onLongClick = onLongPress
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = habit.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
                Text(
                    text = "${weekData.count { it }} / 7",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (weekData.count { it } >= 4) MaterialTheme.colorScheme.primary else textSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 7-day grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (i in 0..6) {
                    val date = weekStart.plusDays(i.toLong())
                    val isChecked = weekData.getOrElse(i) { false }
                    val isToday = date == today
                    val isPast = date.isBefore(today) || date == today

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable(enabled = isPast) { onToggleDay(i) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    when {
                                        isChecked -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        isPast -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        else -> Color.Transparent
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChecked) {
                                Icon(
                                    Icons.Default.Check, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text(
                                    text = "${date.dayOfMonth}",
                                    fontSize = 12.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isToday) MaterialTheme.colorScheme.primary else textSecondary.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Text(
                            text = listOf("一", "二", "三", "四", "五", "六", "日")[i],
                            fontSize = 9.sp,
                            color = textSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun parseHabitData(data: String): List<Boolean> {
    if (data.isBlank()) return listOf(false, false, false, false, false, false, false)
    return try {
        data.split(",").map { it.trim() == "1" }
    } catch (_: Exception) {
        listOf(false, false, false, false, false, false, false)
    }
}

// ── Memo Tab ──

@Composable
private fun MemoTab(
    items: List<TodoItem>,
    viewModel: TodoViewModel,
    textColor: Color,
    textSecondary: Color,
    onAdd: () -> Unit,
    onDeleteRequest: (TodoItem) -> Unit
) {
    val activeItems = items.filter { !it.isCompleted }
    val completedItems = items.filter { it.isCompleted }

    if (items.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Check,
            title = "还没有备忘",
            subtitle = "随手记下怕忘的小事",
            modifier = Modifier.fillMaxSize()
        ) {
            AddButton(onClick = onAdd)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            items(activeItems + completedItems, key = { it.id }) { item ->
                MemoItem(
                    item = item,
                    textColor = textColor,
                    textSecondary = textSecondary,
                    onToggle = { viewModel.toggleTodo(item) },
                    onDelete = { onDeleteRequest(item) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                AddButton(onClick = onAdd)
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MemoItem(
    item: TodoItem,
    textColor: Color,
    textSecondary: Color,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onToggle, onLongClick = onDelete)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (item.isCompleted) Icons.Default.Check else Icons.Default.Add,
            contentDescription = null,
            tint = if (item.isCompleted) SuccessColor.copy(alpha = 0.6f) else textSecondary.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = item.title,
            fontSize = 15.sp,
            color = if (item.isCompleted) textSecondary.copy(alpha = 0.6f) else textColor,
            textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (item.isCompleted) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Default.Close, "删除",
                tint = textSecondary.copy(alpha = 0.3f),
                modifier = Modifier.size(16.dp).clickable(onClick = onDelete)
            )
        }
    }
}

// ── Deadline Tab ──

@Composable
private fun DeadlineTab(
    items: List<TodoItem>,
    viewModel: TodoViewModel,
    textColor: Color,
    textSecondary: Color,
    onAdd: () -> Unit,
    onDeleteRequest: (TodoItem) -> Unit
) {
    val today = remember { LocalDate.now() }
    val activeItems = items.filter { !it.isCompleted }
    val completedItems = items.filter { it.isCompleted }

    if (items.isEmpty()) {
        EmptyState(
            icon = Icons.Default.CalendarMonth,
            title = "还没有待办",
            subtitle = "添加带截止日期的事项",
            modifier = Modifier.fillMaxSize()
        ) {
            AddButton(onClick = onAdd)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            items(activeItems + completedItems, key = { it.id }) { item ->
                DeadlineItem(
                    item = item,
                    today = today,
                    textColor = textColor,
                    textSecondary = textSecondary,
                    onToggle = { viewModel.toggleTodo(item) },
                    onDelete = { onDeleteRequest(item) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                AddButton(onClick = onAdd)
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeadlineItem(
    item: TodoItem,
    today: LocalDate,
    textColor: Color,
    textSecondary: Color,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val dueDate = item.dueDate?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    val daysLeft = dueDate?.let { it.toEpochDay() - today.toEpochDay() } ?: 0
    val isOverdue = daysLeft < 0 && !item.isCompleted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onToggle, onLongClick = onDelete)
            .background(if (isOverdue) ErrorColor.copy(alpha = 0.06f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Days badge
        if (!item.isCompleted && dueDate != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            isOverdue -> ErrorColor.copy(alpha = 0.15f)
                            daysLeft <= 1 -> WarningColor.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        }
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        isOverdue -> "过期"
                        daysLeft == 0L -> "今天"
                        daysLeft == 1L -> "明天"
                        else -> "${daysLeft}天"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        isOverdue -> ErrorColor
                        daysLeft <= 1 -> WarningColor
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        Text(
            text = item.title,
            fontSize = 15.sp,
            color = if (item.isCompleted) textSecondary.copy(alpha = 0.6f) else textColor,
            textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (dueDate != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${dueDate.monthValue}/${dueDate.dayOfMonth}",
                fontSize = 11.sp,
                color = textSecondary.copy(alpha = 0.65f)
            )
        }

        if (item.isCompleted) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Default.Close, "删除",
                tint = textSecondary.copy(alpha = 0.3f),
                modifier = Modifier.size(16.dp).clickable(onClick = onDelete)
            )
        }
    }
}

// ── Shared Add Button ──

@Composable
private fun AddButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Add, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "添加",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
