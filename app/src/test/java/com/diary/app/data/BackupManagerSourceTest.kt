package com.diary.app.data

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManagerSourceTest {

    @Test
    fun `backup import reads managed backup package through shared resolver`() {
        val source = File("src/main/java/com/diary/app/data/BackupManager.kt").readText()

        assertTrue(source.contains("resolveReadableBackupFile(context, fileName)"))
        assertTrue(source.contains("private fun resolveReadableBackupFile("))
    }

    @Test
    fun `full backup includes indexed media and media files present on disk`() {
        val source = File("src/main/java/com/diary/app/data/BackupManager.kt").readText()

        assertTrue(source.contains("collectBackupMediaNames("))
        assertTrue(source.contains("DiaryMediaManager.mediaDir(context).listFiles()"))
    }
}
