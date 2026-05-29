package com.diary.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val content: String = "",        // Quill.js delta JSON
    val plainText: String = "",      // Plain text for preview/search
    val moodLevel: Int? = null,      // 1-10, 1=沮丧 10=兴奋
    val weather: String? = null,     // 晴/多云/阴/雨/雪/风
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
