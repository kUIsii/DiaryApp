package com.diary.app.ui.quickcheckin

import com.diary.app.data.QuickCheckin

private val quickCheckinMoodLabels = mapOf(
    1 to "沮丧",
    2 to "低落",
    3 to "平静",
    4 to "开心",
    5 to "愉快",
    6 to "兴奋"
)

fun shouldEnableQuickCheckinSubmit(selectedMood: Int?, text: String, photoUri: String?): Boolean {
    return selectedMood != null || text.isNotBlank() || photoUri != null
}

fun buildQuickCheckinHistorySummary(checkins: List<QuickCheckin>): String {
    if (checkins.isEmpty()) {
        return "暂无签到记录"
    }
    val mostCommonMood = checkins
        .mapNotNull { it.moodLevel }
        .groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
    val moodLabel = quickCheckinMoodLabels[mostCommonMood] ?: "未记录"
    return "最近 ${checkins.size} 条 · 最常见心情 $moodLabel"
}

fun resolveQuickCheckinMoodLabel(level: Int?): String {
    return quickCheckinMoodLabels[level] ?: "未选择"
}
