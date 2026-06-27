package com.diary.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 写作实验 - 定期推出的写作挑战
 */
@Entity(
    tableName = "writing_experiments",
    indices = [
        Index(value = ["startDate"]),
        Index(value = ["status"])
    ]
)
data class WritingExperiment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val rules: String,           // 实验规则说明
    val badgeName: String = "",  // 完成后解锁的徽章
    val startDate: Long,
    val endDate: Long,
    val status: String = "upcoming",  // upcoming/active/completed/expired
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 写作实验参与记录
 */
@Entity(
    tableName = "experiment_participations",
    indices = [
        Index(value = ["experimentId"]),
        Index(value = ["diaryId"])
    ]
)
data class ExperimentParticipation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val experimentId: Long,
    val diaryId: Long?,          // 关联的日记(可选)
    val dayNumber: Int,          // 第几天
    val note: String = "",       // 当天笔记
    val completedAt: Long = System.currentTimeMillis()
)
