package com.diary.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val plan = resolveBootReminderPlan(intent.action)
        if (!plan.shouldRestoreDiaryReminder &&
            !plan.shouldRestoreTodoReminders &&
            !plan.shouldRestoreTodoSummary
        ) {
            return
        }

        if (plan.shouldRestoreDiaryReminder && ReminderManager.isReminderEnabled(context)) {
            val (hour, minute) = ReminderManager.getReminderTime(context)
            ReminderManager.scheduleReminder(context, hour, minute)
        }

        if (plan.shouldRestoreTodoReminders) {
            TodoReminderManager.rescheduleAllPendingReminders(context)
        }

        if (plan.shouldRestoreTodoSummary) {
            TodoReminderManager.scheduleDailySummary(context)
        }
    }
}
