package com.diary.app.ui.writinghint

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.ai.AiMessage
import com.diary.app.ai.AiRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class WritingHint(
    val category: String,
    val content: String
)

data class WritingHintState(
    val hints: List<WritingHint> = emptyList(),
    val isLoading: Boolean = false,
    val errorMsg: String? = null
)

class WritingHintViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as DiaryApplication
    private val dao = app.database.diaryDao()

    private val _state = MutableStateFlow(WritingHintState())
    val state: StateFlow<WritingHintState> = _state.asStateFlow()

    init {
        generateHints()
    }

    fun generateHints() {
        viewModelScope.launch {
            _state.value = WritingHintState(isLoading = true)

            val entries = withContext(Dispatchers.IO) { dao.getAllPreviewsOnce() }
            if (entries.isEmpty()) {
                _state.value = WritingHintState(
                    hints = listOf(
                        WritingHint("入门", "写下今天让你印象最深的一件事"),
                        WritingHint("入门", "描述你此刻的心情，用一个词概括"),
                        WritingHint("探索", "试试用第三人称记录今天发生了什么")
                    ),
                    isLoading = false
                )
                return@launch
            }

            if (!app.aiService.isAiEnabled()) {
                _state.value = WritingHintState(
                    hints = generateLocalHints(entries),
                    isLoading = false
                )
                return@launch
            }

            val recentCount = entries.size.coerceAtMost(10)
            val recentTitles = entries.take(recentCount).joinToString("\n") { "- ${it.title}" }
            val avgLength = entries.map { it.plainText.length }.average().toInt()
            val recentMoods = entries.take(10).mapNotNull { it.moodLevel }
            val dominantMood = if (recentMoods.isNotEmpty()) {
                recentMoods.groupBy { it }.maxByOrNull { it.value.size }?.key?.toString() ?: "未知"
            } else "未知"

            val prompt = """你是一个日记写作教练。根据用户的写作历史生成3条个性化的写作提示来激发写作灵感。

用户最近的日记标题：
$recentTitles

用户平均每篇日记字数：$avgLength
最近常见心情等级：$dominantMood（1-6级，1最消极6最积极）

请生成3条不同的写作提示，格式：
【类别】提示内容
类别用2-4个字概括（如：反思、感恩、观察、规划、情绪、回忆等）。"""

            try {
                val result = app.aiService.chat(
                    AiRequest(
                        messages = listOf(
                            AiMessage("system", "你是一个温暖的日记写作教练，擅长根据用户的写作历史生成个性化的写作提示。"),
                            AiMessage("user", prompt)
                        ),
                        temperature = 0.8f,
                        maxTokens = 400
                    )
                )

                val hints = result.getOrNull()?.content?.let { parseHints(it) }
                if (hints != null && hints.isNotEmpty()) {
                    _state.value = WritingHintState(hints = hints, isLoading = false)
                } else {
                    _state.value = WritingHintState(hints = generateLocalHints(entries), isLoading = false)
                }
            } catch (e: Exception) {
                _state.value = WritingHintState(
                    hints = generateLocalHints(entries),
                    isLoading = false,
                    errorMsg = "AI生成失败，使用本地推荐"
                )
            }
        }
    }

    private fun generateLocalHints(entries: List<com.diary.app.data.DiaryPreview>): List<WritingHint> {
        val hints = mutableListOf<WritingHint>()
        val hasRecentContent = entries.isNotEmpty()

        if (hasRecentContent) {
            hints.add(WritingHint("回顾", "回顾最近记录的一件小事，补充你当时的感受"))
            hints.add(WritingHint("对比", "对比今天的你和一个月前的你，有什么变化？"))
        }
        hints.add(WritingHint("感恩", "今天有什么值得感恩的三件小事？"))
        hints.add(WritingHint("观察", "描述你此刻窗外的一个细节"))
        hints.add(WritingHint("感受", "今天哪个瞬间让你感到最真实？"))

        if (entries.size > 5) {
            val lastEntry = entries.first()
            if (lastEntry.moodLevel != null && lastEntry.moodLevel!! < 3) {
                hints.add(WritingHint("情绪", "上次你感到低落，现在感觉如何？写下来释放一下"))
            } else if (lastEntry.moodLevel != null && lastEntry.moodLevel!! > 4) {
                hints.add(WritingHint("记录", "捕捉那个让你开心的时刻，让未来的你也能感受到"))
            }
        }

        return hints.shuffled().take(3)
    }

    private fun parseHints(text: String): List<WritingHint> {
        val lines = text.lines().filter { it.isNotBlank() }
        val hints = mutableListOf<WritingHint>()
        for (line in lines) {
            val match = Regex("【(.+?)】(.+)").find(line)
            if (match != null) {
                hints.add(WritingHint(match.groupValues[1], match.groupValues[2].trim()))
            }
        }
        return hints.take(3)
    }
}
