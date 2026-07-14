package com.diary.app.data.ambientsound

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class AudioCacheManager(private val context: Context) {
    private val cacheDir: File
        get() = File(context.filesDir, "ambient_sounds").also { it.mkdirs() }

    fun isCached(trackId: String): Boolean = getFile(trackId).exists()

    fun getFile(trackId: String): File = File(cacheDir, "${trackId}.mp3")

    // 写入前的「中毒缓存」防护阈值：小于该体积的文件视为无效（错误页 / 空响应）
    private val MIN_VALID_BYTES = 1024

    suspend fun prepare(trackId: String, url: String? = null): Result<File> = withContext(Dispatchers.IO) {
        try {
            val file = getFile(trackId)
            if (file.exists() && file.length() > MIN_VALID_BYTES) return@withContext Result.success(file)
            if (file.exists()) file.delete()

            // 1) 本地 assets 优先（用户打包进 APK 的真实音频）
            val assetPath = "ambient_sounds/${trackId}.mp3"
            try {
                context.assets.open(assetPath).use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                if (file.length() > MIN_VALID_BYTES) {
                    return@withContext Result.success(file)
                }
                file.delete()
            } catch (e: Exception) {
                Log.w("AudioCache", "assets load failed: $assetPath", e)
            }

            // 2) 远程兜底：加入中毒缓存防护，绝不把错误页 / 空文件写进缓存
            if (url != null) {
                try {
                    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                        setRequestProperty("User-Agent", "DiaryApp/1.0")
                        connectTimeout = 10000
                        readTimeout = 60000
                        instanceFollowRedirects = true
                    }
                    val code = conn.responseCode
                    if (code != HttpURLConnection.HTTP_OK) {
                        conn.disconnect()
                        return@withContext Result.failure(Exception("音频下载失败 (HTTP $code)"))
                    }
                    val contentType = conn.contentType ?: ""
                    val contentLength = conn.contentLength
                    val looksLikeAudio = contentType.contains("audio", ignoreCase = true)
                        || contentType.contains("mpeg", ignoreCase = true)
                    if (contentLength <= MIN_VALID_BYTES || (!looksLikeAudio && contentType.isNotBlank())) {
                        conn.disconnect()
                        return@withContext Result.failure(Exception("音频来源无效"))
                    }
                    conn.inputStream.use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                    // 二次校验：写入后仍需为有效音频
                    if (!file.exists() || file.length() <= MIN_VALID_BYTES) {
                        file.delete()
                        return@withContext Result.failure(Exception("音频文件无效"))
                    }
                    return@withContext Result.success(file)
                } catch (e: Exception) {
                    file.delete()
                    return@withContext Result.failure(e)
                }
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
