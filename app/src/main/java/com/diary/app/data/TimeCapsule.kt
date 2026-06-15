package com.diary.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CapsuleTheme {
    NORMAL,      // 普通
    BIRTHDAY,    // 生日
    NEW_YEAR,    // 新年
    GRADUATION,  // 毕业
    TRAVEL,      // 旅行
    LOVE,        // 爱情
    DREAM        // 梦想
}

@Entity(tableName = "time_capsules")
data class TimeCapsule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: Long,
    val unlockDate: Long,
    val isRead: Boolean = false,
    val isOpened: Boolean = false,  // 是否已打开过（看过内容）
    val theme: CapsuleTheme = CapsuleTheme.NORMAL,
    val imageUri: String? = null,   // 附件图片路径
    val unlockHour: Int = 0,        // 解锁小时 (0-23)
    val unlockMinute: Int = 0       // 解锁分钟 (0-59)
)
