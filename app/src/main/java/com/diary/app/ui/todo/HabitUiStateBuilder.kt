package com.diary.app.ui.todo

import com.diary.app.data.DiaryPreview
import com.diary.app.data.HabitRecord
import com.diary.app.data.TodoItem
import java.time.LocalDate

fun buildHabitItemUiState(
    habit: TodoItem,
    records: List<HabitRecord>,
    today: LocalDate = LocalDate.now()
): HabitItemUiState {
    val byDay = records.associateBy { it.recordDate }
    val recentDays = (6L downTo 0L).map { offset ->
        val date = today.minusDays(offset)
        HabitDayState(
            date = date,
            record = byDay[date.toEpochDay()],
            isToday = date == today
        )
    }

    return HabitItemUiState(
        habit = habit,
        todayRecord = byDay[today.toEpochDay()],
        streak = calculateHabitStreak(byDay.keys, today),
        recentDays = recentDays
    )
}

fun buildHabitSummaryUiState(habits: List<HabitItemUiState>): HabitSummaryUiState {
    return HabitSummaryUiState(
        total = habits.size,
        recordedToday = habits.count { it.todayRecord != null },
        diaryToday = habits.count { it.todayRecord?.source == HabitRecord.SOURCE_DIARY },
        manualToday = habits.count { it.todayRecord?.source == HabitRecord.SOURCE_MANUAL },
        detailToday = habits.count { it.todayRecord?.source == HabitRecord.SOURCE_DETAIL }
    )
}

fun calculateHabitStreak(recordDays: Set<Long>, today: LocalDate): Int {
    var streak = 0
    var cursor = today
    while (recordDays.contains(cursor.toEpochDay())) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}

fun buildDiaryHabitSummary(preview: DiaryPreview?): String {
    if (preview == null) return ""
    return when {
        preview.title.isNotBlank() -> preview.title
        preview.plainText.isNotBlank() -> preview.plainText.take(42)
        else -> "来自今日日记"
    }
}
