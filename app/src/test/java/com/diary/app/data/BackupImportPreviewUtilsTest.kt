package com.diary.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupImportPreviewUtilsTest {

    @Test
    fun `preview warns when json backup contains image refs without embedded media`() {
        val pending = PendingBackupImport(
            backup = DiaryBackup(
                app = "DiaryApp",
                version = "2.69.0",
                exportDate = "2026-06-26",
                entries = listOf(
                    BackupEntry(
                        title = "有图日记",
                        content = "\"diary-media://photo_1.jpg\"",
                        plainText = "有图日记",
                        moodLevel = null,
                        weather = null,
                        location = null,
                        latitude = null,
                        longitude = null,
                        tags = emptyList(),
                        createdAt = 1L,
                        updatedAt = 1L
                    )
                ),
                tags = emptyList()
            ),
            isFullBackupPackage = false
        )

        val preview = buildBackupImportPreview(pending)

        assertEquals("旧版 JSON 备份", preview.sourceLabel)
        assertEquals(1, preview.referencedMediaCount)
        assertEquals(0, preview.mediaFileCount)
        assertEquals(1, preview.missingMediaCount)
        assertTrue(preview.warningMessage!!.contains("不含图片文件"))
    }

    @Test
    fun `preview for full backup counts embedded media and no warning when complete`() {
        val pending = PendingBackupImport(
            backup = DiaryBackup(
                app = "DiaryApp",
                version = "2.70.0",
                exportDate = "2026-06-26",
                entries = listOf(
                    BackupEntry(
                        title = "有图日记",
                        content = "\"diary-media://photo_1.jpg\"",
                        plainText = "有图日记",
                        moodLevel = null,
                        weather = null,
                        location = null,
                        latitude = null,
                        longitude = null,
                        tags = emptyList(),
                        createdAt = 1L,
                        updatedAt = 1L
                    )
                ),
                tags = emptyList()
            ),
            mediaFiles = mapOf(
                "${DiaryMediaManager.MEDIA_DIR_NAME}/photo_1.jpg" to byteArrayOf(1, 2, 3)
            ),
            isFullBackupPackage = true
        )

        val preview = buildBackupImportPreview(pending)

        assertEquals("完整备份包", preview.sourceLabel)
        assertEquals(1, preview.mediaFileCount)
        assertEquals(0, preview.missingMediaCount)
        assertEquals(null, preview.warningMessage)
    }
}
