package com.diary.app.ui.todo

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diary.app.DiaryApplication
import com.diary.app.data.HabitRecord
import com.diary.app.data.Tag
import com.diary.app.data.TodoItem
import com.diary.app.ui.experimental.orderMemoItemsForDisplay
import com.diary.app.ui.experimental.orderTodoItemsForDisplay
import com.diary.app.ui.components.EmptyState
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.GradientBackground
import com.diary.app.ui.components.rememberHapticFeedback
import com.diary.app.ui.theme.ErrorColor
import com.diary.app.ui.theme.SuccessColor
import com.diary.app.ui.theme.WarningColor
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class TodoTab(val label: String) { HABIT("打卡"), MEMO("备忘"), DEADLINE("待办") }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun TodoScreen(
    viewModel: TodoViewModel = viewModel(),
    onMainScreenSwipe: ((Float) -> Unit)? = null
) {
    val app = LocalContext.current.applicationContext as? DiaryApplication ?: return
    val haptic = rememberHapticFeedback()
    val experimentalFeatures by app.experimentalFeatures.collectAsState()
    val allTodos by viewModel.allTodos.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val habitUiState by viewModel.habitUiState.collectAsState()
    val habitSummary by viewModel.habitSummary.collectAsState()
    val selectedHabit by viewModel.selectedHabit.collectAsState()
    val selectedHabitRecords by viewModel.selectedHabitRecords.collectAsState()
    val selectedHabitMonth by viewModel.selectedHabitMonth.collectAsState()
    val selectedHabitDate by viewModel.selectedHabitDate.collectAsState()
    val showHabitDetail by viewModel.showHabitDetail.collectAsState()
    val showHabitRecordDialog by viewModel.showHabitRecordDialog.collectAsState()

    var currentPageIndex by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    val currentTab = TodoTab.entries[currentPageIndex]
    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    val habitItems = allTodos.filter { it.category == TodoItem.CATEGORY_GOAL }
    val memoItems = orderMemoItemsForDisplay(
        items = allTodos.filter { it.category != TodoItem.CATEGORY_GOAL && it.dueDate == null },
        keepCompletedInPlace = experimentalFeatures.keepCompletedItemsInPlace
    )
    val deadlineItems = orderTodoItemsForDisplay(
        items = allTodos.filter { it.dueDate != null }.sortedBy { it.dueDate ?: Long.MAX_VALUE },
        keepCompletedInPlace = experimentalFeatures.keepCompletedItemsInPlace
    )

    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var editingTodo by remember { mutableStateOf<TodoItem?>(null) }
    var editingHabit by remember { mutableStateOf<TodoItem?>(null) }
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

    if (isMultiSelectMode && selectedIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { isMultiSelectMode = false; selectedIds = emptySet() },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("批量删除") },
            text = { Text("确定要删除选中的 ${selectedIds.size} 项吗？") },
            confirmButton = {
                TextButton(onClick = {
                    haptic.warning()
                    selectedIds.forEach { id ->
                        allTodos.find { it.id == id }?.let { viewModel.deleteTodo(it) }
                    }
                    isMultiSelectMode = false
                    selectedIds = emptySet()
                }) { Text("删除", color = ErrorColor) }
            },
            dismissButton = {
                TextButton(onClick = { isMultiSelectMode = false; selectedIds = emptySet() }) { Text("取消") }
            }
        )
    }

    editingTodo?.let { todo ->
        var editTitle by remember(todo) { mutableStateOf(todo.title) }
        AlertDialog(
            onDismissRequest = { editingTodo = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("编辑") },
            text = {
                TextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    placeholder = { Text("标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editTitle.isNotBlank()) {
                        viewModel.updateTodo(todo.copy(title = editTitle.trim()))
                        editingTodo = null
                    }
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { editingTodo = null }) { Text("取消") } }
        )
    }

    editingHabit?.let { habit ->
        var editName by remember(habit) { mutableStateOf(habit.title) }
        var selectedLinkedTagIds by remember(habit) {
            mutableStateOf(TodoItem.getLinkedTagIds(habit.linkedTagIds).toSet())
        }
        AlertDialog(
            onDismissRequest = { editingHabit = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("编辑打卡项") },
            text = {
                Column {
                    TextField(
                        value = editName,
                        onValueChange = { editName = it },
                        placeholder = { Text("例如：运动、早睡、背单词") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (allTags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "关联日记分类（可选）",
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        HabitTagSelector(
                            tags = allTags,
                            selectedTagIds = selectedLinkedTagIds,
                            onToggle = { tagId ->
                                selectedLinkedTagIds = if (tagId in selectedLinkedTagIds) {
                                    selectedLinkedTagIds - tagId
                                } else {
                                    selectedLinkedTagIds + tagId
                                }
                            },
                            textSecondary = textSecondary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editName.isNotBlank()) {
                        viewModel.updateHabit(
                            habitId = habit.id,
                            name = editName.trim(),
                            linkedTagIds = selectedLinkedTagIds.toList()
                        )
                        editingHabit = null
                    }
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { editingHabit = null }) { Text("取消") } }
        )
    }

    if (showAddDialog) {
        when (currentTab) {
            TodoTab.HABIT -> {
                var name by remember { mutableStateOf("") }
                var selectedLinkedTagIds by remember { mutableStateOf(setOf<Long>()) }
                AlertDialog(
                    onDismissRequest = { showAddDialog = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    title = { Text("新建打卡项") },
                    text = {
                        Column {
                            TextField(
                                value = name,
                                onValueChange = { name = it },
                                placeholder = { Text("例如：运动、早睡、背单词") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (allTags.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "关联日记分类（可选）",
                                    fontSize = 12.sp,
                                    color = textSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                HabitTagSelector(
                                    tags = allTags,
                                    selectedTagIds = selectedLinkedTagIds,
                                    onToggle = { tagId ->
                                        selectedLinkedTagIds = if (tagId in selectedLinkedTagIds) {
                                            selectedLinkedTagIds - tagId
                                        } else {
                                            selectedLinkedTagIds + tagId
                                        }
                                    },
                                    textSecondary = textSecondary
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (name.isNotBlank()) {
                                viewModel.addHabit(name.trim(), selectedLinkedTagIds.toList())
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
                            value = content,
                            onValueChange = { content = it },
                            placeholder = { Text("要记住的事情") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
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
                                value = content,
                                onValueChange = { content = it },
                                placeholder = { Text("要做什么事") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
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
                                        datePickerState.selectedDateMillis?.let { millis -> selectedDate = millis }
                                        showDatePicker = false
                                    }) { Text("确定") }
                                },
                                dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } }
                            ) { DatePicker(state = datePickerState) }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val currentSelectedDate = selectedDate
                                if (content.isNotBlank() && currentSelectedDate != null) {
                                    viewModel.addDeadline(content.trim(), currentSelectedDate)
                                    showAddDialog = false
                                }
                            },
                            enabled = content.isNotBlank() && selectedDate != null
                        ) { Text("创建") }
                    },
                    dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("取消") } }
                )
            }
        }
    }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
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
                            .clickable {
                                currentPageIndex = idx
                            }
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
                                    color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    else textSecondary.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
                    .pointerInput(currentPageIndex) {
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                totalDrag += dragAmount
                            },
                            onDragEnd = {
                                when {
                                    totalDrag <= -48f && currentPageIndex < TodoTab.entries.lastIndex -> {
                                        currentPageIndex += 1
                                    }
                                    totalDrag >= 48f && currentPageIndex > 0 -> {
                                        currentPageIndex -= 1
                                    }
                                    totalDrag <= -48f && currentPageIndex == TodoTab.entries.lastIndex -> {
                                        onMainScreenSwipe?.invoke(totalDrag)
                                    }
                                    totalDrag >= 48f && currentPageIndex == 0 -> {
                                        onMainScreenSwipe?.invoke(totalDrag)
                                    }
                                }
                            }
                        )
                    }
            ) {
                AnimatedContent(
                    targetState = currentPageIndex,
                    transitionSpec = {
                        val forward = targetState > initialState
                        val enter = slideInHorizontally(
                            animationSpec = tween(durationMillis = 210, easing = LinearOutSlowInEasing),
                            initialOffsetX = { fullWidth ->
                                if (forward) fullWidth / 10 else -fullWidth / 10
                            }
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 190, easing = LinearOutSlowInEasing),
                            initialAlpha = 0.78f
                        )
                        val exit = slideOutHorizontally(
                            animationSpec = tween(durationMillis = 110, easing = FastOutLinearInEasing),
                            targetOffsetX = { fullWidth ->
                                if (forward) -fullWidth / 24 else fullWidth / 24
                            }
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = 90, easing = FastOutLinearInEasing)
                        )
                        enter togetherWith exit
                    },
                    label = "todo_tab_cutover"
                ) { page ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (TodoTab.entries[page]) {
                            TodoTab.HABIT -> HabitTab(
                                habits = habitUiState,
                                summary = habitSummary,
                                viewModel = viewModel,
                                textColor = textColor,
                                textSecondary = textSecondary,
                                onAdd = { showAddDialog = true },
                                onDeleteRequest = { deletingTodo = it }
                            )
                            TodoTab.MEMO -> MemoTab(
                                items = memoItems,
                                viewModel = viewModel,
                                textColor = textColor,
                                textSecondary = textSecondary,
                                onAdd = { showAddDialog = true },
                                onDeleteRequest = { deletingTodo = it },
                                onEdit = { editingTodo = it },
                                isMultiSelectMode = isMultiSelectMode,
                                selectedIds = selectedIds,
                                onToggleSelection = { id ->
                                    selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                                },
                                onMultiSelectModeChange = { mode ->
                                    isMultiSelectMode = mode
                                    if (!mode) selectedIds = emptySet()
                                }
                            )
                            TodoTab.DEADLINE -> DeadlineTab(
                                items = deadlineItems,
                                viewModel = viewModel,
                                textColor = textColor,
                                textSecondary = textSecondary,
                                onAdd = { showAddDialog = true },
                                onDeleteRequest = { deletingTodo = it },
                                onEdit = { editingTodo = it },
                                isMultiSelectMode = isMultiSelectMode,
                                selectedIds = selectedIds,
                                onToggleSelection = { id ->
                                    selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                                },
                                onMultiSelectModeChange = { mode ->
                                    isMultiSelectMode = mode
                                    if (!mode) selectedIds = emptySet()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    val currentSelectedHabit = selectedHabit
    if (showHabitDetail && currentSelectedHabit != null) {
        HabitDetailDialog(
            habit = currentSelectedHabit,
            records = selectedHabitRecords,
            selectedMonth = selectedHabitMonth,
            selectedDate = selectedHabitDate,
            onDismiss = { viewModel.closeHabitDetail() },
            onEdit = { editingHabit = selectedHabit },
            onMonthChange = { delta -> viewModel.moveSelectedHabitMonth(delta) },
            onDateSelect = { date -> viewModel.selectHabitDate(date) },
            onQuickRecord = { summaryText ->
                viewModel.saveHabitQuickRecord(
                    habitId = currentSelectedHabit.id,
                    date = selectedHabitDate,
                    summary = summaryText,
                    source = HabitRecord.SOURCE_MANUAL
                )
            },
            onOpenMore = { viewModel.showHabitRecordDialog(currentSelectedHabit.id, selectedHabitDate) },
            onClear = { viewModel.clearHabitRecordForDay(currentSelectedHabit.id, selectedHabitDate) }
        )
    }

    if (showHabitRecordDialog && currentSelectedHabit != null) {
        HabitRecordDialog(
            habit = currentSelectedHabit,
            selectedDate = selectedHabitDate,
            existingRecord = selectedHabitRecords.firstOrNull { it.recordDate == selectedHabitDate.toEpochDay() },
            onDismiss = { viewModel.hideHabitRecordDialog() },
            onSave = { summaryText ->
                viewModel.saveHabitQuickRecord(
                    habitId = currentSelectedHabit.id,
                    date = selectedHabitDate,
                    summary = summaryText,
                    source = HabitRecord.SOURCE_DETAIL
                )
                viewModel.hideHabitRecordDialog()
            }
        )
    }
}

@Composable
private fun HabitTab(
    habits: List<HabitItemUiState>,
    summary: HabitSummaryUiState,
    viewModel: TodoViewModel,
    textColor: Color,
    textSecondary: Color,
    onAdd: () -> Unit,
    onDeleteRequest: (TodoItem) -> Unit
) {
    if (habits.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Repeat,
            title = "还没有打卡项",
            subtitle = "添加一个简单的每日项目，之后写一句话或用日记自动打卡",
            modifier = Modifier.fillMaxSize()
        ) {
            AddButton(onClick = onAdd)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            HabitSummaryCard(summary = summary, textColor = textColor, textSecondary = textSecondary)
        }

        items(habits, key = { it.habit.id }) { item ->
            HabitCard(
                item = item,
                textColor = textColor,
                textSecondary = textSecondary,
                onOpen = { viewModel.openHabitDetail(item.habit.id) },
                onQuickCheckIn = {
                    if (item.todayRecord == null) {
                        viewModel.showHabitRecordDialog(item.habit.id)
                    } else {
                        viewModel.openHabitDetail(item.habit.id)
                    }
                },
                onLongPress = { onDeleteRequest(item.habit) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            AddButton(onClick = onAdd)
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun HabitSummaryCard(
    summary: HabitSummaryUiState,
    textColor: Color,
    textSecondary: Color
) {
    val progress = if (summary.total == 0) 0f else summary.recordedToday.toFloat() / summary.total.toFloat()

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        gradientColors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), CircleShape)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${summary.recordedToday}/${summary.total}",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
                Text(
                    text = "今日打卡",
                    fontSize = 11.sp,
                    color = textSecondary
                )
            }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "今天完成 ${summary.recordedToday} 项",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
                Text(
                    text = when {
                        summary.recordedToday == 0 -> "今天还没有打卡"
                        summary.recordedToday == summary.total -> "今天已经全部完成"
                        else -> "还差 ${summary.total - summary.recordedToday} 项"
                    },
                    fontSize = 12.sp,
                    color = textSecondary
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.72f))
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HabitCard(
    item: HabitItemUiState,
    textColor: Color,
    textSecondary: Color,
    onOpen: () -> Unit,
    onQuickCheckIn: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    val statusText = item.todayRecord?.let { HabitRecord.sourceLabel(it.source) } ?: "未打卡"
    val summaryText = item.todayRecord?.summary?.takeIf { it.isNotBlank() } ?: "今天还没有记录"
    val statusColor = when (item.todayRecord?.source) {
        HabitRecord.SOURCE_DIARY -> MaterialTheme.colorScheme.primary
        HabitRecord.SOURCE_DETAIL -> SuccessColor
        HabitRecord.SOURCE_MANUAL -> MaterialTheme.colorScheme.secondary
        else -> textSecondary
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onOpen, onLongClick = onLongPress),
        cornerRadius = 16.dp,
        innerPadding = 14.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.habit.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (item.todayRecord != null) statusColor.copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                )
                                .border(
                                    width = if (item.todayRecord != null) 1.dp else 0.dp,
                                    color = if (item.todayRecord != null) statusColor.copy(alpha = 0.35f) else Color.Transparent,
                                    shape = RoundedCornerShape(999.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                if (item.todayRecord != null) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = statusColor,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                                Text(
                                    text = if (item.todayRecord != null) "已打卡" else statusText,
                                    fontSize = 10.sp,
                                    fontWeight = if (item.todayRecord != null) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (item.todayRecord != null) statusColor else textSecondary
                                )
                            }
                        }
                    }
                }

                Text(
                    text = if (item.streak > 0) "${item.streak} 天" else "未开始",
                    fontSize = 10.sp,
                    color = textSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = summaryText,
                    fontSize = 11.sp,
                    color = if (item.todayRecord == null) textSecondary.copy(alpha = 0.9f) else textColor.copy(alpha = 0.92f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                TextButton(onClick = onQuickCheckIn) {
                    Text(if (item.todayRecord == null) "记录" else "详情", fontSize = 12.sp)
                }
            }

            HabitRecentStrip(days = item.recentDays, textSecondary = textSecondary)
        }
    }
}

