package com.diary.app.ui.stats

import com.diary.app.data.DiaryPreview
import com.diary.app.data.WritingGoal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class GoalProgress(
    val goal: WritingGoal,
    val progress: Float,
    val currentDisplay: String,
    val targetDisplay: String,
    val isCompleted: Boolean,
    val periodLabel: String,
)

fun computeGoalProgress(
    goals: List<WritingGoal>,
    entries: List<DiaryPreview>,
    zone: ZoneId,
    now: LocalDate
): List<GoalProgress> {
    return goals.map { goal ->
        when (goal.type) {
            "weekly_entries" -> {
                val weekStart = now.minusDays(now.dayOfWeek.value.toLong() - 1)
                val startMillis = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
                val count = entries.count { it.createdAt >= startMillis }
                GoalProgress(
                    goal = goal,
                    progress = (count.toFloat() / goal.targetValue.coerceAtLeast(1)).coerceIn(0f, 1f),
                    currentDisplay = "$count 篇",
                    targetDisplay = "目标 ${goal.targetValue} 篇/周",
                    isCompleted = count >= goal.targetValue,
                    periodLabel = "本周"
                )
            }
            "monthly_entries" -> {
                val startMillis = now.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val count = entries.count { it.createdAt >= startMillis }
                GoalProgress(
                    goal = goal,
                    progress = (count.toFloat() / goal.targetValue.coerceAtLeast(1)).coerceIn(0f, 1f),
                    currentDisplay = "$count 篇",
                    targetDisplay = "目标 ${goal.targetValue} 篇/月",
                    isCompleted = count >= goal.targetValue,
                    periodLabel = "本月"
                )
            }
            "monthly_words" -> {
                val startMillis = now.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val totalChars = entries.filter { it.createdAt >= startMillis }.sumOf { it.plainText.length }
                GoalProgress(
                    goal = goal,
                    progress = (totalChars.toFloat() / goal.targetValue.coerceAtLeast(1)).coerceIn(0f, 1f),
                    currentDisplay = formatGoalCharCount(totalChars),
                    targetDisplay = "目标 ${formatGoalCharCount(goal.targetValue)}",
                    isCompleted = totalChars >= goal.targetValue,
                    periodLabel = "本月"
                )
            }
            else -> GoalProgress(goal, 0f, "0", "未知目标", false, "")
        }
    }
}

fun formatGoalCharCount(chars: Int): String {
    return if (chars >= 10000) "${"%.1f".format(chars / 10000.0)}万字"
    else if (chars >= 1000) "${"%.1f".format(chars / 1000.0)}k字"
    else "${chars}字"
}

fun latestGoalProgress(goals: List<GoalProgress>): GoalProgress? {
    return goals.maxWithOrNull(
        compareBy<GoalProgress> { it.isCompleted }
            .thenByDescending { it.progress }
            .thenByDescending { it.goal.targetValue }
    )
}
