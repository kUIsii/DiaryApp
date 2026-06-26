package com.diary.app.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderSettingsResolverTest {

    @Test
    fun `legacy reminder stays disabled when notification gate was turned off`() {
        val resolved = resolveUnifiedReminderSettings(
            app = AppReminderSettingsSnapshot(),
            legacy = LegacyReminderSettingsSnapshot(
                reminderEnabled = true,
                reminderHour = 20,
                reminderMinute = 15,
                notificationDailyEnabled = false
            )
        )

        assertFalse(resolved.writingReminderEnabled)
        assertEquals(20, resolved.writingReminderHour)
        assertEquals(15, resolved.writingReminderMinute)
    }

    @Test
    fun `app reminder settings override divergent legacy values`() {
        val resolved = resolveUnifiedReminderSettings(
            app = AppReminderSettingsSnapshot(
                writingReminderConfigured = true,
                writingReminderEnabled = true,
                reminderTimeConfigured = true,
                writingReminderHour = 22,
                writingReminderMinute = 45,
                weatherReminderConfigured = true,
                weatherReminder = true
            ),
            legacy = LegacyReminderSettingsSnapshot(
                reminderEnabled = false,
                reminderHour = 18,
                reminderMinute = 0,
                notificationDailyEnabled = false,
                weatherAlertsEnabled = false
            )
        )

        assertTrue(resolved.writingReminderEnabled)
        assertEquals(22, resolved.writingReminderHour)
        assertEquals(45, resolved.writingReminderMinute)
        assertTrue(resolved.weatherReminderEnabled)
    }

    @Test
    fun `settings dnd range becomes active even without legacy quiet hours flag`() {
        val resolved = resolveUnifiedReminderSettings(
            app = AppReminderSettingsSnapshot(
                quietHoursRangeConfigured = true,
                quietHoursStartHour = 23,
                quietHoursStartMinute = 0,
                quietHoursEndHour = 7,
                quietHoursEndMinute = 30
            ),
            legacy = LegacyReminderSettingsSnapshot()
        )

        assertTrue(resolved.quietHoursEnabled)
        assertEquals(23, resolved.quietHoursStartHour)
        assertEquals(30, resolved.quietHoursEndMinute)
    }
}
