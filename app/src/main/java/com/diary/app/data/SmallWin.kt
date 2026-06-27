package com.diary.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 小确幸记录 - 记录每天的小胜利和美好瞬间
 */
@Entity(
    tableName = "small_wins",
    indices = [
        Index(value = ["recordDate"]),
        Index(value = ["createdAt"])
    ]
)
data class SmallWin(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val recordDate: Long, // 记录的日期（当天0点的时间戳）
    val createdAt: Long = System.currentTimeMillis()
)
