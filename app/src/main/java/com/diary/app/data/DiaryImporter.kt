package com.diary.app.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

internal const val READ_BACKUP_ERROR_MESSAGE = "无法读取备份文件"
internal const val INVALID_BACKUP_FORMAT_MESSAGE = "备份文件格式无效"
internal const val EMPTY_BACKUP_MESSAGE = "备份文件中没有可导入的数据"
internal const val DEFAULT_IMPORTED_CONVERSATION_TITLE = "新对话"

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
    val notifications: List<BackupNotification>? = null,
    val chatConversations: List<BackupChatConversation>? = null,
    val chatMessages: List<BackupChatMessage>? = null
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
    val id: Long? = null,
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
    val id: Long? = null,
    val title: String?,
    val content: String?,
    val createdAt: Long?,
    val unlockDate: Long?,
    val isRead: Boolean?,
    val isOpened: Boolean? = null,
    val theme: String? = null,
    val imageUri: String? = null,
    val unlockHour: Int? = null,
    val unlockMinute: Int? = null
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

data class BackupChatConversation(
    val id: Long?,
    val title: String?,
    val createdAt: Long?,
    val updatedAt: Long?
)

data class BackupChatMessage(
    val id: Long?,
    val conversationId: Long?,
    val role: String?,
    val content: String?,
    val createdAt: Long?
)

data class ImportResult(
    val entryCount: Int,
    val tagCount: Int
)

data class PendingBackupImport(
    val backup: DiaryBackup,
    val mediaFiles: Map<String, ByteArray> = emptyMap(),
    val sourceFileName: String? = null,
    val isFullBackupPackage: Boolean = false
)

data class BackupImportPreview(
    val sourceLabel: String,
    val entryCount: Int,
    val tagCount: Int,
    val todoCount: Int,
    val capsuleCount: Int,
    val mediaFileCount: Int,
    val referencedMediaCount: Int,
    val missingMediaCount: Int,
    val appName: String?,
    val version: String?,
    val exportDate: String?,
    val warningMessage: String?
)

internal fun buildBackupImportPreview(pending: PendingBackupImport): BackupImportPreview {
    val backup = pending.backup
    val referencedMediaNames = backup.entries.orEmpty()
        .flatMap { DiaryMediaManager.extractMediaNames(it.content.orEmpty()) }
        .filter(String::isNotBlank)
        .toSet()
    val embeddedMediaNames = pending.mediaFiles.keys
        .map { it.replace('\\', '/') }
        .filter { it.startsWith("${DiaryMediaManager.MEDIA_DIR_NAME}/") }
        .map { it.substringAfterLast('/') }
        .filter(String::isNotBlank)
        .toSet()
    val missingMediaCount = (referencedMediaNames - embeddedMediaNames).size
    val warningMessage = when {
        !pending.isFullBackupPackage && referencedMediaNames.isNotEmpty() ->
            "这是旧版 JSON 备份，检测到 $referencedMediaNames 张图片引用，但备份内不含图片文件，导入后图片无法恢复。"
        !pending.isFullBackupPackage ->
            "这是旧版 JSON 备份，不包含图片媒体，只能恢复文字和结构数据。"
        missingMediaCount > 0 ->
            "这个备份包检测到 $missingMediaCount 张图片缺失，导入后这些图片无法恢复。"
        else -> null
    }

    return BackupImportPreview(
        sourceLabel = if (pending.isFullBackupPackage) "完整备份包" else "旧版 JSON 备份",
        entryCount = backup.entries?.size ?: 0,
        tagCount = backup.tags?.size ?: 0,
        todoCount = backup.todos?.size ?: 0,
        capsuleCount = backup.capsules?.size ?: 0,
        mediaFileCount = embeddedMediaNames.size,
        referencedMediaCount = referencedMediaNames.size,
        missingMediaCount = missingMediaCount,
        appName = backup.app?.trim()?.takeIf(String::isNotBlank),
        version = backup.version?.trim()?.takeIf(String::isNotBlank),
        exportDate = backup.exportDate?.trim()?.takeIf(String::isNotBlank),
        warningMessage = warningMessage
    )
}

