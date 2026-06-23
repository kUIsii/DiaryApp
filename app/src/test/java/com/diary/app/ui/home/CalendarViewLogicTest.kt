package com.diary.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalendarViewLogicTest {

    @Test
    fun `month page mapping stays symmetric around the pager center`() {
        val june = YearMonth.of(2026, 6)
        val april = YearMonth.of(2026, 4)

        val aprilPage = targetPageForMonth(june, april)

        assertEquals(CENTER_PAGE - 2, aprilPage)
        assertEquals(april, monthForPage(june, aprilPage))
    }

    @Test
    fun `week page mapping stays symmetric around the pager center`() {
        val baseWeek = LocalDate.of(2026, 6, 22)
        val targetWeek = LocalDate.of(2026, 6, 8)

        val targetPage = targetPageForWeek(baseWeek, targetWeek)

        assertEquals(CENTER_PAGE - 2, targetPage)
        assertEquals(targetWeek, weekStartForPage(baseWeek, targetPage))
    }

    @Test
    fun `centered picker value maps visible index back to value`() {
        val days = (1..31).toList()

        assertEquals(15, centeredPickerValue(rawIndex = 16, paddingItems = 2, items = days))
    }

    @Test
    fun `picker list index centers the target value`() {
        val years = (2000..2030).toList()

        assertEquals(26, pickerListIndexForValue(value = 2026, items = years, paddingItems = 2))
    }

    @Test
    fun `day picker clamps invalid day when month changes`() {
        assertEquals(29, clampedDayForMonth(year = 2024, month = 2, day = 31))
        assertEquals(28, clampedDayForMonth(year = 2025, month = 2, day = 31))
        assertEquals(30, clampedDayForMonth(year = 2026, month = 4, day = 31))
    }

    @Test
    fun `jump picker initial date uses selected date before today`() {
        val today = LocalDate.of(2026, 6, 23)
        val selected = LocalDate.of(2024, 3, 5)

        assertEquals(selected, initialJumpPickerDate(selectedDate = selected, today = today))
        assertEquals(today, initialJumpPickerDate(selectedDate = null, today = today))
    }
}
