package com.diary.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 目标 - 支持层级分解
 */
@Entity(
    tableName = "goals",
    indices = [
        Index(value = ["parentId"]),
        Index(value = ["createdAt"])
    ]
)
data class Goal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val parentId: Long? = null,
    val progress: Int = 0,
    val targetValue: Int = 100,
    val unit: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
