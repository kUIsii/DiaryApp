package com.diary.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE isTrashed = 0 ORDER BY createdAt DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE isTrashed = 0 AND type = :type ORDER BY createdAt DESC")
    fun getNotificationsByType(type: String): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE isTrashed = 1 ORDER BY trashedAt DESC")
    fun getTrashedNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE id = :id")
    suspend fun getNotificationById(id: String): NotificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationRead(id: String)

    @Query("UPDATE notifications SET isTrashed = 1, trashedAt = :trashedAt WHERE id = :id")
    suspend fun trashNotification(id: String, trashedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notifications SET isTrashed = 0, trashedAt = NULL WHERE id = :id")
    suspend fun restoreNotification(id: String)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: String)

    @Query("DELETE FROM notifications WHERE isTrashed = 1 AND trashedAt < :before")
    suspend fun deleteTrashedNotificationsBefore(before: Long)

    @Query("SELECT COUNT(*) FROM notifications WHERE isTrashed = 0 AND isRead = 0")
    fun getUnreadNotificationCount(): Flow<Int>

    @Query("SELECT * FROM notifications WHERE isTrashed = 0 AND createdAt >= :start AND createdAt < :end ORDER BY createdAt DESC")
    fun getNotificationsByDateRange(start: Long, end: Long): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications")
    suspend fun getAllNotificationsOnce(): List<NotificationEntity>

    @Query("DELETE FROM notifications")
    suspend fun deleteAllNotifications()
}
