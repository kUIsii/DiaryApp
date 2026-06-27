package com.diary.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 写作习惯指纹 - 记录写作特征
 */
@Entity(
    tableName = "writing_fingerprints",
    indices = [
        Index(value = ["periodStart"]),
        Index(value = ["diaryId"])
    ]
)
data class WritingFingerprint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val diaryId: Long,
    val periodStart: Long,       // 时间段起始
    val avgSentenceLength: Float,
    val vocabularyRichness: Float,  // 用词丰富度
    val avgWordLength: Float,
    val punctuationRatio: Float,  // 标点使用频率
    val paragraphCount: Int,
    val avgParagraphLength: Float,
    val createdAt: Long = System.currentTimeMillis()
)
