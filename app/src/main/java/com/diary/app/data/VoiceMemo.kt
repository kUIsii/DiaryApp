package com.diary.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 语音备忘录
 */
@Entity(
    tableName = "voice_memos",
    indices = [
        Index(value = ["diaryId"]),
        Index(value = ["createdAt"])
    ]
)
data class VoiceMemo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val diaryId: Long? = null,
    val audioPath: String,
    val durationSeconds: Int,
    val transcript: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
