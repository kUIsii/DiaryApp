package com.diary.app.weather

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class WeatherWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val weather = WeatherManager.fetchWeather(applicationContext)
            if (weather != null && weather.alerts.isNotEmpty()) {
                val app = applicationContext as com.diary.app.DiaryApplication
                val dao = app.database.diaryDao()
                for (alert in weather.alerts) {
                    val entity = com.diary.app.data.NotificationEntity(
                        id = "weather_alert_${System.currentTimeMillis()}_${alert.hashCode()}",
                        type = "weather_alert",
                        title = "${alert.level}预警 · ${alert.type}",
                        subtitle = alert.text,
                        iconType = "thunderstorm",
                        colorHex = when (alert.level) {
                            "红色" -> 0xFFDC2626L
                            "橙色" -> 0xFFEA580CL
                            "黄色" -> 0xFFF59E0BL
                            "蓝色" -> 0xFF3B82F6L
                            else -> 0xFFE53935L
                        },
                        relatedId = null,
                        createdAt = System.currentTimeMillis()
                    )
                    dao.insertNotification(entity)
                }
                if (com.diary.app.reminder.NotificationPreferencesManager.isWeatherAlertsEnabled(applicationContext)) {
                    sendAlerts(weather.alerts, weather.city)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Weather refresh failed", e)
            Result.retry()
        }
    }

    private fun sendAlerts(alerts: List<WeatherAlert>, city: String) {
        val ctx = applicationContext
        ensureChannel(ctx)
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var notifId = NOTIFICATION_ID

        for (alert in alerts) {
            val openIntent = android.content.Intent(ctx, com.diary.app.MainActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = android.app.PendingIntent.getActivity(ctx, notifId, openIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)

            val levelColor = when (alert.level) {
                "红色" -> NotificationCompat.PRIORITY_MAX
                "橙色" -> NotificationCompat.PRIORITY_HIGH
                "黄色" -> NotificationCompat.PRIORITY_DEFAULT
                "蓝色" -> NotificationCompat.PRIORITY_LOW
                else -> NotificationCompat.PRIORITY_HIGH
            }

            val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("${alert.level}\u9884\u8B66\uFF1A${alert.type}")
                .setContentText("$city\uFF1A${alert.text.take(80)}")
                .setStyle(NotificationCompat.BigTextStyle().bigText("$city\uFF1A${alert.text}"))
                .setPriority(levelColor)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()

            nm.notify(notifId, notification)
            notifId++
        }
    }

    companion object {
        private const val TAG = "WeatherWorker"
        private const val WORK_NAME = "weather_periodic_refresh"
        const val CHANNEL_ID = "weather_alert"
        private const val NOTIFICATION_ID = 1002

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WeatherWorker>(1, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
            val oneTimeRequest = androidx.work.OneTimeWorkRequestBuilder<WeatherWorker>()
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("weather_initial", androidx.work.ExistingWorkPolicy.REPLACE, oneTimeRequest)
        }

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                    nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "天气预警", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "恶劣天气预警通知"
                    })
                }
            }
        }
    }
}
