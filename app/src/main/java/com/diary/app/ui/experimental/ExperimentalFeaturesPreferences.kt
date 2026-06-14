package com.diary.app.ui.experimental

import android.content.Context

object ExperimentalFeaturesPreferences {
    private const val PREFS_NAME = "diary_prefs"
    private const val KEY_MAIN_SCREEN_SWIPE = "experimental_main_screen_swipe"
    private const val KEY_KEEP_COMPLETED_IN_PLACE = "experimental_keep_completed_in_place"
    private const val KEY_AI_ENABLED = "ai_enabled"
    private const val KEY_AI_SILENT_TITLE = "ai_silent_title"
    private const val KEY_AI_MEMORY_ECHO = "ai_memory_echo"
    private const val KEY_AI_ON_THIS_DAY = "ai_on_this_day"
    private const val KEY_AI_MOOD_TREND = "ai_mood_trend"
    private const val KEY_AI_WRITING_RHYTHM = "ai_writing_rhythm"
    private const val KEY_AI_TAG_INTUITION = "ai_tag_intuition"
    private const val KEY_AI_MILESTONES = "ai_milestones"

    fun getState(context: Context): ExperimentalFeaturesState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return ExperimentalFeaturesState(
            mainScreenSwipeEnabled = prefs.getBoolean(KEY_MAIN_SCREEN_SWIPE, false),
            keepCompletedItemsInPlace = prefs.getBoolean(KEY_KEEP_COMPLETED_IN_PLACE, false),
            aiEnabled = prefs.getBoolean(KEY_AI_ENABLED, false),
            aiSilentTitle = prefs.getBoolean(KEY_AI_SILENT_TITLE, false),
            aiMemoryEcho = prefs.getBoolean(KEY_AI_MEMORY_ECHO, false),
            aiOnThisDay = prefs.getBoolean(KEY_AI_ON_THIS_DAY, false),
            aiMoodTrend = prefs.getBoolean(KEY_AI_MOOD_TREND, false),
            aiWritingRhythm = prefs.getBoolean(KEY_AI_WRITING_RHYTHM, false),
            aiTagIntuition = prefs.getBoolean(KEY_AI_TAG_INTUITION, false),
            aiMilestones = prefs.getBoolean(KEY_AI_MILESTONES, false)
        )
    }

    fun setMainScreenSwipeEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_MAIN_SCREEN_SWIPE, enabled).apply()
    }

    fun setKeepCompletedItemsInPlace(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_KEEP_COMPLETED_IN_PLACE, enabled).apply()
    }

    fun setAiEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AI_ENABLED, enabled).apply()
    }

    fun setAiSilentTitle(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AI_SILENT_TITLE, enabled).apply()
    }

    fun setAiMemoryEcho(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AI_MEMORY_ECHO, enabled).apply()
    }

    fun setAiOnThisDay(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AI_ON_THIS_DAY, enabled).apply()
    }

    fun setAiMoodTrend(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AI_MOOD_TREND, enabled).apply()
    }

    fun setAiWritingRhythm(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AI_WRITING_RHYTHM, enabled).apply()
    }

    fun setAiTagIntuition(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AI_TAG_INTUITION, enabled).apply()
    }

    fun setAiMilestones(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AI_MILESTONES, enabled).apply()
    }
}
