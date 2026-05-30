package com.diary.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.diary.app.data.DiaryDatabase
import com.diary.app.reminder.ReminderReceiver
import com.diary.app.ui.theme.ThemeMode
import com.diary.app.ui.theme.ThemePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DiaryApplication : Application() {
    val database by lazy { DiaryDatabase.getDatabase(this) }

    private val _themeMode = MutableStateFlow(ThemeMode.PURE_LIGHT)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        _themeMode.value = ThemePreferences.getThemeMode(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ReminderReceiver.CHANNEL_ID,
                "日记提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "每日写日记提醒"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        ThemePreferences.setThemeMode(this, mode)
    }
}
