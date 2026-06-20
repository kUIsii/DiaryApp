package com.diary.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DiaryImportUtilsTest {

    @Test
    fun `normalize tag name trims collapses whitespace and ignores case`() {
        assertEquals("daily note", normalizeTagNameForMatching("  Daily   Note  "))
        assertEquals("旅行", normalizeTagNameForMatching("  旅行 "))
    }

    @Test
    fun `build tags for import skips normalized duplicates already in database`() {
        val existingTags = listOf(
            Tag(id = 1, name = "心情记录", color = 1L, isPreset = true),
            Tag(id = 2, name = "Daily Note", color = 2L, isPreset = false)
        )

        val newTags = buildTagsForImport(
            existingTags = existingTags,
            backupTags = listOf(
                BackupTag(name = "  心情记录 ", color = 9L, isPreset = false),
                BackupTag(name = "daily   note", color = 10L, isPreset = true),
                BackupTag(name = "旅行", color = 11L, isPreset = false)
            )
        )

        assertEquals(1, newTags.size)
        assertEquals("旅行", newTags.single().name)
    }

    @Test
    fun `build tags for restore deduplicates duplicate default categories`() {
        val restored = buildTagsForRestore(
            listOf(
                BackupTag(name = "生活", color = 1L, isPreset = true),
                BackupTag(name = "  生活  ", color = 2L, isPreset = false),
                BackupTag(name = "Work", color = 3L, isPreset = false),
                BackupTag(name = "work", color = 4L, isPreset = true)
            )
        )

        assertEquals(2, restored.size)
        assertEquals("生活", restored[0].name)
        assertEquals(true, restored[0].isPreset)
        assertEquals("Work", restored[1].name)
        assertEquals(true, restored[1].isPreset)
    }

    @Test
    fun `find imported tag matches backup names by normalized value`() {
        val tags = listOf(
            Tag(id = 1, name = "Daily Note", color = 1L),
            Tag(id = 2, name = "旅行", color = 2L)
        )

        assertEquals(1L, findImportedTag(tags, " daily   note ")?.id)
        assertEquals(2L, findImportedTag(tags, "旅行")?.id)
        assertNull(findImportedTag(tags, "不存在"))
    }

    @Test
    fun `remap backup chat messages keeps mapped conversations and preserves timestamps`() {
        val remapped = remapBackupChatMessages(
            messages = listOf(
                BackupChatMessage(
                    id = 10,
                    conversationId = 100,
                    role = "user",
                    content = "hello",
                    createdAt = 111L
                ),
                BackupChatMessage(
                    id = 11,
                    conversationId = 999,
                    role = "assistant",
                    content = "ignored",
                    createdAt = 222L
                )
            ),
            conversationIdMap = mapOf(100L to 900L),
            now = 555L
        )

        assertEquals(1, remapped.size)
        assertEquals(900L, remapped.single().conversationId)
        assertEquals("user", remapped.single().role)
        assertEquals("hello", remapped.single().content)
        assertEquals(111L, remapped.single().createdAt)
    }

    @Test
    fun `validate backup accepts history only payloads without entries or tags`() {
        val backup = DiaryBackup(
            app = "DiaryApp",
            version = "1.0",
            exportDate = "2026-06-20",
            entries = emptyList(),
            tags = emptyList(),
            todos = listOf(
                BackupTodo(
                    id = 99L,
                    title = "回顾今天",
                    description = "历史待办",
                    isCompleted = true,
                    priority = 1,
                    dueDate = null,
                    createdAt = 100L,
                    completedAt = 120L,
                    sortOrder = 0,
                    category = "task",
                    reminderTime = null,
                    tags = "",
                    parentId = null,
                    recurringType = "none",
                    progress = 100,
                    isPinned = false,
                    linkedTagIds = ""
                )
            )
        )

        assertTrue(hasImportableData(backup))
        assertSame(backup, validateBackupHasImportableData(backup))
    }

    @Test
    fun `validate backup rejects payloads with no supported import data`() {
        val backup = DiaryBackup(
            app = "DiaryApp",
            version = "1.0",
            exportDate = "2026-06-20",
            entries = emptyList(),
            tags = emptyList()
        )

        assertFalse(hasImportableData(backup))

        try {
            validateBackupHasImportableData(backup)
            fail("Expected validation to reject empty backup")
        } catch (error: Exception) {
            assertEquals(EMPTY_BACKUP_MESSAGE, error.message)
        }
    }

    @Test
    fun `filter backup entries skips duplicates already in database and inside same backup`() {
        val existingEntries = listOf(
            DiaryEntry(
                id = 1L,
                title = "旅行日记",
                content = "{\"ops\":[{\"insert\":\"Hello\"}]}",
                plainText = "Hello",
                moodLevel = 4,
                weather = "晴",
                location = "上海",
                latitude = 31.23,
                longitude = 121.47,
                createdAt = 1000L,
                updatedAt = 2000L
            )
        )
        val duplicateEntry = BackupEntry(
            title = "  旅行日记  ",
            content = "{\"ops\":[{\"insert\":\"Hello\"}]}",
            plainText = " Hello ",
            moodLevel = 4,
            weather = "晴",
            location = "上海",
            latitude = 31.23,
            longitude = 121.47,
            tags = listOf("旅行"),
            createdAt = 1000L,
            updatedAt = 2000L
        )
        val uniqueEntry = BackupEntry(
            title = "新的一天",
            content = "{\"ops\":[{\"insert\":\"World\"}]}",
            plainText = "World",
            moodLevel = 5,
            weather = "多云",
            location = "杭州",
            latitude = 30.27,
            longitude = 120.15,
            tags = listOf("生活"),
            createdAt = 3000L,
            updatedAt = 4000L
        )
        val filtered = filterBackupEntriesForImport(
            existingEntries = existingEntries,
            backupEntries = listOf(duplicateEntry, uniqueEntry, uniqueEntry.copy(tags = listOf("重复标签"))))

        assertEquals(1, filtered.size)
        assertEquals("新的一天", filtered.single().title)
    }

    @Test
    fun `filter backup entries keeps entries without timestamps to avoid losing legacy data`() {
        val legacyEntry = BackupEntry(
            title = "老数据",
            content = "{\"ops\":[]}",
            plainText = "老数据",
            moodLevel = null,
            weather = null,
            location = null,
            latitude = null,
            longitude = null,
            tags = emptyList(),
            createdAt = null,
            updatedAt = null
        )

        val filtered = filterBackupEntriesForImport(
            existingEntries = emptyList(),
            backupEntries = listOf(legacyEntry, legacyEntry)
        )

        assertEquals(2, filtered.size)
    }

    @Test
    fun `remap imported todo id returns mapped id when available`() {
        val todoIdMap = mapOf(10L to 110L)

        assertEquals(110L, remapImportedTodoId(10L, todoIdMap))
        assertNull(remapImportedTodoId(99L, todoIdMap))
        assertNull(remapImportedTodoId(null, todoIdMap))
    }

    @Test
    fun `remap imported todo parent id returns mapped id when available`() {
        val todoIdMap = mapOf(5L to 205L)

        assertEquals(205L, remapImportedTodoParentId(5L, todoIdMap))
        assertNull(remapImportedTodoParentId(8L, todoIdMap))
        assertNull(remapImportedTodoParentId(null, todoIdMap))
    }

    @Test
    fun `remap capsule notification related id only keeps remapped capsule ids`() {
        val capsuleIdMap = mapOf(7L to 77L)

        assertEquals(77L, remapImportedNotificationRelatedId("capsule", 7L, capsuleIdMap))
        assertNull(remapImportedNotificationRelatedId("capsule", 9L, capsuleIdMap))
        assertNull(remapImportedNotificationRelatedId("milestone", 7L, capsuleIdMap))
        assertNull(remapImportedNotificationRelatedId(null, 7L, capsuleIdMap))
    }

    @Test
    fun `parse backup capsule theme falls back to normal for invalid values`() {
        assertEquals(CapsuleTheme.BIRTHDAY, parseBackupCapsuleTheme("BIRTHDAY"))
        assertEquals(CapsuleTheme.NORMAL, parseBackupCapsuleTheme("unknown"))
        assertEquals(CapsuleTheme.NORMAL, parseBackupCapsuleTheme(null))
    }
}
