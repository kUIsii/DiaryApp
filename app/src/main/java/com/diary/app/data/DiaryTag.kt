package com.diary.app.data

import androidx.room.Entity

@Entity(
    tableName = "diary_tag_cross_ref",
    primaryKeys = ["diaryId", "tagId"]
)
data class DiaryTag(
    val diaryId: Long,
    val tagId: Long
)
