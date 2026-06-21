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
import com.diary.app.R
import java.util.concurrent.TimeUnit

class WeatherWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val weather = WeatherManager.fetchWeather(applicationContext)
            if (weather != null) {
                checkAndSendAlert(weather)
            }
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Weather refresh failed", e)
            Result.retry()
        }
    }

    private fun checkAndSendAlert(weather: CurrentWeather) {
        val severeTypes = listOf("雷", "暴", "冰雹", "台风")
        val isSevere = severeTypes.any { weather.weather.contains(it) }
        if (isSevere) {
            sendWeatherAlert(weather)
        }
    }

    private fun sendWeatherAlert(weather: CurrentWeather) {
        val context = applicationContext
        ensureChannel(context)

        val openIntent = android.content.Intent(context, com.diary.app.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, 2, openIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("天气预警")
            .setContentText("${weather.city}：${weather.weather}，气温${weather.temperature}度")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "WeatherWorker"
        private const val WORK_NAME = "weather_periodic_refresh"
        const val CHANNEL_ID = "weather_alert"
        private const val NOTIFICATION_ID = 1002

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WeatherWorker>(
                3, TimeUnit.HOURS
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        "天气预警",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "恶劣天气预警通知"
                    }
                    nm.createNotificationChannel(channel)
                }
            }
        }
    }
}
