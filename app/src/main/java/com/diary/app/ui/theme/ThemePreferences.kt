package com.diary.app.ui.theme

import android.content.Context

object ThemePreferences {
    private const val KEY_THEME_MODE = "theme_mode"

    fun getThemeMode(context: Context): ThemeMode {
        val prefs = context.getSharedPreferences("diary_prefs", Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_THEME_MODE, ThemeMode.PURE_LIGHT.name)
        return try {
            val mode = ThemeMode.valueOf(name ?: ThemeMode.PURE_LIGHT.name)
            mode
        } catch (_: Exception) {
            // Migration: old theme values map to PURE_LIGHT
            migrateOldTheme(context)
            ThemeMode.PURE_LIGHT
        }
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        context.getSharedPreferences("diary_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, mode.name)
            .apply()
    }

    private fun migrateOldTheme(context: Context): ThemeMode {
        val prefs = context.getSharedPreferences("diary_prefs", Context.MODE_PRIVATE)
        val oldName = prefs.getString(KEY_THEME_MODE, null)

        val newMode = when {
            oldName == null -> ThemeMode.PURE_LIGHT
            oldName == "PURE_LIGHT" -> ThemeMode.PURE_LIGHT
            oldName == "PURE_DARK" -> ThemeMode.PURE_DARK
            // SYSTEM/GRADIENT/WARM_ROSE/OCEAN_BLUE -> default to PURE_LIGHT
            else -> ThemeMode.PURE_LIGHT
        }

        setThemeMode(context, newMode)
        return newMode
    }
}
