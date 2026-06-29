package com.diary.app.ui.writinghint

import android.app.Application
import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.ai.AiMessage
import com.diary.app.ai.AiRequest
import com.diary.app.data.DiaryPreview
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class HintCategory(val name: String, val icon: ImageVector)

data class WritingHint(val category: String, val content: String, val id: String = UUID.randomUUID().toString())

data class GenerationHistory(
    val hints: List<WritingHint>,
    val timestamp: Long = System.currentTimeMillis()
)

data class SavedHint(
    val id: String,
    val category: String,
    val content: String,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val isUsed: Boolean = false,
    val source: String = "ai"
)

enum class WritingHintTab { HINTS, SAVED, CUSTOM }

data class WritingHintState(
    val hints: List<WritingHint> = emptyList(),
    val isLoading: Boolean = false,
    val errorMsg: String? = null,
    val selectedCategory: String? = null,
    val savedHints: List<SavedHint> = emptyList(),
    val customHints: List<SavedHint> = emptyList(),
    val totalGenerated: Int = 0,
    val generationHistory: List<GenerationHistory> = emptyList(),
    val activeTab: WritingHintTab = WritingHintTab.HINTS,
    val refineDialogHint: WritingHint? = null,
    val refinedContent: String? = null,
    val isRefining: Boolean = false
) {
    val filteredHints: List<WritingHint>
        get() = if (selectedCategory == null) hints else hints.filter { it.category == selectedCategory }

    val favoriteHints: List<SavedHint>
        get() = savedHints.filter { it.isFavorite }

    companion object {
        val allCategories = listOf("反思", "感恩", "观察", "规划", "情绪", "回忆", "创造", "日常", "对比")

        val categoryIcons: Map<String, ImageVector> = mapOf(
            "反思" to Icons.Default.Sync,
            "感恩" to Icons.Default.Favorite,
            "观察" to Icons.Default.Visibility,
            "规划" to Icons.Default.DateRange,
            "情绪" to Icons.Default.Mood,
            "回忆" to Icons.Default.Bookmark,
            "创造" to Icons.Default.AutoAwesome,
            "日常" to Icons.Default.Home,
            "对比" to Icons.Default.Star
        )
    }
}

class WritingHintViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as DiaryApplication
    private val dao = app.database.diaryDao()
    private val gson = Gson()

    private val _state = MutableStateFlow(WritingHintState())
    val state: StateFlow<WritingHintState> = _state.asStateFlow()

    companion object {
        private const val PREFS_NAME = "writing_hint_prefs"
        private const val PREFS_SAVED_HINTS = "saved_hints"
        private const val PREFS_CUSTOM_HINTS = "custom_hints"
        private const val PREFS_TOTAL_GENERATED = "total_generated"
        private const val PREFS_HISTORY = "generation_history"
    }

    private fun prefs() = getApplication<DiaryApplication>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        loadSavedState()
        generateHints()
    }

    private fun loadSavedState() {
        val p = prefs()
        try {
            val savedType = object : TypeToken<List<SavedHint>>() {}.type
            val savedHints: List<SavedHint> = gson.fromJson(p.getString(PREFS_SAVED_HINTS, "[]"), savedType) ?: emptyList()
            val customType = object : TypeToken<List<SavedHint>>() {}.type
            val customHints: List<SavedHint> = gson.fromJson(p.getString(PREFS_CUSTOM_HINTS, "[]"), customType) ?: emptyList()
            val totalGenerated = p.getInt(PREFS_TOTAL_GENERATED, 0)
            val historyType = object : TypeToken<List<GenerationHistory>>() {}.type
            val history: List<GenerationHistory> = gson.fromJson(p.getString(PREFS_HISTORY, "[]"), historyType) ?: emptyList()
            _state.value = _state.value.copy(
                savedHints = savedHints,
                customHints = customHints,
                totalGenerated = totalGenerated,
                generationHistory = history
            )
        } catch (_: Exception) { }
    }

    private fun saveSavedHints(hints: List<SavedHint>) {
        prefs().edit().putString(PREFS_SAVED_HINTS, gson.toJson(hints)).apply()
    }

    private fun saveCustomHints(hints: List<SavedHint>) {
        prefs().edit().putString(PREFS_CUSTOM_HINTS, gson.toJson(hints)).apply()
    }

    private fun saveGenerationHistory(history: List<GenerationHistory>) {
        prefs().edit().putString(PREFS_HISTORY, gson.toJson(history)).apply()
    }

    private fun saveCurrentHintsToHistory() {
        val currentHints = _state.value.hints
        if (currentHints.isNotEmpty()) {
            val currentHistory = _state.value.generationHistory
            val entry = GenerationHistory(hints = currentHints)
            val updated = (listOf(entry) + currentHistory).take(5)
            _state.value = _state.value.copy(generationHistory = updated)
            saveGenerationHistory(updated)
        }
    }

    fun generateHints() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMsg = null)

            val entries = withContext(Dispatchers.IO) { dao.getAllPreviewsOnce() }
            saveCurrentHintsToHistory()
            if (entries.isEmpty()) {
                _state.value = _state.value.copy(
                    hints = generateLocalHints(entries),
                    isLoading = false
                )
                return@launch
            }

            if (!app.aiService.isAiEnabled()) {
                _state.value = _state.value.copy(
                    hints = generateLocalHints(entries),
                    isLoading = false
                )
                return@launch
            }

            val prompt = buildGeneratePrompt(entries)
            try {
                val result = app.aiService.chat(
                    AiRequest(
                        messages = listOf(
                            AiMessage("system", "你是一个温暖的日记写作教练，擅长根据用户的写作历史生成个性化的写作提示。"),
                            AiMessage("user", prompt)
                        ),
                        temperature = 0.8f,
                        maxTokens = 600
                    )
                )
                val hints = result.getOrNull()?.content?.let { parseHints(it) }
                if (hints != null && hints.size >= 3) {
                    val total = _state.value.totalGenerated + hints.size
                    prefs().edit().putInt(PREFS_TOTAL_GENERATED, total).apply()
                    _state.value = _state.value.copy(
                        hints = hints,
                        isLoading = false,
                        totalGenerated = total
                    )
                } else {
                    _state.value = _state.value.copy(
                        hints = generateLocalHints(entries),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    hints = generateLocalHints(entries),
                    isLoading = false,
                    errorMsg = "AI生成失败，已使用本地推荐"
                )
            }
        }
    }

    private fun buildGeneratePrompt(entries: List<DiaryPreview>): String {
        val recentCount = entries.size.coerceAtMost(15)
        val recentTitles = entries.take(recentCount).joinToString("\n") { "- ${it.title}" }
        val avgLength = entries.map { it.plainText.length }.average().toInt()
        val recentMoods = entries.take(10).mapNotNull { it.moodLevel }
        val dominantMood = if (recentMoods.isNotEmpty()) {
            recentMoods.groupBy { it }.maxByOrNull { it.value.size }?.key?.toString() ?: "未知"
        } else "未知"

        return """你是一个日记写作教练。根据用户的写作历史生成10条个性化的写作提示。

用户最近的日记标题：
$recentTitles

平均每篇字数：$avgLength
常见心情等级：$dominantMood（1-6级，1最消极6最积极）

请生成10条不同的写作提示，均匀覆盖以下类别：
反思、感恩、观察、规划、情绪、回忆、创造、日常

每条格式：
【类别】提示内容

类别必须是上述8个之一。提示要具体、有画面感、能激发写作欲望。"""
    }

    fun setCategory(category: String?) {
        _state.value = _state.value.copy(selectedCategory = category)
    }

    fun toggleFavorite(hint: WritingHint) {
        val existing = _state.value.savedHints.find { it.id == hint.id }
        val updatedSaved = if (existing != null) {
            _state.value.savedHints.map {
                if (it.id == hint.id) it.copy(isFavorite = !it.isFavorite) else it
            }
        } else {
            _state.value.savedHints + SavedHint(
                id = hint.id,
                category = hint.category,
                content = hint.content,
                isFavorite = true,
                source = "ai"
            )
        }
        _state.value = _state.value.copy(savedHints = updatedSaved)
        saveSavedHints(updatedSaved)
    }

    fun toggleFavoriteSaved(hint: SavedHint) {
        val updated = _state.value.savedHints.map {
            if (it.id == hint.id) it.copy(isFavorite = !it.isFavorite) else it
        }
        _state.value = _state.value.copy(savedHints = updated)
        saveSavedHints(updated)
    }

    fun markAsUsed(hint: WritingHint) {
        val existing = _state.value.savedHints.find { it.id == hint.id }
        val updatedSaved = if (existing != null) {
            _state.value.savedHints.map {
                if (it.id == hint.id) it.copy(isUsed = true) else it
            }
        } else {
            _state.value.savedHints + SavedHint(
                id = hint.id,
                category = hint.category,
                content = hint.content,
                isUsed = true,
                source = "ai"
            )
        }
        _state.value = _state.value.copy(savedHints = updatedSaved)
        saveSavedHints(updatedSaved)
    }

    fun saveCustomHint(category: String, content: String) {
        val hint = SavedHint(
            id = UUID.randomUUID().toString(),
            category = category,
            content = content,
            source = "custom"
        )
        val updated = _state.value.customHints + hint
        _state.value = _state.value.copy(customHints = updated)
        saveCustomHints(updated)
    }

    fun deleteCustomHint(id: String) {
        val updated = _state.value.customHints.filter { it.id != id }
        _state.value = _state.value.copy(customHints = updated)
        saveCustomHints(updated)
    }

    fun expandHint(hint: WritingHint) {
        _state.value = _state.value.copy(refineDialogHint = hint, refinedContent = null, isRefining = true)
        viewModelScope.launch {
            if (!app.aiService.isAiEnabled()) {
                _state.value = _state.value.copy(
                    refinedContent = "试试从这个角度开始：写下你的第一反应，不要思考太多。让文字自然流淌。",
                    isRefining = false
                )
                return@launch
            }
            val prompt = buildRefinePrompt(hint)
            try {
                val result = app.aiService.chat(
                    AiRequest(
                        messages = listOf(
                            AiMessage("system", "你是一个温暖的日记写作教练，擅长帮助用户深化写作思路。"),
                            AiMessage("user", prompt)
                        ),
                        temperature = 0.7f,
                        maxTokens = 300
                    )
                )
                _state.value = _state.value.copy(
                    refinedContent = result.getOrNull()?.content ?: "无法生成扩展建议，请稍后再试。",
                    isRefining = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    refinedContent = "扩展建议生成失败，请稍后再试。",
                    isRefining = false
                )
            }
        }
    }

    fun clearRefineDialog() {
        _state.value = _state.value.copy(refineDialogHint = null, refinedContent = null, isRefining = false)
    }

    fun setActiveTab(tab: WritingHintTab) {
        _state.value = _state.value.copy(activeTab = tab)
    }

    private fun buildRefinePrompt(hint: WritingHint): String {
        return """用户选中了以下写作提示，请对其进行扩展和深化。

原始提示：【${hint.category}】${hint.content}

请提供更具体的写作建议，包括：
1. 可以深入探讨的具体问题（2-3个）
2. 不同的写作角度或切入点
3. 可以结合的个人经历方向

用2-4句连贯的话给出扩展建议。"""
    }

    private fun generateLocalHints(entries: List<DiaryPreview>): List<WritingHint> {
        val hints = mutableListOf<WritingHint>()
        val hasRecent = entries.isNotEmpty()

        if (hasRecent) {
            hints.add(WritingHint("反思", "回顾最近记录的一件小事，补充你当时的真实感受"))
            hints.add(WritingHint("回忆", "翻看之前的日记，找出一个被遗忘的细节"))
            hints.add(WritingHint("对比", "对比今天的你和一个月前的你，有什么值得注意的变化？"))
        }
        hints.add(WritingHint("感恩", "今天有什么值得感恩的三件小事？"))
        hints.add(WritingHint("观察", "描述此刻窗外的一个细节——光线、声音或气味"))
        hints.add(WritingHint("情绪", "今天哪个瞬间让你感到最真实？写下来"))
        hints.add(WritingHint("规划", "明天有什么期待的事？写下你的计划"))
        hints.add(WritingHint("创造", "如果今天是一部电影的开场，你会怎么写？"))
        hints.add(WritingHint("日常", "记录一件你每天做却从未认真描述过的事"))
        hints.add(WritingHint("感恩", "谁今天对你产生了积极影响？哪怕很小"))

        if (entries.size > 5) {
            val last = entries.first()
            if (last.moodLevel != null && last.moodLevel < 3) {
                hints.add(WritingHint("情绪", "上次你感到低落，现在感觉如何？写下来释放一下"))
            } else if (last.moodLevel != null && last.moodLevel > 4) {
                hints.add(WritingHint("回忆", "捕捉那个让你开心的时刻，让未来的你也能感受到"))
            }
        }

        return hints.shuffled().take(10)
    }

    private fun parseHints(text: String): List<WritingHint> {
        val lines = text.lines().filter { it.isNotBlank() }
        val hints = mutableListOf<WritingHint>()
        val validCategories = WritingHintState.allCategories.toSet()
        for (line in lines) {
            val match = Regex("【(.+?)】(.+)").find(line)
            if (match != null) {
                val cat = match.groupValues[1]
                if (cat in validCategories) {
                    hints.add(WritingHint(cat, match.groupValues[2].trim()))
                }
            }
        }
        if (hints.size < 3) {
            for (line in lines) {
                val simpleMatch = Regex("""【(.+?)】(.+)""").find(line)
                if (simpleMatch != null) {
                    val cat = simpleMatch.groupValues[1]
                    hints.add(WritingHint(cat, simpleMatch.groupValues[2].trim()))
                }
            }
        }
        if (hints.size < 3) {
            for (line in lines) {
                val sepMatch = Regex("""(.+?)[：:─\-]\s*(.+)""").find(line)
                if (sepMatch != null) {
                    hints.add(WritingHint(sepMatch.groupValues[1], sepMatch.groupValues[2].trim()))
                }
            }
        }
        if (hints.size < 3) {
            val pads = generateLocalHints(emptyList()).shuffled()
            hints.addAll(pads.take(3 - hints.size))
        }
        return hints.take(10)
    }
}
