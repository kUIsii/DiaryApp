package com.diary.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_conversations")
data class ChatConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "新对话",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
