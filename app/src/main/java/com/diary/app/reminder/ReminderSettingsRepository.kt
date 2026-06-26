package com.diary.app.reminder

import android.content.Context
import android.content.SharedPreferences
import com.diary.app.ui.settings.AppPreferences
import java.time.LocalTime

data class AppReminderSettingsSnapshot(
    val writingReminderConfigured: Boolean = false,
    val writingReminderEnabled: Boolean = false,
    val reminderTimeConfigured: Boolean = false,
    val writingReminderHour: Int = 21,
    val writingReminderMinute: Int = 0,
    val weatherReminderConfigured: Boolean = false,
    val weatherReminder: Boolean = false,
    val dailyReviewConfigured: Boolean = false,
    val dailyReviewEnabled: Boolean = true,
    val achievementsConfigured: Boolean = false,
    val achievementsEnabled: Boolean = true,
    val quietHoursConfigured: Boolean = false,
    val quietHoursEnabled: Boolean = false,
    val quietHoursRangeConfigured: Boolean = false,
    val quietHoursStartHour: Int = 22,
    val quietHoursStartMinute: Int = 0,
    val quietHoursEndHour: Int = 8,
    val quietHoursEndMinute: Int = 0
)

data class LegacyReminderSettingsSnapshot(
    val reminderEnabled: Boolean? = null,
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val notificationDailyEnabled: Boolean? = null,
    val weatherAlertsEnabled: Boolean? = null,
    val achievementsEnabled: Boolean? = null,
    val quietHoursEnabled: Boolean? = null,
    val quietHoursStartHour: Int? = null,
    val quietHoursStartMinute: Int? = null,
    val quietHoursEndHour: Int? = null,
    val quietHoursEndMinute: Int? = null
)

data class UnifiedReminderSettings(
    val writingReminderEnabled: Boolean,
    val writingReminderHour: Int,
    val writingReminderMinute: Int,
    val weatherReminderEnabled: Boolean,
    val dailyReviewEnabled: Boolean,
    val achievementsEnabled: Boolean,
    val quietHoursEnabled: Boolean,
    val quietHoursStartHour: Int,
    val quietHoursStartMinute: Int,
    val quietHoursEndHour: Int,
    val quietHoursEndMinute: Int
)

fun resolveUnifiedReminderSettings(
    app: AppReminderSettingsSnapshot,
    legacy: LegacyReminderSettingsSnapshot
): UnifiedReminderSettings {
    val reminderEnabled = when {
        app.writingReminderConfigured -> app.writingReminderEnabled
        legacy.reminderEnabled != null || legacy.notificationDailyEnabled != null ->
            (legacy.reminderEnabled ?: false) && (legacy.notificationDailyEnabled ?: true)
        else -> false
    }

    val reminderHour = when {
        app.reminderTimeConfigured -> app.writingReminderHour
        legacy.reminderHour != null -> legacy.reminderHour
        else -> 21
    }
    val reminderMinute = when {
        app.reminderTimeConfigured -> app.writingReminderMinute
        legacy.reminderMinute != null -> legacy.reminderMinute
        else -> 0
    }

    val quietHoursEnabled = when {
        app.quietHoursConfigured -> app.quietHoursEnabled
        app.quietHoursRangeConfigured -> true
        legacy.quietHoursEnabled != null -> legacy.quietHoursEnabled
        else -> false
    }

    return UnifiedReminderSettings(
        writingReminderEnabled = reminderEnabled,
        writingReminderHour = reminderHour,
        writingReminderMinute = reminderMinute,
        weatherReminderEnabled = when {
            app.weatherReminderConfigured -> app.weatherReminder
            legacy.weatherAlertsEnabled != null -> legacy.weatherAlertsEnabled
            else -> false
        },
        dailyReviewEnabled = when {
            app.dailyReviewConfigured -> app.dailyReviewEnabled
            else -> true
        },
        achievementsEnabled = when {
            app.achievementsConfigured -> app.achievementsEnabled
            legacy.achievementsEnabled != null -> legacy.achievementsEnabled
            else -> true
        },
        quietHoursEnabled = quietHoursEnabled,
        quietHoursStartHour = when {
            app.quietHoursRangeConfigured -> app.quietHoursStartHour
            legacy.quietHoursStartHour != null -> legacy.quietHoursStartHour
            else -> 22
        },
        quietHoursStartMinute = when {
            app.quietHoursRangeConfigured -> app.quietHoursStartMinute
            legacy.quietHoursStartMinute != null -> legacy.quietHoursStartMinute
            else -> 0
        },
        quietHoursEndHour = when {
            app.quietHoursRangeConfigured -> app.quietHoursEndHour
            legacy.quietHoursEndHour != null -> legacy.quietHoursEndHour
            else -> 8
        },
        quietHoursEndMinute = when {
            app.quietHoursRangeConfigured -> app.quietHoursEndMinute
            legacy.quietHoursEndMinute != null -> legacy.quietHoursEndMinute
            else -> 0
        }
    )
}

