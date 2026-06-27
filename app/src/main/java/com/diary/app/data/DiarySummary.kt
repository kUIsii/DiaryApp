package com.diary.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 日记摘要 - AI生成
 */
@Entity(
    tableName = "diary_summaries",
    indices = [
        Index(value = ["diaryId"], unique = true)
    ]
)
data class DiarySummary(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val diaryId: Long,
    val summary: String,
    val createdAt: Long = System.currentTimeMillis()
)
