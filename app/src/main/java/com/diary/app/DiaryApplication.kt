package com.diary.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.diary.app.data.DiaryDatabase
import com.diary.app.di.AppContainer
import com.diary.app.reminder.ReminderReceiver
import com.diary.app.reminder.TodoReminderManager
import com.diary.app.ai.AiServiceManager
import com.diary.app.ui.experimental.ExperimentalFeaturesPreferences
import com.diary.app.ui.experimental.ExperimentalFeaturesState
import com.diary.app.ui.theme.ThemeMode
import com.diary.app.ui.theme.ThemePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DiaryApplication : Application() {
    val database by lazy { DiaryDatabase.getDatabase(this) }
    val container by lazy { AppContainer(this) }
    val aiService by lazy { AiServiceManager(this) }

    private val _themeMode = MutableStateFlow(ThemeMode.PURE_LIGHT)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _experimentalFeatures = MutableStateFlow(ExperimentalFeaturesState())
    val experimentalFeatures: StateFlow<ExperimentalFeaturesState> = _experimentalFeatures.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        _themeMode.value = ThemePreferences.getThemeMode(this)
        _experimentalFeatures.value = ExperimentalFeaturesPreferences.getState(this)
        createNotificationChannel()
        TodoReminderManager.createNotificationChannel(this)
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

    fun setMainScreenSwipeEnabled(enabled: Boolean) {
        _experimentalFeatures.value = _experimentalFeatures.value.copy(mainScreenSwipeEnabled = enabled)
        ExperimentalFeaturesPreferences.setMainScreenSwipeEnabled(this, enabled)
    }

    fun setKeepCompletedItemsInPlace(enabled: Boolean) {
        _experimentalFeatures.value = _experimentalFeatures.value.copy(keepCompletedItemsInPlace = enabled)
        ExperimentalFeaturesPreferences.setKeepCompletedItemsInPlace(this, enabled)
    }

    fun setWritingMilestonesEnabled(enabled: Boolean) {
        _experimentalFeatures.value = _experimentalFeatures.value.copy(writingMilestonesEnabled = enabled)
        ExperimentalFeaturesPreferences.setWritingMilestonesEnabled(this, enabled)
    }

    fun setAiInsightCardEnabled(enabled: Boolean) {
        _experimentalFeatures.value = _experimentalFeatures.value.copy(aiInsightCardEnabled = enabled)
        ExperimentalFeaturesPreferences.setAiInsightCardEnabled(this, enabled)
    }

    fun setAiAssistantEnabled(enabled: Boolean) {
        _experimentalFeatures.value = _experimentalFeatures.value.copy(aiAssistantEnabled = enabled)
        ExperimentalFeaturesPreferences.setAiAssistantEnabled(this, enabled)
    }

    fun setFloatingBubbleEnabled(enabled: Boolean) {
        _experimentalFeatures.value = _experimentalFeatures.value.copy(floatingBubbleEnabled = enabled)
        ExperimentalFeaturesPreferences.setFloatingBubbleEnabled(this, enabled)
    }
}
