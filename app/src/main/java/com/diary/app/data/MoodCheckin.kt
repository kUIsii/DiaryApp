package com.diary.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 心情签到 — 轻量级心情记录，不需要写完整日记
 */
@Entity(tableName = "mood_checkins")
data class MoodCheckin(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 1-6 心情等级 */
    @ColumnInfo(name = "mood_level")
    val moodLevel: Int,

    val note: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
