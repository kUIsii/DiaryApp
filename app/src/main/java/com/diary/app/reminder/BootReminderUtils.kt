package com.diary.app.reminder

data class BootReminderPlan(
    val shouldRestoreDiaryReminder: Boolean = false,
    val shouldRestoreTodoReminders: Boolean = false,
    val shouldRestoreTodoSummary: Boolean = false
)

fun resolveBootReminderPlan(action: String?): BootReminderPlan {
    return if (action == android.content.Intent.ACTION_BOOT_COMPLETED) {
        BootReminderPlan(
            shouldRestoreDiaryReminder = true,
            shouldRestoreTodoReminders = true,
            shouldRestoreTodoSummary = true
        )
    } else {
        BootReminderPlan()
    }
}