fun isWithinQuietHours(now: LocalTime, start: LocalTime, end: LocalTime): Boolean {
    return if (start.isBefore(end) || start == end) {
        now in start..end
    } else {
        now >= start || now <= end
    }
}

object ReminderSettingsRepository {

    private const val APP_PREFS_NAME = "app_preferences"
    private const val LEGACY_REMINDER_PREFS_NAME = "diary_reminder_prefs"
    private const val LEGACY_NOTIFICATION_PREFS_NAME = "notification_preferences"

    private const val KEY_WRITING_REMINDER_ENABLED = "writing_reminder_enabled"
    private const val KEY_WRITING_REMINDER_HOUR = "writing_reminder_hour"
    private const val KEY_WRITING_REMINDER_MINUTE = "writing_reminder_minute"
    private const val KEY_WEATHER_REMINDER = "weather_reminder"
    private const val KEY_DAILY_REVIEW_PUSH = "daily_review_push"
    private const val KEY_ACHIEVEMENT_NOTIFICATIONS_ENABLED = "achievement_notifications_enabled"
    private const val KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled"
    private const val KEY_DND_START_HOUR = "dnd_start_hour"
    private const val KEY_DND_START_MINUTE = "dnd_start_minute"
    private const val KEY_DND_END_HOUR = "dnd_end_hour"
    private const val KEY_DND_END_MINUTE = "dnd_end_minute"

    private const val LEGACY_KEY_REMINDER_ENABLED = "reminder_enabled"
    private const val LEGACY_KEY_REMINDER_HOUR = "reminder_hour"
    private const val LEGACY_KEY_REMINDER_MINUTE = "reminder_minute"
    private const val LEGACY_KEY_NOTIFICATION_DAILY_ENABLED = "daily_reminder_enabled"
    private const val LEGACY_KEY_WEATHER_ALERTS_ENABLED = "weather_alerts_enabled"
    private const val LEGACY_KEY_ACHIEVEMENTS_ENABLED = "achievements_enabled"
    private const val LEGACY_KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled"
    private const val LEGACY_KEY_QUIET_START_HOUR = "quiet_hours_start_hour"
    private const val LEGACY_KEY_QUIET_START_MINUTE = "quiet_hours_start_minute"
    private const val LEGACY_KEY_QUIET_END_HOUR = "quiet_hours_end_hour"
    private const val LEGACY_KEY_QUIET_END_MINUTE = "quiet_hours_end_minute"

    fun syncFromLegacy(context: Context): UnifiedReminderSettings {
        AppPreferences.init(context)
        if (!AppPreferences.reminderSettingsMigrated) {
            val resolved = resolveUnifiedReminderSettings(
                app = readAppSnapshot(context),
                legacy = readLegacySnapshot(context)
            )
            persistResolvedSettings(resolved)
            AppPreferences.reminderSettingsMigrated = true
            return resolved
        }
        return getSettings(context)
    }

