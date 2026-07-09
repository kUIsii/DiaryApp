package com.diary.app.weather

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
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
            // 仅刷新天气并缓存。天气预警由 WeatherAlertWorker 独管，
            // 这里不再写 notifications / 发通知，避免两套机制重复打扰、开关语义错乱。
            WeatherManager.fetchWeather(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Weather refresh failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "WeatherWorker"
        private const val WORK_NAME = "weather_periodic_refresh"
        const val CHANNEL_ID = "weather_alert"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<WeatherWorker>(1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
            val oneTimeRequest = OneTimeWorkRequestBuilder<WeatherWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("weather_initial", ExistingWorkPolicy.REPLACE, oneTimeRequest)
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
