package com.diary.app.ui.backup

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BackupImportPreviewSourceTest {

    @Test
    fun `backup screen shows import preview source media summary and warning message`() {
        val source = File("src/main/java/com/diary/app/ui/backup/BackupScreen.kt").readText()

        assertTrue(source.contains("val preview = remember(pendingImportData)"))
        assertTrue(source.contains("preview.sourceLabel"))
        assertTrue(source.contains("preview.mediaFileCount"))
        assertTrue(source.contains("preview.warningMessage"))
    }
}
