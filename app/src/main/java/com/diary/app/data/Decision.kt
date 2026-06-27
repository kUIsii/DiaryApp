package com.diary.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 决策记录 - 追踪重大决定及后续
 */
@Entity(
    tableName = "decisions",
    indices = [
        Index(value = ["diaryId"]),
        Index(value = ["madeAt"])
    ]
)
data class Decision(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val diaryId: Long,
    val title: String,
    val context: String = "",          // 决策背景
    val options: String = "",          // 考虑的选项(JSON)
    val chosenOption: String = "",     // 最终选择
    val concerns: String = "",         // 当时的顾虑
    val madeAt: Long = System.currentTimeMillis(),
    val followUpAt: Long? = null,      // 计划回顾时间
    val outcome: String? = null,       // 后续结果
    val createdAt: Long = System.currentTimeMillis()
)
