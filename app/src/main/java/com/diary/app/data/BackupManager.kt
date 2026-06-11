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

    // 确保以 diary_backup_ 开头，这样导入扫描能找到
    val prefixed = if (baseName.startsWith("diary_backup_")) baseName else "diary_backup_$baseName"
    return "$prefixed.json"
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
        deleteDownloadsFile(context, record.fileName)
    }

    fun renameBackup(context: Context, record: BackupRecord, requestedName: String): BackupRecord {
        val targetName = normalizeBackupFileName(requestedName)

        // 读取原文件内容
        val content = readFromDownloads(context, record.fileName)
            ?: throw Exception("备份文件不存在")

        // 删除旧文件
        deleteDownloadsFile(context, record.fileName)

        // 创建新文件
        val dataBytes = content.toByteArray(Charsets.UTF_8)
        val newFilePath = writeToDownloads(context, targetName, dataBytes)

        val updated = record.copy(
            fileName = targetName,
            filePath = newFilePath,
            fileSize = dataBytes.size.toLong()
        )
        val history = getBackupHistory(context).map {
            if (it.filePath == record.filePath) updated else it
        }
        saveHistory(context, history)
        return updated
    }

    private fun readFromDownloads(context: Context, fileName: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val uri = findDownloadsUri(context, fileName) ?: return null
                context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
            } else {
                @Suppress("DEPRECATION")
                val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                if (file.exists()) file.readText() else null
            }
        } catch (_: Exception) { null }
    }

    private fun deleteDownloadsFile(context: Context, fileName: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val uri = findDownloadsUri(context, fileName)
                if (uri != null) context.contentResolver.delete(uri, null, null)
            } else {
                @Suppress("DEPRECATION")
                val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                if (file.exists()) file.delete()
            }
        } catch (_: Exception) {}
    }

    private fun writeToDownloads(context: Context, fileName: String, data: ByteArray): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/json")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw Exception("无法创建文件")
            context.contentResolver.openOutputStream(uri)?.use { it.write(data) }
                ?: throw Exception("无法写入文件")
            return queryFilePath(context, uri) ?: "${Environment.DIRECTORY_DOWNLOADS}/$fileName"
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName)
            file.writeBytes(data)
            return file.absolutePath
        }
    }

    private fun findDownloadsUri(context: Context, fileName: String): android.net.Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val projection = arrayOf(MediaStore.Downloads._ID)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
        return context.contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI, projection, selection, arrayOf(fileName), null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(0)
                android.net.Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id.toString())
            } else null
        }
    }

    suspend fun createBackup(context: Context, dao: DiaryDao): BackupRecord {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val fileName = "diary_backup_$timestamp.json"
        val json = buildBackupJson(dao)
        val dataBytes = json.toByteArray(Charsets.UTF_8)

        val filePath: String
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/json")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw Exception("无法创建文件")
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(dataBytes)
            } ?: throw Exception("无法写入文件")
            // 通过 query 获取实际路径
            filePath = queryFilePath(context, uri) ?: "${Environment.DIRECTORY_DOWNLOADS}/$fileName"
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val file = File(downloadsDir, fileName)
            file.writeText(json, Charsets.UTF_8)
            filePath = file.absolutePath
        }

        val record = BackupRecord(
            fileName = fileName,
            filePath = filePath,
            timestamp = System.currentTimeMillis(),
            entryCount = dao.getEntryCount(),
            fileSize = dataBytes.size.toLong()
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

    private fun queryFilePath(context: Context, uri: android.net.Uri): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val projection = arrayOf(MediaStore.Downloads.DATA)
        return context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(MediaStore.Downloads.DATA)
                if (idx >= 0) cursor.getString(idx) else null
            } else null
        }
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

    data class DownloadBackupFile(
        val fileName: String,
        val fileSize: Long,
        val lastModified: Long
    )

    /**
     * 扫描 Downloads 目录中所有的 diary_backup_*.json 文件
     */
    fun scanDownloadsBackups(context: Context): List<DownloadBackupFile> {
        val results = mutableListOf<DownloadBackupFile>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val projection = arrayOf(
                MediaStore.Downloads.DISPLAY_NAME,
                MediaStore.Downloads.SIZE,
                MediaStore.Downloads.DATE_MODIFIED
            )
            val selection = "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("diary_backup_%")
            context.contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs,
                "${MediaStore.Downloads.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(MediaStore.Downloads.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(MediaStore.Downloads.SIZE)
                val dateIdx = cursor.getColumnIndex(MediaStore.Downloads.DATE_MODIFIED)
                while (cursor.moveToNext()) {
                    results.add(DownloadBackupFile(
                        fileName = cursor.getString(nameIdx),
                        fileSize = cursor.getLong(sizeIdx),
                        lastModified = cursor.getLong(dateIdx) * 1000
                    ))
                }
            }
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (dir.exists()) {
                dir.listFiles()?.filter { it.name.startsWith("diary_backup_") && it.name.endsWith(".json") }
                    ?.sortedByDescending { it.lastModified() }
                    ?.forEach {
                        results.add(DownloadBackupFile(it.name, it.length(), it.lastModified()))
                    }
            }
        }
        return results
    }

    /**
     * 读取 Downloads 中某个备份文件的内容
     */
    fun readDownloadBackup(context: Context, fileName: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val uri = findDownloadsUri(context, fileName) ?: return null
                context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
            } else {
                @Suppress("DEPRECATION")
                val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                if (file.exists()) file.readText() else null
            }
        } catch (_: Exception) { null }
    }

}
