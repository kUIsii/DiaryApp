package com.diary.app.data.ambientsound

import androidx.room.*

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val trackId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent")
data class RecentEntity(
    @PrimaryKey val trackId: String,
    val playedAt: Long = System.currentTimeMillis()
)

@Dao
interface AmbientSoundDao {
    @Query("SELECT trackId FROM favorites")
    suspend fun getFavoriteIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(fav: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE trackId = :id")
    suspend fun removeFavorite(id: String)

    @Query("SELECT trackId FROM recent ORDER BY playedAt DESC LIMIT 20")
    suspend fun getRecentIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addRecent(recent: RecentEntity)

    @Query("DELETE FROM recent")
    suspend fun clearRecent()
}
