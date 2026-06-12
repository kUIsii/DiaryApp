package com.diary.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "diary_images",
    foreignKeys = [
        ForeignKey(
            entity = DiaryEntry::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("entryId")]
)
data class DiaryImage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val localPath: String,
    val thumbPath: String? = null,
    val mediaName: String = "",
    val mediaRef: String = "",
    val mimeType: String = "image/jpeg",
    val fileSize: Long = 0L,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
