package com.diary.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.amap.api.maps.MapsInitializer
import com.diary.app.data.DiaryDatabase
import com.diary.app.di.AppContainer
import com.diary.app.reminder.AchievementNotificationManager
import com.diary.app.reminder.PetReminderManager
import com.diary.app.reminder.ReminderReceiver
import com.diary.app.reminder.TodoReminderManager
import com.diary.app.ai.AiServiceManager
import com.diary.app.data.AchievementRepository
import com.diary.app.data.BackupManager
import com.diary.app.data.TrashCleanupWorker
import com.diary.app.weather.WeatherWorker
import com.diary.app.ui.experimental.ExperimentalFeaturesPreferences
import com.diary.app.ui.experimental.ExperimentalFeaturesState
import com.diary.app.ui.theme.ThemeMode
import com.diary.app.ui.theme.ThemePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DiaryApplication : Application() {
    val database by lazy { DiaryDatabase.getDatabase(this) }
    val container by lazy { AppContainer(this) }
    val aiService by lazy { AiServiceManager(this) }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
        AchievementNotificationManager.ensureChannel(this)
        PetReminderManager.ensureChannel(this)
        // Schedule periodic auto-backup via WorkManager
        if (BackupManager.isAutoBackupEnabled(this)) {
            BackupManager.scheduleAutoBackup(this)
        }
        // Schedule daily trash cleanup (delete entries older than 30 days)
        TrashCleanupWorker.schedule(this)
        // Schedule periodic weather refresh
        WeatherWorker.ensureChannel(this)
        WeatherWorker.schedule(this)
        PetReminderManager.schedule(this)

        // Initialize unified achievement system
        try {
            val achievementDao = database.achievementDao()
            val diaryDao = database.diaryDao()
            val repo = AchievementRepository(achievementDao, diaryDao)
            appScope.launch {
                runCatching {
                    repo.initialize()
                    repo.checkAndUnlock()
                    AchievementNotificationManager.scheduleCheck(this@DiaryApplication)
                }.onFailure {
                    android.util.Log.w("DiaryApplication", "Achievement check skipped", it)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DiaryApplication", "Achievement init skipped", e)
        }
        // Still run a one-shot check at cold start for immediate needs
        appScope.launch {
            runCatching {
                if (BackupManager.shouldAutoBackup(this@DiaryApplication)) {
                    BackupManager.performAutoBackup(this@DiaryApplication, database.diaryDao())
                }
            }.onFailure {
                android.util.Log.w("DiaryApplication", "Auto backup skipped", it)
            }
        }

        // Initialize Amap SDK
        try {
            MapsInitializer.updatePrivacyShow(this, true, true)
            MapsInitializer.updatePrivacyAgree(this, true)
        } catch (e: Exception) {
            android.util.Log.e("DiaryApplication", "Failed to initialize Amap SDK", e)
        }
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

    fun setHealthDataEnabled(enabled: Boolean) {
        _experimentalFeatures.value = _experimentalFeatures.value.copy(healthDataEnabled = enabled)
        ExperimentalFeaturesPreferences.setHealthDataEnabled(this, enabled)
    }

    fun setDiaryMapEnabled(enabled: Boolean) {
        _experimentalFeatures.value = _experimentalFeatures.value.copy(diaryMapEnabled = enabled)
        ExperimentalFeaturesPreferences.setDiaryMapEnabled(this, enabled)
    }

    fun setAiBiographyEnabled(enabled: Boolean) {
        _experimentalFeatures.value = _experimentalFeatures.value.copy(aiBiographyEnabled = enabled)
        ExperimentalFeaturesPreferences.setAiBiographyEnabled(this, enabled)
    }
}
