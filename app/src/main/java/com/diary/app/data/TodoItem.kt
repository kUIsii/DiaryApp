package com.diary.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "todo_items",
    indices = [
        Index(value = ["dueDate"]),
        Index(value = ["isCompleted"]),
        Index(value = ["category"]),
        Index(value = ["reminderTime"])
    ]
)
data class TodoItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val isCompleted: Boolean = false,
    val priority: Int = 0,  // 0=normal, 1=important, 2=urgent
    val dueDate: Long? = null,  // null = no due date
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val sortOrder: Int = 0,
    val category: String = "task",  // task, reminder, goal
    val reminderTime: Long? = null  // epoch millis for notification reminder, null = no reminder
) {
    companion object {
        const val CATEGORY_TASK = "task"
        const val CATEGORY_REMINDER = "reminder"
        const val CATEGORY_GOAL = "goal"

        fun categoryLabel(category: String): String = when (category) {
            CATEGORY_TASK -> "任务"
            CATEGORY_REMINDER -> "提醒"
            CATEGORY_GOAL -> "目标"
            else -> "任务"
        }

        fun categoryIcon(category: String): String = when (category) {
            CATEGORY_TASK -> "任务"
            CATEGORY_REMINDER -> "提醒"
            CATEGORY_GOAL -> "目标"
            else -> "任务"
        }
    }
}