@Composable
private fun HabitRecentStrip(
    days: List<HabitDayState>,
    textSecondary: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        days.forEach { day ->
            val tint = when (day.record?.source) {
                HabitRecord.SOURCE_DIARY -> MaterialTheme.colorScheme.primary
                HabitRecord.SOURCE_DETAIL -> SuccessColor
                HabitRecord.SOURCE_MANUAL -> MaterialTheme.colorScheme.secondary
                else -> textSecondary.copy(alpha = 0.36f)
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .clip(RoundedCornerShape(if (day.isToday) 12.dp else 10.dp))
                        .background(
                            when {
                                day.record != null -> tint.copy(alpha = if (day.isToday) 0.2f else 0.12f)
                                else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                            }
                        )
                        .border(
                            width = if (day.isToday) 1.dp else 0.dp,
                            color = if (day.isToday) tint.copy(alpha = 0.35f) else Color.Transparent,
                            shape = RoundedCornerShape(if (day.isToday) 12.dp else 10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.date.dayOfMonth.toString(),
                        fontSize = 12.sp,
                        fontWeight = if (day.isToday) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (day.record != null) tint else textSecondary
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (day.isToday) "今" else day.date.format(DateTimeFormatter.ofPattern("E")),
                    fontSize = 9.sp,
                    color = textSecondary.copy(alpha = 0.88f),
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitTagSelector(
    tags: List<Tag>,
    selectedTagIds: Set<Long>,
    onToggle: (Long) -> Unit,
    textSecondary: Color
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tags.forEach { tag ->
            val isSelected = tag.id in selectedTagIds
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    .clickable { onToggle(tag.id) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = tag.name,
                    fontSize = 12.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else textSecondary
                )
            }
        }
    }
}

@Composable
private fun HabitDetailDialog(
    habit: TodoItem,
    records: List<HabitRecord>,
    selectedMonth: YearMonth,
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onMonthChange: (Long) -> Unit,
    onDateSelect: (LocalDate) -> Unit,
    onQuickRecord: (String) -> Unit,
    onOpenMore: () -> Unit,
    onClear: () -> Unit
) {
    val selectedRecord = records.firstOrNull { it.recordDate == selectedDate.toEpochDay() }
    var quickText by remember(selectedDate, selectedRecord?.summary) { mutableStateOf(selectedRecord?.summary.orEmpty()) }
    var showDeleteConfirm by remember(selectedDate, selectedRecord?.id) { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("清除记录") },
            text = { Text("确定要清除这一天的打卡记录吗？") },
            confirmButton = {
                TextButton(onClick = {
                    onClear()
                    showDeleteConfirm = false
                }) { Text("清除", color = ErrorColor) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            cornerRadius = 24.dp,
            innerPadding = 14.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = habit.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "查看这一项过去的打卡情况",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onEdit) { Text("编辑", fontSize = 12.sp) }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                }

                HabitCalendar(
                    selectedMonth = selectedMonth,
                    selectedDate = selectedDate,
                    records = records,
                    onMonthChange = onMonthChange,
                    onDateSelect = onDateSelect,
                    onJumpToToday = { onDateSelect(LocalDate.now()) }
                )

                TextField(
                    value = quickText,
                    onValueChange = { quickText = it },
                    minLines = 1,
                    maxLines = 3,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    placeholder = { Text("写一句今天的打卡记录", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = {
                        if (quickText.isNotBlank()) onQuickRecord(quickText.trim())
                    }) { Text("保存", fontSize = 13.sp) }
                    TextButton(onClick = onOpenMore) { Text("详细", fontSize = 13.sp) }
                    if (selectedRecord != null && selectedRecord.source != HabitRecord.SOURCE_DIARY) {
                        TextButton(onClick = { showDeleteConfirm = true }) { Text("删除", fontSize = 13.sp, color = ErrorColor) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitCalendar(
    selectedMonth: YearMonth,
    selectedDate: LocalDate,
    records: List<HabitRecord>,
    onMonthChange: (Long) -> Unit,
    onDateSelect: (LocalDate) -> Unit,
    onJumpToToday: () -> Unit
) {
    val firstDay = selectedMonth.atDay(1)
    val leading = firstDay.dayOfWeek.value % 7
    val daysInMonth = selectedMonth.lengthOfMonth()
    val cells = buildList {
        repeat(leading) { add(null) }
        repeat(daysInMonth) { add(selectedMonth.atDay(it + 1)) }
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        innerPadding = 12.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onMonthChange(-1) }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null)
                    }
                    Text(
                        text = selectedMonth.format(DateTimeFormatter.ofPattern("yyyy年M月")),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(onClick = { onMonthChange(1) }) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                    }
                }
                TextButton(onClick = onJumpToToday) {
                    Text("今天", fontSize = 12.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("日", "一", "二", "三", "四", "五", "六").forEach {
                    Text(
                        text = it,
                        modifier = Modifier.weight(1f),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            cells.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    week.forEach { date ->
                        if (date == null) {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(0.82f))
                        } else {
                            val record = records.firstOrNull { it.recordDate == date.toEpochDay() }
                            val tint = when (record?.source) {
                                HabitRecord.SOURCE_DIARY -> MaterialTheme.colorScheme.primary
                                HabitRecord.SOURCE_DETAIL -> SuccessColor
                                HabitRecord.SOURCE_MANUAL -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)
                            }
                            val isSelected = date == selectedDate
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(0.82f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isSelected) tint.copy(alpha = 0.18f)
                                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                    )
                                    .border(
                                        width = if (isSelected) 1.dp else 0.dp,
                                        color = if (isSelected) tint.copy(alpha = 0.4f) else Color.Transparent,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onDateSelect(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = date.dayOfMonth.toString(),
                                        fontSize = 13.sp,
                                        color = if (record != null) tint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f)
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    if (record != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(tint)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    repeat(7 - week.size) {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(0.82f))
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitRecordDialog(
    habit: TodoItem,
    selectedDate: LocalDate,
    existingRecord: HabitRecord?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var content by remember(existingRecord?.summary, selectedDate) {
        mutableStateOf(existingRecord?.summary.orEmpty())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("${habit.title} · ${selectedDate.format(DateTimeFormatter.ofPattern("M月d日"))}", fontSize = 17.sp) },
        text = {
            TextField(
                value = content,
                onValueChange = { content = it },
                minLines = 2,
                maxLines = 5,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                placeholder = { Text("可以只写一句，也可以多写一点", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)) },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (content.isNotBlank()) onSave(content.trim()) },
                enabled = content.isNotBlank()
            ) { Text("保存", fontSize = 13.sp) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", fontSize = 13.sp) } }
    )
}

@Composable
private fun MemoTab(
    items: List<TodoItem>,
    viewModel: TodoViewModel,
    textColor: Color,
    textSecondary: Color,
    onAdd: () -> Unit,
    onDeleteRequest: (TodoItem) -> Unit,
    onEdit: (TodoItem) -> Unit,
    isMultiSelectMode: Boolean,
    selectedIds: Set<Long>,
    onToggleSelection: (Long) -> Unit,
    onMultiSelectModeChange: (Boolean) -> Unit
) {
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

            items(items, key = { it.id }) { item ->
                MemoItem(
                    item = item,
                    textColor = textColor,
                    textSecondary = textSecondary,
                    onToggle = { viewModel.toggleTodo(item) },
                    onDelete = { onDeleteRequest(item) },
                    onEdit = { onEdit(item) },
                    isMultiSelectMode = isMultiSelectMode,
                    isSelected = item.id in selectedIds,
                    onToggleSelection = { onToggleSelection(item.id) },
                    onLongPress = {
                        if (!isMultiSelectMode) {
                            onMultiSelectModeChange(true)
                            onToggleSelection(item.id)
                        }
                    }
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
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onLongPress: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = {
                    if (isMultiSelectMode) onToggleSelection() else onEdit()
                },
                onLongClick = onLongPress
            )
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .clickable { if (isMultiSelectMode) onToggleSelection() else onToggle() }
                .background(
                    when {
                        isMultiSelectMode && isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        item.isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else -> Color.Transparent
                    }
                )
                .border(
                    width = 1.5.dp,
                    color = when {
                        isMultiSelectMode && isSelected -> MaterialTheme.colorScheme.primary
                        item.isCompleted -> MaterialTheme.colorScheme.primary
                        else -> textSecondary.copy(alpha = 0.3f)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if ((isMultiSelectMode && isSelected) || item.isCompleted) {
                Icon(
                    Icons.Default.Check,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
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
        if (!isMultiSelectMode) {
            Icon(
                Icons.Default.Edit,
                "编辑",
                tint = textSecondary.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp).clickable(onClick = onEdit)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.Close,
                "删除",
                tint = textSecondary.copy(alpha = 0.3f),
                modifier = Modifier.size(16.dp).clickable(onClick = onDelete)
            )
        }
    }
}

@Composable
private fun DeadlineTab(
    items: List<TodoItem>,
    viewModel: TodoViewModel,
    textColor: Color,
    textSecondary: Color,
    onAdd: () -> Unit,
    onDeleteRequest: (TodoItem) -> Unit,
    onEdit: (TodoItem) -> Unit,
    isMultiSelectMode: Boolean,
    selectedIds: Set<Long>,
    onToggleSelection: (Long) -> Unit,
    onMultiSelectModeChange: (Boolean) -> Unit
) {
    val today = remember { LocalDate.now() }

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

            items(items, key = { it.id }) { item ->
                DeadlineItem(
                    item = item,
                    today = today,
                    textColor = textColor,
                    textSecondary = textSecondary,
                    onToggle = { viewModel.toggleTodo(item) },
                    onDelete = { onDeleteRequest(item) },
                    onEdit = { onEdit(item) },
                    isMultiSelectMode = isMultiSelectMode,
                    isSelected = item.id in selectedIds,
                    onToggleSelection = { onToggleSelection(item.id) },
                    onLongPress = {
                        if (!isMultiSelectMode) {
                            onMultiSelectModeChange(true)
                            onToggleSelection(item.id)
                        }
                    }
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
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onLongPress: () -> Unit
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
            .combinedClickable(
                onClick = { if (isMultiSelectMode) onToggleSelection() else onToggle() },
                onLongClick = onLongPress
            )
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    isOverdue -> ErrorColor.copy(alpha = 0.06f)
                    else -> Color.Transparent
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isMultiSelectMode) {
            Icon(
                imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Add,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else textSecondary.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        if (!item.isCompleted && dueDate != null && !isMultiSelectMode) {
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

        if (dueDate != null && !isMultiSelectMode) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${dueDate.monthValue}/${dueDate.dayOfMonth}",
                fontSize = 11.sp,
                color = textSecondary.copy(alpha = 0.65f)
            )
        }

        if (!isMultiSelectMode) {
            Icon(
                Icons.Default.Edit,
                "编辑",
                tint = textSecondary.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp).clickable(onClick = onEdit)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.Close,
                "删除",
                tint = textSecondary.copy(alpha = 0.3f),
                modifier = Modifier.size(16.dp).clickable(onClick = onDelete)
            )
        }
    }
}

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
                Icons.Default.Add,
                null,
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
