package com.diary.app.ui.biography

import android.app.Application
import android.util.Log
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

enum class BiographyStyle(val label: String, val prompt: String) {
    NARRATIVE("叙事风格", "用叙事散文的方式，像写一本自传的章节"),
    TIMELINE("时间线", "按时间顺序梳理重要事件，像一份个人年表"),
    EMOTIONAL("情感视角", "从情感和内心感受出发，记录心情变化的轨迹"),
    CREATIVE("创意写作", "用文学化的手法，把日记片段编织成一个有故事感的叙事")
}

data class BiographyUiState(
    val isGenerating: Boolean = false,
    val biography: String = "",
    val selectedStyle: BiographyStyle = BiographyStyle.NARRATIVE,
    val error: String? = null,
    val entryCount: Int = 0,
    val dayCount: Int = 0
)

class BiographyViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DiaryApplication
    private val dao = app.database.diaryDao()

    private val _uiState = MutableStateFlow(BiographyUiState())
    val uiState: StateFlow<BiographyUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            val previews = withContext(Dispatchers.IO) { dao.getAllPreviewsOnce() }
            val dates = previews.map {
                java.time.Instant.ofEpochMilli(it.createdAt)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
            }.distinct()
            _uiState.value = _uiState.value.copy(
                entryCount = previews.size,
                dayCount = dates.size
            )
        }
    }

    fun selectStyle(style: BiographyStyle) {
        _uiState.value = _uiState.value.copy(selectedStyle = style)
    }

    fun generateBiography() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true, error = null, biography = "")
            try {
                // Check if AI is configured (just need API key)
                val provider = app.aiService.getActiveProvider()
                if (provider == null) {
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        error = "请先在设置中配置 AI 服务"
                    )
                    return@launch
                }

                val previews = withContext(Dispatchers.IO) { dao.getAllPreviewsOnce() }
                if (previews.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        error = "还没有日记，写几篇再来吧"
                    )
                    return@launch
                }

                // Build context from entries
                val context = buildBiographyContext(previews)
                val style = _uiState.value.selectedStyle

                val systemPrompt = """你是小墨，一个擅长写作的 AI 助手。

用户请你根据 ta 的日记内容，生成一篇个人传记。

写作要求：
- ${style.prompt}
- 用第三人称或第一人称均可，选择最合适的
- 保留真实的细节和情感
- 不要编造日记中没有的内容
- 长度在 800-1500 字之间
- 不要用格式符号（如 #、*、- 等），用纯文本
- 段落之间用空行分隔"""

                val userPrompt = """以下是用户的日记内容摘要，请据此生成一篇传记：

$context"""

                val result = app.aiService.chat(
                    AiRequest(
                        messages = listOf(
                            AiMessage("system", systemPrompt),
                            AiMessage("user", userPrompt)
                        ),
                        maxTokens = 2048,
                        temperature = 0.8f
                    ),
                    useCache = false
                )

                val reply = result.getOrNull()?.content?.trim()
                if (!reply.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        biography = reply
                    )
                } else {
                    val ex = result.exceptionOrNull()
                    val errorMsg = when {
                        ex == null -> "生成失败"
                        ex.message?.contains("NotConfigured") == true -> "请先在设置中配置 AI 服务"
                        ex.message?.contains("unexpected end of stream") == true -> "网络连接中断，请检查网络后重试"
                        ex.message?.contains("timeout") == true -> "请求超时，请稍后重试"
                        ex.message?.contains("Unable to resolve host") == true -> "无法连接到 AI 服务器，请检查网络"
                        else -> "生成失败：${ex.message}"
                    }
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        error = errorMsg
                    )
                }
            } catch (e: Exception) {
                Log.e("Biography", "Generation failed", e)
                val errorMsg = when {
                    e.message?.contains("unexpected end of stream") == true -> "网络连接中断，请检查网络后重试"
                    e.message?.contains("timeout") == true -> "请求超时，请稍后重试"
                    e.message?.contains("Unable to resolve host") == true -> "无法连接到 AI 服务器，请检查网络"
                    else -> "出了点问题，稍后再试"
                }
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    error = errorMsg
                )
            }
        }
    }

    private fun buildBiographyContext(previews: List<com.diary.app.data.DiaryPreview>): String {
        val sb = StringBuilder()

        // Basic stats
        val dates = previews.map {
            java.time.Instant.ofEpochMilli(it.createdAt)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        }.distinct().sorted()

        val firstDate = dates.firstOrNull()
        val lastDate = dates.lastOrNull()
        sb.appendLine("写作时间跨度：${firstDate} 至 ${lastDate}，共 ${previews.size} 篇日记，${dates.size} 天。")

        // Mood distribution
        val moodCounts = previews.groupBy { it.moodLevel }.mapValues { it.value.size }
        val moodSummary = moodCounts.entries.mapNotNull { (level, count) ->
            level?.let { "${com.diary.app.ui.components.moodLabelForLevel(it)} $count 次" }
        }.joinToString("、")
        if (moodSummary.isNotBlank()) {
            sb.appendLine("心情分布：$moodSummary")
        }

        // Location summary
        val locations = previews.mapNotNull { it.location }.filter { it.isNotBlank() }.distinct()
        if (locations.isNotEmpty()) {
            sb.appendLine("出现过的地点：${locations.take(10).joinToString("、")}")
        }

        // Sample entries (spread across time)
        sb.appendLine("\n日记片段摘录：")
        val sampleSize = minOf(20, previews.size)
        val step = maxOf(1, previews.size / sampleSize)
        val samples = previews.sortedByDescending { it.createdAt }.filterIndexed { index, _ -> index % step == 0 }.take(sampleSize)

        for (entry in samples) {
            val date = java.text.SimpleDateFormat("yyyy年M月d日", java.util.Locale.CHINA)
                .format(java.util.Date(entry.createdAt))
            val mood = entry.moodLevel?.let { com.diary.app.ui.components.moodLabelForLevel(it) } ?: ""
            val preview = entry.plainText.take(150)
            sb.appendLine("[$date] ${if (mood.isNotEmpty()) "($mood) " else ""}${entry.title}: $preview")
        }

        return sb.toString()
    }
}
