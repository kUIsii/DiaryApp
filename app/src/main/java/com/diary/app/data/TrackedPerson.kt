package com.diary.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 人物关系追踪 - 追踪日记中提到的人物及情感变化
 */
@Entity(
    tableName = "tracked_persons",
    indices = [
        Index(value = ["name"]),
        Index(value = ["createdAt"])
    ]
)
data class TrackedPerson(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val mentionCount: Int = 0,
    val lastMentionedAt: Long? = null,
    val avgSentiment: Float = 0f,  // 平均情感倾向 -1 到 1
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 人物提及记录 - 每次提到某人的日记
 */
@Entity(
    tableName = "person_mentions",
    indices = [
        Index(value = ["personId"]),
        Index(value = ["diaryId"])
    ]
)
data class PersonMention(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val personId: Long,
    val diaryId: Long,
    val context: String = "",       // 提及的上下文
    val sentiment: Float = 0f,      // 当次情感倾向
    val createdAt: Long = System.currentTimeMillis()
)
