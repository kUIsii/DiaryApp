package com.diary.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trash_entries",
    indices = [
        Index(value = ["deletedAt"])
    ]
)
data class TrashEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalId: Long,
    val title: String = "",
    val content: String = "",
    val plainText: String = "",
    val moodLevel: Int? = null,
    val weather: String? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isFavorite: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long = System.currentTimeMillis()
)
