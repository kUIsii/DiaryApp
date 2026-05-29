package com.diary.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todo_items")
data class TodoItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val isCompleted: Boolean = false,
    val priority: Int = 0,  // 0=normal, 1=important, 2=urgent
    val dueDate: Long? = null,  // null = no due date
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val sortOrder: Int = 0
)
