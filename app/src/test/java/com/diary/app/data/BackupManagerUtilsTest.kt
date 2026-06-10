package com.diary.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class BackupManagerUtilsTest {

    @Test
    fun `normalized backup file name appends json extension and trims whitespace`() {
        assertEquals(
            "trip-notes.json",
            normalizeBackupFileName("  trip-notes  ")
        )
    }

    @Test
    fun `normalized backup file name removes invalid path characters`() {
        assertEquals(
            "2026-06-10-backup.json",
            normalizeBackupFileName("2026/06/10:backup")
        )
    }

    @Test
    fun `normalized backup file name falls back when name becomes empty`() {
        assertEquals(
            "backup.json",
            normalizeBackupFileName("   <>   ")
        )
    }
}
