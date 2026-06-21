package com.diary.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "achievements",
    indices = [Index(value = ["key"], unique = true)]
)
data class Achievement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val name: String,
    val description: String,
    val iconName: String,
    val unlockedAt: Long? = null,
    val progress: Int = 0,
    val target: Int = 1
)
