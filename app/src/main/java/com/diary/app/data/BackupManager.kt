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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

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

private const val FULL_BACKUP_EXTENSION = ".diarybackup"

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

    // 确保以 diary_backup_ 开头，这样导入扫描能找到
    val prefixed = if (baseName.startsWith("diary_backup_")) baseName else "diary_backup_$baseName"
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
        val fileName = "diary_backup_$timestamp$FULL_BACKUP_EXTENSION"
        val json = buildBackupJson(dao)
        val dataBytes = buildFullBackupBytes(context, dao, json)

        val filePath: String
        if (hasStoragePermission()) {
            val dir = createBackupDir()
            val file = File(dir, fileName)
            file.writeBytes(dataBytes)
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
                out.write(dataBytes)
            } ?: throw Exception("无法写入文件")
            filePath = queryFilePath(context, uri) ?: "${Environment.DIRECTORY_DOWNLOADS}/$fileName"
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val file = File(downloadsDir, fileName)
            file.writeBytes(dataBytes)
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
            null
        }
    }

    private suspend fun buildFullBackupBytes(context: Context, dao: DiaryDao, json: String): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
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
        return output.toByteArray()
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
     * 扫描 Downloads 目录中所有的 diary_backup_* 备份文件。
     */
    fun scanDownloadsBackups(context: Context): List<DownloadBackupFile> {
        val results = mutableListOf<DownloadBackupFile>()
        if (hasStoragePermission()) {
            scanBackupDir().forEach { file ->
                results.add(DownloadBackupFile(file.name, file.length(), file.lastModified()))
            }
        }
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
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (dir.exists()) {
                dir.listFiles()?.filter {
                    it.name.startsWith("diary_backup_") &&
                        (it.name.endsWith(".json") || it.name.endsWith(FULL_BACKUP_EXTENSION))
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
        } catch (_: Exception) { null }
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
        if (!hasStoragePermission()) return
        createBackupDir()

        // 扫描备份目录中已有文件，恢复历史记录
        val existingFiles = scanBackupDir()
        if (existingFiles.isNotEmpty()) {
            val history = getBackupHistory(context)
            val knownFileNames = history.map { it.fileName }.toSet()
            for (file in existingFiles) {
                if (file.name in knownFileNames) continue
                val entryCount = try {
                    val parsed = Gson().fromJson(readBackupJsonFromFile(file), DiaryBackup::class.java)
                    parsed?.entries?.size ?: 0
                } catch (_: Exception) { 0 }
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
    private fun scanBackupDir(): Array<File> {
        val dir = getBackupDir()
        if (!dir.exists()) return emptyArray()
        return dir.listFiles { file ->
            file.isFile && file.name.startsWith("diary_backup_") &&
                (file.name.endsWith(".json") || file.name.endsWith(FULL_BACKUP_EXTENSION))
        } ?: emptyArray()
    }

    /**
     * 从备份目录读取文件内容
     */
    private fun readBackupJsonFromBackupDir(fileName: String): String? {
        return try {
            val file = File(getBackupDir(), fileName)
            readBackupJsonFromFile(file)
        } catch (_: Exception) { null }
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
                if (oldFile.fileName.endsWith(FULL_BACKUP_EXTENSION)) continue
                val content = readDownloadBackup(context, oldFile.fileName) ?: continue

                val newFile = File(dir, oldFile.fileName)
                if (!newFile.exists()) {
                    newFile.writeText(content)
                }

                val entryCount = try {
                    Gson().fromJson(content, DiaryBackup::class.java)?.entries?.size ?: 0
                } catch (_: Exception) { 0 }

                history.add(0, BackupRecord(
                    fileName = oldFile.fileName,
                    filePath = newFile.absolutePath,
                    timestamp = oldFile.lastModified,
                    entryCount = entryCount,
                    fileSize = newFile.length()
                ))
                changed = true
            }

            if (changed) saveHistory(context, history)
        } catch (_: Exception) {}
    }

}
