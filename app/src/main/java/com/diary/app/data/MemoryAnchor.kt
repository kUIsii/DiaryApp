package com.diary.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 记忆锚点 - 标记重要日记并追踪关联
 */
@Entity(
    tableName = "memory_anchors",
    indices = [
        Index(value = ["diaryId"], unique = true),
        Index(value = ["topic"])
    ]
)
data class MemoryAnchor(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val diaryId: Long,
    val topic: String,           // 锚点主题
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 锚点关联 - 记录与锚点相关的日记
 */
@Entity(
    tableName = "anchor_relations",
    indices = [
        Index(value = ["anchorId"]),
        Index(value = ["diaryId"])
    ]
)
data class AnchorRelation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val anchorId: Long,
    val diaryId: Long,
    val relevanceScore: Float = 0f,
    val createdAt: Long = System.currentTimeMillis()
)
