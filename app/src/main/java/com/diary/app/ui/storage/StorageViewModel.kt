package com.diary.app.ui.storage

import android.app.Application
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

enum class StorageIcon { DATABASE, IMAGE, THUMBNAIL, BACKUP, CACHE }

data class StorageState(
    val isLoading: Boolean = true,
    val databaseSize: Long = 0,
    val mediaSize: Long = 0,
    val imageThumbSize: Long = 0,
    val backupSize: Long = 0,
    val cacheSize: Long = 0,
    val totalSize: Long = 0,
    val imageCount: Int = 0,
    val entryCount: Int = 0
) {
    val categories: List<StorageCategory>
        get() = listOf(
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

    init {
        calculateStorage()
    }

    fun calculateStorage() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val context = getApplication<Application>()

            val result = withContext(Dispatchers.IO) {
                // Database file size
                val dbFile = context.getDatabasePath("diary_database")
                val dbSize = if (dbFile.exists()) dbFile.length() else 0L

                // Scan media directory on disk (images, videos, audio)
                val mediaDir = DiaryMediaManager.mediaDir(context)
                val thumbDir = DiaryMediaManager.thumbDir(context)
                val mediaSize = calculateMediaSize(mediaDir, thumbDir)
                val thumbSize = calculateDirectorySize(thumbDir)

                // Count files in media dir
                val imageCount = countFiles(mediaDir, thumbDir)

                val entryCount = dao.getEntryCount()

                // Backup sizes
                val backupHistory = BackupManager.getBackupHistory(context)
                val backupSize = backupHistory.sumOf { it.fileSize }

                // Cache size
                val cacheSize = calculateCacheSize(context)

                StorageResult(dbSize, mediaSize, thumbSize, backupSize, cacheSize, imageCount, entryCount)
            }

            val total = result.dbSize + result.mediaSize + result.thumbSize + result.backupSize + result.cacheSize

            _state.value = StorageState(
                isLoading = false,
                databaseSize = result.dbSize,
                mediaSize = result.mediaSize,
                imageThumbSize = result.thumbSize,
                backupSize = result.backupSize,
                cacheSize = result.cacheSize,
                totalSize = total,
                imageCount = result.imageCount,
                entryCount = result.entryCount
            )
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val context = getApplication<Application>()
                context.cacheDir.deleteRecursively()
            }
            calculateStorage()
        }
    }

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

    private fun calculateCacheSize(context: android.content.Context): Long {
        var size = 0L
        val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        if (prefsDir.exists()) {
            size += prefsDir.listFiles()?.sumOf { it.length() } ?: 0L
        }
        val cacheDir = context.cacheDir
        if (cacheDir.exists()) {
            size += calculateDirectorySize(cacheDir)
        }
        return size
    }

    private data class StorageResult(
        val dbSize: Long,
        val mediaSize: Long,
        val thumbSize: Long,
        val backupSize: Long,
        val cacheSize: Long,
        val imageCount: Int,
        val entryCount: Int
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