internal fun buildDiaryMediaIndexRows(
    entry: DiaryEntry,
    mediaDir: java.io.File,
    thumbDir: java.io.File,
    createdAt: Long = System.currentTimeMillis()
): List<DiaryImage> {
    return DiaryMediaManager.extractMediaNames(entry.content).mapIndexed { index, mediaName ->
        val displayFile = java.io.File(mediaDir, mediaName)
        val thumbFile = java.io.File(thumbDir, mediaName)
        DiaryImage(
            entryId = entry.id,
            localPath = displayFile.absolutePath,
            thumbPath = thumbFile.absolutePath.takeIf { thumbFile.exists() },
            mediaName = mediaName,
            mediaRef = DiaryMediaManager.toMediaRef(mediaName),
            mimeType = when (mediaName.substringAfterLast('.', "").lowercase()) {
                "png" -> "image/png"
                "webp" -> "image/webp"
                "gif" -> "image/gif"
                else -> "image/jpeg"
            },
            fileSize = displayFile.takeIf { it.exists() }?.length() ?: 0L,
            sortOrder = index,
            createdAt = createdAt
        )
    }
}

private suspend fun rebuildDiaryMediaIndex(
    database: DiaryDatabase,
    entries: List<DiaryEntry>,
    mediaDir: java.io.File,
    thumbDir: java.io.File,
    clearExisting: Boolean
) {
    val mediaDao = database.mediaDao()
    if (clearExisting) {
        mediaDao.deleteAllImages()
    } else {
        entries.forEach { mediaDao.deleteImagesForEntry(it.id) }
    }

    entries
        .asSequence()
        .flatMap { buildDiaryMediaIndexRows(it, mediaDir, thumbDir).asSequence() }
        .forEach { mediaDao.insertImage(it) }
}

object DiaryImporter {

    fun readAndValidate(context: Context, uri: Uri): DiaryBackup {
        val json = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader().readText()
        } ?: throw Exception(READ_BACKUP_ERROR_MESSAGE)

