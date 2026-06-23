package com.diary.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseVersionUtilsTest {

    @Test
    fun `compare version names uses numeric ordering instead of lexicographic ordering`() {
        assertTrue(compareVersionNames("2.64.10", "2.64.2") > 0)
        assertTrue(compareVersionNames("2.64.50-experimental", "2.64.01-experimental") > 0)
        assertTrue(compareVersionNames("2.64.50", "2.64.50-experimental") > 0)
    }

    @Test
    fun `sort releases for display keeps newest version first`() {
        val releases = listOf(
            ChangelogRelease(tagName = "v2.64.01-experimental", name = null, body = null, publishedAt = "2026-06-01"),
            ChangelogRelease(tagName = "v2.64.50-experimental", name = null, body = null, publishedAt = "2026-06-19"),
            ChangelogRelease(tagName = "v2.63.08", name = null, body = null, publishedAt = "2026-05-01")
        )

        val sorted = sortReleasesForDisplay(releases)

        assertEquals("v2.64.50-experimental", sorted[0].tagName)
        assertEquals("v2.64.01-experimental", sorted[1].tagName)
        assertEquals("v2.63.08", sorted[2].tagName)
    }

    @Test
    fun `select update returns update only for newer matching channel release with apk`() {
        val releases = listOf(
            GitHubRelease("v2.64.95-experimental", "old", listOf(GitHubAsset("app.apk", "old.apk"))),
            GitHubRelease("v2.64.96", "stable", listOf(GitHubAsset("stable.apk", "stable.apk"))),
            GitHubRelease("v2.64.97-experimental", "[force]\nnew", listOf(GitHubAsset("app.apk", "new.apk")))
        )

        val result = selectUpdateFromReleases(
            releases = releases,
            currentVersionName = "2.64.95-experimental",
            isExperimental = true
        )

        val update = result as UpdateCheckResult.UpdateAvailable
        assertEquals("2.64.97-experimental", update.info.versionName)
        assertEquals("new.apk", update.info.downloadUrl)
        assertTrue(update.info.isForceUpdate)
        assertEquals("new", update.info.releaseNotes)
    }

    @Test
    fun `select update reports latest when newest matching release is not newer`() {
        val releases = listOf(
            GitHubRelease("v2.64.95-experimental", "", listOf(GitHubAsset("app.apk", "app.apk")))
        )

        val result = selectUpdateFromReleases(
            releases = releases,
            currentVersionName = "2.64.95-experimental",
            isExperimental = true
        )

        assertEquals(UpdateCheckResult.Latest, result)
    }

    @Test
    fun `select update reports no matching release before apk errors`() {
        val releases = listOf(
            GitHubRelease("v2.64.96", "", listOf(GitHubAsset("app.apk", "app.apk")))
        )

        val result = selectUpdateFromReleases(
            releases = releases,
            currentVersionName = "2.64.95-experimental",
            isExperimental = true
        )

        assertEquals(UpdateCheckResult.NoMatchingRelease, result)
    }

    @Test
    fun `select update reports missing apk for matching releases without apk assets`() {
        val releases = listOf(
            GitHubRelease("v2.64.96-experimental", "", listOf(GitHubAsset("notes.txt", "notes.txt")))
        )

        val result = selectUpdateFromReleases(
            releases = releases,
            currentVersionName = "2.64.95-experimental",
            isExperimental = true
        )

        assertEquals(UpdateCheckResult.NoApkAsset, result)
    }
}
