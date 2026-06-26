package com.diary.app.ui.settings

import android.content.Context
import android.content.SharedPreferences

/**
 * 统一管理所有用户偏好设置
 */
object AppPreferences {


    private const val PREFS_NAME = "app_preferences"

    private fun prefs(): SharedPreferences =
        appContext!!.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── 写作设置 ──
    var defaultMoodLevel: Int
        get() = prefs().getInt("default_mood_level", -1)
        set(value) { prefs().edit().putInt("default_mood_level", value).apply() }

    var defaultWeather: String
        get() = prefs().getString("default_weather", "") ?: ""
        set(value) { prefs().edit().putString("default_weather", value).apply() }

    var autoSaveInterval: Int
        get() = prefs().getInt("auto_save_interval", 60)
        set(value) { prefs().edit().putInt("auto_save_interval", value).apply() }

    var defaultSortBy: String
        get() = prefs().getString("default_sort_by", "created_desc") ?: "created_desc"
        set(value) { prefs().edit().putString("default_sort_by", value).apply() }

    var defaultCalendarMode: String
        get() = prefs().getString("default_calendar_mode", "week") ?: "week"
        set(value) { prefs().edit().putString("default_calendar_mode", value).apply() }

    // ── 通知设置 ──
    var writingReminderEnabled: Boolean
        get() = prefs().getBoolean("writing_reminder_enabled", false)
        set(value) { prefs().edit().putBoolean("writing_reminder_enabled", value).apply() }

    var writingReminderHour: Int
        get() = prefs().getInt("writing_reminder_hour", 21)
        set(value) { prefs().edit().putInt("writing_reminder_hour", value).apply() }

    var writingReminderMinute: Int
        get() = prefs().getInt("writing_reminder_minute", 0)
        set(value) { prefs().edit().putInt("writing_reminder_minute", value).apply() }

    var streakBreakReminder: Boolean
        get() = prefs().getBoolean("streak_break_reminder", true)
        set(value) { prefs().edit().putBoolean("streak_break_reminder", value).apply() }

    var weatherReminder: Boolean
        get() = prefs().getBoolean("weather_reminder", false)
        set(value) { prefs().edit().putBoolean("weather_reminder", value).apply() }

    var dailyReviewPush: Boolean
        get() = prefs().getBoolean("daily_review_push", true)
        set(value) { prefs().edit().putBoolean("daily_review_push", value).apply() }

    var achievementNotificationsEnabled: Boolean
        get() = prefs().getBoolean("achievement_notifications_enabled", true)
        set(value) { prefs().edit().putBoolean("achievement_notifications_enabled", value).apply() }

    var quietHoursEnabled: Boolean
        get() = prefs().getBoolean("quiet_hours_enabled", false)
        set(value) { prefs().edit().putBoolean("quiet_hours_enabled", value).apply() }

    var doNotDisturbStart: Int
        get() = prefs().getInt("dnd_start_hour", 22)
        set(value) { prefs().edit().putInt("dnd_start_hour", value).apply() }

    var doNotDisturbStartMinute: Int
        get() = prefs().getInt("dnd_start_minute", 0)
        set(value) { prefs().edit().putInt("dnd_start_minute", value).apply() }

    var doNotDisturbEnd: Int
        get() = prefs().getInt("dnd_end_hour", 8)
        set(value) { prefs().edit().putInt("dnd_end_hour", value).apply() }

    var doNotDisturbEndMinute: Int
        get() = prefs().getInt("dnd_end_minute", 0)
        set(value) { prefs().edit().putInt("dnd_end_minute", value).apply() }

    // ── 数据管理 ──
    var trashRetentionDays: Int
        get() = prefs().getInt("trash_retention_days", 30)
        set(value) { prefs().edit().putInt("trash_retention_days", value).apply() }

    var autoCleanOrphanMedia: Boolean
        get() = prefs().getBoolean("auto_clean_orphan_media", false)
        set(value) { prefs().edit().putBoolean("auto_clean_orphan_media", value).apply() }

    // ── 编辑器设置 ──
    var editorFontSize: Float
        get() = prefs().getFloat("editor_font_size", 16f)
        set(value) { prefs().edit().putFloat("editor_font_size", value).apply() }

    var editorToolbarCompact: Boolean
        get() = prefs().getBoolean("editor_toolbar_compact", false)
        set(value) { prefs().edit().putBoolean("editor_toolbar_compact", value).apply() }

    var autoTagSuggestion: Boolean
        get() = prefs().getBoolean("auto_tag_suggestion", true)
        set(value) { prefs().edit().putBoolean("auto_tag_suggestion", value).apply() }

    // ── 隐私设置 ──
    var locationRecordingEnabled: Boolean
        get() = prefs().getBoolean("location_recording_enabled", true)
        set(value) { prefs().edit().putBoolean("location_recording_enabled", value).apply() }

    var aiDataUsageConsent: Boolean
        get() = prefs().getBoolean("ai_data_usage_consent", false)
        set(value) { prefs().edit().putBoolean("ai_data_usage_consent", value).apply() }

    var screenshotProtection: Boolean
        get() = prefs().getBoolean("screenshot_protection", false)
        set(value) { prefs().edit().putBoolean("screenshot_protection", value).apply() }

    var reminderSettingsMigrated: Boolean
        get() = prefs().getBoolean("reminder_settings_migrated", false)
        set(value) { prefs().edit().putBoolean("reminder_settings_migrated", value).apply() }

    // ── Helper ──
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }
}
