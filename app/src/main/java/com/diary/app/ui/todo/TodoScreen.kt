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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDismissState
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
import com.diary.app.ui.theme.DarkAccentEnd
import com.diary.app.ui.theme.DarkAccentStart
import com.diary.app.ui.theme.ErrorColor
import com.diary.app.ui.theme.WarningColor
import androidx.compose.ui.res.stringResource
import com.diary.app.R
import com.diary.app.ui.theme.PrimaryBlue
import com.diary.app.ui.theme.SuccessColor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val CategoryColors = mapOf(
    TodoItem.CATEGORY_TASK to PrimaryBlue,
    TodoItem.CATEGORY_REMINDER to WarningColor,
    TodoItem.CATEGORY_GOAL to SuccessColor
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(viewModel: TodoViewModel = viewModel()) {
    val haptic = rememberHapticFeedback()
    val todos by viewModel.allTodos.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var editingTodo by remember { mutableStateOf<TodoItem?>(null) }

    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    val pendingCount = todos.count { !it.isCompleted }
    val today = remember { LocalDate.now() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("M月d日 EEEE") }

    val filteredTodos = if (selectedCategoryFilter != null) {
        todos.filter { it.category == selectedCategoryFilter }
    } else {
        todos
    }

    // Edit dialog
    editingTodo?.let { todo ->
        EditTodoDialog(
            todo = todo,
            onDismiss = { editingTodo = null },
            onConfirm = { updatedTodo ->
                viewModel.updateTodo(updatedTodo)
                editingTodo = null
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
                    Text(
                        text = stringResource(R.string.todo_title),
                        color = textPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${today.format(dateFormatter)} · ${stringResource(R.string.todo_remaining, pendingCount)}",
                        color = textSecondary,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
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
                        isSelected = selectedCategoryFilter == null,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { selectedCategoryFilter = null }
                    )
                    CategoryFilterChip(
                        label = "任务",
                        isSelected = selectedCategoryFilter == TodoItem.CATEGORY_TASK,
                        color = CategoryColors[TodoItem.CATEGORY_TASK]!!,
                        onClick = { selectedCategoryFilter = if (selectedCategoryFilter == TodoItem.CATEGORY_TASK) null else TodoItem.CATEGORY_TASK }
                    )
                    CategoryFilterChip(
                        label = "提醒",
                        isSelected = selectedCategoryFilter == TodoItem.CATEGORY_REMINDER,
                        color = CategoryColors[TodoItem.CATEGORY_REMINDER]!!,
                        onClick = { selectedCategoryFilter = if (selectedCategoryFilter == TodoItem.CATEGORY_REMINDER) null else TodoItem.CATEGORY_REMINDER }
                    )
                    CategoryFilterChip(
                        label = "目标",
                        isSelected = selectedCategoryFilter == TodoItem.CATEGORY_GOAL,
                        color = CategoryColors[TodoItem.CATEGORY_GOAL]!!,
                        onClick = { selectedCategoryFilter = if (selectedCategoryFilter == TodoItem.CATEGORY_GOAL) null else TodoItem.CATEGORY_GOAL }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Input field
            item {
                InputRow(
                    value = inputText,
                    onValueChange = { inputText = it },
                    onAdd = {
                        if (inputText.isNotBlank()) {
                            viewModel.addTodo(inputText, category = selectedCategoryFilter ?: TodoItem.CATEGORY_TASK)
                            inputText = ""
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Todo items
            if (filteredTodos.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.EventNote,
                        title = stringResource(R.string.todo_empty_title),
                        subtitle = stringResource(R.string.todo_empty_subtitle)
                    )
                }
            } else {
                itemsIndexed(
                    items = filteredTodos,
                    key = { _, todo -> todo.id }
                ) { index, todo ->
                    val enterDelay = (index * 40).coerceAtMost(400)
                    AnimatedVisibility(
                        visible = true,
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
                                        .background(
                                            ErrorColor.copy(alpha = 0.15f * progress)
                                        )
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
private fun InputRow(
    value: String,
    onValueChange: (String) -> Unit,
    onAdd: () -> Unit
) {
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
                            colors = if (value.isNotBlank()) listOf(DarkAccentStart, DarkAccentEnd)
                            else listOf(
                                DarkAccentStart.copy(alpha = 0.3f),
                                DarkAccentEnd.copy(alpha = 0.3f)
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
                    modifier = Modifier.size(22.dp)
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
    onConfirm: (TodoItem) -> Unit
) {
    var title by remember { mutableStateOf(todo.title) }
    var priority by remember { mutableStateOf(todo.priority) }
    var category by remember { mutableStateOf(todo.category) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑待办") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("优先级", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
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

                Spacer(modifier = Modifier.height(12.dp))

                Text("分类", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (title.isNotBlank()) onConfirm(todo.copy(title = title.trim(), priority = priority, category = category)) },
                enabled = title.isNotBlank()
            ) { Text("确定") }
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
        animationSpec = tween(300),
        label = "item_alpha"
    )
    val checkboxScale by animateFloatAsState(
        targetValue = if (isCompleted) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 400f
        ),
        label = "checkbox_scale"
    )

    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = itemAlpha },
        cornerRadius = 16.dp,
        innerPadding = 14.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onToggle,
                    onLongClick = onLongPress
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Custom checkbox with bounce animation
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
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
                        .size(22.dp)
                        .graphicsLayer {
                            scaleX = checkboxScale
                            scaleY = checkboxScale
                        }
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Content column
            Column(modifier = Modifier.weight(1f)) {
                // Title
                Text(
                    text = todo.title,
                    color = textPrimary,
                    fontSize = 16.sp,
                    fontWeight = if (isCompleted) FontWeight.Normal else FontWeight.Medium,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Category tag + priority + due date row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // Category tag
                    val catColor = CategoryColors[todo.category] ?: MaterialTheme.colorScheme.primary
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(catColor.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = TodoItem.categoryLabel(todo.category),
                            fontSize = 10.sp,
                            color = catColor
                        )
                    }

                    // Priority indicator
                    if (todo.priority > 0) {
                        val prioColor = when (todo.priority) {
                            1 -> WarningColor
                            2 -> ErrorColor
                            else -> Color.Transparent
                        }
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(prioColor)
                        )
                    }

                    // Due date
                    todo.dueDate?.let { due ->
                        val dueDate = Instant.ofEpochMilli(due).atZone(ZoneId.systemDefault()).toLocalDate()
                        val isOverdue = dueDate.isBefore(LocalDate.now()) && !isCompleted
                        Text(
                            text = dueDate.format(DateTimeFormatter.ofPattern("M月d日")),
                            fontSize = 10.sp,
                            color = if (isOverdue) ErrorColor else textSecondary.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = textSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
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
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
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
