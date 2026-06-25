package com.diary.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 写作目标
 */
@Entity(tableName = "writing_goals")
data class WritingGoal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** weekly_entries | monthly_entries | monthly_words */
    val type: String = "weekly_entries",

    @ColumnInfo(name = "target_value")
    val targetValue: Int = 5,

    @ColumnInfo(name = "current_value")
    val currentValue: Int = 0,

    @ColumnInfo(name = "period_start")
    val periodStart: Long = 0,

    val enabled: Boolean = true
)
