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

    suspend fun prepare(trackId: String, url: String? = null): Result<File> = withContext(Dispatchers.IO) {
        try {
            val file = getFile(trackId)
            if (file.exists()) return@withContext Result.success(file)

            try {
                val assetPath = "ambient_sounds/${trackId}.mp3"
                context.assets.open(assetPath).use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                return@withContext Result.success(file)
            } catch (_: Exception) { }

            if (url != null) {
                val conn = URL(url).openConnection()
                conn.setRequestProperty("User-Agent", "DiaryApp/1.0")
                conn.connectTimeout = 10000
                conn.readTimeout = 60000
                conn.getInputStream().use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                return@withContext Result.success(file)
            }

            Result.failure(Exception("No audio source available"))
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
