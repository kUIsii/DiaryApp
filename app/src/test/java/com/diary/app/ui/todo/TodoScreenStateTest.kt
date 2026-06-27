package com.diary.app.ui.todo

import com.diary.app.data.HabitRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class TodoScreenStateTest {

    @Test
    fun `closing habit detail clears selected habit when no follow up dialog remains`() {
        val opened = openHabitDetailState(
            habitId = 7L,
            initialDate = LocalDate.of(2026, 6, 27)
        )

        val dismissed = dismissHabitDialogState(opened)

        assertEquals(HabitDialogSurface.NONE, dismissed.activeDialog)
        assertNull(dismissed.selectedHabitId)
        assertFalse(dismissed.returnToDetailAfterRecord)
    }

    @Test
    fun `opening record dialog from detail remembers to return to detail on dismiss`() {
        val detailState = openHabitDetailState(
            habitId = 9L,
            initialDate = LocalDate.of(2026, 6, 27)
        )

        val recordState = openHabitRecordDialogState(
            current = detailState,
            habitId = 9L,
            date = LocalDate.of(2026, 6, 28)
        )
        val dismissed = dismissHabitDialogState(recordState)

        assertEquals(HabitDialogSurface.RECORD, recordState.activeDialog)
        assertTrue(recordState.returnToDetailAfterRecord)
        assertEquals(HabitDialogSurface.DETAIL, dismissed.activeDialog)
        assertEquals(9L, dismissed.selectedHabitId)
        assertFalse(dismissed.returnToDetailAfterRecord)
    }

    @Test
    fun `opening record dialog directly clears stale selection on dismiss`() {
        val recordState = openHabitRecordDialogState(
            current = HabitDialogState(
                selectedDate = LocalDate.of(2026, 6, 27),
                selectedMonth = YearMonth.of(2026, 6)
            ),
            habitId = 12L,
            date = LocalDate.of(2026, 6, 27)
        )

        val dismissed = dismissHabitDialogState(recordState)

        assertEquals(HabitDialogSurface.NONE, dismissed.activeDialog)
        assertNull(dismissed.selectedHabitId)
    }

    @Test
    fun `habit record copy makes it clear whether user is creating or updating`() {
        assertEquals("写一句今天的打卡记录", habitQuickRecordPlaceholder(existingRecord = null))
        assertEquals("补充今天的记录", habitQuickRecordPlaceholder(existingRecord = sampleRecord()))
        assertEquals("保存记录", habitQuickRecordConfirmLabel(existingRecord = null))
        assertEquals("更新记录", habitQuickRecordConfirmLabel(existingRecord = sampleRecord()))
    }

    private fun sampleRecord(): HabitRecord {
        return HabitRecord(
            id = 1L,
            todoId = 7L,
            recordDate = LocalDate.of(2026, 6, 27).toEpochDay(),
            summary = "今天完成了"
        )
    }
}
