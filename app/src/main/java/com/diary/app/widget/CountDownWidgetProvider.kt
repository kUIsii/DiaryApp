package com.diary.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.diary.app.MainActivity
import com.diary.app.R
import com.diary.app.data.DiaryDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class CountDownWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_REFRESH -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, CountDownWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                scope.launch {
                    try {
                        for (appWidgetId in appWidgetIds) {
                            updateAppWidget(context, appWidgetManager, appWidgetId)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_OPEN_APP -> {
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("navigate_to", "countdown")
                }
                context.startActivity(launchIntent)
                pendingResult.finish()
            }
            else -> pendingResult.finish()
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.diary.app.widget.ACTION_COUNTDOWN_REFRESH"
        const val ACTION_OPEN_APP = "com.diary.app.widget.ACTION_COUNTDOWN_OPEN_APP"

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, CountDownWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_countdown_list)

            // Set up the list view with RemoteViewsService
            val intent = Intent(context, CountDownWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.lv_countdown, intent)
            views.setEmptyView(R.id.lv_countdown, R.id.tv_empty)

            // Set up click handler for list items
            val openAppIntent = Intent(context, CountDownWidgetProvider::class.java).apply {
                action = ACTION_OPEN_APP
            }
            val openAppPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setPendingIntentTemplate(R.id.lv_countdown, openAppPendingIntent)

            // Update count
            scope.launch {
                try {
                    val db = DiaryDatabase.getDatabase(context)
                    val items = db.countDownDao().getAllCountDownItemsOnce()
                    views.setTextViewText(
                        R.id.tv_count,
                        context.getString(R.string.countdown_widget_count, items.size)
                    )
                } catch (e: Exception) {
                    // Ignore
                }
                appWidgetManager.updateAppWidget(appWidgetId, views)
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.lv_countdown)
            }
        }
    }
}
