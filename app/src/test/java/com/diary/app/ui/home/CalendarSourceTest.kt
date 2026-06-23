package com.diary.app.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CalendarSourceTest {

    @Test
    fun `calendar pager keeps month and week calculations anchored to visible state`() {
        val source = File("src/main/java/com/diary/app/ui/home/CalendarView.kt").readText()

        assertTrue(source.contains("LaunchedEffect(pagerState, calendarMode, monthPagerBase, weekPagerBase)"))
        assertTrue(source.contains("monthForPage(monthPagerBase, page)"))
        assertTrue(source.contains("weekStartForPage(weekPagerBase, page)"))
        assertFalse(source.contains("LaunchedEffect(pagerState) {"))
        assertFalse(source.contains("YearMonth.now().plusMonths(offset.toLong())"))
    }

    @Test
    fun `jump date uses material date picker instead of wheel picker`() {
        val source = File("src/main/java/com/diary/app/ui/home/CalendarView.kt").readText()

        assertTrue(source.contains("DatePickerDialog("))
        assertTrue(source.contains("rememberDatePickerState("))
        assertTrue(source.contains("selectedDate?.let"))
        assertTrue(source.contains("localDateToPickerMillis(today)"))
        assertFalse(source.contains("WheelPicker("))
        assertFalse(source.contains("centeredPickerValue("))
    }
}