        return parseBackupJson(json)
    }

    suspend fun importOverwrite(
        database: DiaryDatabase,
        backup: DiaryBackup,
        mediaDir: java.io.File? = null,
        thumbDir: java.io.File? = null
    ): ImportResult {
        val dao = database.diaryDao()
        val tagDao = database.tagDao()
        val todoDao = database.todoDao()
        val notificationDao = database.notificationDao()
        val chatDao = database.chatDao()
        val mediaDao = database.mediaDao()
        val trashDao = database.trashDao()
        val countDownDao = database.countDownDao()
        val capsuleDao = database.capsuleDao()
        val now = System.currentTimeMillis()
        val tagEntries = buildTagsForRestore(backup.tags.orEmpty())
        val diaryEntries = filterBackupEntriesForImport(
            existingEntries = emptyList(),
            backupEntries = backup.entries.orEmpty()
        )

        val result = database.withTransaction {
            notificationDao.deleteAllNotifications()
            chatDao.deleteAllChatMessages()
            chatDao.deleteAllConversations()
            todoDao.deleteAllHabitRecords()
            capsuleDao.deleteAllCapsules()
            countDownDao.deleteAllCountDownItems()
            todoDao.deleteAllTodos()
            trashDao.deleteAllTrashEntries()
            mediaDao.deleteAllImages()
            tagDao.deleteAllDiaryTags()
            dao.deleteAllEntries()
            tagDao.deleteAllTags()

            val importedTagCount = insertBackupTags(tagDao, tagEntries)
            val importedTags = tagDao.getAllTagsOnce()
            val importedEntryCount = insertEntries(dao, tagDao, importedTags, diaryEntries, now)

            val todoIdMap = insertTodos(todoDao, backup.todos.orEmpty(), now)
            insertCountdowns(countDownDao, backup.countdowns.orEmpty(), now)
            val capsuleIdMap = insertCapsules(capsuleDao, backup.capsules.orEmpty(), now)
            insertTrash(trashDao, backup.trash.orEmpty(), now)
            insertHabitRecords(todoDao, backup.habitRecords.orEmpty(), todoIdMap, now)
            insertNotifications(notificationDao, backup.notifications.orEmpty(), capsuleIdMap, now)
            restoreChatData(chatDao, backup, now)

            ImportResult(entryCount = importedEntryCount, tagCount = importedTagCount)
        }

        if (mediaDir != null && thumbDir != null) {
            rebuildDiaryMediaIndex(
                database = database,
                entries = database.diaryDao().getAllEntriesOnce(),
                mediaDir = mediaDir,
                thumbDir = thumbDir,
                clearExisting = true
            )
        }

        return result
    }

    suspend fun import(
        database: DiaryDatabase,
        backup: DiaryBackup,
        mediaDir: java.io.File? = null,
        thumbDir: java.io.File? = null
    ): ImportResult {
        val dao = database.diaryDao()
        val tagDao = database.tagDao()
        val todoDao = database.todoDao()
        val notificationDao = database.notificationDao()
        val chatDao = database.chatDao()
        val countDownDao = database.countDownDao()
        val capsuleDao = database.capsuleDao()
        val trashDao = database.trashDao()
        val now = System.currentTimeMillis()
        val importedEntries = mutableListOf<DiaryEntry>()

        val result = database.withTransaction {
            val existingTags = tagDao.getAllTagsOnce()
            val existingEntries = dao.getAllEntriesOnce()
            val tagEntries = buildTagsForImport(existingTags, backup.tags.orEmpty())
            val diaryEntries = filterBackupEntriesForImport(existingEntries, backup.entries.orEmpty())
            val importedTagCount = insertTags(tagDao, tagEntries)
            val importedTags = tagDao.getAllTagsOnce()
            val importedEntryCount = insertEntries(dao, tagDao, importedTags, diaryEntries, now, importedEntries)

            val todoIdMap = insertTodos(todoDao, backup.todos.orEmpty(), now)
            insertCountdowns(countDownDao, backup.countdowns.orEmpty(), now)
            val capsuleIdMap = insertCapsules(capsuleDao, backup.capsules.orEmpty(), now)
            insertTrash(trashDao, backup.trash.orEmpty(), now)
            insertHabitRecords(todoDao, backup.habitRecords.orEmpty(), todoIdMap, now)
            insertNotifications(notificationDao, backup.notifications.orEmpty(), capsuleIdMap, now)
            restoreChatData(chatDao, backup, now)

            ImportResult(entryCount = importedEntryCount, tagCount = importedTagCount)
        }

        if (mediaDir != null && thumbDir != null && importedEntries.isNotEmpty()) {
            rebuildDiaryMediaIndex(
                database = database,
                entries = importedEntries.toList(),
                mediaDir = mediaDir,
                thumbDir = thumbDir,
                clearExisting = false
            )
        }

        return result
    }
}

internal fun parseBackupJson(json: String): DiaryBackup {
    val backup = try {
        Gson().fromJson(json, DiaryBackup::class.java)
    } catch (_: JsonSyntaxException) {
        throw Exception(INVALID_BACKUP_FORMAT_MESSAGE)
    }
    return validateBackupHasImportableData(backup)
}

internal fun validateBackupHasImportableData(backup: DiaryBackup): DiaryBackup {
    if (!hasImportableData(backup)) {
        throw Exception(EMPTY_BACKUP_MESSAGE)
    }
    return backup
}

internal fun hasImportableData(backup: DiaryBackup): Boolean {
    return !backup.entries.isNullOrEmpty() ||
        !backup.tags.isNullOrEmpty() ||
        !backup.todos.isNullOrEmpty() ||
        !backup.countdowns.isNullOrEmpty() ||
        !backup.capsules.isNullOrEmpty() ||
        !backup.trash.isNullOrEmpty() ||
        !backup.habitRecords.isNullOrEmpty() ||
        !backup.notifications.isNullOrEmpty() ||
        !backup.chatConversations.isNullOrEmpty() ||
        !backup.chatMessages.isNullOrEmpty()
}

internal fun normalizeTagNameForMatching(name: String?): String {
    return name.orEmpty().trim().replace(Regex("\\s+"), " ").lowercase()
}

internal data class EntryImportDedupKey(
    val createdAt: Long,
    val updatedAt: Long,
    val title: String,
    val content: String,
    val plainText: String,
    val moodLevel: Int?,
    val weather: String,
    val location: String,
    val latitude: Double?,
    val longitude: Double?
)

