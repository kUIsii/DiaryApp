package com.diary.app.update

import android.content.Context
import com.diary.app.BuildConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

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

    suspend fun checkForUpdate(context: Context, currentVersionName: String): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val isExperimental = BuildConfig.FLAVOR == "experimental"
                val url = URL(
                    "https://api.github.com/repos/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/releases"
                )
                connection = url.openConnection() as HttpURLConnection
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                if (BuildConfig.GITHUB_TOKEN.isNotBlank()) {
                    connection.setRequestProperty("Authorization", "Bearer ${BuildConfig.GITHUB_TOKEN}")
                }
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode != 200) {
                    return@withContext null
                }

                val json = connection.inputStream.bufferedReader().use { it.readText() }
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
                } ?: return@withContext null

                val latestVersion = matchingRelease.tagName.removePrefix("v")
                if (!isNewerVersion(currentVersionName, latestVersion)) {
                    return@withContext null
                }

                val apkAsset = matchingRelease.assets?.firstOrNull {
                    it.name.endsWith(".apk")
                } ?: return@withContext null

                val releaseBody = matchingRelease.body ?: ""
                val isForce = releaseBody.contains("[force]", ignoreCase = true) ||
                        releaseBody.contains("[强制更新]")

                UpdateInfo(
                    versionName = latestVersion,
                    releaseNotes = releaseBody
                        .replace("[force]", "", ignoreCase = true)
                        .replace("[强制更新]", "")
                        .trim(),
                    downloadUrl = apkAsset.downloadUrl,
                    isForceUpdate = isForce
                )
            } catch (e: Exception) {
                null
            } finally {
                connection?.disconnect()
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

        if (currentSuffix == latestSuffix) return false
        if (latestSuffix.isEmpty()) return true
        if (currentSuffix.isEmpty()) return false

        return latestSuffix > currentSuffix
    }
}
