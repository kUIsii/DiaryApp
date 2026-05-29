package com.diary.app.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class BackupFrequency(val label: String, val days: Int) {
    DAILY("每日", 1),
    WEEKLY("每周", 7),
    DISABLED("关闭", 0)
}

data class BackupRecord(
    val fileName: String,
    val filePath: String,
    val timestamp: Long,
    val entryCount: Int,
    val fileSize: Long
)

object BackupManager {

    private const val PREFS_NAME = "diary_backup_prefs"
    private const val KEY_AUTO_BACKUP = "auto_backup_enabled"
    private const val KEY_FREQUENCY = "backup_frequency"
    private const val KEY_LAST_BACKUP = "last_backup_time"
    private const val KEY_BACKUP_HISTORY = "backup_history"
    private const val KEY_MAX_BACKUPS = "max_backups"
    private const val DEFAULT_MAX_BACKUPS = 10

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isAutoBackupEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUTO_BACKUP, false)
    }

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUTO_BACKUP, enabled).apply()
    }

    fun getFrequency(context: Context): BackupFrequency {
        val name = getPrefs(context).getString(KEY_FREQUENCY, BackupFrequency.WEEKLY.name)
        return try {
            BackupFrequency.valueOf(name ?: BackupFrequency.WEEKLY.name)
        } catch (_: Exception) {
            BackupFrequency.WEEKLY
        }
    }

    fun setFrequency(context: Context, frequency: BackupFrequency) {
        getPrefs(context).edit().putString(KEY_FREQUENCY, frequency.name).apply()
    }

    fun getLastBackupTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_BACKUP, 0L)
    }

    fun setLastBackupTime(context: Context, time: Long) {
        getPrefs(context).edit().putLong(KEY_LAST_BACKUP, time).apply()
    }

    fun getMaxBackups(context: Context): Int {
        return getPrefs(context).getInt(KEY_MAX_BACKUPS, DEFAULT_MAX_BACKUPS)
    }

    fun setMaxBackups(context: Context, max: Int) {
        getPrefs(context).edit().putInt(KEY_MAX_BACKUPS, max).apply()
    }

    fun shouldAutoBackup(context: Context): Boolean {
        if (!isAutoBackupEnabled(context)) return false
        val frequency = getFrequency(context)
        if (frequency == BackupFrequency.DISABLED) return false
        val lastBackup = getLastBackupTime(context)
        if (lastBackup == 0L) return true
        val elapsed = System.currentTimeMillis() - lastBackup
        val intervalMs = frequency.days * 24L * 60L * 60L * 1000L
        return elapsed >= intervalMs
    }

    fun getBackupHistory(context: Context): List<BackupRecord> {
        val json = getPrefs(context).getString(KEY_BACKUP_HISTORY, "[]") ?: "[]"
        return try {
            val type = object : TypeToken<List<BackupRecord>>() {}.type
            Gson().fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addBackupRecord(context: Context, record: BackupRecord) {
        val history = getBackupHistory(context).toMutableList()
        history.add(0, record)

        // Enforce max backups: delete oldest files
        val maxBackups = getMaxBackups(context)
        while (history.size > maxBackups) {
            val oldest = history.removeLast()
            try {
                File(oldest.filePath).delete()
            } catch (_: Exception) {}
        }

        saveHistory(context, history)
        setLastBackupTime(context, System.currentTimeMillis())
    }

    fun deleteBackup(context: Context, record: BackupRecord) {
        val history = getBackupHistory(context).toMutableList()
        history.removeAll { it.fileName == record.fileName }
        saveHistory(context, history)
        try {
            File(record.filePath).delete()
        } catch (_: Exception) {}
    }

    suspend fun performAutoBackup(context: Context, dao: DiaryDao): BackupRecord? {
        return try {
            val entries = dao.getAllEntriesOnce()
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val fileName = "auto_backup_$timestamp.json"

            val result = DiaryExporter.export(context, dao)
            val file = getLatestBackupFile(context)

            val record = BackupRecord(
                fileName = fileName,
                filePath = result,
                timestamp = System.currentTimeMillis(),
                entryCount = entries.size,
                fileSize = file?.length() ?: 0L
            )
            addBackupRecord(context, record)
            record
        } catch (_: Exception) {
            null
        }
    }

    private fun getLatestBackupFile(context: Context): File? {
        val dir = getBackupDir(context)
        return dir.listFiles()?.maxByOrNull { it.lastModified() }
    }

    fun getBackupDir(context: Context): File {
        val dir = File(context.filesDir, "backups")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun saveHistory(context: Context, history: List<BackupRecord>) {
        val json = Gson().toJson(history)
        getPrefs(context).edit().putString(KEY_BACKUP_HISTORY, json).apply()
    }

    fun getBackupSize(context: Context): String {
        val history = getBackupHistory(context)
        val totalBytes = history.sumOf { it.fileSize }
        return when {
            totalBytes < 1024 -> "${totalBytes}B"
            totalBytes < 1024 * 1024 -> "${totalBytes / 1024}KB"
            else -> String.format("%.1fMB", totalBytes / (1024.0 * 1024.0))
        }
    }
}
