package com.diary.app.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

data class DiaryBackup(
    val app: String?,
    val version: String?,
    val exportDate: String?,
    val entries: List<BackupEntry>?,
    val tags: List<BackupTag>?,
    val todos: List<BackupTodo>? = null,
    val countdowns: List<BackupCountDown>? = null,
    val capsules: List<BackupCapsule>? = null,
    val trash: List<BackupTrashEntry>? = null,
    val habitRecords: List<BackupHabitRecord>? = null,
    val notifications: List<BackupNotification>? = null
)

data class BackupEntry(
    val title: String?,
    val content: String?,
    val plainText: String?,
    val moodLevel: Int?,
    val weather: String?,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val tags: List<String>?,
    val createdAt: Long?,
    val updatedAt: Long?
)

data class BackupTag(
    val name: String?,
    val color: Long?,
    val isPreset: Boolean?
)

data class BackupTodo(
    val title: String?,
    val description: String?,
    val isCompleted: Boolean?,
    val priority: Int?,
    val dueDate: Long?,
    val createdAt: Long?,
    val completedAt: Long?,
    val sortOrder: Int?,
    val category: String?,
    val reminderTime: Long?,
    val tags: String?,
    val parentId: Long?,
    val recurringType: String?,
    val progress: Int?,
    val isPinned: Boolean?,
    val linkedTagIds: String?
)

data class BackupCountDown(
    val title: String?,
    val targetDate: Long?,
    val isCountUp: Boolean?,
    val color: Long?,
    val isRepeatYearly: Boolean?,
    val isPinned: Boolean?,
    val createdAt: Long?
)

data class BackupCapsule(
    val title: String?,
    val content: String?,
    val createdAt: Long?,
    val unlockDate: Long?,
    val isRead: Boolean?
)

data class BackupTrashEntry(
    val originalId: Long?,
    val title: String?,
    val content: String?,
    val plainText: String?,
    val moodLevel: Int?,
    val weather: String?,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val isFavorite: Boolean?,
    val createdAt: Long?,
    val updatedAt: Long?,
    val deletedAt: Long?
)

data class BackupHabitRecord(
    val todoId: Long?,
    val recordDate: Long?,
    val source: String?,
    val summary: String?,
    val diaryEntryId: Long?,
    val createdAt: Long?,
    val updatedAt: Long?
)

data class BackupNotification(
    val id: String?,
    val type: String?,
    val title: String?,
    val subtitle: String?,
    val iconType: String?,
    val colorHex: Long?,
    val relatedId: Long?,
    val isRead: Boolean?,
    val isTrashed: Boolean?,
    val createdAt: Long?,
    val trashedAt: Long?
)

data class ImportResult(
    val entryCount: Int,
    val tagCount: Int
)

data class PendingBackupImport(
    val backup: DiaryBackup,
    val mediaFiles: Map<String, ByteArray> = emptyMap()
)

object DiaryImporter {

    fun readAndValidate(context: Context, uri: Uri): DiaryBackup {
        val json = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader().readText()
        } ?: throw Exception("无法读取文件")

        val backup = try {
            Gson().fromJson(json, DiaryBackup::class.java)
        } catch (e: JsonSyntaxException) {
            throw Exception("JSON 格式不正确")
        }

        if (backup.entries.isNullOrEmpty() && backup.tags.isNullOrEmpty()) {
            throw Exception("备份文件中没有数据")
        }

