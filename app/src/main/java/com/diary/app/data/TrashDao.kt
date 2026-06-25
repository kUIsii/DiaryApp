package com.diary.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrashDao {
    @Query("SELECT * FROM trash_entries ORDER BY deletedAt DESC")
    fun getTrashEntries(): Flow<List<TrashEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrashEntry(trashEntry: TrashEntry): Long

    @Query("DELETE FROM trash_entries WHERE id = :id")
    suspend fun deleteTrashEntryById(id: Long)

    @Query("DELETE FROM trash_entries WHERE deletedAt < :before")
    suspend fun deleteTrashEntriesBefore(before: Long)

    @Query("SELECT * FROM trash_entries WHERE id = :id")
    suspend fun getTrashEntryById(id: Long): TrashEntry?

    @Query("SELECT * FROM trash_entries")
    suspend fun getAllTrashEntriesOnce(): List<TrashEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrashEntries(trashEntries: List<TrashEntry>)

    @Query("DELETE FROM trash_entries")
    suspend fun deleteAllTrashEntries()
}
