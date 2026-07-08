package com.diary.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,  // 格式: "type_uniqueId" 如 "monthly_2026_06", "capsule_123"
    val type: String,  // "monthly_report", "annual_report", "capsule", "milestone", "streak", "on_this_day"
    val title: String,
    val subtitle: String,
    val iconType: String,  // 用于在 UI 中选择图标
    val colorHex: Long,  // 图标颜色
    val relatedId: Long?,  // 关联的实体 ID（如 capsule.id, diary.id）
    val isRead: Boolean = false,
    val isTrashed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val trashedAt: Long? = null,
    // 天气预警详情字段
    val alertProvince: String = "",
    val alertPublishTime: String = "",
    val alertSource: String = ""
)
