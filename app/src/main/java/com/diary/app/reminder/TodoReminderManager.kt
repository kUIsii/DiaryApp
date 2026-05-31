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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TodoReminderManager {
    const val CHANNEL_ID = "todo_reminder"
    private const val CHANNEL_NAME = "待办提醒"

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "待办事项提醒通知"
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
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

        val timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

        val notification = NotificationCompat.Builder(context, TodoReminderManager.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("待办提醒")
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText("[$timeStr] $title"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(todoId.toInt(), notification)
    }
}
