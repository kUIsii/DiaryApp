package com.diary.app.data

import android.content.Context
import android.content.SharedPreferences
import com.diary.app.BuildConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.content.ContentValues
import java.io.File
import java.io.OutputStream
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

fun normalizeBackupFileName(rawName: String, fallbackBaseName: String = "backup"): String {
    val baseName = rawName
        .trim()
        .removeSuffix(".json")
        .replace(Regex("[\\\\/:*?\"<>|]+"), "-")
        .replace(Regex("\\s+"), "-")
        .replace(Regex("-+"), "-")
        .trim('-', '.')
        .ifBlank { fallbackBaseName }

    return if (baseName.lowercase().endsWith(".json")) baseName else "$baseName.json"
}

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

        val maxBackups = getMaxBackups(context)
        while (history.size > maxBackups) {
            val oldest = history.removeLast()
            runCatching { File(oldest.filePath).delete() }
        }

        saveHistory(context, history)
        setLastBackupTime(context, System.currentTimeMillis())
    }

    fun deleteBackup(context: Context, record: BackupRecord) {
        val history = getBackupHistory(context).toMutableList()
        history.removeAll { it.filePath == record.filePath }
        saveHistory(context, history)
        runCatching { File(record.filePath).delete() }
    }

    fun renameBackup(context: Context, record: BackupRecord, requestedName: String): BackupRecord {
        val sourceFile = File(record.filePath)
        require(sourceFile.exists()) { "Backup file does not exist" }

        val targetName = normalizeBackupFileName(requestedName)
        val targetFile = File(sourceFile.parentFile ?: getBackupDir(context), targetName)
        require(
            sourceFile.absolutePath == targetFile.absolutePath || !targetFile.exists()
        ) { "Backup with the same name already exists" }

        if (sourceFile.absolutePath != targetFile.absolutePath) {
            check(sourceFile.renameTo(targetFile)) { "Failed to rename backup file" }
        }

        val updated = record.copy(
            fileName = targetName,
            filePath = targetFile.absolutePath,
            fileSize = targetFile.length()
        )
        val history = getBackupHistory(context).map {
            if (it.filePath == record.filePath) updated else it
        }
        saveHistory(context, history)
        return updated
    }

    suspend fun createBackup(context: Context, dao: DiaryDao): BackupRecord {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val file = File(getBackupDir(context), "diary_backup_$timestamp.json")
        val json = buildBackupJson(dao)
        file.parentFile?.mkdirs()
        file.writeText(json, Charsets.UTF_8)

        val record = BackupRecord(
            fileName = file.name,
            filePath = file.absolutePath,
            timestamp = System.currentTimeMillis(),
            entryCount = dao.getEntryCount(),
            fileSize = file.length()
        )
        addBackupRecord(context, record)
        return record
    }

    suspend fun performAutoBackup(context: Context, dao: DiaryDao): BackupRecord? {
        return runCatching { createBackup(context, dao) }.getOrNull()
    }

    fun getBackupDir(context: Context): File {
        val dir = File(context.filesDir, "backups")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private suspend fun buildBackupJson(dao: DiaryDao): String {
        val entries = mutableListOf<DiaryEntry>()
        var offset = 0
        val batchSize = 50
        while (true) {
            val batch = dao.getEntriesBatchForExport(offset, batchSize)
            if (batch.isEmpty()) break
            entries.addAll(batch)
            offset += batchSize
        }

        val tags = dao.getAllTagsOnce()
        val allDiaryTags = dao.getAllDiaryTags()
        val tagMap = tags.associateBy { it.id }
        val diaryTagMap = allDiaryTags.groupBy({ it.diaryId }, { tagMap[it.tagId]?.name ?: "" })

        val payload = DiaryBackup(
            app = "DiaryApp",
            version = BuildConfig.VERSION_NAME,
            exportDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")),
            entries = entries.map { entry ->
                BackupEntry(
                    title = entry.title,
                    content = normalizeContentForExport(entry.content),
                    plainText = entry.plainText,
                    moodLevel = entry.moodLevel,
                    weather = entry.weather,
                    location = entry.location,
                    latitude = entry.latitude,
                    longitude = entry.longitude,
                    tags = diaryTagMap[entry.id] ?: emptyList(),
                    createdAt = entry.createdAt,
                    updatedAt = entry.updatedAt
                )
            },
            tags = tags.map { tag ->
                BackupTag(
                    name = tag.name,
                    color = tag.color,
                    isPreset = tag.isPreset
                )
            }
        )

        return GsonBuilder().setPrettyPrinting().create().toJson(payload)
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

    /**
     * 导出备份到公共 Downloads 目录，用户可在文件管理器中找到。
     * 返回保存的文件名。
     */
    suspend fun exportToDownloads(context: Context, dao: DiaryDao): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val fileName = "diary_backup_$timestamp.json"
        val json = buildBackupJson(dao)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/json")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw Exception("无法创建文件")
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            } ?: throw Exception("无法写入文件")
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val file = File(downloadsDir, fileName)
            file.writeText(json, Charsets.UTF_8)
        }

        return fileName
    }

    /**
     * 从内部备份记录导出到 Downloads。如果备份文件还存在，直接复制；否则重新生成。
     */
    suspend fun exportRecordToDownloads(context: Context, record: BackupRecord, dao: DiaryDao): String {
        val sourceFile = File(record.filePath)
        val fileName = record.fileName

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/json")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw Exception("无法创建文件")
            context.contentResolver.openOutputStream(uri)?.use { out ->
                if (sourceFile.exists()) {
                    sourceFile.inputStream().use { it.copyTo(out) }
                } else {
                    // 文件已被删除，重新生成
                    val json = buildBackupJson(dao)
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
            } ?: throw Exception("无法写入文件")
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val targetFile = File(downloadsDir, fileName)
            if (sourceFile.exists()) {
                sourceFile.copyTo(targetFile, overwrite = true)
            } else {
                val json = buildBackupJson(dao)
                targetFile.writeText(json, Charsets.UTF_8)
            }
        }

        return fileName
    }
}
