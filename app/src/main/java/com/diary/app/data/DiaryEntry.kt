package com.diary.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "diary_entries",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["isFavorite"]),
        Index(value = ["moodLevel"])
    ]
)
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val content: String = "",        // Quill.js delta JSON
    val plainText: String = "",      // Plain text for preview/search
    val moodLevel: Int? = null,      // 1-6, 1=沮丧 6=兴奋
    val weather: String? = null,     // 晴天/多云/阴天/雨天/大风
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "writing_duration_seconds")
    val writingDurationSeconds: Int? = null
)
