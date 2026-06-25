package com.diary.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.diary.app.MainActivity
import com.diary.app.R
import com.diary.app.data.DiaryDatabase
import com.diary.app.data.TodoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TodoWidgetProvider : AppWidgetProvider() {

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
            ACTION_TOGGLE_TODO -> {
                val todoId = intent.getLongExtra(EXTRA_TODO_ID, -1)
                if (todoId != -1L) {
                    scope.launch {
                        try {
                            toggleTodo(context, todoId)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                } else {
                    pendingResult.finish()
                }
            }
            ACTION_ADD_TODO -> {
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("navigate_to", "todo")
                    putExtra("action", "add")
                }
                context.startActivity(launchIntent)
                pendingResult.finish()
            }
            ACTION_REFRESH -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, TodoWidgetProvider::class.java)
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
            else -> pendingResult.finish()
        }
    }

    companion object {
        const val ACTION_TOGGLE_TODO = "com.diary.app.widget.ACTION_TOGGLE_TODO"
        const val ACTION_ADD_TODO = "com.diary.app.widget.ACTION_ADD_TODO"
        const val ACTION_REFRESH = "com.diary.app.widget.ACTION_REFRESH"
        const val EXTRA_TODO_ID = "todo_id"

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TodoWidgetProvider::class.java)
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
            val views = RemoteViews(context.packageName, R.layout.widget_todo_list)

            // Set up the list view with RemoteViewsService
            val intent = Intent(context, TodoWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.lv_todos, intent)
            views.setEmptyView(R.id.lv_todos, R.id.tv_empty)

            // Set up click handler for list items
            val toggleIntent = Intent(context, TodoWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_TODO
            }
            val togglePendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.lv_todos, togglePendingIntent)

            // Set up add button click handler
            val addIntent = Intent(context, TodoWidgetProvider::class.java).apply {
                action = ACTION_ADD_TODO
            }
            val addPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_add, addPendingIntent)

            // Update pending count
            scope.launch {
                try {
                    val db = DiaryDatabase.getDatabase(context)
                    val todos = db.todoDao().getTopPendingTodos(100)
                    val pendingCount = todos.count { !it.isCompleted }
                    views.setTextViewText(
                        R.id.tv_pending_count,
                        context.getString(R.string.widget_pending_count, pendingCount)
                    )
                } catch (e: Exception) {
                    // Ignore
                }
                appWidgetManager.updateAppWidget(appWidgetId, views)
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.lv_todos)
            }
        }

        private suspend fun toggleTodo(context: Context, todoId: Long) {
            try {
                val db = DiaryDatabase.getDatabase(context)
                val todo = db.todoDao().getTodoById(todoId) ?: return
                val nowCompleted = !todo.isCompleted
                db.todoDao().toggleTodo(
                    id = todoId,
                    completed = nowCompleted,
                    completedAt = if (nowCompleted) System.currentTimeMillis() else null
                )
                // Refresh widgets
                updateAllWidgets(context)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
