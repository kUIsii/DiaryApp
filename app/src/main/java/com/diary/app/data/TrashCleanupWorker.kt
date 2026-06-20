package com.diary.app.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class TrashCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = DiaryDatabase.getDatabase(applicationContext)
            val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
            db.diaryDao().deleteTrashEntriesBefore(cutoff)
            Log.i("TrashCleanupWorker", "Trash cleanup completed, cutoff=$cutoff")
            Result.success()
        } catch (e: Exception) {
            Log.w("TrashCleanupWorker", "Trash cleanup failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "trash_cleanup"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<TrashCleanupWorker>(1, TimeUnit.DAYS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
