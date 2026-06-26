package com.diary.app.reminder

import org.junit.Assert.assertEquals
import org.junit.Test

class BootReminderUtilsTest {

    @Test
    fun `boot receiver enables all reminder recoveries on boot completed`() {
        assertEquals(
            BootReminderPlan(
                shouldRestoreDiaryReminder = true,
                shouldRestoreTodoReminders = true,
                shouldRestoreTodoSummary = true
            ),
            resolveBootReminderPlan("android.intent.action.BOOT_COMPLETED")
        )
    }

    @Test
    fun `boot receiver ignores unrelated broadcasts`() {
        assertEquals(
            BootReminderPlan(),
            resolveBootReminderPlan("android.intent.action.TIMEZONE_CHANGED")
        )
    }
}
