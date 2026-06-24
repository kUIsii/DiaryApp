package com.diary.app.reminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.diary.app.MainActivity
import com.diary.app.R
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
 * Handles system notifications and in-app banners for achievement unlocks.
 *
 * Usage:
 * - Call [notifyAchievementUnlocked] when an achievement is unlocked.
 * - The method posts a system notification and optionally triggers an in-app banner.
 * - Call [scheduleCheck] periodically (e.g. from [DiaryApplication]) to detect newly
 *   unlocked achievements and notify.
 */
object AchievementNotificationManager {

    const val CHANNEL_ID = "achievement_unlock"
    private const val NOTIFICATION_TAG = "achievement"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Reference to in-app notification state, set from MainActivity
    @Volatile
    var inAppNotificationState: InAppNotificationState? = null

    fun ensureChannel(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = android.app.NotificationChannel(
                    CHANNEL_ID,
                    "成就解锁",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "成就解锁时通知你"
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    /**
     * Post a notification and in-app banner for a single achievement.
     */
    fun notifyAchievementUnlocked(context: Context, achievement: Achievement) {
        if (!NotificationPreferencesManager.isAchievementsEnabled(context)) return
        if (NotificationPreferencesManager.isInQuietHours(context)) return
        // System notification
        postSystemNotification(context, achievement)
        // In-app banner
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

    private fun postSystemNotification(context: Context, achievement: Achievement) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "achievements")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            achievement.key.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val emoji = achievement.iconEmoji.ifEmpty { "\u2B50" }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("成就解锁")
            .setContentText("$emoji ${achievement.name}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${achievement.description}\n${achievement.flavorText}")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_TAG, achievement.key.hashCode(), notification)
    }

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
