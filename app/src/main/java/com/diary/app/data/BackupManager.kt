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
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import android.util.Log
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class BackupFrequency(val label: String, val days: Int) {
    DAILY("每日", 1),
    EVERY_3_DAYS("每3天", 3),
    EVERY_5_DAYS("每5天", 5),
    WEEKLY("每周", 7),
    BIWEEKLY("每两周", 14),
    MONTHLY("每月", 30),
    DISABLED("关闭", 0)
}

data class BackupRecord(
    val fileName: String,
    val filePath: String,
    val timestamp: Long,
    val entryCount: Int,
    val fileSize: Long
)

private const val FULL_BACKUP_EXTENSION = ".diarybackup"
internal val BACKUP_SCAN_PREFIXES = listOf("diary_backup_", "日记备份_")
internal val BACKUP_SCAN_EXTENSIONS = listOf(".json", FULL_BACKUP_EXTENSION)

fun normalizeBackupFileName(rawName: String, fallbackBaseName: String = "backup"): String {
    val baseName = rawName
        .trim()
        .removeSuffix(FULL_BACKUP_EXTENSION)
        .removeSuffix(".json")
        .replace(Regex("[\\\\/:*?\"<>|]+"), "-")
        .replace(Regex("\\s+"), "-")
        .replace(Regex("-+"), "-")
        .trim('-', '.')
        .ifBlank { fallbackBaseName }

    // 确保以备份前缀开头，这样导入扫描能找到
    val prefixed = if (BACKUP_SCAN_PREFIXES.any { baseName.startsWith(it) }) baseName else "diary_backup_$baseName"
    return "$prefixed$FULL_BACKUP_EXTENSION"
}

object BackupManager {