internal fun filterBackupEntriesForImport(
    existingEntries: List<DiaryEntry>,
    backupEntries: List<BackupEntry>
): List<BackupEntry> {
    val seenKeys = existingEntries.mapTo(linkedSetOf()) { buildEntryImportDedupKey(it) }
    val filteredEntries = mutableListOf<BackupEntry>()

    for (entry in backupEntries) {
        val dedupKey = buildEntryImportDedupKey(entry)
        if (dedupKey != null && !seenKeys.add(dedupKey)) continue
        filteredEntries += entry
    }

    return filteredEntries
}

internal fun buildEntryImportDedupKey(entry: DiaryEntry): EntryImportDedupKey {
    return EntryImportDedupKey(
        createdAt = entry.createdAt,
        updatedAt = entry.updatedAt,
        title = normalizeEntryText(entry.title),
        content = normalizeStructuredEntryContent(entry.content),
        plainText = normalizeEntryText(entry.plainText),
        moodLevel = entry.moodLevel,
        weather = normalizeEntryText(entry.weather),
        location = normalizeEntryText(entry.location),
        latitude = entry.latitude,
        longitude = entry.longitude
    )
}

internal fun buildEntryImportDedupKey(entry: BackupEntry): EntryImportDedupKey? {
    val createdAt = entry.createdAt ?: return null
    val updatedAt = entry.updatedAt ?: return null
    return EntryImportDedupKey(
        createdAt = createdAt,
        updatedAt = updatedAt,
        title = normalizeEntryText(entry.title),
        content = normalizeStructuredEntryContent(entry.content),
        plainText = normalizeEntryText(entry.plainText),
        moodLevel = entry.moodLevel,
        weather = normalizeEntryText(entry.weather),
        location = normalizeEntryText(entry.location),
        latitude = entry.latitude,
        longitude = entry.longitude
    )
}

internal fun normalizeEntryText(value: String?): String {
    return value.orEmpty().trim().replace(Regex("\\s+"), " ")
}

internal fun normalizeStructuredEntryContent(value: String?): String {
    return value.orEmpty().trim()
}

internal fun buildTagsForImport(existingTags: List<Tag>, backupTags: List<BackupTag>): List<Tag> {
    val existingNames = existingTags.map { normalizeTagNameForMatching(it.name) }.toMutableSet()
    val tags = mutableListOf<Tag>()
    for (backupTag in backupTags) {
        val rawName = backupTag.name?.trim().orEmpty()
        val normalizedName = normalizeTagNameForMatching(rawName)
        if (rawName.isBlank() || normalizedName.isBlank() || normalizedName in existingNames) continue
        tags += Tag(
            name = rawName,
            color = backupTag.color ?: 4278210282L,
            isPreset = backupTag.isPreset ?: false
        )
        existingNames += normalizedName
    }
    return tags
}

internal fun buildTagsForRestore(backupTags: List<BackupTag>): List<BackupTag> {
    val uniqueTags = LinkedHashMap<String, BackupTag>()
    for (backupTag in backupTags) {
        val rawName = backupTag.name?.trim().orEmpty()
        val normalizedName = normalizeTagNameForMatching(rawName)
        if (rawName.isBlank() || normalizedName.isBlank()) continue
        val existing = uniqueTags[normalizedName]
        uniqueTags[normalizedName] = when {
            existing == null -> backupTag.copy(name = rawName)
            backupTag.isPreset == true && existing.isPreset != true -> backupTag.copy(name = existing.name ?: rawName)
            else -> existing
        }
    }
    return uniqueTags.values.toList()
}

internal fun findImportedTag(tags: List<Tag>, backupName: String?): Tag? {
    val normalizedName = normalizeTagNameForMatching(backupName)
    return tags.firstOrNull { normalizeTagNameForMatching(it.name) == normalizedName }
}

internal fun remapBackupChatMessages(
    messages: List<BackupChatMessage>,
    conversationIdMap: Map<Long, Long>,
    now: Long
): List<ChatMessageEntity> {
    return messages.mapNotNull { message ->
        val originalConversationId = message.conversationId ?: return@mapNotNull null
        val mappedConversationId = conversationIdMap[originalConversationId] ?: return@mapNotNull null
        val role = message.role?.trim().orEmpty()
        val content = message.content?.trim().orEmpty()
        if (role.isBlank() || content.isBlank()) return@mapNotNull null
        ChatMessageEntity(
            conversationId = mappedConversationId,
            role = role,
            content = content,
            createdAt = message.createdAt ?: now
        )
    }
}

