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
    val tags: List<BackupTag>?
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

data class ImportResult(
    val entryCount: Int,
    val tagCount: Int
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

            ImportResult(
                entryCount = importedEntryCount,
                tagCount = importedTagCount
            )
        }
    }
}
