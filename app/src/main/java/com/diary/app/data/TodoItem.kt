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
        Index(value = ["reminderTime"]),
        Index(value = ["parentId"]),
        Index(value = ["tags"])
    ]
)
data class TodoItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val description: String = "",  // 详细描述/备注
    val isCompleted: Boolean = false,
    val priority: Int = 0,  // 0=normal, 1=important, 2=urgent
    val dueDate: Long? = null,  // null = no due date
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val sortOrder: Int = 0,
    val category: String = "task",  // task, reminder, goal
    val reminderTime: Long? = null,  // epoch millis for notification reminder, null = no reminder
    val tags: String = "",  // 逗号分隔的标签
    val parentId: Long? = null,  // 父任务ID，用于子任务
    val recurringType: String = "none",  // none, daily, weekly, monthly, yearly
    val progress: Int = 0,  // 0-100 进度百分比，用于目标追踪
    val isPinned: Boolean = false,  // 是否置顶
    val linkedTagIds: String = ""  // 关联的日记标签ID，逗号分隔
) {
    companion object {
        const val CATEGORY_TASK = "task"
        const val CATEGORY_REMINDER = "reminder"
        const val CATEGORY_GOAL = "goal"

        const val RECURRING_NONE = "none"
        const val RECURRING_DAILY = "daily"
        const val RECURRING_WEEKLY = "weekly"
        const val RECURRING_MONTHLY = "monthly"
        const val RECURRING_YEARLY = "yearly"

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

        fun recurringLabel(recurringType: String): String = when (recurringType) {
            RECURRING_DAILY -> "每天"
            RECURRING_WEEKLY -> "每周"
            RECURRING_MONTHLY -> "每月"
            RECURRING_YEARLY -> "每年"
            else -> "不重复"
        }

        fun getTagList(tags: String): List<String> {
            return if (tags.isBlank()) emptyList()
            else tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
        }

        fun setTagList(tags: List<String>): String {
            return tags.joinToString(", ")
        }

        fun getLinkedTagIds(linkedTagIds: String): List<Long> {
            if (linkedTagIds.isBlank()) return emptyList()
            return linkedTagIds.split(",").mapNotNull { it.trim().toLongOrNull() }
        }

        fun setLinkedTagIds(ids: List<Long>): String {
            return ids.joinToString(", ")
        }
    }
}
