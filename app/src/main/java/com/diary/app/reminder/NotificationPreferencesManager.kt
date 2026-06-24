package com.diary.app.reminder

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalTime

/**
 * Centralized notification preferences manager.
 *
 * Stores per-type toggles and quiet hours configuration in SharedPreferences.
 */
object NotificationPreferencesManager {

    private const val PREFS_NAME = "notification_preferences"

    // Preference keys
    private const val KEY_DAILY_REMINDER_ENABLED = "daily_reminder_enabled"
    private const val KEY_ACHIEVEMENTS_ENABLED = "achievements_enabled"
    private const val KEY_PET_CARE_ENABLED = "pet_care_enabled"
    private const val KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled"
    private const val KEY_QUIET_HOURS_START_HOUR = "quiet_hours_start_hour"
    private const val KEY_QUIET_HOURS_START_MINUTE = "quiet_hours_start_minute"
    private const val KEY_QUIET_HOURS_END_HOUR = "quiet_hours_end_hour"
    private const val KEY_QUIET_HOURS_END_MINUTE = "quiet_hours_end_minute"

    // Defaults
    private const val DEFAULT_DAILY_REMINDER = true
    private const val DEFAULT_ACHIEVEMENTS = true
    private const val DEFAULT_PET_CARE = true
    private const val DEFAULT_QUIET_HOURS_ENABLED = false
    private const val DEFAULT_QUIET_START_HOUR = 22
    private const val DEFAULT_QUIET_START_MINUTE = 0
    private const val DEFAULT_QUIET_END_HOUR = 8
    private const val DEFAULT_QUIET_END_MINUTE = 0

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ── Daily reminder ─────────────────────────────────────────

    fun isDailyReminderEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DAILY_REMINDER_ENABLED, DEFAULT_DAILY_REMINDER)
    }

    fun setDailyReminderEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DAILY_REMINDER_ENABLED, enabled).apply()
    }

    // ── Achievements ───────────────────────────────────────────

    fun isAchievementsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ACHIEVEMENTS_ENABLED, DEFAULT_ACHIEVEMENTS)
    }

    fun setAchievementsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ACHIEVEMENTS_ENABLED, enabled).apply()
    }

    // ── Pet care ───────────────────────────────────────────────

    fun isPetCareEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_PET_CARE_ENABLED, DEFAULT_PET_CARE)
    }

    fun setPetCareEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_PET_CARE_ENABLED, enabled).apply()
    }

    // ── Quiet hours ────────────────────────────────────────────

    fun isQuietHoursEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_QUIET_HOURS_ENABLED, DEFAULT_QUIET_HOURS_ENABLED)
    }

    fun setQuietHoursEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_QUIET_HOURS_ENABLED, enabled).apply()
    }

    fun getQuietHoursStart(context: Context): Pair<Int, Int> {
        val prefs = getPrefs(context)
        return Pair(
            prefs.getInt(KEY_QUIET_HOURS_START_HOUR, DEFAULT_QUIET_START_HOUR),
            prefs.getInt(KEY_QUIET_HOURS_START_MINUTE, DEFAULT_QUIET_START_MINUTE)
        )
    }

    fun setQuietHoursStart(context: Context, hour: Int, minute: Int) {
        getPrefs(context).edit()
            .putInt(KEY_QUIET_HOURS_START_HOUR, hour)
            .putInt(KEY_QUIET_HOURS_START_MINUTE, minute)
            .apply()
    }

    fun getQuietHoursEnd(context: Context): Pair<Int, Int> {
        val prefs = getPrefs(context)
        return Pair(
            prefs.getInt(KEY_QUIET_HOURS_END_HOUR, DEFAULT_QUIET_END_HOUR),
            prefs.getInt(KEY_QUIET_HOURS_END_MINUTE, DEFAULT_QUIET_END_MINUTE)
        )
    }

    fun setQuietHoursEnd(context: Context, hour: Int, minute: Int) {
        getPrefs(context).edit()
            .putInt(KEY_QUIET_HOURS_END_HOUR, hour)
            .putInt(KEY_QUIET_HOURS_END_MINUTE, minute)
            .apply()
    }

    /**
     * Returns true if the current time falls within quiet hours.
     * Handles overnight spans (e.g., 22:00 - 08:00).
     */
    fun isInQuietHours(context: Context): Boolean {
        if (!isQuietHoursEnabled(context)) return false

        val now = LocalTime.now()
        val (startH, startM) = getQuietHoursStart(context)
        val (endH, endM) = getQuietHoursEnd(context)
        val start = LocalTime.of(startH, startM)
        val end = LocalTime.of(endH, endM)

        return if (start.isBefore(end) || start == end) {
            // Same-day range (e.g., 09:00 - 17:00)
            now in start..end
        } else {
            // Overnight range (e.g., 22:00 - 08:00)
            now >= start || now <= end
        }
    }

    // ── Convenience: check if any notification type is enabled ──

    fun isAnyNotificationEnabled(context: Context): Boolean {
        return isDailyReminderEnabled(context) ||
                isAchievementsEnabled(context) ||
                isPetCareEnabled(context)
    }
}
