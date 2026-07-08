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
import com.diary.app.DiaryApplication
import com.diary.app.data.NotificationEntity
import com.diary.app.reminder.NotificationPreferencesManager
import com.diary.app.weather.WeatherAlertStore
import java.util.concurrent.TimeUnit

/**
 * 天气预警专用巡检 Worker。
 *
 * 与每小时的天气刷新（WeatherWorker）解耦：
 *  - 每 15 分钟巡检一次预警源，保证"暴雨来临前"的及时性；
 *  - 按 alertId 去重，仅对"新出现的预警"推送系统通知并写入收件箱，避免重复打扰。
 */
class WeatherAlertWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            if (!NotificationPreferencesManager.isWeatherAlertsEnabled(applicationContext)) {
                return Result.success()
            }
            val alerts = WeatherAlertFetcher.fetchAlerts(applicationContext)
            // 始终把当前生效的预警写入本地仓库，供首页横幅展示（与系统推送 / APP 内开关无关）
            WeatherAlertStore.saveActiveAlerts(applicationContext, alerts)
            if (alerts.isEmpty()) return Result.success()

            val newAlerts = filterNewAlerts(applicationContext, alerts)
            if (newAlerts.isNotEmpty()) {
                // APP 内提醒（收件箱）：单独开关控制
                if (NotificationPreferencesManager.isWeatherAlertInAppEnabled(applicationContext)) {
                    insertInbox(newAlerts)
                }
                // 系统推送（状态栏通知）：单独开关控制
                if (NotificationPreferencesManager.isWeatherAlertSystemEnabled(applicationContext)) {
                    sendNotifications(newAlerts)
                }
                Log.d(TAG, "处理 ${newAlerts.size} 条新预警")
            }
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Weather alert refresh failed", e)
            Result.retry()
        }
    }

    /** 与已通知集合比对，返回本次新增的预警；同时把新预警记入去重集合。 */
    private fun filterNewAlerts(context: Context, alerts: List<WeatherAlert>): List<WeatherAlert> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getStringSet(KEY_NOTIFIED, emptySet())?.toMutableSet() ?: mutableSetOf()
        val cutoff = System.currentTimeMillis() - STALE_MS

        // 清理 24 小时前的旧记录，避免集合无限增长
        val live = raw.filter { entry ->
            val ts = entry.substringAfter('\t', "").toLongOrNull() ?: 0L
            ts > cutoff
        }.toMutableSet()

        val knownIds = live.map { it.substringBefore('\t') }.toSet()
        val newOnes = alerts.filter { it.alertId.isNotBlank() && it.alertId !in knownIds }

        if (newOnes.isNotEmpty()) {
            val now = System.currentTimeMillis()
            newOnes.forEach { live.add("${it.alertId}\t$now") }
            prefs.edit().putStringSet(KEY_NOTIFIED, live).apply()
        }
        return newOnes
    }

    private suspend fun insertInbox(alerts: List<WeatherAlert>) {
        val app = applicationContext as DiaryApplication
        val dao = app.database.diaryDao()
        val now = System.currentTimeMillis()
            for (alert in alerts) {
            val entity = NotificationEntity(
                id = "weather_alert_${alert.alertId}",
                type = "weather_alert",
                title = buildTitle(alert),
                subtitle = alert.text,
                iconType = "thunderstorm",
                colorHex = levelColor(alert.level),
                relatedId = null,
                createdAt = now,
                alertProvince = alert.province,
                alertPublishTime = alert.publishTime,
                alertSource = alert.source
            )
            dao.insertNotification(entity)
        }
    }

    private fun sendNotifications(alerts: List<WeatherAlert>) {
        val ctx = applicationContext
        WeatherWorker.ensureChannel(ctx)
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var notifId = NOTIFICATION_ID

        for (alert in alerts) {
            val openIntent = android.content.Intent(ctx, com.diary.app.MainActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = android.app.PendingIntent.getActivity(
                ctx, notifId, openIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            val priority = when (alert.level) {
                "红色" -> NotificationCompat.PRIORITY_MAX
                "橙色" -> NotificationCompat.PRIORITY_HIGH
                "黄色" -> NotificationCompat.PRIORITY_DEFAULT
                "蓝色" -> NotificationCompat.PRIORITY_LOW
                else -> NotificationCompat.PRIORITY_HIGH
            }

            val notification = NotificationCompat.Builder(ctx, WeatherWorker.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(buildTitle(alert))
                .setContentText("${alert.city}：${alert.text.take(80)}")
                .setStyle(NotificationCompat.BigTextStyle().bigText("${alert.city}：${alert.text}"))
                .setPriority(priority)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()

            nm.notify(notifId, notification)
            notifId++
        }
    }

    private fun buildTitle(alert: WeatherAlert): String {
        val levelPart = if (alert.level.isNotBlank()) "${alert.level}预警 · " else ""
        return "$levelPart${alert.type}"
    }

    private fun levelColor(level: String): Long = when (level) {
        "红色" -> 0xFFDC2626L
        "橙色" -> 0xFFEA580CL
        "黄色" -> 0xFFF59E0BL
        "蓝色" -> 0xFF3B82F6L
        else -> 0xFFE53935L
    }

    companion object {
        private const val TAG = "WeatherAlertWorker"
        private const val WORK_NAME = "weather_alert_periodic"
        private const val PREFS = "weather_alert_prefs"
        private const val KEY_NOTIFIED = "notified_alert_ids"
        private const val NOTIFICATION_ID = 2002
        private const val STALE_MS = 24 * 60 * 60 * 1000L // 去重记录保留 24 小时

        fun schedule(context: Context) {
            // WorkManager 最短周期 15 分钟，足够满足"暴雨来临前及时通知"
            val request = PeriodicWorkRequestBuilder<WeatherAlertWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)

            val oneTime = androidx.work.OneTimeWorkRequestBuilder<WeatherAlertWorker>().build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("weather_alert_initial", androidx.work.ExistingWorkPolicy.REPLACE, oneTime)
        }

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (nm.getNotificationChannel(WeatherWorker.CHANNEL_ID) == null) {
                    nm.createNotificationChannel(
                        NotificationChannel(
                            WeatherWorker.CHANNEL_ID,
                            "天气预警",
                            NotificationManager.IMPORTANCE_HIGH
                        ).apply { description = "恶劣天气预警通知" }
                    )
                }
            }
        }
    }
}
