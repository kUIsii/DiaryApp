package com.diary.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import java.util.Calendar

object ReminderManager {

    private const val PREFS_NAME = "diary_reminder_prefs"
    private const val KEY_ENABLED = "reminder_enabled"
    private const val KEY_HOUR = "reminder_hour"
    private const val KEY_MINUTE = "reminder_minute"
    private const val KEY_MESSAGE = "reminder_message"
    private const val DEFAULT_HOUR = 21
    private const val DEFAULT_MINUTE = 0
    private const val REQUEST_CODE = 1001

    // Gentle reminder messages that rotate daily
    private val gentleMessages = listOf(
        "记录今天的点滴，留住美好回忆",
        "今天有什么值得记住的事呢？",
        "花几分钟，和自己聊聊天吧",
        "写下今天的心情，明天会感谢自己",
        "每一天都值得被记录",
        "用文字定格今天的瞬间",
        "今天的你，过得怎么样？",
        "写下感悟，释放一天的心情"
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getReminderMessage(context: Context): String {
        val customMessage = getPrefs(context).getString(KEY_MESSAGE, null)
        if (!customMessage.isNullOrBlank()) return customMessage
        // Rotate through gentle messages based on day of year
        val dayOfYear = java.time.LocalDate.now().dayOfYear
        return gentleMessages[dayOfYear % gentleMessages.size]
    }

    fun setCustomMessage(context: Context, message: String?) {
        getPrefs(context).edit().putString(KEY_MESSAGE, message).apply()
    }

    fun isReminderEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ENABLED, false)
    }

    fun getReminderTime(context: Context): Pair<Int, Int> {
        val prefs = getPrefs(context)
        return Pair(
            prefs.getInt(KEY_HOUR, DEFAULT_HOUR),
            prefs.getInt(KEY_MINUTE, DEFAULT_MINUTE)
        )
    }

    fun scheduleReminder(context: Context, hour: Int, minute: Int) {
        val prefs = getPrefs(context)
        prefs.edit()
            .putBoolean(KEY_ENABLED, true)
            .putInt(KEY_HOUR, hour)
            .putInt(KEY_MINUTE, minute)
            .apply()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createPendingIntent(context)

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // SCHEDULE_EXACT_ALARM permission not granted on API 31+
            // Fall back to inexact alarm
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelReminder(context: Context) {
        val prefs = getPrefs(context)
        prefs.edit().putBoolean(KEY_ENABLED, false).apply()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createPendingIntent(context)
        alarmManager.cancel(pendingIntent)
    }

    private fun createPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
