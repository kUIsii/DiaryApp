package com.diary.app.ui.theme

import android.content.Context

object ThemePreferences {
    private const val KEY_THEME_MODE = "theme_mode"

    fun getThemeMode(context: Context): ThemeMode {
        val prefs = context.getSharedPreferences("diary_prefs", Context.MODE_PRIVATE)
        val rawName = prefs.getString(KEY_THEME_MODE, null)
        val resolved = resolveThemeModeName(rawName)
        if (rawName != resolved.name) {
            setThemeMode(context, resolved)
        }
        return resolved
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        context.getSharedPreferences("diary_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, mode.name)
            .apply()
    }

}
