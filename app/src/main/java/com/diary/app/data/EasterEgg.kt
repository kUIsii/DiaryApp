package com.diary.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 隐藏彩蛋 - 记录已触发的彩蛋
 */
@Entity(
    tableName = "easter_eggs",
    indices = [
        Index(value = ["eggId"], unique = true),
        Index(value = ["triggeredAt"])
    ]
)
data class EasterEgg(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eggId: String,           // 彩蛋唯一标识
    val title: String,
    val description: String,
    val triggeredAt: Long = System.currentTimeMillis()
)
