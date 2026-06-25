package com.diary.app.data.repository

import com.diary.app.data.DiaryDao
import com.diary.app.data.DiaryEntry
import com.diary.app.data.DiaryImage
import com.diary.app.data.DiaryPreview
import com.diary.app.data.DiaryTag
import com.diary.app.data.MediaDao
import com.diary.app.data.RecentLocation
import com.diary.app.data.Tag
import com.diary.app.data.TagDao
import com.diary.app.data.TrashDao
import com.diary.app.data.TrashEntry
import com.diary.app.data.normalizeContentForExport
import kotlinx.coroutines.flow.Flow

class DiaryEntryRepository(
    private val dao: DiaryDao,
    private val tagDao: TagDao,
    private val mediaDao: MediaDao,
    private val trashDao: TrashDao
) {

    // ── Diary Entry methods ──────────────────────────────────────

    fun getAllPreviews(): Flow<List<DiaryPreview>> = dao.getAllPreviews()
    fun getAllPreviewsOnce(): Flow<List<DiaryPreview>> = dao.getAllPreviews()
    suspend fun getPreviewById(id: Long): DiaryPreview? = dao.getPreviewById(id)
    suspend fun getEntryByIdSafe(id: Long): DiaryEntry? = dao.getEntryByIdSafe(id)
    suspend fun getEntryById(id: Long): DiaryEntry? = dao.getEntryById(id)
    suspend fun insertEntry(entry: DiaryEntry): Long = dao.insertEntry(entry)
    suspend fun updateEntry(entry: DiaryEntry) = dao.updateEntry(entry)
    suspend fun deleteEntry(entry: DiaryEntry) = dao.deleteEntry(entry)
    suspend fun deleteEntryById(id: Long) = dao.deleteEntryById(id)
    suspend fun getEntryCount(): Int = dao.getEntryCount()
    suspend fun getRandomEntryId(): Long? = dao.getRandomEntryId()

    suspend fun moveToTrash(entry: DiaryEntry) {
        trashDao.insertTrashEntry(TrashEntry(
            originalId = entry.id, title = entry.title,
            content = normalizeContentForExport(entry.content),
            plainText = entry.plainText, moodLevel = entry.moodLevel,
            weather = entry.weather, location = entry.location,
            latitude = entry.latitude, longitude = entry.longitude,
            isFavorite = entry.isFavorite, createdAt = entry.createdAt,
            updatedAt = entry.updatedAt
        ))
        dao.deleteEntryWithTags(entry)
    }

    suspend fun moveToTrashBatch(entries: List<DiaryEntry>) {
        trashDao.insertTrashEntries(entries.map { e -> TrashEntry(
            originalId = e.id, title = e.title,
            content = normalizeContentForExport(e.content),
            plainText = e.plainText, moodLevel = e.moodLevel,
            weather = e.weather, location = e.location,
            latitude = e.latitude, longitude = e.longitude,
            isFavorite = e.isFavorite, createdAt = e.createdAt,
            updatedAt = e.updatedAt
        ) })
        dao.deleteEntriesWithTags(entries)
    }

    fun searchPreviews(query: String): Flow<List<DiaryPreview>> = dao.searchPreviews(query)
    suspend fun searchPreviewsOnce(query: String): List<DiaryPreview> = dao.searchPreviewsOnce(query)
    fun getAllTimestamps(): Flow<List<Long>> = dao.getAllTimestamps()
    suspend fun getPreviewsByDateRange(start: Long, end: Long) = dao.getPreviewsByDateRange(start, end)
    fun getPreviewsByDateRangeFlow(start: Long, end: Long) = dao.getPreviewsByDateRangeFlow(start, end)
    suspend fun getPreviewsByMonthDay(month: Int, day: Int) = dao.getPreviewsByMonthDay(month, day)
    fun getOnThisDayPreviews(month: Int, day: Int, year: Int, excludeId: Long) = dao.getOnThisDayPreviews(month, day, year, excludeId)

    // ── Tag methods (delegated to TagDao) ────────────────────────

    fun getAllTags(): Flow<List<Tag>> = tagDao.getAllTags()
    suspend fun getAllTagsOnce(): List<Tag> = tagDao.getAllTagsOnce()
    suspend fun insertTag(tag: Tag): Long = tagDao.insertTag(tag)
    suspend fun deleteTag(tag: Tag) = tagDao.deleteTag(tag)
    suspend fun updateTagById(id: Long, name: String, color: Long) = tagDao.updateTagById(id, name, color)
    suspend fun getTagByName(name: String): Tag? = tagDao.getTagByName(name)
    fun getTagUsage() = tagDao.getTagUsage()
    fun getAllDiaryTagPairs() = tagDao.getAllDiaryTagPairs()

    // ── Media methods (delegated to MediaDao) ────────────────────

    suspend fun getImagesForEntry(entryId: Long) = mediaDao.getImagesForEntry(entryId)
    suspend fun deleteImagesForEntry(entryId: Long) = mediaDao.deleteImagesForEntry(entryId)
    suspend fun getAllImages() = mediaDao.getAllImages()

    // ── Trash methods (delegated to TrashDao) ────────────────────

    fun getTrashEntries(): Flow<List<TrashEntry>> = trashDao.getTrashEntries()

    // ── Other diary entry methods ────────────────────────────────

    suspend fun getRecentLocations() = dao.getRecentLocations()
    suspend fun getAverageWritingDurationSeconds() = dao.getAverageWritingDurationSeconds()
}
