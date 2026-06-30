package com.diary.app.data.ambientsound

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class AudioCacheManager(private val context: Context) {
    private val cacheDir: File
        get() = File(context.filesDir, "ambient_sounds").also { it.mkdirs() }

    fun isCached(trackId: String): Boolean = getFile(trackId).exists()

    fun getFile(trackId: String): File = File(cacheDir, "${trackId}.mp3")

    suspend fun download(
        trackId: String,
        url: String,
        onProgress: ((Float) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val file = getFile(trackId)
            if (file.exists()) return@withContext Result.success(file)

            val connection = URL(url).openConnection()
            connection.connect()
            val contentLength = connection.contentLength
            val input = connection.getInputStream()
            val output = FileOutputStream(file)
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalRead = 0L

            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                if (contentLength > 0) {
                    onProgress?.invoke(totalRead.toFloat() / contentLength)
                }
            }

            output.close()
            input.close()
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun delete(trackId: String) {
        getFile(trackId).delete()
    }

    fun clearAll() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }
}
