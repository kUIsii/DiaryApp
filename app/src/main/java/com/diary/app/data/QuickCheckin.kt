package com.diary.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 快速签到 - 轻量级日记入口
 */
@Entity(
    tableName = "quick_checkins",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["moodLevel"])
    ]
)
data class QuickCheckin(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val moodLevel: Int?,
    val photoUri: String? = null,
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
