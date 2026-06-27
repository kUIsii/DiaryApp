package com.diary.app.ui.todo

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.diary.app.data.HabitRecord
import com.diary.app.data.Tag
import com.diary.app.data.TodoItem
import com.diary.app.ui.components.GlassCard
import com.diary.app.ui.theme.ErrorColor
import com.diary.app.ui.theme.SuccessColor
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoDialogs(
    allTags: List<Tag>,
    currentTabLabel: String,
    textColor: Color,
    textSecondary: Color,
    deletingTodo: TodoItem?,
    editingTodo: TodoItem?,
    editingHabit: TodoItem?,
    showAddDialog: Boolean,
    isMultiSelectMode: Boolean,
    selectedIds: Set<Long>,
    selectedHabit: TodoItem?,
    selectedHabitRecords: List<HabitRecord>,
    selectedHabitMonth: YearMonth,
    selectedHabitDate: LocalDate,
    showHabitDetail: Boolean,
    showHabitRecordDialog: Boolean,
    onDismissDelete: () -> Unit,
    onConfirmDelete: (TodoItem) -> Unit,
    onDismissMultiDelete: () -> Unit,
    onConfirmMultiDelete: () -> Unit,
    onDismissEditTodo: () -> Unit,
    onSaveEditTodo: (TodoItem) -> Unit,
    onDismissEditHabit: () -> Unit,
    onSaveEditHabit: (TodoItem, List<Long>) -> Unit,
    onDismissAddDialog: () -> Unit,
    onAddHabit: (String, List<Long>) -> Unit,
    onAddMemo: (String) -> Unit,
    onAddDeadline: (String, Long) -> Unit,
    onDismissHabitDetail: () -> Unit,
    onEditSelectedHabit: () -> Unit,
    onChangeHabitMonth: (Long) -> Unit,
    onSelectHabitDate: (LocalDate) -> Unit,
    onSaveHabitQuickRecord: (String) -> Unit,
    onOpenHabitRecordDialog: () -> Unit,
    onClearHabitRecord: () -> Unit,
    onDismissHabitRecordDialog: () -> Unit,
    onSaveHabitDetailRecord: (String) -> Unit
) {
    deletingTodo?.let { target ->
        AlertDialog(
            onDismissRequest = onDismissDelete,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("删除确认") },
            text = { Text("确定要删除「${target.title}」吗？") },
            confirmButton = {
                TextButton(onClick = { onConfirmDelete(target) }) {
                    Text("删除", color = ErrorColor)
                }
            },
            dismissButton = { TextButton(onClick = onDismissDelete) { Text("取消") } }
        )
    }

    if (isMultiSelectMode && selectedIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = onDismissMultiDelete,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("批量删除") },
            text = { Text("确定要删除选中的 ${selectedIds.size} 项吗？") },
            confirmButton = {
                TextButton(onClick = onConfirmMultiDelete) {
                    Text("删除", color = ErrorColor)
                }
            },
            dismissButton = { TextButton(onClick = onDismissMultiDelete) { Text("取消") } }
        )
    }

    editingTodo?.let { todo ->
        var editTitle by remember(todo) { mutableStateOf(todo.title) }
        AlertDialog(
            onDismissRequest = onDismissEditTodo,
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
                        onSaveEditTodo(todo.copy(title = editTitle.trim()))
                    }
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = onDismissEditTodo) { Text("取消") } }
        )
    }

    editingHabit?.let { habit ->
        var editName by remember(habit) { mutableStateOf(habit.title) }
        var selectedLinkedTagIds by remember(habit) {
            mutableStateOf(TodoItem.getLinkedTagIds(habit.linkedTagIds).toSet())
        }
        AlertDialog(
            onDismissRequest = onDismissEditHabit,
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
                        onSaveEditHabit(
                            habit.copy(title = editName.trim()),
                            selectedLinkedTagIds.toList()
                        )
                    }
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = onDismissEditHabit) { Text("取消") } }
        )
    }

    if (showAddDialog) {
        when (currentTabLabel) {
            "打卡" -> AddHabitDialog(
                allTags = allTags,
                textSecondary = textSecondary,
                onDismiss = onDismissAddDialog,
                onConfirm = onAddHabit
            )
            "备忘" -> AddMemoDialog(
                onDismiss = onDismissAddDialog,
                onConfirm = onAddMemo
            )
            "待办" -> AddDeadlineDialog(
                textColor = textColor,
                textSecondary = textSecondary,
                onDismiss = onDismissAddDialog,
                onConfirm = onAddDeadline
            )
        }
    }

    if (showHabitDetail && selectedHabit != null) {
        HabitDetailDialog(
            habit = selectedHabit,
            records = selectedHabitRecords,
            selectedMonth = selectedHabitMonth,
            selectedDate = selectedHabitDate,
            onDismiss = onDismissHabitDetail,
            onEdit = onEditSelectedHabit,
            onMonthChange = onChangeHabitMonth,
            onDateSelect = onSelectHabitDate,
            onQuickRecord = onSaveHabitQuickRecord,
            onOpenMore = onOpenHabitRecordDialog,
            onClear = onClearHabitRecord
        )
    }

    if (showHabitRecordDialog && selectedHabit != null) {
        HabitRecordDialog(
            habit = selectedHabit,
            selectedDate = selectedHabitDate,
            existingRecord = selectedHabitRecords.firstOrNull { it.recordDate == selectedHabitDate.toEpochDay() },
            onDismiss = onDismissHabitRecordDialog,
            onSave = onSaveHabitDetailRecord
        )
    }
}

