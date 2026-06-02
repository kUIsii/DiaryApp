package com.diary.app.ui.todo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDismissState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.data.TodoItem
import com.diary.app.ui.components.EmptyState
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.components.rememberHapticFeedback
import com.diary.app.ui.theme.LocalExtendedColors
import com.diary.app.ui.theme.ErrorColor
import com.diary.app.ui.theme.WarningColor
import androidx.compose.ui.res.stringResource
import com.diary.app.R
import com.diary.app.ui.theme.PrimaryBlue
import com.diary.app.ui.theme.SuccessColor
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val CategoryColors = mapOf(
    TodoItem.CATEGORY_TASK to PrimaryBlue,
    TodoItem.CATEGORY_REMINDER to WarningColor,
    TodoItem.CATEGORY_GOAL to SuccessColor
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TodoScreen(viewModel: TodoViewModel = viewModel()) {
    val haptic = rememberHapticFeedback()
    val todos by viewModel.allTodos.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<TodoItem?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var searchInputText by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val extendedColors = LocalExtendedColors.current
    val gradientStart = extendedColors.gradientStart
    val gradientEnd = extendedColors.gradientEnd

    val pendingCount = todos.count { !it.isCompleted }
    val completedCount = todos.count { it.isCompleted }
    val today = remember { LocalDate.now() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("M月d日 EEEE") }

    // Edit dialog
    editingTodo?.let { todo ->
        EditTodoDialog(
            todo = todo,
            onDismiss = { editingTodo = null },
            onConfirm = { updatedTodo ->
                viewModel.updateTodo(updatedTodo)
                editingTodo = null
            },
            viewModel = viewModel
        )
    }

    // Add dialog with full options
    if (showAddDialog) {
        AddTodoDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, description, priority, category, dueDate, reminderTime, tags, recurringType ->
                viewModel.addTodo(
                    title = title,
                    description = description,
                    priority = priority,
                    dueDate = dueDate,
                    category = category,
                    reminderTime = reminderTime,
                    tags = tags,
                    recurringType = recurringType
                )
                showAddDialog = false
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Header
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.todo_title),
                                color = textPrimary,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = today.format(dateFormatter),
                                color = textSecondary,
                                fontSize = 14.sp
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Search button
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable {
                                        showSearch = !showSearch
                                        if (!showSearch) {
                                            searchInputText = ""
                                            viewModel.setSearchQuery("")
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (showSearch) Icons.Default.Close else Icons.Default.Search,
                                    contentDescription = if (showSearch) "关闭搜索" else "搜索",
                                    tint = textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            // Add with options button
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(gradientStart, gradientEnd)
                                        )
                                    )
                                    .clickable { showAddDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "添加待办",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    // Stats row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatBadge(
                            label = "待完成",
                            count = pendingCount,
                            color = MaterialTheme.colorScheme.primary
                        )
                        StatBadge(
                            label = "已完成",
                            count = completedCount,
                            color = SuccessColor
                        )
                        StatBadge(
                            label = "总计",
                            count = todos.size,
                            color = textSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Search bar
            if (showSearch) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        innerPadding = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "搜索",
                                tint = textSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            TextField(
                                value = searchInputText,
                                onValueChange = {
                                    searchInputText = it
                                    viewModel.setSearchQuery(it)
                                },
                                modifier = Modifier.weight(1f),
                                placeholder = {
                                    Text(
                                        "搜索待办...",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                singleLine = true
                            )
                            if (searchInputText.isNotBlank()) {
                                IconButton(onClick = {
                                    searchInputText = ""
                                    viewModel.setSearchQuery("")
                                }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "清除",
                                        tint = textSecondary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Category filter chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryFilterChip(
                        label = "全部",
                        isSelected = selectedCategory == null && selectedTag == null,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { viewModel.clearFilters() }
                    )
                    CategoryFilterChip(
                        label = "任务",
                        isSelected = selectedCategory == TodoItem.CATEGORY_TASK,
                        color = CategoryColors[TodoItem.CATEGORY_TASK]!!,
                        onClick = { viewModel.setCategoryFilter(if (selectedCategory == TodoItem.CATEGORY_TASK) null else TodoItem.CATEGORY_TASK) }
                    )
                    CategoryFilterChip(
                        label = "提醒",
                        isSelected = selectedCategory == TodoItem.CATEGORY_REMINDER,
                        color = CategoryColors[TodoItem.CATEGORY_REMINDER]!!,
                        onClick = { viewModel.setCategoryFilter(if (selectedCategory == TodoItem.CATEGORY_REMINDER) null else TodoItem.CATEGORY_REMINDER) }
                    )
                    CategoryFilterChip(
                        label = "目标",
                        isSelected = selectedCategory == TodoItem.CATEGORY_GOAL,
                        color = CategoryColors[TodoItem.CATEGORY_GOAL]!!,
                        onClick = { viewModel.setCategoryFilter(if (selectedCategory == TodoItem.CATEGORY_GOAL) null else TodoItem.CATEGORY_GOAL) }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Quick input field
            item {
                InputRow(
                    value = inputText,
                    onValueChange = { inputText = it },
                    onAdd = {
                        if (inputText.isNotBlank()) {
                            viewModel.addTodo(inputText, category = selectedCategory ?: TodoItem.CATEGORY_TASK)
                            inputText = ""
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Todo items
            if (todos.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.EventNote,
                        title = stringResource(R.string.todo_empty_title),
                        subtitle = stringResource(R.string.todo_empty_subtitle)
                    )
                }
            } else {
                itemsIndexed(
                    items = todos,
                    key = { _, todo -> todo.id }
                ) { index, todo ->
                    val enterDelay = (index * 40).coerceAtMost(400)
                    AnimatedVisibility(
                        visible = true,
                        modifier = Modifier.animateItemPlacement(),
                        enter = fadeIn(animationSpec = tween(300, delayMillis = enterDelay)) +
                                slideInVertically(
                                    animationSpec = tween(300, delayMillis = enterDelay),
                                    initialOffsetY = { it / 4 }
                                )
                    ) {
                        val dismissState = rememberDismissState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == DismissValue.DismissedToStart) {
                                    viewModel.deleteTodo(todo)
                                    true
                                } else false
                            }
                        )

                        SwipeToDismiss(
                            state = dismissState,
                            directions = setOf(DismissDirection.EndToStart),
                            background = {
                                val progress by animateFloatAsState(
                                    targetValue = if (dismissState.dismissDirection == DismissDirection.EndToStart) 1f else 0f,
                                    label = "swipe_progress"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(ErrorColor.copy(alpha = 0.15f * progress))
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "删除",
                                        tint = ErrorColor.copy(alpha = progress),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            dismissContent = {
                                TodoItemCard(
                                    todo = todo,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    onToggle = {
                                        haptic.click()
                                        viewModel.toggleTodo(todo)
                                    },
                                    onDelete = {
                                        haptic.warning()
                                        viewModel.deleteTodo(todo)
                                    },
                                    onLongPress = {
                                        haptic.click()
                                        editingTodo = todo
                                    }
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Clear completed button
            if (todos.any { it.isCompleted }) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    ClearCompletedButton(
                        onClick = { showClearDialog = true },
                        textSecondary = textSecondary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            } else {
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Clear confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.clear_completed_title)) },
            text = { Text(stringResource(R.string.clear_completed_message)) },
            confirmButton = {
                TextButton(onClick = {
                    haptic.warning()
                    viewModel.clearCompleted()
                    showClearDialog = false
                }) {
                    Text(stringResource(R.string.confirm), color = ErrorColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun StatBadge(label: String, count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label ",
            fontSize = 12.sp,
            color = color.copy(alpha = 0.7f)
        )
        Text(
            text = "$count",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun InputRow(
    value: String,
    onValueChange: (String) -> Unit,
    onAdd: () -> Unit
) {
    val extendedColors = LocalExtendedColors.current
    val gradientStart = extendedColors.gradientStart
    val gradientEnd = extendedColors.gradientEnd
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        innerPadding = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        stringResource(R.string.todo_add_placeholder),
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
                textStyle = MaterialTheme.typography.bodyLarge
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = if (value.isNotBlank()) listOf(gradientStart, gradientEnd)
                            else listOf(
                                gradientStart.copy(alpha = 0.3f),
                                gradientEnd.copy(alpha = 0.3f)
                            )
                        )
                    )
                    .clickable(
                        enabled = value.isNotBlank(),
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAdd
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "添加",
                    tint = Color.White.copy(alpha = if (value.isNotBlank()) 1f else 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

@Composable
private fun CategoryFilterChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) color.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTodoDialog(
    todo: TodoItem,
    onDismiss: () -> Unit,
    onConfirm: (TodoItem) -> Unit,
    viewModel: TodoViewModel
) {
    var title by remember { mutableStateOf(todo.title) }
    var description by remember { mutableStateOf(todo.description) }
    var priority by remember { mutableStateOf(todo.priority) }
    var category by remember { mutableStateOf(todo.category) }
    var reminderTime by remember { mutableStateOf(todo.reminderTime) }
    var dueDate by remember { mutableStateOf(todo.dueDate) }
    var tagsInput by remember { mutableStateOf(todo.tags) }
    var recurringType by remember { mutableStateOf(todo.recurringType) }
    var progress by remember { mutableStateOf(todo.progress) }
    var showSubTasks by remember { mutableStateOf(false) }
    var newSubTaskTitle by remember { mutableStateOf("") }

    val subTodos by viewModel.getSubTodos(todo.id).collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dueDate ?: reminderTime ?: System.currentTimeMillis()
    )
    val timePickerState = rememberTimePickerState(
        initialHour = if (reminderTime != null) {
            Instant.ofEpochMilli(reminderTime!!).atZone(ZoneId.systemDefault()).toLocalTime().hour
        } else 9,
        initialMinute = if (reminderTime != null) {
            Instant.ofEpochMilli(reminderTime!!).atZone(ZoneId.systemDefault()).toLocalTime().minute
        } else 0,
        is24Hour = true
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dateMillis ->
                        val date = Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                        val time = if (reminderTime != null) {
                            Instant.ofEpochMilli(reminderTime!!).atZone(ZoneId.systemDefault()).toLocalTime()
                        } else LocalTime.of(9, 0)
                        dueDate = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        reminderTime = date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("选择提醒时间") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val date = if (reminderTime != null) {
                        Instant.ofEpochMilli(reminderTime!!).atZone(ZoneId.systemDefault()).toLocalDate()
                    } else LocalDate.now()
                    reminderTime = date.atTime(LocalTime.of(timePickerState.hour, timePickerState.minute))
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    showTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("编辑待办") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Priority
                Text("优先级", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "普通", 1 to "重要", 2 to "紧急").forEach { (level, label) ->
                        val isSelected = priority == level
                        val chipColor = when (level) {
                            1 -> WarningColor
                            2 -> ErrorColor
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) chipColor.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable { priority = level }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                color = if (isSelected) chipColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Category
                Text("分类", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        TodoItem.CATEGORY_TASK to "任务",
                        TodoItem.CATEGORY_REMINDER to "提醒",
                        TodoItem.CATEGORY_GOAL to "目标"
                    ).forEach { (cat, label) ->
                        val isSelected = category == cat
                        val chipColor = CategoryColors[cat] ?: MaterialTheme.colorScheme.primary
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) chipColor.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable { category = cat }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                color = if (isSelected) chipColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Progress (for goals)
                if (category == TodoItem.CATEGORY_GOAL) {
                    Text("进度: $progress%", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = progress.toFloat(),
                        onValueChange = { progress = it.toInt() },
                        valueRange = 0f..100f,
                        steps = 99
                    )
                }

                // Due date & reminder
                Text("截止日期 & 提醒", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (dueDate != null) {
                                Instant.ofEpochMilli(dueDate!!).atZone(ZoneId.systemDefault())
                                    .toLocalDate().format(DateTimeFormatter.ofPattern("M月d日"))
                            } else "选择日期",
                            fontSize = 12.sp,
                            color = if (dueDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { showTimePicker = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (reminderTime != null) {
                                Instant.ofEpochMilli(reminderTime!!).atZone(ZoneId.systemDefault())
                                    .toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                            } else "选择时间",
                            fontSize = 12.sp,
                            color = if (reminderTime != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (dueDate != null || reminderTime != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ErrorColor.copy(alpha = 0.1f))
                                .clickable {
                                    dueDate = null
                                    reminderTime = null
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("清除", fontSize = 12.sp, color = ErrorColor)
                        }
                    }
                }

                // Recurring
                Text("重复", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        TodoItem.RECURRING_NONE to "不重复",
                        TodoItem.RECURRING_DAILY to "每天",
                        TodoItem.RECURRING_WEEKLY to "每周",
                        TodoItem.RECURRING_MONTHLY to "每月",
                        TodoItem.RECURRING_YEARLY to "每年"
                    ).forEach { (type, label) ->
                        val isSelected = recurringType == type
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable { recurringType = type }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Tags
                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    label = { Text("标签（用逗号分隔）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Subtasks section
                if (todo.parentId == null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSubTasks = !showSubTasks }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "子任务 (${subTodos.size})",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            if (showSubTasks) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (showSubTasks) "收起" else "展开",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (showSubTasks) {
                        // Add subtask input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newSubTaskTitle,
                                onValueChange = { newSubTaskTitle = it },
                                placeholder = { Text("添加子任务", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (newSubTaskTitle.isNotBlank()) {
                                        viewModel.addSubTodo(todo.id, newSubTaskTitle)
                                        newSubTaskTitle = ""
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "添加")
                            }
                        }

                        // Subtask list
                        subTodos.forEach { subTodo ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (subTodo.isCompleted) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = if (subTodo.isCompleted) "取消完成" else "完成",
                                    tint = if (subTodo.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable {
                                            viewModel.toggleTodo(subTodo)
                                        }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = subTodo.title,
                                    fontSize = 13.sp,
                                    textDecoration = if (subTodo.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.deleteTodo(subTodo) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(
                            todo.copy(
                                title = title.trim(),
                                description = description.trim(),
                                priority = priority,
                                category = category,
                                dueDate = dueDate,
                                reminderTime = reminderTime,
                                tags = tagsInput.trim(),
                                recurringType = recurringType,
                                progress = progress
                            )
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun TodoItemCard(
    todo: TodoItem,
    textPrimary: Color,
    textSecondary: Color,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    val isCompleted = todo.isCompleted
    val itemAlpha by animateFloatAsState(
        targetValue = if (isCompleted) 0.5f else 1f,
        animationSpec = tween(400),
        label = "item_alpha"
    )
    val checkboxScale by animateFloatAsState(
        targetValue = if (isCompleted) 1.2f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "checkbox_scale"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val hasReminder = todo.reminderTime != null && todo.reminderTime > System.currentTimeMillis()

    // Priority color
    val prioColor = when (todo.priority) {
        1 -> WarningColor
        2 -> ErrorColor
        else -> Color.Transparent
    }
    val catColor = CategoryColors[todo.category] ?: MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = itemAlpha }
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggle,
                onLongClick = onLongPress
            )
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isCompleted) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                contentDescription = if (isCompleted) "取消完成" else "标记完成",
                tint = if (isCompleted) MaterialTheme.colorScheme.primary else textSecondary,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer {
                        scaleX = checkboxScale
                        scaleY = checkboxScale
                    }
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Priority indicator bar
        if (todo.priority > 0) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(prioColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Title
        Text(
            text = todo.title,
            color = if (isCompleted) textSecondary else textPrimary,
            fontSize = 15.sp,
            fontWeight = if (isCompleted) FontWeight.Normal else FontWeight.Medium,
            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Right side indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Recurring indicator
            if (todo.recurringType != TodoItem.RECURRING_NONE) {
                Icon(
                    Icons.Default.Repeat,
                    contentDescription = "重复",
                    tint = textSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }

            // Reminder time
            if (hasReminder) {
                val reminderText = Instant.ofEpochMilli(todo.reminderTime!!)
                    .atZone(ZoneId.systemDefault())
                    .toLocalTime()
                    .format(DateTimeFormatter.ofPattern("HH:mm"))
                Text(
                    text = reminderText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Due date
            todo.dueDate?.let { due ->
                val dueDate = Instant.ofEpochMilli(due).atZone(ZoneId.systemDefault()).toLocalDate()
                val isOverdue = dueDate.isBefore(LocalDate.now()) && !isCompleted
                Text(
                    text = dueDate.format(DateTimeFormatter.ofPattern("M/d")),
                    fontSize = 11.sp,
                    color = if (isOverdue) ErrorColor else textSecondary.copy(alpha = 0.5f)
                )
            }

            // Category dot
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(catColor)
            )
        }
    }
}

@Composable
private fun ClearCompletedButton(
    onClick: () -> Unit,
    textSecondary: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.DeleteSweep,
                contentDescription = "清除已完成",
                tint = textSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.clear_completed),
                color = textSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTodoDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, priority: Int, category: String, dueDate: Long?, reminderTime: Long?, tags: List<String>, recurringType: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(0) }
    var category by remember { mutableStateOf(TodoItem.CATEGORY_TASK) }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var reminderTime by remember { mutableStateOf<Long?>(null) }
    var tagsInput by remember { mutableStateOf("") }
    var recurringType by remember { mutableStateOf(TodoItem.RECURRING_NONE) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    val timePickerState = rememberTimePickerState(
        initialHour = 9,
        initialMinute = 0,
        is24Hour = true
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dateMillis ->
                        val date = Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                        val time = reminderTime?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime()
                        } ?: LocalTime.of(9, 0)
                        dueDate = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        reminderTime = date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("选择提醒时间") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val date = dueDate?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    } ?: LocalDate.now()
                    reminderTime = date.atTime(LocalTime.of(timePickerState.hour, timePickerState.minute))
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    showTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("新建待办") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述（可选）") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Priority
                Text("优先级", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "普通", 1 to "重要", 2 to "紧急").forEach { (level, label) ->
                        val isSelected = priority == level
                        val chipColor = when (level) {
                            1 -> WarningColor
                            2 -> ErrorColor
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) chipColor.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable { priority = level }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                color = if (isSelected) chipColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Category
                Text("分类", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        TodoItem.CATEGORY_TASK to "任务",
                        TodoItem.CATEGORY_REMINDER to "提醒",
                        TodoItem.CATEGORY_GOAL to "目标"
                    ).forEach { (cat, label) ->
                        val isSelected = category == cat
                        val chipColor = CategoryColors[cat] ?: MaterialTheme.colorScheme.primary
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) chipColor.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable { category = cat }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                color = if (isSelected) chipColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Due date & reminder
                Text("截止日期 & 提醒", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (dueDate != null) {
                                Instant.ofEpochMilli(dueDate!!).atZone(ZoneId.systemDefault())
                                    .toLocalDate().format(DateTimeFormatter.ofPattern("M月d日"))
                            } else "选择日期",
                            fontSize = 12.sp,
                            color = if (dueDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { showTimePicker = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (reminderTime != null) {
                                Instant.ofEpochMilli(reminderTime!!).atZone(ZoneId.systemDefault())
                                    .toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                            } else "选择时间",
                            fontSize = 12.sp,
                            color = if (reminderTime != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (dueDate != null || reminderTime != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ErrorColor.copy(alpha = 0.1f))
                                .clickable {
                                    dueDate = null
                                    reminderTime = null
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("清除", fontSize = 12.sp, color = ErrorColor)
                        }
                    }
                }

                // Recurring
                Text("重复", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        TodoItem.RECURRING_NONE to "不重复",
                        TodoItem.RECURRING_DAILY to "每天",
                        TodoItem.RECURRING_WEEKLY to "每周",
                        TodoItem.RECURRING_MONTHLY to "每月",
                        TodoItem.RECURRING_YEARLY to "每年"
                    ).forEach { (type, label) ->
                        val isSelected = recurringType == type
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable { recurringType = type }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Tags
                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    label = { Text("标签（用逗号分隔）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        val tags = tagsInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        onConfirm(title.trim(), description.trim(), priority, category, dueDate, reminderTime, tags, recurringType)
                    }
                },
                enabled = title.isNotBlank()
            ) { Text("创建") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