        return backup
    }

    suspend fun importOverwrite(database: DiaryDatabase, backup: DiaryBackup): ImportResult {
        val dao = database.diaryDao()
        val now = System.currentTimeMillis()
        val tagEntries = backup.tags.orEmpty()
        val diaryEntries = backup.entries.orEmpty()

        return database.withTransaction {
            // 清空所有现有数据
            dao.deleteAllNotifications()
            dao.deleteAllHabitRecords()
            dao.deleteAllCapsules()
            dao.deleteAllCountDownItems()
            dao.deleteAllTodos()
            dao.deleteAllTrashEntries()
            dao.deleteAllImages()
            dao.deleteAllDiaryTags()
            dao.deleteAllEntries()
            dao.deleteAllTags()

            // 重新插入所有数据
            var importedTagCount = 0
            for (tagEntry in tagEntries) {
                val name = tagEntry.name ?: continue
                dao.insertTag(
                    Tag(
                        name = name,
                        color = tagEntry.color ?: 4278210282L,
                        isPreset = tagEntry.isPreset ?: false
                    )
                )
                importedTagCount++
            }

            var importedEntryCount = 0
            for (entry in diaryEntries) {
                val newId = dao.insertEntry(
                    DiaryEntry(
                        title = entry.title ?: "",
                        content = entry.content ?: "",
                        plainText = entry.plainText ?: "",
                        moodLevel = entry.moodLevel,
                        weather = entry.weather,
                        location = entry.location,
                        latitude = entry.latitude,
                        longitude = entry.longitude,
                        createdAt = entry.createdAt ?: now,
                        updatedAt = entry.updatedAt ?: now
                    )
                )
                val tagNames = entry.tags.orEmpty()
                for (tagName in tagNames) {
                    val tag = dao.getTagByName(tagName)
                    if (tag != null) {
                        dao.insertDiaryTag(DiaryTag(diaryId = newId, tagId = tag.id))
                    }
                }
                importedEntryCount++
            }

            // 恢复待办事项
            for (todo in backup.todos.orEmpty()) {
                dao.insertTodo(
                    TodoItem(
                        title = todo.title ?: "",
                        description = todo.description ?: "",
                        isCompleted = todo.isCompleted ?: false,
                        priority = todo.priority ?: 0,
                        dueDate = todo.dueDate,
                        createdAt = todo.createdAt ?: now,
                        completedAt = todo.completedAt,
                        sortOrder = todo.sortOrder ?: 0,
                        category = todo.category ?: "task",
                        reminderTime = todo.reminderTime,
                        tags = todo.tags ?: "",
                        parentId = todo.parentId,
                        recurringType = todo.recurringType ?: "none",
                        progress = todo.progress ?: 0,
                        isPinned = todo.isPinned ?: false,
                        linkedTagIds = todo.linkedTagIds ?: ""
                    )
                )
            }

            // 恢复倒数日
            for (item in backup.countdowns.orEmpty()) {
                dao.insertCountDownItem(
                    CountDownItem(
                        title = item.title ?: "",
                        targetDate = item.targetDate ?: now,
                        isCountUp = item.isCountUp ?: false,
                        color = item.color ?: 0xFF4A90D9,
                        isRepeatYearly = item.isRepeatYearly ?: false,
                        isPinned = item.isPinned ?: false,
                        createdAt = item.createdAt ?: now
                    )
                )
            }

            // 恢复时间胶囊
            for (capsule in backup.capsules.orEmpty()) {
                dao.insertCapsule(
                    TimeCapsule(
                        title = capsule.title ?: "",
                        content = capsule.content ?: "",
                        createdAt = capsule.createdAt ?: now,
                        unlockDate = capsule.unlockDate ?: now,
                        isRead = capsule.isRead ?: false
                    )
                )
            }

            // 恢复回收站
            for (entry in backup.trash.orEmpty()) {
                dao.insertTrashEntry(
                    TrashEntry(
                        originalId = entry.originalId ?: 0,
                        title = entry.title ?: "",
                        content = entry.content ?: "",
                        plainText = entry.plainText ?: "",
                        moodLevel = entry.moodLevel,
                        weather = entry.weather,
                        location = entry.location,
                        latitude = entry.latitude,
                        longitude = entry.longitude,
                        isFavorite = entry.isFavorite ?: false,
                        createdAt = entry.createdAt ?: now,
                        updatedAt = entry.updatedAt ?: now,
                        deletedAt = entry.deletedAt ?: now
                    )
                )
            }

            // 恢复习惯记录
            for (record in backup.habitRecords.orEmpty()) {
                dao.insertHabitRecord(
                    HabitRecord(
                        todoId = record.todoId ?: 0,
                        recordDate = record.recordDate ?: 0,
                        source = record.source ?: "manual",
                        summary = record.summary ?: "",
                        diaryEntryId = record.diaryEntryId,
                        createdAt = record.createdAt ?: now,
                        updatedAt = record.updatedAt ?: now
                    )
                )
            }

            // 恢复通知
            for (notification in backup.notifications.orEmpty()) {
                dao.insertNotification(
                    NotificationEntity(
                        id = notification.id ?: continue,
                        type = notification.type ?: "",
                        title = notification.title ?: "",
                        subtitle = notification.subtitle ?: "",
                        iconType = notification.iconType ?: "",
                        colorHex = notification.colorHex ?: 0,
                        relatedId = notification.relatedId,
                        isRead = notification.isRead ?: false,
                        isTrashed = notification.isTrashed ?: false,
                        createdAt = notification.createdAt ?: now,
                        trashedAt = notification.trashedAt
                    )
                )
            }

            ImportResult(entryCount = importedEntryCount, tagCount = importedTagCount)
        }
    }

    suspend fun import(database: DiaryDatabase, backup: DiaryBackup): ImportResult {
        val dao = database.diaryDao()
        val now = System.currentTimeMillis()
        val tagEntries = backup.tags.orEmpty()
        val diaryEntries = backup.entries.orEmpty()

        return database.withTransaction {
            // Import tags: skip if name already exists, use existing tag ID
            var importedTagCount = 0
            for (tagEntry in tagEntries) {
                val name = tagEntry.name ?: continue
                val existing = dao.getTagByName(name)
                if (existing == null) {
                    dao.insertTag(
                        Tag(
                            name = name,
                            color = tagEntry.color ?: 4278210282L,
                            isPreset = tagEntry.isPreset ?: false
                        )
                    )
                    importedTagCount++
                }
            }

            // Import entries preserving original timestamps
            var importedEntryCount = 0
            for (entry in diaryEntries) {
                val title = entry.title ?: ""
                val content = entry.content ?: ""
                val plainText = entry.plainText ?: ""

                val newId = dao.insertEntry(
                    DiaryEntry(
                        title = title,
                        content = content,
                        plainText = plainText,
                        moodLevel = entry.moodLevel,
                        weather = entry.weather,
                        location = entry.location,
                        latitude = entry.latitude,
                        longitude = entry.longitude,
                        createdAt = entry.createdAt ?: now,
                        updatedAt = entry.updatedAt ?: now
                    )
                )

                // Link tags by name, reusing existing tags
                val tagNames = entry.tags.orEmpty()
                for (tagName in tagNames) {
                    val tag = dao.getTagByName(tagName)
                    if (tag != null) {
                        dao.insertDiaryTag(DiaryTag(diaryId = newId, tagId = tag.id))
                    }
                }

                importedEntryCount++
            }

            // 恢复待办事项
            for (todo in backup.todos.orEmpty()) {
                dao.insertTodo(
                    TodoItem(
                        title = todo.title ?: "",
                        description = todo.description ?: "",
                        isCompleted = todo.isCompleted ?: false,
                        priority = todo.priority ?: 0,
                        dueDate = todo.dueDate,
                        createdAt = todo.createdAt ?: now,
                        completedAt = todo.completedAt,
                        sortOrder = todo.sortOrder ?: 0,
                        category = todo.category ?: "task",
                        reminderTime = todo.reminderTime,
                        tags = todo.tags ?: "",
                        parentId = todo.parentId,
                        recurringType = todo.recurringType ?: "none",
                        progress = todo.progress ?: 0,
                        isPinned = todo.isPinned ?: false,
                        linkedTagIds = todo.linkedTagIds ?: ""
                    )
                )
            }

            // 恢复倒数日
            for (item in backup.countdowns.orEmpty()) {
                dao.insertCountDownItem(
                    CountDownItem(
                        title = item.title ?: "",
                        targetDate = item.targetDate ?: now,
                        isCountUp = item.isCountUp ?: false,
                        color = item.color ?: 0xFF4A90D9,
                        isRepeatYearly = item.isRepeatYearly ?: false,
                        isPinned = item.isPinned ?: false,
                        createdAt = item.createdAt ?: now
                    )
                )
            }

            // 恢复时间胶囊
            for (capsule in backup.capsules.orEmpty()) {
                dao.insertCapsule(
                    TimeCapsule(
                        title = capsule.title ?: "",
                        content = capsule.content ?: "",
                        createdAt = capsule.createdAt ?: now,
                        unlockDate = capsule.unlockDate ?: now,
                        isRead = capsule.isRead ?: false
                    )
                )
            }

            // 恢复回收站
            for (entry in backup.trash.orEmpty()) {
                dao.insertTrashEntry(
                    TrashEntry(
                        originalId = entry.originalId ?: 0,
                        title = entry.title ?: "",
                        content = entry.content ?: "",
                        plainText = entry.plainText ?: "",
                        moodLevel = entry.moodLevel,
                        weather = entry.weather,
                        location = entry.location,
                        latitude = entry.latitude,
                        longitude = entry.longitude,
                        isFavorite = entry.isFavorite ?: false,
                        createdAt = entry.createdAt ?: now,
                        updatedAt = entry.updatedAt ?: now,
                        deletedAt = entry.deletedAt ?: now
                    )
                )
            }

            // 恢复习惯记录
            for (record in backup.habitRecords.orEmpty()) {
                dao.insertHabitRecord(
                    HabitRecord(
                        todoId = record.todoId ?: 0,
                        recordDate = record.recordDate ?: 0,
                        source = record.source ?: "manual",
                        summary = record.summary ?: "",
                        diaryEntryId = record.diaryEntryId,
                        createdAt = record.createdAt ?: now,
                        updatedAt = record.updatedAt ?: now
                    )
                )
            }

            // 恢复通知
            for (notification in backup.notifications.orEmpty()) {
                dao.insertNotification(
                    NotificationEntity(
                        id = notification.id ?: continue,
                        type = notification.type ?: "",
                        title = notification.title ?: "",
                        subtitle = notification.subtitle ?: "",
                        iconType = notification.iconType ?: "",
                        colorHex = notification.colorHex ?: 0,
                        relatedId = notification.relatedId,
                        isRead = notification.isRead ?: false,
                        isTrashed = notification.isTrashed ?: false,
                        createdAt = notification.createdAt ?: now,
                        trashedAt = notification.trashedAt
                    )
                )
            }

            ImportResult(
                entryCount = importedEntryCount,
                tagCount = importedTagCount
            )
        }
    }
}
