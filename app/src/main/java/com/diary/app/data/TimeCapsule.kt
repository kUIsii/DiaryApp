package com.diary.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_capsules")
data class TimeCapsule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: Long,
    val unlockDate: Long,
    val isRead: Boolean = false
)
