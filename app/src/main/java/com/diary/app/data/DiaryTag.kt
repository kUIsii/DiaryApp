package com.diary.app.data

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "diary_tag_cross_ref",
    primaryKeys = ["diaryId", "tagId"],
    indices = [
        Index(value = ["tagId"]),
        Index(value = ["diaryId"])
    ]
)
data class DiaryTag(
    val diaryId: Long,
    val tagId: Long
)
