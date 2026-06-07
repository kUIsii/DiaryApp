package com.diary.app.ui.todo

import androidx.compose.ui.graphics.Color
import com.diary.app.ui.theme.PrimaryBlue
import com.diary.app.ui.theme.SuccessColor
import com.diary.app.ui.theme.WarningColor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diary.app.data.TodoItem
import com.diary.app.ui.theme.ErrorColor
import com.diary.app.ui.theme.WarningColor
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal val CategoryColors = mapOf(
    TodoItem.CATEGORY_TASK to PrimaryBlue,
    TodoItem.CATEGORY_REMINDER to WarningColor,
    TodoItem.CATEGORY_GOAL to SuccessColor
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoFormContent(
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    priority: Int,
    onPriorityChange: (Int) -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit,
    dueDate: Long?,
    onDueDateChange: (Long?) -> Unit,
    reminderTime: Long?,
    onReminderTimeChange: (Long?) -> Unit,
    recurringType: String,
    onRecurringTypeChange: (String) -> Unit,
    tags: String,
    onTagsChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dueDate ?: reminderTime ?: System.currentTimeMillis()
    )
    val timePickerState = rememberTimePickerState(
        initialHour = if (reminderTime != null) {
            Instant.ofEpochMilli(reminderTime).atZone(ZoneId.systemDefault()).toLocalTime().hour
        } else 9,
        initialMinute = if (reminderTime != null) {
            Instant.ofEpochMilli(reminderTime).atZone(ZoneId.systemDefault()).toLocalTime().minute
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
                            Instant.ofEpochMilli(reminderTime).atZone(ZoneId.systemDefault()).toLocalTime()
                        } else LocalTime.of(9, 0)
                        onDueDateChange(date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
                        onReminderTimeChange(date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
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
                        Instant.ofEpochMilli(reminderTime).atZone(ZoneId.systemDefault()).toLocalDate()
                    } else if (dueDate != null) {
                        Instant.ofEpochMilli(dueDate).atZone(ZoneId.systemDefault()).toLocalDate()
                    } else LocalDate.now()
                    onReminderTimeChange(
                        date.atTime(LocalTime.of(timePickerState.hour, timePickerState.minute))
                            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    )
                    showTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            }
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("标题") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
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
                        .clickable { onPriorityChange(level) }
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
                        .clickable { onCategoryChange(cat) }
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
                        Instant.ofEpochMilli(dueDate).atZone(ZoneId.systemDefault())
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
                        Instant.ofEpochMilli(reminderTime).atZone(ZoneId.systemDefault())
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
                            onDueDateChange(null)
                            onReminderTimeChange(null)
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
                        .clickable { onRecurringTypeChange(type) }
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
            value = tags,
            onValueChange = onTagsChange,
            label = { Text("标签（用逗号分隔）") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
