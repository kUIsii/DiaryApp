package com.diary.app.ai

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.ChatConversationEntity
import com.diary.app.data.ChatMessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException

data class AssistantMessage(
    val id: Long = 0,
    val role: String,
    val content: String,
    val isUser: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)

data class ConversationInfo(
    val id: Long,
    val title: String,
    val updatedAt: Long
)

class AiAssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DiaryApplication
    private val dao = app.database.diaryDao()

    private val _messages = MutableStateFlow<List<AssistantMessage>>(emptyList())
    val messages: StateFlow<List<AssistantMessage>> = _messages.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _conversations = MutableStateFlow<List<ConversationInfo>>(emptyList())
    val conversations: StateFlow<List<ConversationInfo>> = _conversations.asStateFlow()

    private val _currentConversationId = MutableStateFlow<Long>(0)
    val currentConversationId: StateFlow<Long> = _currentConversationId.asStateFlow()

    init {
        loadConversations()
    }

    private fun loadConversations() {
        viewModelScope.launch {
            val convs = withContext(Dispatchers.IO) { dao.getAllConversationsOnce() }
            if (convs.isEmpty()) {
                // Create a default conversation
                val id = withContext(Dispatchers.IO) {
                    dao.insertConversation(ChatConversationEntity(title = "新对话"))
                }
                _currentConversationId.value = id
                _conversations.value = listOf(ConversationInfo(id, "新对话", System.currentTimeMillis()))
            } else {
                _conversations.value = convs.map { ConversationInfo(it.id, it.title, it.updatedAt) }
                _currentConversationId.value = convs.first().id
                loadMessages(convs.first().id)
            }
        }
    }

    private fun loadMessages(conversationId: Long) {
        viewModelScope.launch {
            val entities = withContext(Dispatchers.IO) { dao.getRecentChatMessages(conversationId, 100) }
            _messages.value = entities.reversed().map {
                AssistantMessage(it.id, it.role, it.content, it.role == "user", it.createdAt)
            }
        }
    }

    fun switchConversation(conversationId: Long) {
        _currentConversationId.value = conversationId
        loadMessages(conversationId)
    }

    fun createNewConversation() {
        viewModelScope.launch {
            val id = withContext(Dispatchers.IO) {
                dao.insertConversation(ChatConversationEntity(title = "新对话"))
            }
            _currentConversationId.value = id
            _messages.value = emptyList()
            // Refresh conversation list
            val convs = withContext(Dispatchers.IO) { dao.getAllConversationsOnce() }
            _conversations.value = convs.map { ConversationInfo(it.id, it.title, it.updatedAt) }
        }
    }

    fun deleteConversation(conversationId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { dao.deleteConversation(conversationId) }
            // If deleting current conversation, switch to another or create new
            if (_currentConversationId.value == conversationId) {
                val convs = withContext(Dispatchers.IO) { dao.getAllConversationsOnce() }
                if (convs.isEmpty()) {
                    createNewConversation()
                } else {
                    switchConversation(convs.first().id)
                }
            }
            // Refresh list
            val convs = withContext(Dispatchers.IO) { dao.getAllConversationsOnce() }
            _conversations.value = convs.map { ConversationInfo(it.id, it.title, it.updatedAt) }
        }
    }

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return
        val convId = _currentConversationId.value
        if (convId == 0L) return

        viewModelScope.launch {
            // Save user message
            val userEntity = ChatMessageEntity(conversationId = convId, role = "user", content = userMessage)
            val userId = withContext(Dispatchers.IO) { dao.insertChatMessage(userEntity) }
            _messages.value = _messages.value + AssistantMessage(userId, "user", userMessage, true)

            // Update conversation title if first message
            if (_messages.value.size == 1) {
                val title = userMessage.take(20) + if (userMessage.length > 20) "..." else ""
                withContext(Dispatchers.IO) {
                    dao.insertConversation(ChatConversationEntity(id = convId, title = title))
                }
                // Refresh conversation list
                val convs = withContext(Dispatchers.IO) { dao.getAllConversationsOnce() }
                _conversations.value = convs.map { ConversationInfo(it.id, it.title, it.updatedAt) }
            }

            _loading.value = true
            try {
                // Build context from recent diary entries
                val context = withContext(Dispatchers.IO) { buildContext() }

                // Build conversation history for API
                val history = _messages.value.takeLast(20).map {
                    AiMessage(role = it.role, content = it.content)
                }

                val systemPrompt = """你是小墨，这个日记应用里的聊天伙伴。你看过用户的日记，了解ta的生活。

说话风格：
- 像朋友聊天一样自然，不要太正式
- 不要用任何格式符号（比如星号、井号、破折号列表），就用普通文字
- 回复适中长度，不要太短显得敷衍，也不要太长像在写文章
- 可以有自己的想法，不用总是赞同用户
- 偶尔可以问问用户的近况，但不要每次都问

你能做的事：
- 聊天，聊日记内容、心情、生活琐事
- 帮用户整理思路、分析问题
- 给建议，但不是说教
- 如果用户问你能做什么，直接说

不要做的事：
- 不要说"作为AI"、"我可以帮你"这类话
- 不要用markdown格式
- 不要每次开头都问好

$context"""

                val messages = listOf(AiMessage("system", systemPrompt)) + history

                val result = app.aiService.chat(
                    AiRequest(
                        messages = messages,
                        maxTokens = 512,
                        temperature = 0.85f
                    ),
                    useCache = false
                )

                val reply = result.getOrNull()?.content?.trim()
                if (!reply.isNullOrBlank()) {
                    val assistantEntity = ChatMessageEntity(conversationId = convId, role = "assistant", content = reply)
                    val assistantId = withContext(Dispatchers.IO) { dao.insertChatMessage(assistantEntity) }
                    _messages.value = _messages.value + AssistantMessage(assistantId, "assistant", reply, false)
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "没想好怎么说"
                    val errEntity = ChatMessageEntity(conversationId = convId, role = "assistant", content = "嗯...$errorMsg")
                    val errId = withContext(Dispatchers.IO) { dao.insertChatMessage(errEntity) }
                    _messages.value = _messages.value + AssistantMessage(errId, "assistant", "嗯...$errorMsg", false)
                }
            } catch (e: Exception) {
                Log.e("AiAssistant", "Chat failed", e)
                val msg = when {
                    e is SocketTimeoutException -> "等太久了，网络不太好"
                    e.message?.contains("timeout", true) == true -> "等太久了，网络不太好"
                    else -> "出了点问题，稍后再聊"
                }
                val errEntity = ChatMessageEntity(conversationId = convId, role = "assistant", content = msg)
                val errId = withContext(Dispatchers.IO) { dao.insertChatMessage(errEntity) }
                _messages.value = _messages.value + AssistantMessage(errId, "assistant", msg, false)
            } finally {
                _loading.value = false
            }

            // Update conversation time
            withContext(Dispatchers.IO) { dao.updateConversationTime(convId) }

            // Trim old messages if too many
            withContext(Dispatchers.IO) {
                val count = dao.getChatMessageCount(convId)
                if (count > 200) {
                    dao.deleteOldestChatMessages(convId, count - 200)
                }
            }
        }
    }

    private suspend fun buildContext(): String {
        return try {
            val previews = dao.getAllPreviewsOnce()
            if (previews.isEmpty()) return "用户还没有写过日记。"

            val sb = StringBuilder()

            // Streak info
            val dates = previews.map {
                java.time.Instant.ofEpochMilli(it.createdAt)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
            }.distinct().sortedDescending()

            var streak = 0
            val today = java.time.LocalDate.now()
            for (i in dates.indices) {
                val expected = today.minusDays(i.toLong())
                if (dates[i] == expected) streak++ else break
            }

            val totalEntries = previews.size
            val totalDays = dates.size

            sb.appendLine("用户已写作 $totalEntries 篇日记，跨越 $totalDays 天。")
            if (streak > 0) sb.appendLine("当前连续写作 $streak 天。")

            // Mood distribution
            val moodCounts = previews.groupBy { it.moodLevel }.mapValues { it.value.size }
            val topMood = moodCounts.filterKeys { it != null }.maxByOrNull { it.value }
            if (topMood != null) {
                sb.appendLine("最常见的心情：${moodLabel(topMood.key!!)}（${topMood.value}次）")
            }

            // Random selection of entries (not just recent ones)
            val randomEntries = previews.shuffled().take(3)
            sb.appendLine("\n随机抽取的几篇日记片段：")
            for (entry in randomEntries) {
                val date = java.text.SimpleDateFormat("MM月dd日", java.util.Locale.CHINA)
                    .format(java.util.Date(entry.createdAt))
                val preview = entry.plainText.take(80)
                sb.appendLine("- [$date] ${entry.title}: $preview...")
            }

            // Recent entries
            val recent = previews.sortedByDescending { it.createdAt }.take(3)
            sb.appendLine("\n最近的日记：")
            for (entry in recent) {
                val date = java.text.SimpleDateFormat("MM月dd日", java.util.Locale.CHINA)
                    .format(java.util.Date(entry.createdAt))
                val mood = entry.moodLevel?.let { moodLabel(it) } ?: ""
                val preview = entry.plainText.take(60)
                sb.appendLine("- [$date] ${entry.title}${if (mood.isNotEmpty()) " ($mood)" else ""}: $preview...")
            }

            sb.toString()
        } catch (e: Exception) {
            Log.e("AiAssistant", "Failed to build context", e)
            ""
        }
    }

    private fun moodLabel(level: Int): String = when (level) {
        1 -> "开心"
        2 -> "平静"
        3 -> "一般"
        4 -> "低落"
        5 -> "难过"
        6 -> "焦虑"
        else -> ""
    }

    fun clearHistory() {
        val convId = _currentConversationId.value
        if (convId == 0L) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { dao.deleteChatMessagesByConversation(convId) }
            _messages.value = emptyList()
        }
    }
}
