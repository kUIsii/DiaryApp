package com.diary.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 封面主题配置
 */
@Entity(
    tableName = "cover_themes",
    indices = [
        Index(value = ["isActive"], unique = false)
    ]
)
data class CoverTheme(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val texturePath: String? = null,
    val fontFamily: String? = null,
    val accentColor: Long? = null,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
