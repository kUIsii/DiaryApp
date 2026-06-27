package com.diary.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 写作连续保护罩
 */
@Entity(
    tableName = "streak_shields",
    indices = [
        Index(value = ["month"]),
        Index(value = ["usedAt"])
    ]
)
data class StreakShield(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val month: String,           // 格式: 2026-06
    val usedAt: Long? = null,    // 使用时间
    val savedDate: Long? = null, // 保护的日期
    val isUsed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
