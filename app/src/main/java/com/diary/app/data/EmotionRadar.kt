package com.diary.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 情绪雷达图数据 - 五维度情绪评估
 */
@Entity(
    tableName = "emotion_radar",
    indices = [
        Index(value = ["diaryId"], unique = true),
        Index(value = ["createdAt"])
    ]
)
data class EmotionRadar(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val diaryId: Long,
    val vitality: Float,      // 活力 0-1
    val calmness: Float,      // 平静 0-1
    val happiness: Float,     // 快乐 0-1
    val gratitude: Float,     // 感恩 0-1
    val socialConnection: Float, // 社交感 0-1
    val createdAt: Long = System.currentTimeMillis()
)
