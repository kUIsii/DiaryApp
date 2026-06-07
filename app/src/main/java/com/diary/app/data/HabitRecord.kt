package com.diary.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_records",
    indices = [
        Index(value = ["todoId", "recordDate"], unique = true),
        Index(value = ["recordDate"]),
        Index(value = ["diaryEntryId"])
    ]
)
data class HabitRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val todoId: Long,
    val recordDate: Long, // LocalDate.toEpochDay()
    val source: String = SOURCE_MANUAL,
    val summary: String = "",
    val diaryEntryId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val SOURCE_DIARY = "diary"
        const val SOURCE_MANUAL = "manual"
        const val SOURCE_DETAIL = "detail"

        fun sourceLabel(source: String): String = when (source) {
            SOURCE_DIARY -> "来自日记"
            SOURCE_DETAIL -> "更多内容"
            else -> "一句记录"
        }
    }
}