    fun getSettings(context: Context): UnifiedReminderSettings {
        AppPreferences.init(context)
        if (!AppPreferences.reminderSettingsMigrated) {
            return syncFromLegacy(context)
        }
        return UnifiedReminderSettings(
            writingReminderEnabled = AppPreferences.writingReminderEnabled,
            writingReminderHour = AppPreferences.writingReminderHour,
            writingReminderMinute = AppPreferences.writingReminderMinute,
            weatherReminderEnabled = AppPreferences.weatherReminder,
            dailyReviewEnabled = AppPreferences.dailyReviewPush,
            achievementsEnabled = AppPreferences.achievementNotificationsEnabled,
            quietHoursEnabled = AppPreferences.quietHoursEnabled,
            quietHoursStartHour = AppPreferences.doNotDisturbStart,
            quietHoursStartMinute = AppPreferences.doNotDisturbStartMinute,
            quietHoursEndHour = AppPreferences.doNotDisturbEnd,
            quietHoursEndMinute = AppPreferences.doNotDisturbEndMinute
        )
    }

    fun setWritingReminderEnabled(context: Context, enabled: Boolean) {
        syncFromLegacy(context)
        AppPreferences.writingReminderEnabled = enabled
    }

    fun setWritingReminderTime(context: Context, hour: Int, minute: Int) {
        syncFromLegacy(context)
        AppPreferences.writingReminderHour = hour
        AppPreferences.writingReminderMinute = minute
    }

    fun setWeatherReminderEnabled(context: Context, enabled: Boolean) {
        syncFromLegacy(context)
        AppPreferences.weatherReminder = enabled
    }

    fun setDailyReviewEnabled(context: Context, enabled: Boolean) {
        syncFromLegacy(context)
        AppPreferences.dailyReviewPush = enabled
    }

    fun setAchievementsEnabled(context: Context, enabled: Boolean) {
        syncFromLegacy(context)
        AppPreferences.achievementNotificationsEnabled = enabled
    }

    fun setQuietHoursEnabled(context: Context, enabled: Boolean) {
        syncFromLegacy(context)
        AppPreferences.quietHoursEnabled = enabled
    }

    fun setQuietHoursStart(context: Context, hour: Int, minute: Int = 0) {
        syncFromLegacy(context)
        AppPreferences.quietHoursEnabled = true
        AppPreferences.doNotDisturbStart = hour
        AppPreferences.doNotDisturbStartMinute = minute
    }

    fun setQuietHoursEnd(context: Context, hour: Int, minute: Int = 0) {
        syncFromLegacy(context)
        AppPreferences.quietHoursEnabled = true
        AppPreferences.doNotDisturbEnd = hour
        AppPreferences.doNotDisturbEndMinute = minute
    }

    private fun readAppSnapshot(context: Context): AppReminderSettingsSnapshot {
        val prefs = context.getSharedPreferences(APP_PREFS_NAME, Context.MODE_PRIVATE)
        return AppReminderSettingsSnapshot(
            writingReminderConfigured = prefs.contains(KEY_WRITING_REMINDER_ENABLED),
            writingReminderEnabled = prefs.getBoolean(KEY_WRITING_REMINDER_ENABLED, false),
            reminderTimeConfigured = prefs.contains(KEY_WRITING_REMINDER_HOUR) || prefs.contains(KEY_WRITING_REMINDER_MINUTE),
            writingReminderHour = prefs.getInt(KEY_WRITING_REMINDER_HOUR, 21),
            writingReminderMinute = prefs.getInt(KEY_WRITING_REMINDER_MINUTE, 0),
            weatherReminderConfigured = prefs.contains(KEY_WEATHER_REMINDER),
            weatherReminder = prefs.getBoolean(KEY_WEATHER_REMINDER, false),
            dailyReviewConfigured = prefs.contains(KEY_DAILY_REVIEW_PUSH),
            dailyReviewEnabled = prefs.getBoolean(KEY_DAILY_REVIEW_PUSH, true),
            achievementsConfigured = prefs.contains(KEY_ACHIEVEMENT_NOTIFICATIONS_ENABLED),
            achievementsEnabled = prefs.getBoolean(KEY_ACHIEVEMENT_NOTIFICATIONS_ENABLED, true),
            quietHoursConfigured = prefs.contains(KEY_QUIET_HOURS_ENABLED),
            quietHoursEnabled = prefs.getBoolean(KEY_QUIET_HOURS_ENABLED, false),
            quietHoursRangeConfigured = prefs.contains(KEY_DND_START_HOUR) || prefs.contains(KEY_DND_END_HOUR),
            quietHoursStartHour = prefs.getInt(KEY_DND_START_HOUR, 22),
            quietHoursStartMinute = prefs.getInt(KEY_DND_START_MINUTE, 0),
            quietHoursEndHour = prefs.getInt(KEY_DND_END_HOUR, 8),
            quietHoursEndMinute = prefs.getInt(KEY_DND_END_MINUTE, 0)
        )
    }

