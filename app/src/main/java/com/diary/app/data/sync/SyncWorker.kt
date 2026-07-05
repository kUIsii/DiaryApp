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
                val allTodos = dao.getAllTodosOnce()
                val payload = mapOf(
                    "version" to 1,
                    "syncMeta" to mapOf(
                        "deviceId" to "android-${android.os.Build.MODEL ?: "phone"}",
                        "exportedAt" to System.currentTimeMillis()
                    ),
                    "summary" to mapOf(
                        "total" to allTodos.size,
                        "completed" to allTodos.count { it.isCompleted },
                        "active" to allTodos.count { !it.isCompleted }
                    ),
                    "tasks" to allTodos.map { task ->
                        mapOf(
                            "id" to task.id,
                            "title" to task.title,
                            "completed" to task.isCompleted,
                            "priority" to task.priority,
                            "dueDate" to task.dueDate,
                            "tags" to com.diary.app.data.TodoItem.getTagList(task.tags),
                            "category" to task.category,
                            "updatedAt" to (task.completedAt ?: task.createdAt)
                        )
                    }
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
