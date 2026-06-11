package com.diary.app.update

import android.content.Context
import com.diary.app.BuildConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class CachedUpdateResult(
    val updateInfo: UpdateInfo?,
    val timestamp: Long
)

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    val body: String?,
    val assets: List<GitHubAsset>?
)

data class GitHubAsset(
    val name: String,
    @SerializedName("browser_download_url") val downloadUrl: String
)

data class UpdateInfo(
    val versionName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val isForceUpdate: Boolean = false
)

object UpdateChecker {

    private const val CACHE_KEY = "update_check_cache"
    private const val CACHE_VERSION_KEY = "update_check_version"
    private const val CACHE_DURATION_MS = 30 * 60 * 1000L // 30 分钟

    suspend fun checkForUpdate(context: Context, currentVersionName: String, forceRefresh: Boolean = false): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences("diary_update_prefs", Context.MODE_PRIVATE)

            // 版本变化时清除缓存
            val cachedVersion = prefs.getString(CACHE_VERSION_KEY, null)
            if (cachedVersion != currentVersionName) {
                prefs.edit().remove(CACHE_KEY).putString(CACHE_VERSION_KEY, currentVersionName).apply()
            }

            // 检查缓存（手动检查时跳过缓存）
            if (!forceRefresh) {
                val cached = try {
                    Gson().fromJson(prefs.getString(CACHE_KEY, null), CachedUpdateResult::class.java)
                } catch (_: Exception) { null }

                if (cached != null && System.currentTimeMillis() - cached.timestamp < CACHE_DURATION_MS) {
                    return@withContext cached.updateInfo
                }
            }

            try {
                val isExperimental = BuildConfig.FLAVOR == "experimental"
                val url = URL(
                    "https://api.github.com/repos/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/releases"
                )
                val connection = url.openConnection() as HttpURLConnection
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                if (BuildConfig.GITHUB_TOKEN.isNotBlank()) {
                    connection.setRequestProperty("Authorization", "Bearer ${BuildConfig.GITHUB_TOKEN}")
                }
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode != 200) {
                    return@withContext null
                }

                val json = connection.inputStream.bufferedReader().readText()
                val releases = Gson().fromJson(json, Array<GitHubRelease>::class.java)

                val matchingRelease = releases.filter { release ->
                    val tag = release.tagName.lowercase()
                    val hasApk = release.assets?.any { it.name.endsWith(".apk") } == true
                    hasApk && if (isExperimental) {
                        tag.contains("experimental")
                    } else {
                        !tag.contains("experimental")
                    }
                }.maxByOrNull { release ->
                    val version = release.tagName.removePrefix("v").substringBefore("-")
                    val parts = version.split(".").map { it.toIntOrNull() ?: 0 }
                    parts.getOrElse(0) { 0 } * 1000000 + parts.getOrElse(1) { 0 } * 1000 + parts.getOrElse(2) { 0 }
                } ?: run {
                    // 没有匹配的 release，缓存 null 结果
                    prefs.edit().putString(CACHE_KEY, Gson().toJson(CachedUpdateResult(null, System.currentTimeMillis()))).apply()
                    return@withContext null
                }

                val latestVersion = matchingRelease.tagName.removePrefix("v")
                if (!isNewerVersion(currentVersionName, latestVersion)) {
                    prefs.edit().putString(CACHE_KEY, Gson().toJson(CachedUpdateResult(null, System.currentTimeMillis()))).apply()
                    return@withContext null
                }

                val apkAsset = matchingRelease.assets?.firstOrNull {
                    it.name.endsWith(".apk")
                } ?: return@withContext null

                val releaseBody = matchingRelease.body ?: ""
                val isForce = releaseBody.contains("[force]", ignoreCase = true) ||
                        releaseBody.contains("[强制更新]")

                val result = UpdateInfo(
                    versionName = latestVersion,
                    releaseNotes = releaseBody
                        .replace("[force]", "", ignoreCase = true)
                        .replace("[强制更新]", "")
                        .trim(),
                    downloadUrl = apkAsset.downloadUrl,
                    isForceUpdate = isForce
                )
                // 缓存结果
                prefs.edit().putString(CACHE_KEY, Gson().toJson(CachedUpdateResult(result, System.currentTimeMillis()))).apply()
                result
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currentBase = current.substringBefore("-")
        val latestBase = latest.substringBefore("-")
        val currentSuffix = current.substringAfter("-", "")
        val latestSuffix = latest.substringAfter("-", "")

        val currentParts = currentBase.split(".").map { it.toIntOrNull() ?: 0 }
        val latestParts = latestBase.split(".").map { it.toIntOrNull() ?: 0 }

        val maxSize = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until maxSize) {
            val currentPart = currentParts.getOrElse(i) { 0 }
            val latestPart = latestParts.getOrElse(i) { 0 }
            if (latestPart > currentPart) return true
            if (latestPart < currentPart) return false
        }

        // 基础版本相同，比较后缀
        // 如果后缀相同，不需要更新
        if (currentSuffix == latestSuffix) return false

        // 有后缀的版本比没有后缀的版本低（正式版 > 测试版）
        if (latestSuffix.isEmpty()) return true
        if (currentSuffix.isEmpty()) return false

        // 两个不同的后缀，按字典序比较
        return latestSuffix > currentSuffix
    }
}
