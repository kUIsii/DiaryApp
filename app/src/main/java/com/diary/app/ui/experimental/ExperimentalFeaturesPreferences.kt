package com.diary.app.ui.experimental

import android.content.Context

object ExperimentalFeaturesPreferences {
    private const val PREFS_NAME = "diary_prefs"
    private const val KEY_MAIN_SCREEN_SWIPE = "experimental_main_screen_swipe"
    private const val KEY_KEEP_COMPLETED_IN_PLACE = "experimental_keep_completed_in_place"

    fun getState(context: Context): ExperimentalFeaturesState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return ExperimentalFeaturesState(
            mainScreenSwipeEnabled = prefs.getBoolean(KEY_MAIN_SCREEN_SWIPE, false),
            keepCompletedItemsInPlace = prefs.getBoolean(KEY_KEEP_COMPLETED_IN_PLACE, false)
        )
    }

    fun setMainScreenSwipeEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_MAIN_SCREEN_SWIPE, enabled)
            .apply()
    }

    fun setKeepCompletedItemsInPlace(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_KEEP_COMPLETED_IN_PLACE, enabled)
            .apply()
    }
}
