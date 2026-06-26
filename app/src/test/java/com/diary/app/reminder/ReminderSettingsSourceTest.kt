package com.diary.app.reminder

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReminderSettingsSourceTest {

    @Test
    fun `reminder runtime and settings screens use unified repository`() {
        val reminderManager = File("src/main/java/com/diary/app/reminder/ReminderManager.kt").readText()
        val notificationManager = File("src/main/java/com/diary/app/reminder/NotificationPreferencesManager.kt").readText()
        val settingsScreen = File("src/main/java/com/diary/app/ui/settings/SettingsScreen.kt").readText()

        assertTrue(reminderManager.contains("ReminderSettingsRepository.getSettings(context)"))
        assertTrue(reminderManager.contains("ReminderSettingsRepository.setWritingReminderEnabled(context, true)"))
        assertTrue(notificationManager.contains("ReminderSettingsRepository.getSettings(context)"))
        assertTrue(settingsScreen.contains("ReminderSettingsRepository.setWritingReminderTime(context, h, m)"))
        assertTrue(settingsScreen.contains("ReminderManager.scheduleReminder(context, writingReminderHour, writingReminderMinute)"))
        assertTrue(settingsScreen.contains("ReminderManager.cancelReminder(context)"))
    }
}
