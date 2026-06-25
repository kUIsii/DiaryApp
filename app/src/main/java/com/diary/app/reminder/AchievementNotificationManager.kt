package com.diary.app.reminder

import android.content.Context
import com.diary.app.data.Achievement
import com.diary.app.data.DiaryDatabase
import com.diary.app.ui.notification.InAppNotification
import com.diary.app.ui.notification.InAppNotificationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Handles in-app banners for achievement unlocks.
 *
 * Usage:
 * - Call [notifyAchievementUnlocked] when an achievement is unlocked.
 * - The method triggers an in-app banner (no system notification).
 * - Call [scheduleCheck] periodically (e.g. from [DiaryApplication]) to detect newly
 *   unlocked achievements and notify.
 */
object AchievementNotificationManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Reference to in-app notification state, set from MainActivity
    @Volatile
    var inAppNotificationState: InAppNotificationState? = null

    /**
     * Trigger an in-app banner for a single achievement.
     */
    fun notifyAchievementUnlocked(context: Context, achievement: Achievement) {
        if (!NotificationPreferencesManager.isAchievementsEnabled(context)) return
        if (NotificationPreferencesManager.isInQuietHours(context)) return
        triggerInAppBanner(achievement)
    }

    /**
     * Periodically check for newly unlocked achievements and notify.
     * Safe to call from DiaryApplication.onCreate(); it only fires once per newly unlocked achievement.
     */
    fun scheduleCheck(context: Context) {
        scope.launch {
            try {
                val db = DiaryDatabase.getDatabase(context)
                val dao = db.achievementDao()
                val allAchievements = dao.getAllUnified().first()
                val newlyUnlocked = allAchievements.filter {
                    it.unlockedAt != null && !hasBeenNotified(context, it.key)
                }
                newlyUnlocked.forEach { achievement ->
                    notifyAchievementUnlocked(context, achievement)
                    markAsNotified(context, achievement.key)
                }
            } catch (e: Exception) {
                android.util.Log.w("AchievementNotifMgr", "Check failed", e)
            }
        }
    }

    // ── Private helpers ────────────────────────────────────────

    private fun triggerInAppBanner(achievement: Achievement) {
        val state = inAppNotificationState ?: return
        val emoji = achievement.iconEmoji.ifEmpty { "\u2B50" }
        state.show(
            InAppNotification(
                id = "achievement_${achievement.key}",
                title = "成就解锁",
                subtitle = "$emoji ${achievement.name} - ${achievement.description}"
            )
        )
    }

    private fun hasBeenNotified(context: Context, key: String): Boolean {
        val prefs = context.getSharedPreferences("achievement_notified", Context.MODE_PRIVATE)
        return prefs.getBoolean(key, false)
    }

    private fun markAsNotified(context: Context, key: String) {
        context.getSharedPreferences("achievement_notified", Context.MODE_PRIVATE)
            .edit().putBoolean(key, true).apply()
    }
}
