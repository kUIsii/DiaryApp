package com.diary.app.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class BackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            if (!BackupManager.shouldAutoBackup(applicationContext)) {
                return Result.success()
            }
            val db = DiaryDatabase.getDatabase(applicationContext)
            BackupManager.performAutoBackup(applicationContext, db)
            Result.success()
        } catch (e: Exception) {
            Log.w("BackupWorker", "Auto backup failed", e)
            Result.retry()
        }
    }
}
