package com.diary.app.ai

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
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

class AiAssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DiaryApplication
    private val dao = app.database.diaryDao()

    private val _messages = MutableStateFlow<List<AssistantMessage>>(emptyList())
    val messages: StateFlow<List<AssistantMessage>> = _messages.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _initialized = MutableStateFlow(false)

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            val entities = withContext(Dispatchers.IO) { dao.getRecentChatMessages(100) }
            _messages.value = entities.reversed().map {
                AssistantMessage(it.id, it.role, it.content, it.role == "user", it.createdAt)
            }
            _initialized.value = true
        }
    }

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return

        viewModelScope.launch {
            // Save user message
            val userEntity = ChatMessageEntity(role = "user", content = userMessage)
            val userId = withContext(Dispatchers.IO) { dao.insertChatMessage(userEntity) }
            _messages.value = _messages.value + AssistantMessage(userId, "user", userMessage, true)

            _loading.value = true
            try {
                // Build context from recent diary entries
                val context = withContext(Dispatchers.IO) { buildContext() }

                // Build conversation history for API
                val history = _messages.value.takeLast(20).map {
                    AiMessage(role = it.role, content = it.content)
                }

                val systemPrompt = """你是这个日记应用里的专属助手，名叫小墨。你熟悉用户的日记记录，像一个老朋友一样了解ta。

你的特点：
- 自然、温暖、真实，不要说客套话
- 回复长度随意，该长则长该短则短，像正常聊天
- 不主动评判用户，但可以给出真实的想法和建议
- 可以聊日记内容、心情、生活，也可以聊任何话题
- 如果用户问你能做什么，如实说明：你可以聊天、分析日记、给建议、帮忙整理想法等
- 不要反复强调自己是AI

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
                    val assistantEntity = ChatMessageEntity(role = "assistant", content = reply)
                    val assistantId = withContext(Dispatchers.IO) { dao.insertChatMessage(assistantEntity) }
                    _messages.value = _messages.value + AssistantMessage(assistantId, "assistant", reply, false)
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "没想好怎么说"
                    val errEntity = ChatMessageEntity(role = "assistant", content = "嗯...$errorMsg")
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
                val errEntity = ChatMessageEntity(role = "assistant", content = msg)
                val errId = withContext(Dispatchers.IO) { dao.insertChatMessage(errEntity) }
                _messages.value = _messages.value + AssistantMessage(errId, "assistant", msg, false)
            } finally {
                _loading.value = false
            }

            // Trim old messages if too many
            withContext(Dispatchers.IO) {
                val count = dao.getChatMessageCount()
                if (count > 200) {
                    dao.deleteOldestChatMessages(count - 200)
                }
            }
        }
    }

    private suspend fun buildContext(): String {
        return try {
            val previews = dao.getAllPreviewsOnce()
            val recent = previews.sortedByDescending { it.createdAt }.take(5)

            if (recent.isEmpty()) return "用户还没有写过日记。"

            val sb = StringBuilder("以下是用户最近的日记摘要，供你了解ta的近况：\n")
            for (entry in recent) {
                val date = java.text.SimpleDateFormat("MM月dd日", java.util.Locale.CHINA)
                    .format(java.util.Date(entry.createdAt))
                val mood = entry.moodLevel?.let { moodLabel(it) } ?: ""
                val preview = entry.plainText.take(100)
                sb.append("- [$date] ${entry.title}${if (mood.isNotEmpty()) " ($mood)" else ""}: $preview\n")
            }

            // Add streak info
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
            if (streak > 0) sb.append("- 当前连续写作 $streak 天\n")

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
        viewModelScope.launch {
            withContext(Dispatchers.IO) { dao.deleteAllChatMessages() }
            _messages.value = emptyList()
        }
    }
}
