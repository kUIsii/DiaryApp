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
    val mood: String? = null,        // 开心/平静/低落/焦虑/兴奋/疲惫/感恩
    val weather: String? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val tags: String? = null,        // JSON array
    val images: String? = null,      // JSON array of paths
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
