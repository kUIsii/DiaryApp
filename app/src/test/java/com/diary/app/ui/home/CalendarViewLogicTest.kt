package com.diary.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
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
    fun `picker date conversion round trips through millis in utc`() {
        val zone = ZoneId.of("UTC")
        val date = LocalDate.of(2026, 6, 23)

        val millis = localDateToPickerMillis(date, zone)

        assertEquals(date, pickerMillisToLocalDate(millis, zone))
    }
}
