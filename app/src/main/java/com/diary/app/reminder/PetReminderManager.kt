package com.diary.app.reminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.diary.app.MainActivity
import com.diary.app.R
import com.diary.app.data.DiaryDatabase
import java.util.concurrent.TimeUnit

/**
 * Periodic pet care reminder via WorkManager.
 *
 * Runs every 12 hours. If the user has not interacted with their pet:
 * - 3+ days: gentle reminder
 * - 7+ days: urgent reminder
 *
 * Respects the user's notification preference (quiet hours / per-type toggle).
 */
object PetReminderManager {

    const val CHANNEL_ID = "pet_care_reminder"
    private const val WORK_NAME = "pet_care_reminder"
    const val NOTIFICATION_ID = 2001

    // Thresholds in milliseconds
    const val GENTLE_THRESHOLD_MS = 3L * 24 * 60 * 60 * 1000   // 3 days
    const val URGENT_THRESHOLD_MS = 7L * 24 * 60 * 60 * 1000   // 7 days

    fun ensureChannel(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = android.app.NotificationChannel(
                    CHANNEL_ID,
                    "宠物关怀提醒",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "宠物太久没见到你了，温柔提醒你"
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<PetReminderWorker>(
            12, TimeUnit.HOURS
        ).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}

class PetReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = DiaryDatabase.getDatabase(applicationContext)
            val petDao = db.petDao()
            val profile = petDao.getPetProfileOnce() ?: return Result.success()

            val now = System.currentTimeMillis()
            val elapsed = now - profile.lastInteraction

            // Check if pet care reminders are enabled
            if (!NotificationPreferencesManager.isPetCareEnabled(applicationContext)) {
                return Result.success()
            }

            // Check quiet hours
            if (NotificationPreferencesManager.isInQuietHours(applicationContext)) {
                return Result.success()
            }

            when {
                elapsed >= PetReminderManager.URGENT_THRESHOLD_MS -> sendReminder(
                    isUrgent = true,
                    petName = profile.name
                )
                elapsed >= PetReminderManager.GENTLE_THRESHOLD_MS -> sendReminder(
                    isUrgent = false,
                    petName = profile.name
                )
            }
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Pet reminder check failed", e)
            Result.retry()
        }
    }

    private fun sendReminder(isUrgent: Boolean, petName: String) {
        val context = applicationContext

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "pet")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            PetReminderManager.NOTIFICATION_ID,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title: String
        val text: String
        if (isUrgent) {
            title = "${petName}很想你"
            text = "已经很久没有和${petName}互动了，快去看看它吧"
        } else {
            title = "${petName}在等你"
            text = "有几天没和${petName}说话了，它有点想你"
        }

        val notification = NotificationCompat.Builder(context, PetReminderManager.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(PetReminderManager.CHANNEL_ID, PetReminderManager.NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "PetReminderWorker"
    }
}
