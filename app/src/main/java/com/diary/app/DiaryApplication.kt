package com.diary.app

import android.app.Application
import com.diary.app.data.DiaryDatabase
import com.diary.app.ui.theme.ThemeMode
import com.diary.app.ui.theme.ThemePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DiaryApplication : Application() {
    val database by lazy { DiaryDatabase.getDatabase(this) }

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        _themeMode.value = ThemePreferences.getThemeMode(this)
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        ThemePreferences.setThemeMode(this, mode)
    }
}
