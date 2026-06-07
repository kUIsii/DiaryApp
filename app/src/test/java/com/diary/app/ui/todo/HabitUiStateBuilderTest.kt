package com.diary.app.ui.todo

import com.diary.app.data.HabitRecord
import com.diary.app.data.TodoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class HabitUiStateBuilderTest {

    @Test
    fun `build summary and streak from recent habit records`() {
        val today = LocalDate.of(2026, 6, 7)
        val habit = TodoItem(id = 7, title = "早睡", category = TodoItem.CATEGORY_GOAL)
        val records = listOf(
            HabitRecord(
                todoId = 7,
                recordDate = today.toEpochDay(),
                source = HabitRecord.SOURCE_MANUAL,
                summary = "十一点前休息"
            ),
            HabitRecord(
                todoId = 7,
                recordDate = today.minusDays(1).toEpochDay(),
                source = HabitRecord.SOURCE_DIARY,
                summary = "来自日记"
            ),
            HabitRecord(
                todoId = 7,
                recordDate = today.minusDays(2).toEpochDay(),
                source = HabitRecord.SOURCE_DETAIL,
                summary = "状态不错"
            )
        )

        val item = buildHabitItemUiState(habit, records, today)

        assertEquals(3, item.streak)
        assertEquals("十一点前休息", item.todayRecord?.summary)
        assertEquals(7, item.recentDays.size)
        assertEquals(today, item.recentDays.last().date)
        assertEquals(today.minusDays(6), item.recentDays.first().date)
    }

    @Test
    fun `summary counts only habits recorded today`() {
        val today = LocalDate.of(2026, 6, 7)
        val habitA = TodoItem(id = 1, title = "喝水", category = TodoItem.CATEGORY_GOAL)
        val habitB = TodoItem(id = 2, title = "散步", category = TodoItem.CATEGORY_GOAL)

        val states = listOf(
            buildHabitItemUiState(
                habit = habitA,
                records = listOf(
                    HabitRecord(
                        todoId = 1,
                        recordDate = today.toEpochDay(),
                        source = HabitRecord.SOURCE_DIARY,
                        summary = "今天有记录"
                    )
                ),
                today = today
            ),
            buildHabitItemUiState(
                habit = habitB,
                records = listOf(
                    HabitRecord(
                        todoId = 2,
                        recordDate = today.minusDays(1).toEpochDay(),
                        source = HabitRecord.SOURCE_MANUAL,
                        summary = "昨天有记录"
                    )
                ),
                today = today
            )
        )

        val summary = buildHabitSummaryUiState(states)

        assertEquals(2, summary.total)
        assertEquals(1, summary.recordedToday)
        assertEquals(1, summary.diaryToday)
        assertEquals(0, summary.manualToday)
        assertEquals(0, summary.detailToday)
        assertNull(states[1].todayRecord)
    }
}
