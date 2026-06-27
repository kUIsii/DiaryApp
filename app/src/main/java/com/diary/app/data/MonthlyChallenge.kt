package com.diary.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 月度主题挑战
 */
@Entity(
    tableName = "monthly_challenges",
    indices = [
        Index(value = ["year", "month"]),
        Index(value = ["status"])
    ]
)
data class MonthlyChallenge(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val year: Int,
    val month: Int,
    val targetDays: Int = 20,      // 目标完成天数
    val completedDays: Int = 0,    // 已完成天数
    val status: String = "upcoming",  // upcoming/active/completed
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 挑战每日记录
 */
@Entity(
    tableName = "challenge_daily_logs",
    indices = [
        Index(value = ["challengeId", "date"], unique = true)
    ]
)
data class ChallengeDailyLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val challengeId: Long,
    val date: Long,                // 日期时间戳
    val completed: Boolean = false,
    val note: String = "",
    val diaryId: Long? = null,     // 关联日记
    val createdAt: Long = System.currentTimeMillis()
)
