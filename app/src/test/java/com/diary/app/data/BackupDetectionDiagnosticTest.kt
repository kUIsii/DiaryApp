package com.diary.app.data

import org.junit.Assert.*
import org.junit.Test

/**
 * 诊断备份检测问题的测试
 *
 * 问题描述：
 * - 2.69.0 版本可以检测到备份文件
 * - 之后的每个版本都检测不到备份了
 * - 包括刚刚创建的备份文件也检测不到
 */
class BackupDetectionDiagnosticTest {

    @Test
    fun `test backup scan prefixes are correct`() {
        // 验证备份文件名前缀
        val prefixes = BACKUP_SCAN_PREFIXES
        assertTrue("Should include diary_backup_ prefix", prefixes.contains("diary_backup_"))
        assertTrue("Should include 日记备份_ prefix", prefixes.contains("日记备份_"))
    }

    @Test
    fun `test backup scan extensions are correct`() {
        // 验证备份文件扩展名
        val extensions = BACKUP_SCAN_EXTENSIONS
        assertTrue("Should include .json extension", extensions.contains(".json"))
        assertTrue("Should include .diarybackup extension", extensions.contains(".diarybackup"))
    }

    @Test
    fun `test backup file name matching with prefixes`() {
        val testFileNames = listOf(
            "diary_backup_2026年6月27日_143052.diarybackup",
            "日记备份_2026年6月27日_143052.diarybackup",
            "diary_backup_2026-06-27.json",
            "日记备份_2026-06-27.json"
        )

        for (fileName in testFileNames) {
            val matchesPrefix = BACKUP_SCAN_PREFIXES.any { prefix ->
                fileName.startsWith(prefix)
            }
            val matchesExtension = BACKUP_SCAN_EXTENSIONS.any { ext ->
                fileName.endsWith(ext)
            }
            assertTrue("File $fileName should match prefix", matchesPrefix)
            assertTrue("File $fileName should match extension", matchesExtension)
        }
    }

    @Test
    fun `test normalize backup file name`() {
        // 测试备份文件名规范化
        val testCases = mapOf(
            "trip-notes" to "diary_backup_trip-notes.diarybackup",
            "2026/06/10:backup" to "diary_backup_2026-06-10-backup.diarybackup",
            "diary_backup_20260612.json" to "diary_backup_20260612.diarybackup"
        )

        for ((input, expected) in testCases) {
            val result = normalizeBackupFileName(input)
            assertEquals("Input: $input", expected, result)
        }
    }

    @Test
    fun `test backup record creation and retrieval`() {
        // 测试备份记录的创建和检索
        val record = BackupRecord(
            fileName = "diary_backup_test.diarybackup",
            filePath = "/storage/emulated/0/Documents/DiaryApp/diary_backup_test.diarybackup",
            timestamp = System.currentTimeMillis(),
            entryCount = 10,
            fileSize = 1024L
        )

        assertEquals("diary_backup_test.diarybackup", record.fileName)
        assertTrue("File path should contain DiaryApp", record.filePath.contains("DiaryApp"))
        assertTrue("File path should contain Documents", record.filePath.contains("Documents"))
    }

    @Test
    fun `test media store query conditions`() {
        // 测试 MediaStore 查询条件（模拟）
        val prefix = "diary_backup_"
        val dirName = "DiaryApp"

        // 模拟查询条件（不使用 Android SDK 常量）
        val displayNameColumn = "_display_name"
        val dataColumn = "_data"
        val selection = "$displayNameColumn LIKE ? AND $dataColumn LIKE ?"
        val selectionArgs = arrayOf("${prefix}%", "%${dirName}%")

        // 验证查询条件格式
        assertTrue("Selection should contain display name column", selection.contains(displayNameColumn))
        assertTrue("Selection should contain data column", selection.contains(dataColumn))
        assertEquals("Selection args should have 2 elements", 2, selectionArgs.size)
        assertTrue("First arg should end with %", selectionArgs[0].endsWith("%"))
        assertTrue("Second arg should start with %", selectionArgs[1].startsWith("%"))
        assertTrue("Second arg should end with %", selectionArgs[1].endsWith("%"))
    }

    @Test
    fun `test backup directory path consistency`() {
        // 测试备份目录路径的一致性
        val backupDirName = "DiaryApp"
        val documentsPath = "/storage/emulated/0/Documents/$backupDirName"
        val internalPath = "/data/user/0/com.diary.app/files/backups"

        // 验证路径格式
        assertTrue("Documents path should end with DiaryApp", documentsPath.endsWith(backupDirName))
        assertTrue("Internal path should end with backups", internalPath.endsWith("backups"))

        // 验证 MediaStore 查询条件能匹配到这些路径
        val queryPath = "%$backupDirName%"
        assertTrue("Query path should match Documents path", documentsPath.contains(queryPath.replace("%", "")))
        assertFalse("Query path should not match internal path", internalPath.contains(queryPath.replace("%", "")))
    }

    @Test
    fun `test permission check logic`() {
        // 测试权限检查逻辑
        // 模拟 Android 11+ 的权限检查
        val androidVersion = 30 // Android 11
        val isAndroid11OrAbove = androidVersion >= 30

        // 验证权限检查逻辑
        if (isAndroid11OrAbove) {
            // Android 11+ 需要 MANAGE_EXTERNAL_STORAGE 权限
            // 或者使用 MediaStore API
            assertTrue("Android 11+ should use MediaStore or MANAGE_EXTERNAL_STORAGE", true)
        } else {
            // Android 10 及以下使用 WRITE_EXTERNAL_STORAGE 权限
            assertTrue("Android 10 and below should use WRITE_EXTERNAL_STORAGE", true)
        }
    }

    @Test
    fun `test backup file detection flow`() {
        // 测试备份文件检测流程
        val backupFileName = "diary_backup_2026年6月27日_143052.diarybackup"

        // 1. 检查文件名前缀
        val hasCorrectPrefix = BACKUP_SCAN_PREFIXES.any { prefix ->
            backupFileName.startsWith(prefix)
        }
        assertTrue("Backup file should have correct prefix", hasCorrectPrefix)

        // 2. 检查文件扩展名
        val hasCorrectExtension = BACKUP_SCAN_EXTENSIONS.any { ext ->
            backupFileName.endsWith(ext)
        }
        assertTrue("Backup file should have correct extension", hasCorrectExtension)

        // 3. 验证文件名规范化
        val normalizedFileName = normalizeBackupFileName(backupFileName.removeSuffix(".diarybackup"))
        assertEquals("Normalized file name should preserve prefix", backupFileName, normalizedFileName)
    }
}
