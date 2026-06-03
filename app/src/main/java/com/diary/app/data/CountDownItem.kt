package com.diary.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "countdown_items")
data class CountDownItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetDate: Long,
    val isCountUp: Boolean = false,
    val color: Long = 0xFF4A90D9,
    val isRepeatYearly: Boolean = false,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
