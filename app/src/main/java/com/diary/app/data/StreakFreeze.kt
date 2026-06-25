package com.diary.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 连续记录冻结 — 保护连续天数不因一天未写而中断
 */
@Entity(tableName = "streak_freezes")
data class StreakFreeze(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "used_at")
    val usedAt: Long = System.currentTimeMillis(),

    /** 使用冻结时的连续天数 */
    @ColumnInfo(name = "streak_at_use")
    val streakAtUse: Int = 0
)
