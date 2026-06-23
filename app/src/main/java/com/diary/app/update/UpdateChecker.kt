package com.diary.app.update

import android.content.Context
import com.diary.app.BuildConfig
import com.diary.app.R
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

sealed interface UpdateCheckResult {
    data class UpdateAvailable(val info: UpdateInfo) : UpdateCheckResult
    data object Latest : UpdateCheckResult
    data object NoMatchingRelease : UpdateCheckResult
    data object NoApkAsset : UpdateCheckResult
    data class NetworkError(val message: String? = null) : UpdateCheckResult
}

object UpdateChecker {

    suspend fun checkForUpdate(context: Context, currentVersionName: String): UpdateInfo? {
        return when (val result = checkForUpdateDetailed(context, currentVersionName)) {
            is UpdateCheckResult.UpdateAvailable -> result.info
            else -> null
        }
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun checkForUpdateDetailed(_context: Context, currentVersionName: String): UpdateCheckResult {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val isExperimental = BuildConfig.FLAVOR == "experimental"
                val url = URL("https://api.github.com/repos/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/releases")
                connection = url.openConnection() as? HttpURLConnection ?: throw IllegalArgumentException("Not HTTP")
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                if (BuildConfig.GITHUB_TOKEN.isNotBlank()) {
                    connection.setRequestProperty("Authorization", "Bearer ${BuildConfig.GITHUB_TOKEN}")
                }
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode != 200) {
                    return@withContext UpdateCheckResult.NetworkError("HTTP ${connection.responseCode}")
                }

                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val releases = Gson().fromJson(json, Array<GitHubRelease>::class.java).toList()

                selectUpdateFromReleases(
                    releases = releases,
                    currentVersionName = currentVersionName,
                    isExperimental = isExperimental
                )
            } catch (e: Exception) {
                UpdateCheckResult.NetworkError(e.message)
            } finally {
                connection?.disconnect()
            }
        }
    }
}

fun UpdateCheckResult.toUserMessage(context: Context): String {
    return when (this) {
        is UpdateCheckResult.UpdateAvailable -> ""
        UpdateCheckResult.Latest -> context.getString(R.string.update_latest_version)
        UpdateCheckResult.NoMatchingRelease -> context.getString(R.string.update_no_matching_release)
        UpdateCheckResult.NoApkAsset -> context.getString(R.string.update_no_apk_asset)
        is UpdateCheckResult.NetworkError -> {
            if (message.isNullOrBlank()) {
                context.getString(R.string.update_check_failed)
            } else {
                context.getString(R.string.update_check_failed_detail, message)
            }
        }
    }
}

internal fun selectUpdateFromReleases(
    releases: List<GitHubRelease>,
    currentVersionName: String,
    isExperimental: Boolean
): UpdateCheckResult {
    val channelReleases = releases.filter { release ->
        val tag = release.tagName.lowercase()
        if (isExperimental) {
            tag.contains("experimental")
        } else {
            !tag.contains("experimental")
        }
    }

    if (channelReleases.isEmpty()) {
        return UpdateCheckResult.NoMatchingRelease
    }

    val releasesWithApk = channelReleases.filter { release ->
        release.assets?.any { it.name.endsWith(".apk") } == true
    }

    if (releasesWithApk.isEmpty()) {
        return UpdateCheckResult.NoApkAsset
    }

    val matchingRelease = releasesWithApk.maxWithOrNull { left, right ->
        compareVersionNames(
            left.tagName.removePrefix("v"),
            right.tagName.removePrefix("v")
        )
    } ?: return UpdateCheckResult.NoMatchingRelease

    val latestVersion = matchingRelease.tagName.removePrefix("v")
    if (compareVersionNames(latestVersion, currentVersionName) <= 0) {
        return UpdateCheckResult.Latest
    }

    val apkAsset = matchingRelease.assets?.firstOrNull { it.name.endsWith(".apk") }
        ?: return UpdateCheckResult.NoApkAsset
    val releaseBody = matchingRelease.body ?: ""
    val isForce = releaseBody.contains("[force]", ignoreCase = true) ||
        releaseBody.contains("[强制更新]")

    return UpdateCheckResult.UpdateAvailable(
        UpdateInfo(
            versionName = latestVersion,
            releaseNotes = releaseBody
                .replace("[force]", "", ignoreCase = true)
                .replace("[强制更新]", "")
                .trim(),
            downloadUrl = apkAsset.downloadUrl,
            isForceUpdate = isForce
        )
    )
}

internal fun compareVersionNames(left: String, right: String): Int {
    val leftBase = left.substringBefore("-")
    val rightBase = right.substringBefore("-")
    val leftSuffix = left.substringAfter("-", "")
    val rightSuffix = right.substringAfter("-", "")

    val leftParts = leftBase.split(".").map { it.toIntOrNull() ?: 0 }
    val rightParts = rightBase.split(".").map { it.toIntOrNull() ?: 0 }

    val maxSize = maxOf(leftParts.size, rightParts.size)
    for (i in 0 until maxSize) {
        val leftPart = leftParts.getOrElse(i) { 0 }
        val rightPart = rightParts.getOrElse(i) { 0 }
        if (leftPart != rightPart) return leftPart.compareTo(rightPart)
    }

    if (leftSuffix == rightSuffix) return 0
    if (leftSuffix.isEmpty()) return 1
    if (rightSuffix.isEmpty()) return -1
    return leftSuffix.compareTo(rightSuffix)
}
