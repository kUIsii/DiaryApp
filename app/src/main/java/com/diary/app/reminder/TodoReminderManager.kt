package com.diary.app.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
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
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TodoReminderManager {
    const val CHANNEL_ID = "todo_reminder"
    const val CHANNEL_SUMMARY_ID = "todo_summary"
    private const val CHANNEL_NAME = "待办提醒"
    private const val CHANNEL_SUMMARY_NAME = "每日摘要"

    private const val DAILY_SUMMARY_REQUEST_CODE = 99999

    fun createNotificationChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Individual reminder channel
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "待办事项提醒通知"
        }
        manager.createNotificationChannel(channel)

        // Daily summary channel
        val summaryChannel = NotificationChannel(
            CHANNEL_SUMMARY_ID,
            CHANNEL_SUMMARY_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "每日待办摘要通知"
        }
        manager.createNotificationChannel(summaryChannel)
    }

    fun scheduleReminder(context: Context, todoId: Long, title: String, reminderTimeMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TodoReminderReceiver::class.java).apply {
            putExtra("todo_id", todoId)
            putExtra("todo_title", title)
            action = "com.diary.app.TODO_REMINDER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            todoId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminderTimeMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTimeMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            // Fallback to inexact alarm
            alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent)
        }
    }

    fun cancelReminder(context: Context, todoId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TodoReminderReceiver::class.java).apply {
            action = "com.diary.app.TODO_REMINDER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            todoId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun scheduleDailySummary(context: Context, hour: Int = 8, minute: Int = 0) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailySummaryReceiver::class.java).apply {
            action = "com.diary.app.DAILY_SUMMARY"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_SUMMARY_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule for today or tomorrow at the specified time
        val now = LocalDateTime.now()
        var targetTime = now.toLocalDate().atTime(LocalTime.of(hour, minute))
        if (now.isAfter(targetTime)) {
            targetTime = targetTime.plusDays(1)
        }
        val targetMillis = targetTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        targetMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    targetMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, targetMillis, pendingIntent)
        }
    }

    fun cancelDailySummary(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailySummaryReceiver::class.java).apply {
            action = "com.diary.app.DAILY_SUMMARY"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_SUMMARY_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun rescheduleAllPendingReminders(context: Context) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            val db = DiaryDatabase.getDatabase(context)
            val todos = db.diaryDao().getPendingReminderTodos()
            todos.forEach { todo ->
                todo.reminderTime?.let { time ->
                    if (time > System.currentTimeMillis()) {
                        scheduleReminder(context, todo.id, todo.title, time)
                    }
                }
            }
        }
    }

    fun showDailySummary(context: Context) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val db = DiaryDatabase.getDatabase(context)
                val today = LocalDate.now()
                val dayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val dayEnd = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

                val allPending = db.diaryDao().getTopPendingTodos(100)
                val todayTodos = allPending.filter { todo ->
                    todo.dueDate != null && todo.dueDate >= dayStart && todo.dueDate < dayEnd
                }
                val todayCount = todayTodos.size
                val pendingCount = allPending.count { !it.isCompleted }

                if (pendingCount == 0) return@launch

                val tapIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("navigate_to", "todo")
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    tapIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val summaryText = if (todayCount > 0) {
                    "今日 $todayCount 项待办，共 $pendingCount 项未完成"
                } else {
                    "共 $pendingCount 项待办未完成"
                }

                // Build inbox style with top todos
                val inboxStyle = NotificationCompat.InboxStyle()
                    .setBigContentTitle("每日待办摘要")
                    .setSummaryText(summaryText)

                allPending.take(5).forEach { todo ->
                    val prefix = when (todo.priority) {
                        1 -> "⚡ "
                        2 -> "🔥 "
                        else -> "• "
                    }
                    inboxStyle.addLine("$prefix${todo.title}")
                }
                if (allPending.size > 5) {
                    inboxStyle.addLine("还有 ${allPending.size - 5} 项...")
                }

                val notification = NotificationCompat.Builder(context, CHANNEL_SUMMARY_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("每日待办摘要")
                    .setContentText(summaryText)
                    .setStyle(inboxStyle)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()

                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(DAILY_SUMMARY_REQUEST_CODE, notification)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}

class TodoReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val todoId = intent.getLongExtra("todo_id", -1)
        val title = intent.getStringExtra("todo_title") ?: "待办提醒"

        if (todoId == -1L) return

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "todo")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            todoId.toInt(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze action
        val snoozeIntent = Intent(context, SnoozeReceiver::class.java).apply {
            putExtra("todo_id", todoId)
            putExtra("todo_title", title)
            action = "com.diary.app.TODO_SNOOZE"
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            todoId.toInt() + 100000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Complete action
        val completeIntent = Intent(context, CompleteReceiver::class.java).apply {
            putExtra("todo_id", todoId)
            action = "com.diary.app.TODO_COMPLETE"
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            todoId.toInt() + 200000,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

        val notification = NotificationCompat.Builder(context, TodoReminderManager.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("待办提醒")
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText("[$timeStr] $title"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .addAction(0, "延迟15分钟", snoozePendingIntent)
            .addAction(0, "标记完成", completePendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(todoId.toInt(), notification)
    }
}

class SnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val todoId = intent.getLongExtra("todo_id", -1)
        val title = intent.getStringExtra("todo_title") ?: "待办提醒"

        if (todoId == -1L) return

        // Cancel current notification
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(todoId.toInt())

        // Schedule new reminder in 15 minutes
        val snoozeTime = System.currentTimeMillis() + 15 * 60 * 1000
        TodoReminderManager.scheduleReminder(context, todoId, title, snoozeTime)
    }
}

class CompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val todoId = intent.getLongExtra("todo_id", -1)

        if (todoId == -1L) return

        // Cancel notification
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(todoId.toInt())

        // Mark todo as completed
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val db = DiaryDatabase.getDatabase(context)
                db.diaryDao().toggleTodo(
                    id = todoId,
                    completed = true,
                    completedAt = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}

class DailySummaryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Show daily summary
        TodoReminderManager.showDailySummary(context)

        // Reschedule for tomorrow
        TodoReminderManager.scheduleDailySummary(context)
    }
}
