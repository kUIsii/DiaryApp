package com.diary.app.data.ambientsound

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AudioCacheManager(private val context: Context) {
    private val cacheDir: File
        get() = File(context.filesDir, "ambient_sounds").also { it.mkdirs() }

    fun isCached(trackId: String): Boolean = getFile(trackId).exists()

    fun getFile(trackId: String): File = File(cacheDir, "${trackId}.mp3")

    suspend fun prepare(trackId: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val file = getFile(trackId)
            if (file.exists()) return@withContext Result.success(file)

            val assetPath = "ambient_sounds/${trackId}.mp3"
            context.assets.open(assetPath).use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
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
