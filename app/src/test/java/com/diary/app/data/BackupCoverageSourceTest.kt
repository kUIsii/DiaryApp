package com.diary.app.data

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BackupCoverageSourceTest {

    @Test
    fun `backup payload includes ai assistant conversations and messages`() {
        val backupManagerSource = File("src/main/java/com/diary/app/data/BackupManager.kt").readText()

        assertTrue(backupManagerSource.contains("val conversations = dao.getAllConversationsOnce()"))
        assertTrue(backupManagerSource.contains("val chatMessages = dao.getAllChatMessagesOnce()"))
        assertTrue(backupManagerSource.contains("chatConversations = conversations.map"))
        assertTrue(backupManagerSource.contains("chatMessages = chatMessages.map"))
    }

    @Test
    fun `importer restores ai assistant conversations and removes standalone category backup flow`() {
        val importerSource = File("src/main/java/com/diary/app/data/DiaryImporter.kt").readText()
        val tagScreenSource = File("src/main/java/com/diary/app/ui/profile/TagManagementScreen.kt").readText()

        assertTrue(importerSource.contains("dao.deleteAllChatMessages()"))
        assertTrue(importerSource.contains("dao.deleteAllConversations()"))
        assertTrue(importerSource.contains("backup.chatConversations.orEmpty()"))
        assertTrue(importerSource.contains("remapBackupChatMessages("))
        assertTrue(tagScreenSource.contains("TagHeader("))
        assertTrue(!tagScreenSource.contains("CapsuleButton(text = \"备份\""))
        assertTrue(!tagScreenSource.contains("CapsuleButton(text = \"恢复\""))
        assertTrue(!tagScreenSource.contains("TagBackup("))
    }

    @Test
    fun `backup import scans current documents directory instead of only downloads specific flow`() {
        val backupManagerSource = File("src/main/java/com/diary/app/data/BackupManager.kt").readText()
        val backupScreenSource = File("src/main/java/com/diary/app/ui/backup/BackupScreen.kt").readText()

        assertTrue(backupManagerSource.contains("fun scanImportableBackupFiles(context: Context): List<DownloadBackupFile>"))
        assertTrue(backupManagerSource.contains("scanBackupDir(context).forEach"))
        assertTrue(backupManagerSource.contains("for (prefix in BACKUP_SCAN_PREFIXES)"))
        assertTrue(backupScreenSource.contains("BackupManager.scanImportableBackupFiles(context)"))
    }
}