internal fun habitQuickRecordPlaceholder(existingRecord: HabitRecord?): String {
    return if (existingRecord == null) "写一句今天的打卡记录" else "补充今天的记录"
}

internal fun habitQuickRecordConfirmLabel(existingRecord: HabitRecord?): String {
    return if (existingRecord == null) "保存记录" else "更新记录"
}

@Composable
private fun AddHabitDialog(
    allTags: List<Tag>,
    textSecondary: Color,
    onDismiss: () -> Unit,
    onConfirm: (String, List<Long>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedLinkedTagIds by remember { mutableStateOf(setOf<Long>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
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
                    onConfirm(name.trim(), selectedLinkedTagIds.toList())
                }
            }) { Text("创建") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun AddMemoDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var content by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
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
                    onConfirm(content.trim())
                }
            }) { Text("创建") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDeadlineDialog(
    textColor: Color,
    textSecondary: Color,
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Today, null, tint = textSecondary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
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
                        onConfirm(content.trim(), currentSelectedDate)
                    }
                },
                enabled = content.isNotBlank() && selectedDate != null
            ) { Text("创建") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HabitTagSelector(
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
                            Icon(Icons.Default.Close, contentDescription = "关闭")
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
                    placeholder = {
                        Text(
                            habitQuickRecordPlaceholder(selectedRecord),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = {
                        if (quickText.isNotBlank()) onQuickRecord(quickText.trim())
                    }) { Text(habitQuickRecordConfirmLabel(selectedRecord), fontSize = 13.sp) }
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
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "上一月")
                    }
                    Text(
                        text = selectedMonth.format(DateTimeFormatter.ofPattern("yyyy年M月")),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(onClick = { onMonthChange(1) }) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "下一月")
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
                                                .background(tint, RoundedCornerShape(999.dp))
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
                placeholder = {
                    Text(
                        if (existingRecord == null) "可以只写一句，也可以多写一点" else "继续补充今天这条记录",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (content.isNotBlank()) onSave(content.trim()) },
                enabled = content.isNotBlank()
            ) { Text(habitQuickRecordConfirmLabel(existingRecord), fontSize = 13.sp) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", fontSize = 13.sp) } }
    )
}
