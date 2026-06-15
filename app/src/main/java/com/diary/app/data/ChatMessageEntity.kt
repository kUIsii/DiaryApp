package com.diary.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,       // "user" or "assistant"
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
