package com.diary.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 地点触发回忆 - 地理围栏
 */
@Entity(
    tableName = "location_memories",
    indices = [
        Index(value = ["latitude", "longitude"]),
        Index(value = ["diaryId"])
    ]
)
data class LocationMemory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val diaryId: Long,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 100f,
    val locationName: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
