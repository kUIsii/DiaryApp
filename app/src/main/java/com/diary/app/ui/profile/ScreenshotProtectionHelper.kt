package com.diary.app.ui.profile

import android.content.Context

object ScreenshotProtectionHelper {
    private const val KEY_SCREENSHOT_PROTECTION = "screenshot_protection_enabled"

    fun isEnabled(context: Context): Boolean {
        return context.getSharedPreferences("diary_prefs", Context.MODE_PRIVATE)
            .getBoolean(KEY_SCREENSHOT_PROTECTION, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences("diary_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SCREENSHOT_PROTECTION, enabled)
            .apply()
    }
}
