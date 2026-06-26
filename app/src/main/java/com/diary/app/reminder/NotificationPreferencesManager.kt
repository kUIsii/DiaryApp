package com.diary.app.reminder

import android.content.Context
import java.time.LocalTime

/**
 * Compatibility facade for legacy notification callers.
 *
 * Runtime consumers now read from [ReminderSettingsRepository] so settings,
 * profile toggles and receivers stay on the same source of truth.
 */
object NotificationPreferencesManager {

    fun isDailyReminderEnabled(context: Context): Boolean {
        return ReminderSettingsRepository.getSettings(context).writingReminderEnabled
    }

    fun setDailyReminderEnabled(context: Context, enabled: Boolean) {
        ReminderSettingsRepository.setWritingReminderEnabled(context, enabled)
    }

    fun isAchievementsEnabled(context: Context): Boolean {
        return ReminderSettingsRepository.getSettings(context).achievementsEnabled
    }

    fun setAchievementsEnabled(context: Context, enabled: Boolean) {
        ReminderSettingsRepository.setAchievementsEnabled(context, enabled)
    }

    fun isWeatherAlertsEnabled(context: Context): Boolean {
        return ReminderSettingsRepository.getSettings(context).weatherReminderEnabled
    }

    fun setWeatherAlertsEnabled(context: Context, enabled: Boolean) {
        ReminderSettingsRepository.setWeatherReminderEnabled(context, enabled)
    }

    fun isQuietHoursEnabled(context: Context): Boolean {
        return ReminderSettingsRepository.getSettings(context).quietHoursEnabled
    }

    fun setQuietHoursEnabled(context: Context, enabled: Boolean) {
        ReminderSettingsRepository.setQuietHoursEnabled(context, enabled)
    }

    fun getQuietHoursStart(context: Context): Pair<Int, Int> {
        val settings = ReminderSettingsRepository.getSettings(context)
        return Pair(settings.quietHoursStartHour, settings.quietHoursStartMinute)
    }

    fun setQuietHoursStart(context: Context, hour: Int, minute: Int) {
        ReminderSettingsRepository.setQuietHoursStart(context, hour, minute)
    }

    fun getQuietHoursEnd(context: Context): Pair<Int, Int> {
        val settings = ReminderSettingsRepository.getSettings(context)
        return Pair(settings.quietHoursEndHour, settings.quietHoursEndMinute)
    }

    fun setQuietHoursEnd(context: Context, hour: Int, minute: Int) {
        ReminderSettingsRepository.setQuietHoursEnd(context, hour, minute)
    }

    fun isInQuietHours(context: Context): Boolean {
        val settings = ReminderSettingsRepository.getSettings(context)
        if (!settings.quietHoursEnabled) return false

        val now = LocalTime.now()
        val start = LocalTime.of(settings.quietHoursStartHour, settings.quietHoursStartMinute)
        val end = LocalTime.of(settings.quietHoursEndHour, settings.quietHoursEndMinute)
        return isWithinQuietHours(now, start, end)
    }

    fun isAnyNotificationEnabled(context: Context): Boolean {
        return isDailyReminderEnabled(context) ||
            isAchievementsEnabled(context) ||
            isWeatherAlertsEnabled(context)
    }
}
