package com.diary.app.ui.experimental

import android.content.Context

object ExperimentalFeaturesPreferences {
    private const val PREFS_NAME = "diary_prefs"
    private const val KEY_MAIN_SCREEN_SWIPE = "experimental_main_screen_swipe"
    private const val KEY_KEEP_COMPLETED_IN_PLACE = "experimental_keep_completed_in_place"
    private const val KEY_WRITING_MILESTONES = "experimental_writing_milestones"
    private const val KEY_AI_INSIGHT_CARD = "experimental_ai_insight_card"
    private const val KEY_AI_ASSISTANT = "experimental_ai_assistant"
    private const val KEY_FLOATING_BUBBLE = "experimental_floating_bubble"
    private const val KEY_HEALTH_DATA = "experimental_health_data"
    private const val KEY_DIARY_MAP = "experimental_diary_map"
    private const val KEY_AI_BIOGRAPHY = "experimental_ai_biography"

    fun getState(context: Context): ExperimentalFeaturesState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return ExperimentalFeaturesState(
            mainScreenSwipeEnabled = prefs.getBoolean(KEY_MAIN_SCREEN_SWIPE, true),
            keepCompletedItemsInPlace = prefs.getBoolean(KEY_KEEP_COMPLETED_IN_PLACE, false),
            writingMilestonesEnabled = prefs.getBoolean(KEY_WRITING_MILESTONES, false),
            aiInsightCardEnabled = prefs.getBoolean(KEY_AI_INSIGHT_CARD, false),
            aiAssistantEnabled = prefs.getBoolean(KEY_AI_ASSISTANT, prefs.getBoolean("experimental_ai_pen_pal", false)),
            floatingBubbleEnabled = prefs.getBoolean(KEY_FLOATING_BUBBLE, false),
            healthDataEnabled = prefs.getBoolean(KEY_HEALTH_DATA, false),
            diaryMapEnabled = prefs.getBoolean(KEY_DIARY_MAP, false),
            aiBiographyEnabled = prefs.getBoolean(KEY_AI_BIOGRAPHY, false)
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

    fun setWritingMilestonesEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_WRITING_MILESTONES, enabled).apply()
    }

    fun setAiInsightCardEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AI_INSIGHT_CARD, enabled).apply()
    }

    fun setAiAssistantEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AI_ASSISTANT, enabled).apply()
    }

    fun setFloatingBubbleEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_FLOATING_BUBBLE, enabled).apply()
    }

    fun setHealthDataEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_HEALTH_DATA, enabled).apply()
    }

    fun setDiaryMapEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_DIARY_MAP, enabled).apply()
    }

    fun setAiBiographyEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AI_BIOGRAPHY, enabled).apply()
    }
}
