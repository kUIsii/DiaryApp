package com.diary.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    // Chat messages
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun getChatMessagesByConversation(conversationId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentChatMessages(conversationId: Long, limit: Int): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages ORDER BY createdAt ASC")
    suspend fun getAllChatMessagesOnce(): List<ChatMessageEntity>

    @Insert
    suspend fun insertChatMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun deleteChatMessagesByConversation(conversationId: Long)

    @Query("SELECT COUNT(*) FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun getChatMessageCount(conversationId: Long): Int

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId AND id IN (SELECT id FROM chat_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC LIMIT :count)")
    suspend fun deleteOldestChatMessages(conversationId: Long, count: Int)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllChatMessages()

    // Chat conversations
    @Query("SELECT * FROM chat_conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ChatConversationEntity>>

    @Query("SELECT * FROM chat_conversations ORDER BY updatedAt DESC")
    suspend fun getAllConversationsOnce(): List<ChatConversationEntity>

    @Insert
    suspend fun insertConversation(conversation: ChatConversationEntity): Long

    @Query("UPDATE chat_conversations SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateConversationTitle(id: Long, title: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE chat_conversations SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateConversationTime(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM chat_conversations WHERE id = :id")
    suspend fun deleteConversation(id: Long)

    @Query("UPDATE chat_messages SET conversationId = :newConversationId WHERE conversationId = 0")
    suspend fun migrateOldMessages(newConversationId: Long)

    @Query("DELETE FROM chat_conversations")
    suspend fun deleteAllConversations()
}
