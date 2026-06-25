package com.diary.app.ui.settings

import android.content.Context
import android.content.SharedPreferences

/**
 * 统一管理所有用户偏好设置
 */
object AppPreferences {
    private const val PREFS_NAME = "app_preferences"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── 写作设置 ──
    var defaultMoodLevel: Int
        get() = prefs(context).getInt("default_mood_level", -1)
        set(value) { prefs(context).edit().putInt("default_mood_level", value).apply() }

    var defaultWeather: String
        get() = prefs(context).getString("default_weather", "") ?: ""
        set(value) { prefs(context).edit().putString("default_weather", value).apply() }

    var autoSaveInterval: Int
        get() = prefs(context).getInt("auto_save_interval", 60)
        set(value) { prefs(context).edit().putInt("auto_save_interval", value).apply() }

    var defaultSortBy: String
        get() = prefs(context).getString("default_sort_by", "created_desc") ?: "created_desc"
        set(value) { prefs(context).edit().putString("default_sort_by", value).apply() }

    var defaultCalendarMode: String
        get() = prefs(context).getString("default_calendar_mode", "week") ?: "week"
        set(value) { prefs(context).edit().putString("default_calendar_mode", value).apply() }

    // ── 通知设置 ──
    var writingReminderEnabled: Boolean
        get() = prefs(context).getBoolean("writing_reminder_enabled", false)
        set(value) { prefs(context).edit().putBoolean("writing_reminder_enabled", value).apply() }

    var writingReminderHour: Int
        get() = prefs(context).getInt("writing_reminder_hour", 21)
        set(value) { prefs(context).edit().putInt("writing_reminder_hour", value).apply() }

    var writingReminderMinute: Int
        get() = prefs(context).getInt("writing_reminder_minute", 0)
        set(value) { prefs(context).edit().putInt("writing_reminder_minute", value).apply() }

    var streakBreakReminder: Boolean
        get() = prefs(context).getBoolean("streak_break_reminder", true)
        set(value) { prefs(context).edit().putBoolean("streak_break_reminder", value).apply() }

    var weatherReminder: Boolean
        get() = prefs(context).getBoolean("weather_reminder", false)
        set(value) { prefs(context).edit().putBoolean("weather_reminder", value).apply() }

    var dailyReviewPush: Boolean
        get() = prefs(context).getBoolean("daily_review_push", true)
        set(value) { prefs(context).edit().putBoolean("daily_review_push", value).apply() }

    var doNotDisturbStart: Int
        get() = prefs(context).getInt("dnd_start_hour", 22)
        set(value) { prefs(context).edit().putInt("dnd_start_hour", value).apply() }

    var doNotDisturbEnd: Int
        get() = prefs(context).getInt("dnd_end_hour", 8)
        set(value) { prefs(context).edit().putInt("dnd_end_hour", value).apply() }

    // ── 数据管理 ──
    var trashRetentionDays: Int
        get() = prefs(context).getInt("trash_retention_days", 30)
        set(value) { prefs(context).edit().putInt("trash_retention_days", value).apply() }

    var autoCleanOrphanMedia: Boolean
        get() = prefs(context).getBoolean("auto_clean_orphan_media", false)
        set(value) { prefs(context).edit().putBoolean("auto_clean_orphan_media", value).apply() }

    // ── 编辑器设置 ──
    var editorFontSize: Float
        get() = prefs(context).getFloat("editor_font_size", 16f)
        set(value) { prefs(context).edit().putFloat("editor_font_size", value).apply() }

    var editorToolbarCompact: Boolean
        get() = prefs(context).getBoolean("editor_toolbar_compact", false)
        set(value) { prefs(context).edit().putBoolean("editor_toolbar_compact", value).apply() }

    var autoTagSuggestion: Boolean
        get() = prefs(context).getBoolean("auto_tag_suggestion", true)
        set(value) { prefs(context).edit().putBoolean("auto_tag_suggestion", value).apply() }

    // ── 隐私设置 ──
    var locationRecordingEnabled: Boolean
        get() = prefs(context).getBoolean("location_recording_enabled", true)
        set(value) { prefs(context).edit().putBoolean("location_recording_enabled", value).apply() }

    var aiDataUsageConsent: Boolean
        get() = prefs(context).getBoolean("ai_data_usage_consent", false)
        set(value) { prefs(context).edit().putBoolean("ai_data_usage_consent", value).apply() }

    var screenshotProtection: Boolean
        get() = prefs(context).getBoolean("screenshot_protection", false)
        set(value) { prefs(context).edit().putBoolean("screenshot_protection", value).apply() }

    // ── Helper ──
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }
}