    private const val PREFS_NAME = "diary_backup_prefs"
    private const val KEY_AUTO_BACKUP = "auto_backup_enabled"
    private const val KEY_FREQUENCY = "backup_frequency"
    private const val KEY_LAST_BACKUP = "last_backup_time"
    private const val KEY_BACKUP_HISTORY = "backup_history"
    private const val KEY_MAX_BACKUPS = "max_backups"
    private const val DEFAULT_MAX_BACKUPS = 10
    private const val BACKUP_DIR_NAME = "DiaryApp"
    private const val BACKUP_JSON_NAME = "backup.json"

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
            Log.w("BackupManager", "Failed to parse backup frequency, using default")
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

    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true // Android 10 以下通过 WRITE_EXTERNAL_STORAGE 处理
        }
    }

    fun getBackupDir(): File {
        return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), BACKUP_DIR_NAME)
    }

    fun createBackupDir(): File {
        val dir = getBackupDir()
        if (!dir.exists()) dir.mkdirs()
        return dir
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
            Log.w("BackupManager", "Failed to parse backup history")
            emptyList()
        }
    }

    fun addBackupRecord(context: Context, record: BackupRecord) {
        val history = getBackupHistory(context).toMutableList()
        history.add(0, record)
        saveHistory(context, history)
        setLastBackupTime(context, System.currentTimeMillis())
    }

    fun deleteBackup(context: Context, record: BackupRecord) {
        val history = getBackupHistory(context).toMutableList()
        history.removeAll { it.filePath == record.filePath }
        saveHistory(context, history)
        // 从备份目录删除
        val file = File(getBackupDir(), record.fileName)
        if (file.exists()) file.delete()
        // 兼容旧版 Downloads 中的文件
        deleteDownloadsFile(context, record.fileName)
    }

    fun renameBackup(context: Context, record: BackupRecord, requestedName: String): BackupRecord {
        val targetName = normalizeBackupFileName(requestedName)
        val originalPackageBytes = readBackupPackageBytes(context, record.fileName)

        // 读取原文件内容
        val content = readBackupJsonFromBackupDir(record.fileName)
            ?: readFromDownloads(context, record.fileName)
            ?: throw Exception("备份文件不存在")

        // 删除旧文件
        File(getBackupDir(), record.fileName).let { if (it.exists()) it.delete() }
        deleteDownloadsFile(context, record.fileName)

        // 创建新文件到备份目录
        val dataBytes = if (targetName.endsWith(FULL_BACKUP_EXTENSION)) {
            originalPackageBytes ?: buildFullBackupBytesFromJson(content)
        } else {
            content.toByteArray(Charsets.UTF_8)
        }
        val dir = createBackupDir()
        val newFile = File(dir, targetName)
        newFile.writeBytes(dataBytes)

        val updated = record.copy(
            fileName = targetName,
            filePath = newFile.absolutePath,
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
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    if (fileName.endsWith(FULL_BACKUP_EXTENSION)) {
                        readBackupJsonFromZip(stream)
                    } else {
                        stream.bufferedReader().readText()
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                readBackupJsonFromFile(file)
            }
        } catch (_: Exception) {
            Log.w("BackupManager", "Failed to read from downloads: $fileName")
            null
        }
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
        } catch (_: Exception) {
            Log.w("BackupManager", "Failed to delete downloads file: $fileName")
        }
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
        val now = LocalDateTime.now()
        val dateStr = now.format(DateTimeFormatter.ofPattern("yyyy年M月d日"))
        val timeStr = now.format(DateTimeFormatter.ofPattern("HHmmss"))
        val fileName = "日记备份_${dateStr}_${timeStr}$FULL_BACKUP_EXTENSION"
        val json = buildBackupJson(dao)
        val tempFile = buildFullBackupFile(context, dao, json)

        try {
            val fileSize = tempFile.length()
            val filePath: String
            if (hasStoragePermission()) {
                val dir = createBackupDir()
                val file = File(dir, fileName)
                tempFile.copyTo(file, overwrite = true)
                filePath = file.absolutePath
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw Exception("无法创建文件")
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    tempFile.inputStream().use { it.copyTo(out) }
                } ?: throw Exception("无法写入文件")
                filePath = queryFilePath(context, uri) ?: "${Environment.DIRECTORY_DOWNLOADS}/$fileName"
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val file = File(downloadsDir, fileName)
                tempFile.copyTo(file, overwrite = true)
                filePath = file.absolutePath
            }

            val record = BackupRecord(
                fileName = fileName,
                filePath = filePath,
                timestamp = System.currentTimeMillis(),
                entryCount = dao.getEntryCount(),
                fileSize = fileSize
            )
            addBackupRecord(context, record)
            return record
        } finally {
            tempFile.delete()
        }
    }

    private fun buildFullBackupBytesFromJson(json: String): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(BACKUP_JSON_NAME))
            zip.write(json.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return output.toByteArray()
    }

    private fun readBackupPackageBytes(context: Context, fileName: String): ByteArray? {
        if (!fileName.endsWith(FULL_BACKUP_EXTENSION)) return null
        return try {
            val backupFile = File(getBackupDir(), fileName)
            if (backupFile.exists()) {
                backupFile.readBytes()
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val uri = findDownloadsUri(context, fileName) ?: return null
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } else {
                @Suppress("DEPRECATION")
                val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                if (file.exists()) file.readBytes() else null
            }
        } catch (_: Exception) {
            Log.w("BackupManager", "Failed to read backup package bytes: $fileName")
            null
        }
    }

    /**
     * Build a full backup package by streaming to a temp file on disk,
     * avoiding OOM when media files are large.
     * Caller is responsible for deleting the returned temp file.
     */
    private suspend fun buildFullBackupFile(context: Context, dao: DiaryDao, json: String): File {
        val tempFile = File(context.cacheDir, "backup_temp_${System.currentTimeMillis()}.zip")
        try {
            java.io.FileOutputStream(tempFile).use { fos ->
                ZipOutputStream(fos).use { zip ->
                    zip.putNextEntry(ZipEntry(BACKUP_JSON_NAME))
                    zip.write(json.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()

                    val mediaNames = referencedMediaNames(dao)
                    for (mediaName in mediaNames) {
                        val displayFile = File(DiaryMediaManager.mediaDir(context), mediaName)
                        if (displayFile.exists() && displayFile.isFile) {
                            zip.putNextEntry(ZipEntry("${DiaryMediaManager.MEDIA_DIR_NAME}/$mediaName"))
                            displayFile.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                        val thumbFile = File(DiaryMediaManager.thumbDir(context), mediaName)
                        if (thumbFile.exists() && thumbFile.isFile) {
                            zip.putNextEntry(ZipEntry("${DiaryMediaManager.MEDIA_DIR_NAME}/${DiaryMediaManager.THUMB_DIR_NAME}/$mediaName"))
                            thumbFile.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        }
        return tempFile
    }

    private suspend fun referencedMediaNames(dao: DiaryDao): Set<String> {
        val names = linkedSetOf<String>()
        dao.getAllImages().mapNotNullTo(names) { it.mediaName.takeIf(String::isNotBlank) }
        var offset = 0
        val batchSize = 50
        while (true) {
            val batch = dao.getEntriesBatchForExport(offset, batchSize)
            if (batch.isEmpty()) break
            batch.forEach { entry ->
                names.addAll(DiaryMediaManager.extractMediaNames(entry.content))
            }
            offset += batchSize
        }
        return names
    }

    suspend fun performAutoBackup(context: Context, dao: DiaryDao): BackupRecord? {
        return runCatching { createBackup(context, dao) }.getOrNull()
    }

    private const val WORK_NAME = "auto_backup_periodic"

    fun scheduleAutoBackup(context: Context, replace: Boolean = false) {
        val frequency = getFrequency(context)
        if (!isAutoBackupEnabled(context) || frequency == BackupFrequency.DISABLED) {
            cancelAutoBackup(context)
            return
        }
        val intervalMinutes = frequency.days * 24L * 60L
        val request = PeriodicWorkRequestBuilder<BackupWorker>(intervalMinutes, TimeUnit.MINUTES)
            .build()
        val policy = if (replace) {
            ExistingPeriodicWorkPolicy.REPLACE
        } else {
            ExistingPeriodicWorkPolicy.KEEP
        }
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, policy, request)
    }

    fun cancelAutoBackup(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
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

        val todos = dao.getAllTodosOnce()
        val countdowns = dao.getAllCountDownItemsOnce()
        val capsules = dao.getAllCapsulesOnce()
        val trash = dao.getAllTrashEntriesOnce()
        val habitRecords = dao.getAllHabitRecordsOnce()
        val notifications = dao.getAllNotificationsOnce()
        val conversations = dao.getAllConversationsOnce()
        val chatMessages = dao.getAllChatMessagesOnce()

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
            },
            todos = todos.map { todo ->
                BackupTodo(
                    id = todo.id,
                    title = todo.title,
                    description = todo.description,
                    isCompleted = todo.isCompleted,
                    priority = todo.priority,
                    dueDate = todo.dueDate,
                    createdAt = todo.createdAt,
                    completedAt = todo.completedAt,
                    sortOrder = todo.sortOrder,
                    category = todo.category,
                    reminderTime = todo.reminderTime,
                    tags = todo.tags,
                    parentId = todo.parentId,
                    recurringType = todo.recurringType,
                    progress = todo.progress,
                    isPinned = todo.isPinned,
                    linkedTagIds = todo.linkedTagIds
                )
            },
            countdowns = countdowns.map { item ->
                BackupCountDown(
                    title = item.title,
                    targetDate = item.targetDate,
                    isCountUp = item.isCountUp,
                    color = item.color,
                    isRepeatYearly = item.isRepeatYearly,
                    isPinned = item.isPinned,
                    createdAt = item.createdAt
                )
            },
            capsules = capsules.map { capsule ->
                BackupCapsule(
                    id = capsule.id,
                    title = capsule.title,
                    content = capsule.content,
                    createdAt = capsule.createdAt,
                    unlockDate = capsule.unlockDate,
                    isRead = capsule.isRead,
                    isOpened = capsule.isOpened,
                    theme = capsule.theme.name,
                    imageUri = capsule.imageUri,
                    unlockHour = capsule.unlockHour,
                    unlockMinute = capsule.unlockMinute
                )
            },
            trash = trash.map { entry ->
                BackupTrashEntry(
                    originalId = entry.originalId,
                    title = entry.title,
                    content = entry.content,
                    plainText = entry.plainText,
                    moodLevel = entry.moodLevel,
                    weather = entry.weather,
                    location = entry.location,
                    latitude = entry.latitude,
                    longitude = entry.longitude,
                    isFavorite = entry.isFavorite,
                    createdAt = entry.createdAt,
                    updatedAt = entry.updatedAt,
                    deletedAt = entry.deletedAt
                )
            },
            habitRecords = habitRecords.map { record ->
                BackupHabitRecord(
                    todoId = record.todoId,
                    recordDate = record.recordDate,
                    source = record.source,
                    summary = record.summary,
                    diaryEntryId = record.diaryEntryId,
                    createdAt = record.createdAt,
                    updatedAt = record.updatedAt
                )
            },
            notifications = notifications.map { notification ->
                BackupNotification(
                    id = notification.id,
                    type = notification.type,
                    title = notification.title,
                    subtitle = notification.subtitle,
                    iconType = notification.iconType,
                    colorHex = notification.colorHex,
                    relatedId = notification.relatedId,
                    isRead = notification.isRead,
                    isTrashed = notification.isTrashed,
                    createdAt = notification.createdAt,
                    trashedAt = notification.trashedAt
                )
            },
            chatConversations = conversations.map { conversation ->
                BackupChatConversation(
                    id = conversation.id,
                    title = conversation.title,
                    createdAt = conversation.createdAt,
                    updatedAt = conversation.updatedAt
                )
            },
            chatMessages = chatMessages.map { message ->
                BackupChatMessage(
                    id = message.id,
                    conversationId = message.conversationId,
                    role = message.role,
                    content = message.content,
                    createdAt = message.createdAt
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
     * 扫描 Downloads 目录中所有的备份文件。
     */
    fun scanImportableBackupFiles(context: Context): List<DownloadBackupFile> {
        val results = mutableListOf<DownloadBackupFile>()
        scanBackupDir(context).forEach { file ->
            if (results.none { it.fileName == file.name }) {
                results.add(DownloadBackupFile(file.name, file.length(), file.lastModified()))
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val projection = arrayOf(
                MediaStore.Downloads.DISPLAY_NAME,
                MediaStore.Downloads.SIZE,
                MediaStore.Downloads.DATE_MODIFIED
            )
            for (prefix in BACKUP_SCAN_PREFIXES) {
                val selection = "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?"
                context.contentResolver.query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, projection, selection, arrayOf("${prefix}%"),
                    "${MediaStore.Downloads.DATE_MODIFIED} DESC"
                )?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(MediaStore.Downloads.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(MediaStore.Downloads.SIZE)
                    val dateIdx = cursor.getColumnIndex(MediaStore.Downloads.DATE_MODIFIED)
                    while (cursor.moveToNext()) {
                        val fileName = cursor.getString(nameIdx)
                        if (results.none { it.fileName == fileName }) {
                            results.add(DownloadBackupFile(
                                fileName = fileName,
                                fileSize = cursor.getLong(sizeIdx),
                                lastModified = cursor.getLong(dateIdx) * 1000
                            ))
                        }
                    }
                }
            }
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (dir.exists()) {
                dir.listFiles()?.filter {
                    BACKUP_SCAN_PREFIXES.any { prefix -> it.name.startsWith(prefix) } &&
                        BACKUP_SCAN_EXTENSIONS.any { extension -> it.name.endsWith(extension) }
                }
                    ?.sortedByDescending { it.lastModified() }
                    ?.forEach {
                        if (results.none { existing -> existing.fileName == it.name }) {
                            results.add(DownloadBackupFile(it.name, it.length(), it.lastModified()))
                        }
                    }
            }
        }
        return results.sortedByDescending { it.lastModified }
    }

    fun scanDownloadsBackups(context: Context): List<DownloadBackupFile> {
        return scanImportableBackupFiles(context)
    }

    /**
     * 读取 Downloads 中某个备份文件的内容
     */
    fun readDownloadBackup(context: Context, fileName: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val uri = findDownloadsUri(context, fileName) ?: return null
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    if (fileName.endsWith(FULL_BACKUP_EXTENSION)) {
                        readBackupJsonFromZip(stream)
                    } else {
                        stream.bufferedReader().readText()
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                readBackupJsonFromFile(file)
            }
        } catch (_: Exception) {
            Log.w("BackupManager", "Failed to read download backup: $fileName")
            null
        }
    }

    fun readBackupForImport(context: Context, fileName: String): PendingBackupImport? {
        return try {
            val file = File(getBackupDir(), fileName)
            if (file.exists()) {
                readPendingImportFromFile(file)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val uri = findDownloadsUri(context, fileName) ?: return null
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    if (fileName.endsWith(FULL_BACKUP_EXTENSION)) {
                        readPendingImportFromZip(stream)
                    } else {
                        PendingBackupImport(Gson().fromJson(stream.bufferedReader().readText(), DiaryBackup::class.java))
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                readPendingImportFromFile(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName))
            }
        } catch (_: Exception) {
            Log.w("BackupManager", "Failed to read backup for import: $fileName")
            null
        }
    }

    fun restorePendingMedia(context: Context, pending: PendingBackupImport) {
        if (pending.mediaFiles.isEmpty()) return
        val mediaDir = DiaryMediaManager.mediaDir(context)
        val thumbDir = DiaryMediaManager.thumbDir(context)
        pending.mediaFiles.forEach { (entryName, bytes) ->
            val normalized = entryName.replace('\\', '/')
            val target = when {
                normalized.startsWith("${DiaryMediaManager.MEDIA_DIR_NAME}/${DiaryMediaManager.THUMB_DIR_NAME}/") -> {
                    File(thumbDir, normalized.substringAfterLast('/'))
                }
                normalized.startsWith("${DiaryMediaManager.MEDIA_DIR_NAME}/") -> {
                    File(mediaDir, normalized.substringAfterLast('/'))
                }
                else -> null
            }
            if (target != null) {
                target.parentFile?.mkdirs()
                target.writeBytes(bytes)
            }
        }
    }

    /**
     * 初始化备份目录：创建目录，扫描已有文件恢复历史记录，迁移旧版 Downloads 备份。
     * 在 BackupScreen 打开时调用。
     */
    fun initBackupDir(context: Context) {
        createBackupDir()

        // 扫描备份目录中已有文件，恢复历史记录
        val existingFiles = scanBackupDir(context)
        if (existingFiles.isNotEmpty()) {
            val history = getBackupHistory(context)
            val knownFileNames = history.map { it.fileName }.toSet()
            for (file in existingFiles) {
                if (file.name in knownFileNames) continue
                val entryCount = try {
                    val parsed = Gson().fromJson(readBackupJsonFromFile(file), DiaryBackup::class.java)
                    parsed?.entries?.size ?: 0
                } catch (_: Exception) {
                    Log.w("BackupManager", "Failed to count entries in backup file: ${file.name}")
                    0
                }
                addBackupRecord(context, BackupRecord(
                    fileName = file.name,
                    filePath = file.absolutePath,
                    timestamp = file.lastModified(),
                    entryCount = entryCount,
                    fileSize = file.length()
                ))
            }
        }

        // 迁移旧版 Downloads 中的备份到新目录
        migrateFromDownloads(context)
    }

    /**
     * 扫描 Documents/DiaryApp/ 目录中的备份文件
     */
    private fun scanBackupDir(context: Context? = null): Array<File> {
        val dir = getBackupDir()
        if (!dir.exists()) return emptyArray()
        val files = dir.listFiles { file ->
            file.isFile &&
                BACKUP_SCAN_PREFIXES.any { prefix -> file.name.startsWith(prefix) } &&
                BACKUP_SCAN_EXTENSIONS.any { extension -> file.name.endsWith(extension) }
        }
        if (!files.isNullOrEmpty()) return files

        // Android 11+: listFiles may silently fail; fall back to MediaStore
        if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return scanBackupDirViaMediaStore(context)
        }
        return emptyArray()
    }

    private fun scanBackupDirViaMediaStore(context: Context): Array<File> {
        val dir = getBackupDir()
        val results = mutableListOf<File>()
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA
        )
        for (prefix in BACKUP_SCAN_PREFIXES) {
            val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ? AND ${MediaStore.Files.FileColumns.DATA} LIKE ?"
            val selectionArgs = arrayOf("${prefix}%", "%${BACKUP_DIR_NAME}%")
            context.contentResolver.query(
                MediaStore.Files.getContentUri("external"), projection, selection, selectionArgs, null
            )?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val fileName = cursor.getString(nameIdx)
                    if (BACKUP_SCAN_EXTENSIONS.any { ext -> fileName.endsWith(ext) }) {
                        val file = File(dir, fileName)
                        if (file.exists()) {
                            results.add(file)
                        }
                    }
                }
            }
        }
        return results.toTypedArray()
    }

    /**
     * 从备份目录读取文件内容
     */
    private fun readBackupJsonFromBackupDir(fileName: String): String? {
        return try {
            val file = File(getBackupDir(), fileName)
            readBackupJsonFromFile(file)
        } catch (_: Exception) {
            Log.w("BackupManager", "Failed to read backup JSON from dir: $fileName")
            null
        }
    }

    private fun readBackupJsonFromFile(file: File): String? {
        if (!file.exists()) return null
        return if (file.name.endsWith(FULL_BACKUP_EXTENSION)) {
            file.inputStream().use { readBackupJsonFromZip(it) }
        } else {
            file.readText()
        }
    }

    private fun readBackupJsonFromZip(input: java.io.InputStream): String? {
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory && entry.name == BACKUP_JSON_NAME) {
                    return zip.readBytes().toString(Charsets.UTF_8)
                }
                zip.closeEntry()
            }
        }
        return null
    }

    private fun readPendingImportFromFile(file: File): PendingBackupImport? {
        if (!file.exists()) return null
        return if (file.name.endsWith(FULL_BACKUP_EXTENSION)) {
            file.inputStream().use { readPendingImportFromZip(it) }
        } else {
            PendingBackupImport(Gson().fromJson(file.readText(), DiaryBackup::class.java))
        }
    }

    private fun readPendingImportFromZip(input: java.io.InputStream): PendingBackupImport? {
        var backupJson: String? = null
        val mediaFiles = linkedMapOf<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory) {
                    val bytes = zip.readBytes()
                    when {
                        entry.name == BACKUP_JSON_NAME -> backupJson = bytes.toString(Charsets.UTF_8)
                        entry.name.startsWith("${DiaryMediaManager.MEDIA_DIR_NAME}/") -> mediaFiles[entry.name] = bytes
                    }
                }
                zip.closeEntry()
            }
        }
        val json = backupJson ?: return null
        return PendingBackupImport(
            backup = Gson().fromJson(json, DiaryBackup::class.java),
            mediaFiles = mediaFiles
        )
    }

    /**
     * 将旧版 Downloads 中的备份文件迁移到 Documents/DiaryApp/
     */
    private fun migrateFromDownloads(context: Context) {
        try {
            val oldFiles = scanDownloadsBackups(context)
            if (oldFiles.isEmpty()) return

            val dir = createBackupDir()
            val history = getBackupHistory(context).toMutableList()
            val knownFileNames = history.map { it.fileName }.toSet()
            var changed = false

            for (oldFile in oldFiles) {
                if (oldFile.fileName in knownFileNames) continue
                val newFile = File(dir, oldFile.fileName)

                if (oldFile.fileName.endsWith(FULL_BACKUP_EXTENSION)) {
                    // Try to copy binary backup file from Downloads to Documents/DiaryApp
                    var filePath = oldFile.fileName  // fallback to Downloads path
                    var fileSize = oldFile.fileSize
                    try {
                        if (!newFile.exists()) {
                            val bytes = readBackupPackageBytes(context, oldFile.fileName)
                            if (bytes != null) {
                                newFile.writeBytes(bytes)
                                filePath = newFile.absolutePath
                                fileSize = newFile.length()
                            }
                        } else {
                            filePath = newFile.absolutePath
                            fileSize = newFile.length()
                        }
                    } catch (_: Exception) { /* keep Downloads path */ }
                    val entryCount = try {
                        val targetFile = if (filePath == newFile.absolutePath) newFile else null
                        val json = targetFile?.let { readBackupJsonFromFile(it) }
                            ?: readDownloadBackup(context, oldFile.fileName)
                        Gson().fromJson(json, DiaryBackup::class.java)?.entries?.size ?: 0
                    } catch (_: Exception) { 0 }
                    history.add(0, BackupRecord(
                        fileName = oldFile.fileName,
                        filePath = filePath,
                        timestamp = oldFile.lastModified,
                        entryCount = entryCount,
                        fileSize = fileSize
                    ))
                    changed = true
                } else {
                    // Migrate old .json backup files
                    val content = readDownloadBackup(context, oldFile.fileName) ?: continue
                    if (!newFile.exists()) {
                        newFile.writeText(content)
                    }
                    val entryCount = try {
                        Gson().fromJson(content, DiaryBackup::class.java)?.entries?.size ?: 0
                    } catch (_: Exception) {
                        Log.w("BackupManager", "Failed to count entries in migration")
                        0
                    }
                    history.add(0, BackupRecord(
                        fileName = oldFile.fileName,
                        filePath = newFile.absolutePath,
                        timestamp = oldFile.lastModified,
                        entryCount = entryCount,
                        fileSize = newFile.length()
                    ))
                    changed = true
                }
            }

            if (changed) saveHistory(context, history)
        } catch (e: Exception) {
            Log.e("BackupManager", "Failed to migrate backups from downloads", e)
        }
    }

}
