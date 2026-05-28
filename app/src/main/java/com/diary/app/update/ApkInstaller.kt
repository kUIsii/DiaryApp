package com.diary.app.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

object ApkInstaller {

    fun downloadAndInstall(context: Context, url: String, fileName: String): Flow<DownloadState> = flow {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("下载更新")
            .setDescription("正在下载新版本...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
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
                            // Get the actual file URI from DownloadManager
                            val localUriIndex = it.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                            val localUri = if (localUriIndex >= 0) it.getString(localUriIndex) else null

                            val file = if (localUri != null) {
                                // Use the URI from DownloadManager
                                val uri = Uri.parse(localUri)
                                File(uri.path!!)
                            } else {
                                // Fallback to expected path
                                File(
                                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                    fileName
                                )
                            }

                            if (file.exists()) {
                                emit(DownloadState.Completed(file))
                                installApk(context, file)
                            } else {
                                emit(DownloadState.Failed("下载文件不存在"))
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
    }.flowOn(Dispatchers.IO)

    private fun installApk(context: Context, file: File) {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } else {
            Uri.fromFile(file)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }
}

sealed class DownloadState {
    data class Completed(val file: File) : DownloadState()
    data class Failed(val message: String) : DownloadState()
}
