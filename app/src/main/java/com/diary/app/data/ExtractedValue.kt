package com.diary.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 价值观提取 - AI从日记中提取的核心价值观
 */
@Entity(
    tableName = "extracted_values",
    indices = [
        Index(value = ["category"]),
        Index(value = ["updatedAt"])
    ]
)
data class ExtractedValue(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String,        // 价值观类别
    val value: String,           // 具体价值观
    val evidence: String = "",   // 支撑证据(JSON数组)
    val confidence: Float = 0f,  // 置信度 0-1
    val updatedAt: Long = System.currentTimeMillis()
)
