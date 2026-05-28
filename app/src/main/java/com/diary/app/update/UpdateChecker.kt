package com.diary.app.update

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
    val downloadUrl: String
)

object UpdateChecker {

    suspend fun checkForUpdate(currentVersionName: String): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(
                    "https://api.github.com/repos/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/releases/latest"
                )
                val connection = url.openConnection() as HttpURLConnection
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode != 200) {
                    return@withContext null
                }

                val json = connection.inputStream.bufferedReader().readText()
                val release = Gson().fromJson(json, GitHubRelease::class.java)

                val latestVersion = release.tagName.removePrefix("v")
                if (!isNewerVersion(currentVersionName, latestVersion)) {
                    return@withContext null
                }

                val apkAsset = release.assets?.firstOrNull {
                    it.name.endsWith(".apk")
                } ?: return@withContext null

                UpdateInfo(
                    versionName = latestVersion,
                    releaseNotes = release.body ?: "",
                    downloadUrl = apkAsset.downloadUrl
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }

        val maxSize = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until maxSize) {
            val currentPart = currentParts.getOrElse(i) { 0 }
            val latestPart = latestParts.getOrElse(i) { 0 }
            if (latestPart > currentPart) return true
            if (latestPart < currentPart) return false
        }
        return false
    }
}
