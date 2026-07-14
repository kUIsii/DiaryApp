package com.diary.app.ui.storage

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.BackupManager
import com.diary.app.data.DiaryMediaManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class StorageCategory(
    val name: String,
    val icon: StorageIcon,
    val sizeBytes: Long,
    val description: String
)

enum class StorageIcon { DATABASE, IMAGE, THUMBNAIL, BACKUP, CACHE, STORAGE }

data class StorageState(
    val isLoading: Boolean = true,
    val databaseSize: Long = 0,
    val mediaSize: Long = 0,
    val imageThumbSize: Long = 0,
    val backupSize: Long = 0,
    val cacheSize: Long = 0,
    val totalSize: Long = 0,
    val totalAppDataSize: Long = 0,
    val imageCount: Int = 0,
    val entryCount: Int = 0,
    // 回收站
    val trashCount: Int = 0,
    // 备份管理
    val backupCount: Int = 0,
    val backupLastTime: Long = 0,
    // 可释放空间（缓存+缩略图+回收站可清理项）
    val cleanableSize: Long = 0
) {
    val categories: List<StorageCategory>
        get() = listOf(
            StorageCategory("应用数据总量", StorageIcon.STORAGE, totalAppDataSize, "包含数据库、媒体、缓存等所有应用数据的总和"),
            StorageCategory("数据库", StorageIcon.DATABASE, databaseSize, "${entryCount} 篇日记"),
            StorageCategory("媒体文件", StorageIcon.IMAGE, mediaSize, "图片、视频等 ${imageCount} 个文件"),
            StorageCategory("缩略图", StorageIcon.THUMBNAIL, imageThumbSize, "自动生成的预览图"),
            StorageCategory("备份", StorageIcon.BACKUP, backupSize, "本地备份文件"),
            StorageCategory("缓存", StorageIcon.CACHE, cacheSize, "临时缓存数据")
        )
}

class StorageViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _state = MutableStateFlow(StorageState())
    val state: StateFlow<StorageState> = _state

    private val prefs by lazy { getApplication<Application>().getSharedPreferences("storage_prefs", Context.MODE_PRIVATE) }

    /** 自动维护：空间紧张时自动清理缓存与缩略图 */
    var autoMaintainEnabled: Boolean
        get() = prefs.getBoolean("auto_maintain", false)
        set(value) { prefs.edit().putBoolean("auto_maintain", value).apply() }

    fun setAutoMaintain(enabled: Boolean) {
        autoMaintainEnabled = enabled
    }

    init {
        calculateStorage()
    }

    private companion object {
        const val AUTO_MAINTAIN_THRESHOLD = 50L * 1024 * 1024 // 50 MB
    }

    fun calculateStorage() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val context = getApplication<Application>()
            val autoMaintain = autoMaintainEnabled

            val result = withContext(Dispatchers.IO) {
                // Database: main .db + WAL + SHM
                val dbFile = context.getDatabasePath("diary_database")
                val dbDir = dbFile.parentFile
                var dbSize = 0L
                if (dbDir != null && dbDir.exists()) {
                    dbDir.listFiles()?.forEach { f ->
                        if (f.name.startsWith("diary_database")) {
                            dbSize += f.length()
                        }
                    }
                }

                // Scan media directory on disk (images, videos, audio)
                val mediaDir = DiaryMediaManager.mediaDir(context)
                val thumbDir = DiaryMediaManager.thumbDir(context)
                val mediaSize = calculateMediaSize(mediaDir, thumbDir)
                val thumbSize = calculateDirectorySize(thumbDir)
                val imageCount = countFiles(mediaDir, thumbDir)

                val entryCount = dao.getEntryCount()

                // Trash: count entries
                val trashCount = dao.getAllTrashEntriesOnce().size

                // Backup: scan actual files on disk + history metadata
                val backupSize = calculateBackupSize(context)
                val backupHistory = BackupManager.getBackupHistory(context)
                val backupCount = backupHistory.size
                val backupLastTime = backupHistory.maxOfOrNull { it.timestamp } ?: 0L

                // Cache: app cacheDir + code_cache
                val cacheSize = calculateCacheSize(context)

                // Total app data size (dataDir includes everything)
                val totalAppDataSize = calculateDirectorySize(context.dataDir)

                StorageResult(
                    dbSize, mediaSize, thumbSize, backupSize, cacheSize,
                    totalAppDataSize, imageCount, entryCount,
                    trashCount, backupCount, backupLastTime
                )
            }

            // 自动维护：空间紧张时自动清理缓存与缩略图（清理后自动重建，不影响日记）
            var cacheSize = result.cacheSize
            var thumbSize = result.thumbSize
            if (autoMaintain && (cacheSize + thumbSize) > AUTO_MAINTAIN_THRESHOLD) {
                context.cacheDir.deleteRecursively()
                context.codeCacheDir.deleteRecursively()
                val thumbDir = DiaryMediaManager.thumbDir(context)
                if (thumbDir.exists()) thumbDir.deleteRecursively()
                cacheSize = calculateCacheSize(context)
                thumbSize = calculateDirectorySize(thumbDir)
            }

            val total = result.dbSize + result.mediaSize + thumbSize + result.backupSize + cacheSize
            // 可释放 = 缓存 + 缩略图（缩略图清理后会自动重新生成）
            val cleanable = cacheSize + thumbSize

            _state.value = StorageState(
                isLoading = false,
                databaseSize = result.dbSize,
                mediaSize = result.mediaSize,
                imageThumbSize = thumbSize,
                backupSize = result.backupSize,
                cacheSize = cacheSize,
                totalSize = total,
                totalAppDataSize = result.totalAppDataSize,
                imageCount = result.imageCount,
                entryCount = result.entryCount,
                trashCount = result.trashCount,
                backupCount = result.backupCount,
                backupLastTime = result.backupLastTime,
                cleanableSize = cleanable
            )
        }
    }

    // ---- Actions ----

    fun clearCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val context = getApplication<Application>()
                context.cacheDir.deleteRecursively()
                context.codeCacheDir.deleteRecursively()
            }
            calculateStorage()
        }
    }

    fun clearThumbnails() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val thumbDir = DiaryMediaManager.thumbDir(getApplication())
                if (thumbDir.exists()) thumbDir.deleteRecursively()
            }
            calculateStorage()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dao.deleteAllTrashEntries()
            }
            calculateStorage()
        }
    }

    fun createBackup() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val context = getApplication<Application>()
                BackupManager.createBackup(context, dao)
            }
            calculateStorage()
        }
    }

    // ---- Private helpers ----

    /** Calculate media dir size, excluding the thumbs/ subdirectory */
    private fun calculateMediaSize(mediaDir: File, thumbDir: File): Long {
        if (!mediaDir.exists()) return 0L
        val thumbPath = thumbDir.canonicalPath
        return mediaDir.walkTopDown()
            .filter { it.isFile && !it.canonicalPath.startsWith(thumbPath) }
            .sumOf { it.length() }
    }

    /** Count files in media dir, excluding thumbs/ */
    private fun countFiles(mediaDir: File, thumbDir: File): Int {
        if (!mediaDir.exists()) return 0
        val thumbPath = thumbDir.canonicalPath
        return mediaDir.walkTopDown()
            .filter { it.isFile && !it.canonicalPath.startsWith(thumbPath) }
            .count()
    }

    private fun calculateDirectorySize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private fun calculateBackupSize(context: android.content.Context): Long {
        var size = 0L
        val internalBackup = File(context.filesDir, "backups")
        if (internalBackup.exists()) {
            size += calculateDirectorySize(internalBackup)
        }
        val docsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS)
        val externalBackup = File(docsDir, "DiaryApp")
        if (externalBackup.exists()) {
            size += calculateDirectorySize(externalBackup)
        }
        return size
    }

    private fun calculateCacheSize(context: android.content.Context): Long {
        var size = 0L
        val cacheDir = context.cacheDir
        if (cacheDir.exists()) size += calculateDirectorySize(cacheDir)
        val codeCacheDir = context.codeCacheDir
        if (codeCacheDir.exists()) size += calculateDirectorySize(codeCacheDir)
        return size
    }

    private data class StorageResult(
        val dbSize: Long,
        val mediaSize: Long,
        val thumbSize: Long,
        val backupSize: Long,
        val cacheSize: Long,
        val totalAppDataSize: Long,
        val imageCount: Int,
        val entryCount: Int,
        val trashCount: Int,
        val backupCount: Int,
        val backupLastTime: Long
    )
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes}B"
        bytes < 1024 * 1024 -> "${bytes / 1024}KB"
        bytes < 1024L * 1024 * 1024 -> String.format("%.1fMB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.2fGB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

fun formatRelativeTime(context: android.content.Context, timestamp: Long): String {
    if (timestamp <= 0) return "从未"
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3_600_000 -> "${diff / 60_000} 分钟前"
        diff < 86_400_000 -> "${diff / 3_600_000} 小时前"
        diff < 604_800_000 -> "${diff / 86_400_000} 天前"
        else -> {
            val sdf = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestamp))
        }
    }
}
