package com.diary.app.data

data class DiaryTemplate(
    val id: String,
    val name: String,
    val icon: String,
    val content: String,
    val category: TemplateCategory
)

enum class TemplateCategory {
    DAILY,      // 日常
    EMOTIONAL,  // 情感
    CREATIVE,   // 创意
    TRAVEL,     // 旅行
    WORK        // 工作
}
