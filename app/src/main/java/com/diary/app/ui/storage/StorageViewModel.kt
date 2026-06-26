package com.diary.app.ui.storage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.BackupManager
import com.diary.app.data.DiaryDatabase
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
    val isVacuuming: Boolean = false,
    val vacuumSavedBytes: Long = 0,
    val isIntegrityOk: Boolean? = null,
    val orphanFiles: List<File> = emptyList(),
    val orphanSizeBytes: Long = 0,
    val isScanningOrphans: Boolean = false,
    val duplicateGroups: List<List<File>> = emptyList(),
    val duplicateSizeBytes: Long = 0,
    val isScanningDuplicates: Boolean = false
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

    init {
        calculateStorage()
    }

    fun calculateStorage() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val context = getApplication<Application>()

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

                // Backup: scan actual files on disk (both internal and external)
                val backupSize = calculateBackupSize(context)

                // Cache: app cacheDir + code_cache
                val cacheSize = calculateCacheSize(context)

                // Total app data size (dataDir includes everything)
                val totalAppDataSize = calculateDirectorySize(context.dataDir)

                StorageResult(dbSize, mediaSize, thumbSize, backupSize, cacheSize, totalAppDataSize, imageCount, entryCount)
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
                totalAppDataSize = result.totalAppDataSize,
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

    fun vacuumDatabase() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isVacuuming = true)
            val context = getApplication<Application>()
            val saved = DiaryDatabase.vacuum(context)
            _state.value = _state.value.copy(
                isVacuuming = false,
                vacuumSavedBytes = saved
            )
            calculateStorage()
        }
    }

    fun checkIntegrity() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val isOk = DiaryDatabase.checkIntegrity(context)
            _state.value = _state.value.copy(isIntegrityOk = isOk)
        }
    }

    fun scanOrphanFiles() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isScanningOrphans = true)
            val context = getApplication<Application>()
            val result = withContext(Dispatchers.IO) {
                val mediaDir = DiaryMediaManager.mediaDir(context)
                if (!mediaDir.exists()) return@withContext emptyList<File>() to 0L

                val dbNames = dao.getAllMediaNames().toSet()
                val thumbDir = DiaryMediaManager.thumbDir(context)
                val thumbPath = thumbDir.canonicalPath

                val orphans = mediaDir.listFiles()?.filter { file ->
                    file.isFile &&
                        !file.canonicalPath.startsWith(thumbPath) &&
                        file.name !in dbNames
                } ?: emptyList()

                val totalSize = orphans.sumOf { it.length() }
                orphans to totalSize
            }
            _state.value = _state.value.copy(
                isScanningOrphans = false,
                orphanFiles = result.first,
                orphanSizeBytes = result.second
            )
        }
    }

    fun cleanOrphanFiles() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _state.value.orphanFiles.forEach { it.delete() }
            }
            _state.value = _state.value.copy(orphanFiles = emptyList(), orphanSizeBytes = 0)
            calculateStorage()
        }
    }

    fun scanDuplicates() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isScanningDuplicates = true)
            val context = getApplication<Application>()
            val result = withContext(Dispatchers.IO) {
                val mediaDir = DiaryMediaManager.mediaDir(context)
                if (!mediaDir.exists()) return@withContext emptyList<List<File>>() to 0L

                val thumbDir = DiaryMediaManager.thumbDir(context)
                val thumbPath = thumbDir.canonicalPath

                val files = mediaDir.listFiles()?.filter { file ->
                    file.isFile && !file.canonicalPath.startsWith(thumbPath)
                } ?: emptyList()

                // Group by file size first (fast filter), then by hash
                val sizeGroups = files.groupBy { it.length() }
                    .filter { it.value.size > 1 }

                val duplicateGroups = mutableListOf<List<File>>()
                var duplicateSize = 0L

                for ((_, group) in sizeGroups) {
                    val hashGroups = group.groupBy { file ->
                        try {
                            file.inputStream().use { stream ->
                                val md = java.security.MessageDigest.getInstance("MD5")
                                val buffer = ByteArray(8192)
                                var read: Int
                                while (stream.read(buffer).also { read = it } != -1) {
                                    md.update(buffer, 0, read)
                                }
                                md.digest().joinToString("") { "%02x".format(it) }
                            }
                        } catch (_: Exception) { file.absolutePath }
                    }.filter { it.value.size > 1 }

                    for ((_, dupes) in hashGroups) {
                        duplicateGroups.add(dupes)
                        duplicateSize += dupes.drop(1).sumOf { it.length() }
                    }
                }

                duplicateGroups to duplicateSize
            }
            _state.value = _state.value.copy(
                isScanningDuplicates = false,
                duplicateGroups = result.first,
                duplicateSizeBytes = result.second
            )
        }
    }

    fun cleanDuplicates(removeFiles: List<File>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                removeFiles.forEach { it.delete() }
            }
            scanDuplicates()
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

    private fun calculateBackupSize(context: android.content.Context): Long {
        var size = 0L
        // Internal backup dir: filesDir/backups/
        val internalBackup = File(context.filesDir, "backups")
        if (internalBackup.exists()) {
            size += calculateDirectorySize(internalBackup)
        }
        // External backup dir: Documents/DiaryApp/
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
