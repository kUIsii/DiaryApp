package com.diary.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 语义搜索向量
 */
@Entity(
    tableName = "diary_embeddings",
    indices = [
        Index(value = ["diaryId"], unique = true)
    ]
)
data class DiaryEmbedding(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val diaryId: Long,
    val embeddingJson: String,
    val createdAt: Long = System.currentTimeMillis()
)
