package com.diary.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 专注模式配置
 */
@Entity(
    tableName = "focus_sessions",
    indices = [
        Index(value = ["startTime"]),
        Index(value = ["endTime"])
    ]
)
data class FocusSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val durationMinutes: Int = 25,
    val wordCountGoal: Int? = null,
    val ambientSound: String? = null,
    val completedAt: Long? = null
)
