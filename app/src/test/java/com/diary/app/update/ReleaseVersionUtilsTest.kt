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
}
