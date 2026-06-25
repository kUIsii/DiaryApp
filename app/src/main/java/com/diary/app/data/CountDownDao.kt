package com.diary.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CountDownDao {
    @Query("SELECT * FROM countdown_items ORDER BY isPinned DESC, targetDate ASC")
    fun getAllCountDownItems(): Flow<List<CountDownItem>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCountDownItem(item: CountDownItem): Long

    @Update
    suspend fun updateCountDownItem(item: CountDownItem)

    @Query("DELETE FROM countdown_items WHERE id = :id")
    suspend fun deleteCountDownItem(id: Long)

    @Query("SELECT * FROM countdown_items WHERE id = :id")
    suspend fun getCountDownItemById(id: Long): CountDownItem?

    @Query("SELECT * FROM countdown_items ORDER BY isPinned DESC, targetDate ASC LIMIT :limit")
    suspend fun getTopCountDownItems(limit: Int = 10): List<CountDownItem>

    @Query("SELECT * FROM countdown_items ORDER BY isPinned DESC, targetDate ASC")
    suspend fun getAllCountDownItemsOnce(): List<CountDownItem>

    @Query("DELETE FROM countdown_items")
    suspend fun deleteAllCountDownItems()
}
