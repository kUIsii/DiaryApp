package com.diary.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class BackupManagerUtilsTest {

    @Test
    fun `normalized backup file name appends full backup extension and trims whitespace`() {
        assertEquals(
            "diary_backup_trip-notes.diarybackup",
            normalizeBackupFileName("  trip-notes  ")
        )
    }

    @Test
    fun `normalized backup file name removes invalid path characters`() {
        assertEquals(
            "diary_backup_2026-06-10-backup.diarybackup",
            normalizeBackupFileName("2026/06/10:backup")
        )
    }

    @Test
    fun `normalized backup file name falls back when name becomes empty`() {
        assertEquals(
            "diary_backup_backup.diarybackup",
            normalizeBackupFileName("   <>   ")
        )
    }

    @Test
    fun `normalized backup file name replaces old json suffix with full backup suffix`() {
        assertEquals(
            "diary_backup_20260612.diarybackup",
            normalizeBackupFileName("diary_backup_20260612.json")
        )
    }
}