    private fun readLegacySnapshot(context: Context): LegacyReminderSettingsSnapshot {
        val reminderPrefs = context.getSharedPreferences(LEGACY_REMINDER_PREFS_NAME, Context.MODE_PRIVATE)
        val notificationPrefs = context.getSharedPreferences(LEGACY_NOTIFICATION_PREFS_NAME, Context.MODE_PRIVATE)
        return LegacyReminderSettingsSnapshot(
            reminderEnabled = reminderPrefs.booleanOrNull(LEGACY_KEY_REMINDER_ENABLED),
            reminderHour = reminderPrefs.intOrNull(LEGACY_KEY_REMINDER_HOUR),
            reminderMinute = reminderPrefs.intOrNull(LEGACY_KEY_REMINDER_MINUTE),
            notificationDailyEnabled = notificationPrefs.booleanOrNull(LEGACY_KEY_NOTIFICATION_DAILY_ENABLED),
            weatherAlertsEnabled = notificationPrefs.booleanOrNull(LEGACY_KEY_WEATHER_ALERTS_ENABLED),
            achievementsEnabled = notificationPrefs.booleanOrNull(LEGACY_KEY_ACHIEVEMENTS_ENABLED),
            quietHoursEnabled = notificationPrefs.booleanOrNull(LEGACY_KEY_QUIET_HOURS_ENABLED),
            quietHoursStartHour = notificationPrefs.intOrNull(LEGACY_KEY_QUIET_START_HOUR),
            quietHoursStartMinute = notificationPrefs.intOrNull(LEGACY_KEY_QUIET_START_MINUTE),
            quietHoursEndHour = notificationPrefs.intOrNull(LEGACY_KEY_QUIET_END_HOUR),
            quietHoursEndMinute = notificationPrefs.intOrNull(LEGACY_KEY_QUIET_END_MINUTE)
        )
    }

    private fun persistResolvedSettings(settings: UnifiedReminderSettings) {
        AppPreferences.writingReminderEnabled = settings.writingReminderEnabled
        AppPreferences.writingReminderHour = settings.writingReminderHour
        AppPreferences.writingReminderMinute = settings.writingReminderMinute
        AppPreferences.weatherReminder = settings.weatherReminderEnabled
        AppPreferences.dailyReviewPush = settings.dailyReviewEnabled
        AppPreferences.achievementNotificationsEnabled = settings.achievementsEnabled
        AppPreferences.quietHoursEnabled = settings.quietHoursEnabled
        AppPreferences.doNotDisturbStart = settings.quietHoursStartHour
        AppPreferences.doNotDisturbStartMinute = settings.quietHoursStartMinute
        AppPreferences.doNotDisturbEnd = settings.quietHoursEndHour
        AppPreferences.doNotDisturbEndMinute = settings.quietHoursEndMinute
    }

    private fun SharedPreferences.booleanOrNull(key: String): Boolean? {
        return if (contains(key)) getBoolean(key, false) else null
    }

    private fun SharedPreferences.intOrNull(key: String): Int? {
        return if (contains(key)) getInt(key, 0) else null
    }
}
