package com.diary.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("conversationId")]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long = 0,
    val role: String,       // "user" or "assistant"
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
