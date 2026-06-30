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
    val source: String = "ai",
    val usageCount: Int = 0,
    val lastUsedAt: Long? = null
)

enum class WritingHintTab { HINTS, SAVED, CUSTOM }

enum class WritingHintLifecycle {
    GENERATING,
    READY,
    LOCAL_FALLBACK,
    EMPTY,
    ERROR
}

data class RefineGuidance(
    val opener: String = "",
    val questions: List<String> = emptyList(),
    val angles: List<String> = emptyList(),
    val nextSteps: List<String> = emptyList()
)

data class WritingHintState(
    val hints: List<WritingHint> = emptyList(),
    val isLoading: Boolean = false,
    val errorMsg: String? = null,
    val lifecycle: WritingHintLifecycle = WritingHintLifecycle.EMPTY,
    val selectedCategory: String? = null,
    val savedHints: List<SavedHint> = emptyList(),
    val customHints: List<SavedHint> = emptyList(),
    val totalGenerated: Int = 0,
    val generationHistory: List<GenerationHistory> = emptyList(),
    val lastGeneratedAt: Long? = null,
    val activeTab: WritingHintTab = WritingHintTab.HINTS,
    val refineDialogHint: WritingHint? = null,
    val refinedContent: String? = null,
    val refineGuidance: RefineGuidance? = null,
    val isRefining: Boolean = false
) {
    val filteredHints: List<WritingHint>
        get() = if (selectedCategory == null) hints else hints.filter { it.category == selectedCategory }

    val favoriteHints: List<SavedHint>
        get() = savedHints.filter { it.isFavorite }

    val summaryHeadline: String
        get() = when (lifecycle) {
            WritingHintLifecycle.GENERATING -> "正在整理你的写作素材"
            WritingHintLifecycle.READY -> "AI 灵感已更新"
            WritingHintLifecycle.LOCAL_FALLBACK -> "本地灵感已准备好"
            WritingHintLifecycle.EMPTY -> "先从一条提示开始"
            WritingHintLifecycle.ERROR -> "灵感刷新失败"
        }

    val summarySubtitle: String
        get() = when (lifecycle) {
            WritingHintLifecycle.GENERATING -> "正在结合日记历史和收藏内容生成可直接开写的提示。"
            WritingHintLifecycle.READY -> "可以直接收藏、扩展，或把提示送到编辑器里开始写。"
            WritingHintLifecycle.LOCAL_FALLBACK -> "AI 不可用时，仍会用你的日记、情绪和自定义库生成实用提示。"
            WritingHintLifecycle.EMPTY -> "先看看通用灵感，再把有用的内容收藏成自己的库。"
            WritingHintLifecycle.ERROR -> "已保留本地结果，稍后刷新可以重新尝试。"
        }

    val lifecycleLabel: String
        get() = when (lifecycle) {
            WritingHintLifecycle.GENERATING -> "加载中"
            WritingHintLifecycle.READY -> "AI 可用"
            WritingHintLifecycle.LOCAL_FALLBACK -> "本地模式"
            WritingHintLifecycle.EMPTY -> "待补充"
            WritingHintLifecycle.ERROR -> "已回退"
        }

    val summaryChips: List<String>
        get() = buildList {
            add("${hints.size} 条灵感")
            add("${favoriteHints.size} 个收藏")
            add("${customHints.size} 条自定义")
            if (lastGeneratedAt != null) add("最近刷新")
        }

    val customPreview: List<SavedHint>
        get() = customHints
            .sortedWith(compareByDescending<SavedHint> { it.usageCount }.thenByDescending { it.createdAt })
            .take(3)

    val quickUsePreview: List<WritingHint>
        get() = (hints.take(3) + customHints.take(2).map { WritingHint(it.category, it.content, it.id) })
            .distinctBy { "${it.category}:${it.content}" }

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
        private const val PREFS_LAST_GENERATED = "last_generated_at"
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
            val lastGeneratedAt = p.getLong(PREFS_LAST_GENERATED, 0L).takeIf { it > 0L }
            _state.value = _state.value.copy(
                savedHints = savedHints,
                customHints = customHints,
                totalGenerated = totalGenerated,
                generationHistory = history,
                lastGeneratedAt = lastGeneratedAt,
                lifecycle = if (savedHints.isNotEmpty() || customHints.isNotEmpty()) {
                    WritingHintLifecycle.LOCAL_FALLBACK
                } else {
                    WritingHintLifecycle.EMPTY
                }
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

    private fun saveLastGeneratedAt(timestamp: Long) {
        prefs().edit().putLong(PREFS_LAST_GENERATED, timestamp).apply()
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
            _state.value = _state.value.copy(isLoading = true, errorMsg = null, lifecycle = WritingHintLifecycle.GENERATING)

            val entries = withContext(Dispatchers.IO) { dao.getAllPreviewsOnce() }
            saveCurrentHintsToHistory()
            if (entries.isEmpty()) {
                val hints = generateLocalHints(entries)
                val now = System.currentTimeMillis()
                _state.value = _state.value.copy(
                    hints = hints,
                    isLoading = false,
                    lifecycle = WritingHintLifecycle.LOCAL_FALLBACK,
                    lastGeneratedAt = now
                )
                saveLastGeneratedAt(now)
                return@launch
            }

            if (!app.aiService.isAiEnabled()) {
                val hints = generateLocalHints(entries)
                val now = System.currentTimeMillis()
                _state.value = _state.value.copy(
                    hints = hints,
                    isLoading = false,
                    lifecycle = WritingHintLifecycle.LOCAL_FALLBACK,
                    lastGeneratedAt = now
                )
                saveLastGeneratedAt(now)
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
                    val now = System.currentTimeMillis()
                    prefs().edit().putInt(PREFS_TOTAL_GENERATED, total).apply()
                    saveLastGeneratedAt(now)
                    _state.value = _state.value.copy(
                        hints = hints,
                        isLoading = false,
                        totalGenerated = total,
                        lifecycle = WritingHintLifecycle.READY,
                        lastGeneratedAt = now
                    )
                } else {
                    val fallback = generateLocalHints(entries)
                    val now = System.currentTimeMillis()
                    _state.value = _state.value.copy(
                        hints = fallback,
                        isLoading = false,
                        lifecycle = WritingHintLifecycle.LOCAL_FALLBACK,
                        lastGeneratedAt = now,
                        errorMsg = "AI 输出不稳定，已使用本地灵感"
                    )
                    saveLastGeneratedAt(now)
                }
            } catch (e: Exception) {
                val fallback = generateLocalHints(entries)
                val now = System.currentTimeMillis()
                _state.value = _state.value.copy(
                    hints = fallback,
                    isLoading = false,
                    errorMsg = "AI 生成失败，已使用本地推荐",
                    lifecycle = WritingHintLifecycle.ERROR,
                    lastGeneratedAt = now
                )
                saveLastGeneratedAt(now)
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
                if (it.id == hint.id) it.copy(isUsed = true, usageCount = it.usageCount + 1, lastUsedAt = System.currentTimeMillis()) else it
            }
        } else {
            _state.value.savedHints + SavedHint(
                id = hint.id,
                category = hint.category,
                content = hint.content,
                isUsed = true,
                source = "ai",
                usageCount = 1,
                lastUsedAt = System.currentTimeMillis()
            )
        }
        _state.value = _state.value.copy(savedHints = updatedSaved)
        saveSavedHints(updatedSaved)
    }

    fun saveCustomHint(category: String, content: String) {
        val normalizedCategory = category.trim().ifBlank { "日常" }
        val normalizedContent = content.trim()
        if (normalizedContent.isBlank()) return
        val hint = SavedHint(
            id = UUID.randomUUID().toString(),
            category = normalizedCategory,
            content = normalizedContent,
            source = "custom"
        )
        val updated = (_state.value.customHints.filterNot { it.category == normalizedCategory && it.content == normalizedContent } + hint)
            .sortedWith(compareByDescending<SavedHint> { it.usageCount }.thenByDescending { it.createdAt })
        _state.value = _state.value.copy(customHints = updated)
        saveCustomHints(updated)
    }

    fun markCustomHintAsUsed(hint: SavedHint) {
        val updated = _state.value.customHints.map {
            if (it.id == hint.id) it.copy(isUsed = true, usageCount = it.usageCount + 1, lastUsedAt = System.currentTimeMillis()) else it
        }
        _state.value = _state.value.copy(customHints = updated)
        saveCustomHints(updated)
    }

    fun deleteCustomHint(id: String) {
        val updated = _state.value.customHints.filter { it.id != id }
        _state.value = _state.value.copy(customHints = updated)
        saveCustomHints(updated)
    }

    fun expandHint(hint: WritingHint) {
        _state.value = _state.value.copy(
            refineDialogHint = hint,
            refinedContent = null,
            refineGuidance = null,
            isRefining = true
        )
        viewModelScope.launch {
            val fallback = buildLocalRefineGuidance(hint)
            if (!app.aiService.isAiEnabled()) {
                _state.value = _state.value.copy(
                    refinedContent = fallback.opener,
                    refineGuidance = fallback,
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
                    refinedContent = result.getOrNull()?.content?.trim().takeUnless { it.isNullOrBlank() } ?: fallback.opener,
                    refineGuidance = fallback,
                    isRefining = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    refinedContent = fallback.opener,
                    refineGuidance = fallback,
                    isRefining = false
                )
            }
        }
    }

    fun clearRefineDialog() {
        _state.value = _state.value.copy(refineDialogHint = null, refinedContent = null, refineGuidance = null, isRefining = false)
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
4. 一个能直接开写的第一句

用2-4句连贯的话给出扩展建议。"""
    }

    private fun generateLocalHints(entries: List<DiaryPreview>): List<WritingHint> {
        val hints = mutableListOf<WritingHint>()
        val hasRecent = entries.isNotEmpty()
        val recentThemes = entries.take(5).mapNotNull { preview ->
            preview.title.trim().takeIf { it.isNotBlank() }?.let { title ->
                title.replace(Regex("""[【】\[\]（）()《》]"""), "").take(18)
            } ?: preview.plainText.take(18).trim().takeIf { it.isNotBlank() }
        }.distinct().take(4)
        val dominantMood = entries.mapNotNull { it.moodLevel }.groupBy { it }.maxByOrNull { it.value.size }?.key ?: -1
        val firstRecent = entries.firstOrNull()
        val localMoodHint = when {
            dominantMood in 1..2 -> "先写下让你紧绷的那一刻，再补一件能安放自己的小事"
            dominantMood in 5..6 -> "把今天最明亮的瞬间写具体，留住那一口轻松"
            else -> "从一个真实细节开始，不用追求完整，先抓住当下"
        }

        if (hasRecent) {
            hints.add(WritingHint("反思", "回顾最近记录的一件小事，补充你当时的真实感受"))
            hints.add(WritingHint("回忆", "翻看之前的日记，找出一个被遗忘的细节，让它重新发光"))
            hints.add(WritingHint("对比", "对比今天的你和一个月前的你，有什么值得注意的变化？"))
            recentThemes.forEachIndexed { index, theme ->
                val category = if (index % 2 == 0) "观察" else "规划"
                hints.add(WritingHint(category, "围绕「$theme」写下：发生了什么、你注意到了什么、这件事为什么值得记住"))
            }
        }
        hints.add(WritingHint("感恩", "今天有什么值得感恩的三件小事？把它们写成具体场景"))
        hints.add(WritingHint("观察", "描述此刻窗外的一个细节，至少写出光线、声音和一个颜色"))
        hints.add(WritingHint("情绪", "今天哪个瞬间让你感到最真实？先写事实，再写感受"))
        hints.add(WritingHint("规划", "明天有什么期待的事？写下计划、阻力和第一步"))
        hints.add(WritingHint("创造", "如果今天是一部电影的开场，你会怎么写？"))
        hints.add(WritingHint("日常", "记录一件你每天做却从未认真描述过的事，让它变得可见"))
        hints.add(WritingHint("感恩", "谁今天对你产生了积极影响？哪怕很小，也值得写下来"))

        if (firstRecent != null && firstRecent.moodLevel != null) {
            when {
                firstRecent.moodLevel < 3 -> hints.add(WritingHint("情绪", "上次你感到低落，现在感觉如何？先写一件让自己慢下来的小事"))
                firstRecent.moodLevel > 4 -> hints.add(WritingHint("回忆", "捕捉那个让你开心的时刻，把它写成未来也愿意回看的片段"))
            }
        }

        hints.add(WritingHint("规划", localMoodHint))

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

    private fun buildLocalRefineGuidance(hint: WritingHint): RefineGuidance {
        val questions = when (hint.category) {
            "反思" -> listOf("这件事最让你在意的点是什么？", "你当时为什么会这样想？", "现在回头看，你会补充哪一句？")
            "感恩" -> listOf("这件事具体发生在什么时候？", "是谁让你被看见了？", "哪一个细节最值得记住？")
            "观察" -> listOf("你看见了什么颜色和形状？", "声音和气味是什么样的？", "如果只保留一个镜头会是什么？")
            "规划" -> listOf("你真正想推进的目标是什么？", "第一步能在 10 分钟内完成吗？", "你最担心的阻力是什么？")
            "情绪" -> listOf("身体先出现了什么反应？", "这个情绪想提醒你什么？", "如果给它命名，会叫什么？")
            "回忆" -> listOf("当时谁在场？", "哪一个细节现在还记得？", "它和今天的你有什么连接？")
            "创造" -> listOf("如果换一个视角会怎样？", "这段可以加入什么意象？", "开头能不能更有画面感？")
            "日常" -> listOf("这件事为什么这么日常却重要？", "重复动作里有什么习惯感？", "如果写成片段会怎么开头？")
            else -> listOf("这件事最真实的部分是什么？", "哪一个细节能让文字立住？", "你想把它写给谁看？")
        }
        val angles = when (hint.category) {
            "反思" -> listOf("先写事实，再写你现在的理解", "从一个小失误里找出改变的机会")
            "感恩" -> listOf("把抽象感谢变成具体场景", "写出别人做了什么、你感受到了什么")
            "观察" -> listOf("把镜头推近，写一个细节", "用五感增加画面")
            "规划" -> listOf("拆成今天能做的一步", "写下为什么值得继续做")
            "情绪" -> listOf("先写身体，再写情绪名字", "把不舒服拆成可以描述的小片段")
            "回忆" -> listOf("写一段对话或动作", "让过去和现在形成对照")
            "创造" -> listOf("给它一个比喻", "用第一人称写出声音")
            "日常" -> listOf("找出重复里的温度", "写一个你常忽略的小仪式")
            else -> listOf("先抓住最具体的一幕", "再补一句你为什么想写")
        }
        val nextSteps = listOf(
            "先写 3 句事实，不急着评价。",
            "再补 1 句感受，允许它不完整。",
            "最后用 1 句收尾，告诉自己为什么记下它。"
        )
        return RefineGuidance(
            opener = "可以先从“${hint.content.take(24)}”这件事最具体的画面写起，别急着总结，先把现场写出来。",
            questions = questions,
            angles = angles,
            nextSteps = nextSteps
        )
    }
}