private suspend fun insertBackupTags(dao: TagDao, tags: List<BackupTag>): Int {
    var count = 0
    for (backupTag in tags) {
        val name = backupTag.name?.trim().orEmpty()
        if (name.isBlank()) continue
        dao.insertTag(
            Tag(
                name = name,
                color = backupTag.color ?: 4278210282L,
                isPreset = backupTag.isPreset ?: false
            )
        )
        count++
    }
    return count
}

private suspend fun insertTags(dao: TagDao, tags: List<Tag>): Int {
    var count = 0
    for (tag in tags) {
        if (tag.name.isBlank()) continue
        dao.insertTag(tag)
        count++
    }
    return count
}

private suspend fun insertEntries(
    dao: DiaryDao,
    tagDao: TagDao,
    importedTags: List<Tag>,
    entries: List<BackupEntry>,
    now: Long,
    importedEntries: MutableList<DiaryEntry>? = null
): Int {
    var count = 0
    for (entry in entries) {
        val insertedEntry = DiaryEntry(
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
        val newId = dao.insertEntry(insertedEntry)
        importedEntries?.add(insertedEntry.copy(id = newId))
        entry.tags.orEmpty().forEach { tagName ->
            findImportedTag(importedTags, tagName)?.let { tag ->
                tagDao.insertDiaryTag(DiaryTag(diaryId = newId, tagId = tag.id))
            }
        }
        count++
    }
    return count
}

private data class ImportedTodo(
    val backup: BackupTodo,
    val newId: Long
)

private suspend fun insertTodos(
    dao: TodoDao,
    todos: List<BackupTodo>,
    now: Long
): Map<Long, Long> {
    val todoIdMap = linkedMapOf<Long, Long>()
    val importedTodos = mutableListOf<ImportedTodo>()

    todos.forEach { todo ->
        val newId = dao.insertTodo(todo.toTodoItem(now = now, parentId = null))
        importedTodos += ImportedTodo(backup = todo, newId = newId)
        todo.id?.let { originalId ->
            todoIdMap[originalId] = newId
        }
    }

    importedTodos.forEach { imported ->
        val remappedParentId = remapImportedTodoParentId(imported.backup.parentId, todoIdMap) ?: return@forEach
        dao.updateTodo(
            imported.backup.toTodoItem(now = now, parentId = remappedParentId).copy(id = imported.newId)
        )
    }

    return todoIdMap
}

private suspend fun insertCountdowns(dao: CountDownDao, countdowns: List<BackupCountDown>, now: Long) {
    countdowns.forEach { item ->
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
}

private suspend fun insertCapsules(
    dao: CapsuleDao,
    capsules: List<BackupCapsule>,
    now: Long
): Map<Long, Long> {
    val capsuleIdMap = linkedMapOf<Long, Long>()
    capsules.forEach { capsule ->
        val newId = dao.insertCapsule(
            TimeCapsule(
                title = capsule.title ?: "",
                content = capsule.content ?: "",
                createdAt = capsule.createdAt ?: now,
                unlockDate = capsule.unlockDate ?: now,
                isRead = capsule.isRead ?: false,
                isOpened = capsule.isOpened ?: false,
                theme = parseBackupCapsuleTheme(capsule.theme),
                imageUri = capsule.imageUri,
                unlockHour = capsule.unlockHour ?: 0,
                unlockMinute = capsule.unlockMinute ?: 0
            )
        )
        capsule.id?.let { originalId ->
            capsuleIdMap[originalId] = newId
        }
    }
    return capsuleIdMap
}

private suspend fun insertTrash(dao: TrashDao, trashEntries: List<BackupTrashEntry>, now: Long) {
    trashEntries.forEach { entry ->
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
}

private suspend fun insertHabitRecords(
    dao: TodoDao,
    records: List<BackupHabitRecord>,
    todoIdMap: Map<Long, Long>,
    now: Long
) {
    records.forEach { record ->
        val mappedTodoId = remapImportedTodoId(record.todoId, todoIdMap) ?: return@forEach
        dao.insertHabitRecord(
            HabitRecord(
                todoId = mappedTodoId,
                recordDate = record.recordDate ?: 0,
                source = record.source ?: HabitRecord.SOURCE_MANUAL,
                summary = record.summary ?: "",
                diaryEntryId = null,
                createdAt = record.createdAt ?: now,
                updatedAt = record.updatedAt ?: now
            )
        )
    }
}

private suspend fun insertNotifications(
    dao: NotificationDao,
    notifications: List<BackupNotification>,
    capsuleIdMap: Map<Long, Long>,
    now: Long
) {
    notifications.forEach { notification ->
        val id = notification.id ?: return@forEach
        dao.insertNotification(
            NotificationEntity(
                id = id,
                type = notification.type ?: "",
                title = notification.title ?: "",
                subtitle = notification.subtitle ?: "",
                iconType = notification.iconType ?: "",
                colorHex = notification.colorHex ?: 0,
                relatedId = remapImportedNotificationRelatedId(
                    notificationType = notification.type,
                    originalRelatedId = notification.relatedId,
                    capsuleIdMap = capsuleIdMap
                ),
                isRead = notification.isRead ?: false,
                isTrashed = notification.isTrashed ?: false,
                createdAt = notification.createdAt ?: now,
                trashedAt = notification.trashedAt
            )
        )
    }
}

private suspend fun restoreChatData(dao: ChatDao, backup: DiaryBackup, now: Long) {
    val conversationIdMap = linkedMapOf<Long, Long>()
    backup.chatConversations.orEmpty().forEach { conversation ->
        val originalId = conversation.id ?: return@forEach
        val newId = dao.insertConversation(
            ChatConversationEntity(
                title = conversation.title?.trim().takeUnless { it.isNullOrEmpty() } ?: DEFAULT_IMPORTED_CONVERSATION_TITLE,
                createdAt = conversation.createdAt ?: now,
                updatedAt = conversation.updatedAt ?: (conversation.createdAt ?: now)
            )
        )
        conversationIdMap[originalId] = newId
    }

    remapBackupChatMessages(
        messages = backup.chatMessages.orEmpty(),
        conversationIdMap = conversationIdMap,
        now = now
    ).forEach { dao.insertChatMessage(it) }
}

private fun BackupTodo.toTodoItem(now: Long, parentId: Long?): TodoItem {
    return TodoItem(
        title = title ?: "",
        description = description ?: "",
        isCompleted = isCompleted ?: false,
        priority = priority ?: 0,
        dueDate = dueDate,
        createdAt = createdAt ?: now,
        completedAt = completedAt,
        sortOrder = sortOrder ?: 0,
        category = category ?: "task",
        reminderTime = reminderTime,
        tags = tags ?: "",
        parentId = parentId,
        recurringType = recurringType ?: "none",
        progress = progress ?: 0,
        isPinned = isPinned ?: false,
        // Old backups only contain serialized tag ids, which are not stable across restores.
        linkedTagIds = ""
    )
}

internal fun remapImportedTodoId(
    originalTodoId: Long?,
    todoIdMap: Map<Long, Long>
): Long? {
    val id = originalTodoId ?: return null
    return todoIdMap[id]
}

internal fun remapImportedTodoParentId(
    originalParentId: Long?,
    todoIdMap: Map<Long, Long>
): Long? {
    val id = originalParentId ?: return null
    return todoIdMap[id]
}

internal fun remapImportedNotificationRelatedId(
    notificationType: String?,
    originalRelatedId: Long?,
    capsuleIdMap: Map<Long, Long>
): Long? {
    val relatedId = originalRelatedId ?: return null
    return when (notificationType?.trim().orEmpty()) {
        "capsule" -> capsuleIdMap[relatedId]
        else -> null
    }
}

internal fun parseBackupCapsuleTheme(theme: String?): CapsuleTheme {
    val normalizedTheme = theme?.trim().orEmpty()
    if (normalizedTheme.isEmpty()) return CapsuleTheme.NORMAL
    return runCatching { CapsuleTheme.valueOf(normalizedTheme) }
        .getOrDefault(CapsuleTheme.NORMAL)
}
