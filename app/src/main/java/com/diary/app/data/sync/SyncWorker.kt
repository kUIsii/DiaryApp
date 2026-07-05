package com.diary.app.data.sync

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.diary.app.DiaryApplication
import com.diary.app.data.auth.AuthManager
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

class SyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    companion object {
        private const val WORK_NAME = "diary_sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(2, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }

        fun syncOnce(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }

    override fun doWork(): Result {
        val app = applicationContext as DiaryApplication
        val authManager = AuthManager(applicationContext)
        if (!authManager.isLoggedIn) return Result.success()

        return try {
            return runBlocking {
                val dao = app.database.diaryDao()
                val syncManager = CloudSyncManager(applicationContext)

                val entries = dao.getAllEntriesOnce()
                val tags = dao.getAllTagsOnce()
                val diaryTags = dao.getAllDiaryTags()
                val todos = dao.getAllTodosOnce()
                val images = dao.getAllImages()
                val habits = dao.getAllHabitRecordsOnce()
                val countdowns = dao.getAllCountDownItemsOnce()
                val capsules = dao.getAllCapsulesOnce()
                val trash = dao.getAllTrashEntriesOnce()
                val notifications = dao.getAllNotificationsOnce()
                val conversations = dao.getAllConversationsOnce()
                val messages = dao.getAllChatMessagesOnce()

                val payload = mapOf(
                    "version" to 2,
                    "syncMeta" to mapOf(
                        "deviceId" to "android-${android.os.Build.MODEL ?: "phone"}",
                        "exportedAt" to System.currentTimeMillis()
                    ),
                    "diaries" to entries.map { e -> mapOf(
                        "id" to e.id, "title" to e.title, "content" to e.content,
                        "plainText" to e.plainText, "moodLevel" to e.moodLevel,
                        "weather" to e.weather, "location" to e.location,
                        "latitude" to e.latitude, "longitude" to e.longitude,
                        "isFavorite" to e.isFavorite, "createdAt" to e.createdAt,
                        "updatedAt" to e.updatedAt, "writingDurationSeconds" to e.writingDurationSeconds
                    )},
                    "tags" to tags.map { t -> mapOf(
                        "id" to t.id, "name" to t.name, "color" to t.color, "isPreset" to t.isPreset
                    )},
                    "diaryTags" to diaryTags.map { dt -> mapOf("diaryId" to dt.diaryId, "tagId" to dt.tagId) },
                    "tasks" to todos.map { t -> mapOf(
                        "id" to t.id, "title" to t.title, "description" to t.description,
                        "isCompleted" to t.isCompleted, "priority" to t.priority,
                        "dueDate" to t.dueDate, "createdAt" to t.createdAt,
                        "completedAt" to t.completedAt, "sortOrder" to t.sortOrder,
                        "category" to t.category, "reminderTime" to t.reminderTime,
                        "tags" to t.tags, "parentId" to t.parentId,
                        "recurringType" to t.recurringType, "progress" to t.progress,
                        "isPinned" to t.isPinned, "linkedTagIds" to t.linkedTagIds
                    )},
                    "images" to images.map { img -> mapOf(
                        "id" to img.id, "entryId" to img.entryId,
                        "localPath" to img.localPath, "thumbPath" to img.thumbPath,
                        "mediaName" to img.mediaName, "mediaRef" to img.mediaRef,
                        "mimeType" to img.mimeType, "fileSize" to img.fileSize,
                        "sortOrder" to img.sortOrder, "createdAt" to img.createdAt
                    )},
                    "habits" to habits.map { h -> mapOf(
                        "id" to h.id, "todoId" to h.todoId, "recordDate" to h.recordDate,
                        "source" to h.source, "summary" to h.summary,
                        "diaryEntryId" to h.diaryEntryId, "createdAt" to h.createdAt,
                        "updatedAt" to h.updatedAt
                    )},
                    "countdowns" to countdowns.map { c -> mapOf(
                        "id" to c.id, "title" to c.title, "targetDate" to c.targetDate,
                        "isCountUp" to c.isCountUp, "color" to c.color,
                        "isRepeatYearly" to c.isRepeatYearly, "isPinned" to c.isPinned,
                        "createdAt" to c.createdAt
                    )},
                    "capsules" to capsules.map { c -> mapOf(
                        "id" to c.id, "title" to c.title, "content" to c.content,
                        "createdAt" to c.createdAt, "unlockDate" to c.unlockDate,
                        "isRead" to c.isRead, "isOpened" to c.isOpened,
                        "theme" to c.theme, "imageUri" to c.imageUri,
                        "unlockHour" to c.unlockHour, "unlockMinute" to c.unlockMinute
                    )},
                    "trash" to trash.map { t -> mapOf(
                        "id" to t.id, "originalId" to t.originalId, "title" to t.title,
                        "content" to t.content, "plainText" to t.plainText,
                        "moodLevel" to t.moodLevel, "weather" to t.weather,
                        "location" to t.location, "latitude" to t.latitude,
                        "longitude" to t.longitude, "isFavorite" to t.isFavorite,
                        "createdAt" to t.createdAt, "updatedAt" to t.updatedAt,
                        "deletedAt" to t.deletedAt
                    )},
                    "notifications" to notifications.map { n -> mapOf(
                        "id" to n.id, "type" to n.type, "title" to n.title,
                        "subtitle" to n.subtitle, "iconType" to n.iconType,
                        "colorHex" to n.colorHex, "relatedId" to n.relatedId,
                        "isRead" to n.isRead, "isTrashed" to n.isTrashed,
                        "createdAt" to n.createdAt, "trashedAt" to n.trashedAt
                    )},
                    "conversations" to conversations.map { c -> mapOf(
                        "id" to c.id, "title" to c.title, "createdAt" to c.createdAt,
                        "updatedAt" to c.updatedAt
                    )},
                    "chatMessages" to messages.map { m -> mapOf(
                        "id" to m.id, "role" to m.role, "content" to m.content,
                        "createdAt" to m.createdAt, "conversationId" to m.conversationId
                    )}
                )

                syncManager.pushBackup(payload)
                Result.success()
            }
        } catch (e: Exception) {
            android.util.Log.w("SyncWorker", "Auto sync failed", e)
            Result.retry()
        }
    }
}
