package com.diary.app.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

object ApkInstaller {

    fun downloadAndInstall(context: Context, url: String, fileName: String): Flow<DownloadState> = flow {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("下载更新")
                .setDescription("正在下载新版本...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadId = downloadManager.enqueue(request)

            // Poll for download completion
            while (true) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor: Cursor? = downloadManager.query(query)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                // Copy APK to app cache for reliable install
                                val cachedApk = copyToCache(context, downloadManager, downloadId, fileName)
                                if (cachedApk != null) {
                                    emit(DownloadState.Completed(cachedApk))
                                    withContext(Dispatchers.Main) {
                                        installApk(context, cachedApk)
                                    }
                                } else {
                                    emit(DownloadState.Failed("下载文件读取失败"))
                                }
                                return@flow
                            }
                            DownloadManager.STATUS_FAILED -> {
                                emit(DownloadState.Failed("下载失败"))
                                return@flow
                            }
                        }
                    }
                }
                delay(1000)
            }
        } catch (e: Exception) {
            emit(DownloadState.Failed("下载出错: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    private fun copyToCache(context: Context, dm: DownloadManager, downloadId: Long, fileName: String): File? {
        return try {
            val uri = dm.getUriForDownloadedFile(downloadId) ?: return null
            val cacheDir = File(context.cacheDir, "updates")
            cacheDir.mkdirs()
            val cacheFile = File(cacheDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                cacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (cacheFile.exists() && cacheFile.length() > 0) cacheFile else null
        } catch (e: Exception) {
            null
        }
    }

    private fun installApk(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // 安装失败时静默处理，用户可以通过文件管理器手动安装
        }
    }
}

sealed class DownloadState {
    data class Completed(val file: File) : DownloadState()
    data class Failed(val message: String) : DownloadState()
}
