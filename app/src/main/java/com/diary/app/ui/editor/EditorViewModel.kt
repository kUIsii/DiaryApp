package com.diary.app.ui.editor

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.ai.aiRequest
import com.diary.app.data.DiaryEntry
import com.diary.app.data.DiaryImage
import com.diary.app.data.DiaryMediaManager
import com.diary.app.data.DiaryPreview
import com.diary.app.data.DiaryTag
import com.diary.app.data.RecentLocation
import com.diary.app.data.Tag
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class DraftData(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String,
    val plainText: String,
    val title: String,
    val moodLevel: Int?,
    val weather: String?,
    val tagIds: Set<Long>,
    val timestamp: Long,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

class EditorViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()
    private val appContext = application.applicationContext
    private val prefs = application.getSharedPreferences("editor_drafts", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _currentEntry = MutableStateFlow<DiaryEntry?>(null)
    val currentEntry = _currentEntry.asStateFlow()

    private val _allTags = MutableStateFlow<List<Tag>>(emptyList())
    val allTags = _allTags.asStateFlow()

    private val _selectedTagIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTagIds = _selectedTagIds.asStateFlow()

    private val _recentLocations = MutableStateFlow<List<RecentLocation>>(emptyList())
    val recentLocations = _recentLocations.asStateFlow()

    // Auto-save and word count state
    private val _autoSaveVisible = MutableStateFlow(false)
    val autoSaveVisible = _autoSaveVisible.asStateFlow()

    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges = _hasUnsavedChanges.asStateFlow()

    // Latest content cache for auto-save
    private var latestContent: String = ""
    private var latestPlainText: String = ""
    private var latestTitle: String = ""

    // Writing duration tracking
    private var writingStartTime: Long = 0L
    private val _writingDuration = MutableStateFlow(0L) // in seconds
    val writingDuration = _writingDuration.asStateFlow()

    fun startWritingTimer() {
        if (writingStartTime == 0L) {
            writingStartTime = System.currentTimeMillis()
        }
    }

    fun updateWritingDuration() {
        if (writingStartTime > 0) {
            val elapsed = (System.currentTimeMillis() - writingStartTime) / 1000
            _writingDuration.value = elapsed
        }
    }

    fun getFormattedDuration(): String {
        val seconds = _writingDuration.value
        return when {
            seconds < 60 -> "${seconds}秒"
            seconds < 3600 -> "${seconds / 60}分钟"
            else -> "${seconds / 3600}小时${(seconds % 3600) / 60}分钟"
        }
    }

    // Writing prompt
    private val _writingPrompt = MutableStateFlow("")
    val writingPrompt = _writingPrompt.asStateFlow()

    fun loadWritingPrompt(moodLevel: Int? = null) {
        _writingPrompt.value = com.diary.app.data.WritingPrompts.getPrompt(moodLevel)
    }

    fun refreshPrompt() {
        _writingPrompt.value = com.diary.app.data.WritingPrompts.getRandomPrompt()
    }

    // --- AI Silent Title ---
    private val _aiTitleSuggestion = MutableStateFlow<String?>(null)
    val aiTitleSuggestion = _aiTitleSuggestion.asStateFlow()
    private var titleJob: Job? = null

    fun onPlainTextForTitleChanged(plainText: String, currentTitle: String) {
        val app = getApplication<Application>()
        val features = (app as DiaryApplication).experimentalFeatures.value
        if (!features.aiEnabled || !features.aiSilentTitle) return
        if (plainText.length <= 50 || currentTitle.isNotBlank()) {
            _aiTitleSuggestion.value = null
            titleJob?.cancel()
            return
        }
        titleJob?.cancel()
        titleJob = viewModelScope.launch {
            delay(3000)
            try {
                val aiService = (app as DiaryApplication).aiService
                val request = aiRequest(
                    userMessage = plainText.take(500),
                    systemPrompt = "根据以下日记内容，生成一个简短的中文标题（8字以内）。只输出标题，不要任何解释。",
                    temperature = 0.6f,
                    maxTokens = 32
                )
                aiService.chat(request, useCache = false).getOrNull()?.let { resp ->
                    val title = resp.content.trim().take(8)
                    if (title.isNotBlank()) _aiTitleSuggestion.value = title
                }
            } catch (_: Exception) { }
        }
    }

    fun consumeTitleSuggestion() {
        _aiTitleSuggestion.value = null
    }

    // --- AI Memory Echo ---
    data class MemoryEcho(val summary: String, val date: String)

    private val _memoryEcho = MutableStateFlow<MemoryEcho?>(null)
    val memoryEcho = _memoryEcho.asStateFlow()
    private var memoryEchoTriggered = false

    fun triggerMemoryEcho(currentPlainText: String, currentMood: Int?) {
        val app = getApplication<Application>()
        val features = (app as DiaryApplication).experimentalFeatures.value
        if (!features.aiEnabled || !features.aiMemoryEcho) return
        if (memoryEchoTriggered) return
        if (currentPlainText.length <= 100 && currentMood == null) return
        memoryEchoTriggered = true

        viewModelScope.launch {
            try {
                val previews = dao.getAllPreviewsOnce()
                val recent30 = previews.sortedByDescending { it.createdAt }.take(30)
                if (recent30.isEmpty()) return@launch

                val snippets = recent30.mapIndexed { idx, p ->
                    val date = Instant.ofEpochMilli(p.createdAt)
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    val text = p.plainText.take(80)
                    "[${idx + 1}] $date: $text"
                }.joinToString("\n")

                val aiService = (app as DiaryApplication).aiService
                val request = aiRequest(
                    userMessage = "当前内容：${currentPlainText.take(200)}\n\n历史日记：\n$snippets",
                    systemPrompt = "从以下历史日记片段中，选出与当前内容情感最相关的一条。输出格式：[序号]。只输出数字。",
                    temperature = 0.3f,
                    maxTokens = 8
                )
                val result = aiService.chat(request, useCache = false).getOrNull()
                val idx = result?.content?.trim()?.filter { it.isDigit() }?.toIntOrNull()
                if (idx != null && idx in 1..recent30.size) {
                    val matched = recent30[idx - 1]
                    val date = Instant.ofEpochMilli(matched.createdAt)
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("M月d日"))
                    val summary = matched.plainText.take(40)
                    _memoryEcho.value = MemoryEcho(summary, date)
                }
            } catch (_: Exception) { }
        }
    }

    // --- AI Tag Intuition ---
    private val _aiRecommendedTagIds = MutableStateFlow<Set<Long>>(emptySet())
    val aiRecommendedTagIds = _aiRecommendedTagIds.asStateFlow()
    private var tagJob: Job? = null

    fun onPlainTextForTagsChanged(plainText: String) {
        val app = getApplication<Application>()
        val features = (app as DiaryApplication).experimentalFeatures.value
        if (!features.aiEnabled || !features.aiTagIntuition) return
        if (plainText.length <= 80) {
            _aiRecommendedTagIds.value = emptySet()
            tagJob?.cancel()
            return
        }
        tagJob?.cancel()
        tagJob = viewModelScope.launch {
            delay(2000)
            try {
                val tags = _allTags.value
                if (tags.isEmpty()) return@launch
                val tagNames = tags.joinToString("、") { it.name }
                val aiService = (app as DiaryApplication).aiService
                val request = aiRequest(
                    userMessage = "日记内容：${plainText.take(300)}\n\n标签列表：$tagNames",
                    systemPrompt = "从以下标签列表中选出最相关的2-3个，只输出标签名，逗号分隔。不要输出其他内容。",
                    temperature = 0.3f,
                    maxTokens = 32
                )
                val result = aiService.chat(request, useCache = false).getOrNull()
                if (result != null) {
                    val names = result.content.trim().split(",", "，", " ").map { it.trim() }
                    val matched = tags.filter { tag -> names.any { it == tag.name } }.map { it.id }.toSet()
                    if (matched.isNotEmpty()) _aiRecommendedTagIds.value = matched
                }
            } catch (_: Exception) { }
        }
    }

    fun onPlainTextForTitleAndTagsChanged(plainText: String, currentTitle: String) {
        onPlainTextForTitleChanged(plainText, currentTitle)
        onPlainTextForTagsChanged(plainText)
    }

    fun markContentChanged() {
        _hasUnsavedChanges.value = true
    }

    fun updateLatestContent(content: String, plainText: String, title: String) {
        latestContent = content
        latestPlainText = plainText
        latestTitle = title
    }

    fun performAutoSave(diaryId: Long?, moodLevel: Int?, weather: String?,
                        location: String? = null, latitude: Double? = null, longitude: Double? = null) {
        if (latestContent.isEmpty() && latestPlainText.isEmpty()) return
        saveDraft(latestContent, latestPlainText, diaryId, latestTitle, moodLevel, weather, location, latitude, longitude)
        _autoSaveVisible.value = true
    }

    fun hideAutoSaveIndicator() {
        _autoSaveVisible.value = false
    }

    fun onManualSaveCompleted(diaryId: Long?) {
        clearDraftsForSavedEntry(diaryId)
        _hasUnsavedChanges.value = false
    }

    fun discardChanges(diaryId: Long?) {
        clearDraft(diaryId)
        latestContent = ""
        latestPlainText = ""
        _hasUnsavedChanges.value = false
    }

    // Draft management
    private fun draftKey(diaryId: Long?): String {
        return if (diaryId != null) "draft_$diaryId" else "draft_new"
    }

    fun saveDraft(
        content: String, plainText: String, diaryId: Long?,
        title: String, moodLevel: Int?, weather: String?,
        location: String? = null, latitude: Double? = null, longitude: Double? = null
    ) {
        val data = DraftData(content = content, plainText = plainText, title = title, moodLevel = moodLevel, weather = weather, tagIds = _selectedTagIds.value, timestamp = System.currentTimeMillis(), location = location, latitude = latitude, longitude = longitude)
        prefs.edit().putString(draftKey(diaryId), gson.toJson(data)).apply()
    }

    fun loadDraft(diaryId: Long?): DraftData? {
        val json = prefs.getString(draftKey(diaryId), null) ?: return null
        return try { gson.fromJson(json, DraftData::class.java) } catch (_: Exception) { null }
    }

    fun clearDraft(diaryId: Long?) {
        prefs.edit().remove(draftKey(diaryId)).apply()
    }

    fun clearDraftsForSavedEntry(diaryId: Long?) {
        val editor = prefs.edit()
        draftKeysToClear(diaryId).forEach(editor::remove)
        editor.apply()
    }

    // Draft list management
    private fun getAllDraftIds(): MutableSet<String> {
        return prefs.getStringSet("draft_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
    }

    private fun saveDraftIds(ids: Set<String>) {
        prefs.edit().putStringSet("draft_ids", ids).apply()
    }

    fun getAllDrafts(): List<DraftData> {
        val ids = getAllDraftIds()
        return ids.mapNotNull { id ->
            val json = prefs.getString(draftListItemKey(id), null) ?: return@mapNotNull null
            try { gson.fromJson(json, DraftData::class.java) } catch (_: Exception) { null }
        }.sortedByDescending { it.timestamp }
    }

    fun saveDraftToList(
        content: String, plainText: String, title: String,
        moodLevel: Int?, weather: String?,
        location: String? = null, latitude: Double? = null, longitude: Double? = null,
        existingDraftId: String? = null
    ): String {
        val id = existingDraftId ?: java.util.UUID.randomUUID().toString()
        val data = DraftData(
            id = id, content = content, plainText = plainText, title = title,
            moodLevel = moodLevel, weather = weather,
            tagIds = _selectedTagIds.value, timestamp = System.currentTimeMillis(),
            location = location, latitude = latitude, longitude = longitude
        )
        prefs.edit().putString(draftListItemKey(id), gson.toJson(data)).apply()
        val ids = getAllDraftIds()
        ids.add(id)
        saveDraftIds(ids)
        return id
    }

    fun deleteDraft(draftId: String) {
        prefs.edit().remove(draftListItemKey(draftId)).apply()
        val ids = getAllDraftIds()
        ids.remove(draftId)
        saveDraftIds(ids)
    }

    fun loadDraftById(draftId: String): DraftData? {
        val json = prefs.getString(draftListItemKey(draftId), null) ?: return null
        return try { gson.fromJson(json, DraftData::class.java) } catch (_: Exception) { null }
    }

    init {
        viewModelScope.launch {
            _recentLocations.value = dao.getRecentLocations()

            val appPrefs = application.getSharedPreferences("diary_prefs", Context.MODE_PRIVATE)
            if (!appPrefs.getBoolean("has_seeded_presets", false)) {
                val presets = listOf(
                    Tag(name = "生活", color = 0xFF667EEA, isPreset = true),
                    Tag(name = "工作", color = 0xFFE74C3C, isPreset = true),
                    Tag(name = "学习", color = 0xFF2ECC71, isPreset = true),
                    Tag(name = "旅行", color = 0xFFE67E22, isPreset = true),
                    Tag(name = "感悟", color = 0xFF9B59B6, isPreset = true),
                    Tag(name = "健康", color = 0xFF1ABC9C, isPreset = true),
                    Tag(name = "财务", color = 0xFFF1C40F, isPreset = true),
                    Tag(name = "社交", color = 0xFFE91E63, isPreset = true)
                )
                presets.forEach { dao.insertTag(it) }
                appPrefs.edit().putBoolean("has_seeded_presets", true).apply()
            }
            dao.getAllTags().collect { _allTags.value = it }
        }
    }

    fun loadEntry(id: Long) {
        viewModelScope.launch {
            _currentEntry.value = dao.getEntryByIdSafe(id)
            val tags = dao.getTagsForDiary(id)
            _selectedTagIds.value = tags.map { it.tagId }.toSet()
        }
    }

    fun toggleTag(tagId: Long) {
        val current = _selectedTagIds.value.toMutableSet()
        if (tagId in current) current.remove(tagId) else current.add(tagId)
        _selectedTagIds.value = current
    }

    fun setSelectedTagIds(tagIds: Set<Long>) {
        _selectedTagIds.value = tagIds
    }

    fun addTag(name: String, color: Long) {
        viewModelScope.launch {
            val id = dao.insertTag(Tag(name = name, color = color))
            _selectedTagIds.value = _selectedTagIds.value + id
        }
    }

    suspend fun saveEntry(
        title: String,
        content: String,
        plainText: String,
        diaryId: Long?,
        moodLevel: Int?,
        weather: String?,
        location: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ): Long {
        // Strip Base64 data URLs from content before saving to prevent OOM on load
        val safeContent = DiaryMediaManager.contentToStableMediaRefs(stripBase64FromContent(content))

        val entryId = if (diaryId != null) {
            val existing = dao.getEntryByIdSafe(diaryId)
            if (existing != null) {
                val updated = existing.copy(
                    title = title,
                    content = safeContent,
                    plainText = plainText,
                    moodLevel = moodLevel,
                    weather = weather,
                    location = location,
                    latitude = latitude,
                    longitude = longitude,
                    updatedAt = System.currentTimeMillis()
                )
                dao.updateEntry(updated)
                diaryId
            } else {
                dao.insertEntry(
                    DiaryEntry(
                        title = title, content = safeContent, plainText = plainText,
                        moodLevel = moodLevel, weather = weather,
                        location = location, latitude = latitude, longitude = longitude
                    )
                )
            }
        } else {
            dao.insertEntry(
                DiaryEntry(
                    title = title, content = safeContent, plainText = plainText,
                    moodLevel = moodLevel, weather = weather,
                    location = location, latitude = latitude, longitude = longitude
                )
            )
        }

        // Save tag associations
        dao.deleteTagsForDiary(entryId)
        _selectedTagIds.value.forEach { tagId ->
            dao.insertDiaryTag(DiaryTag(diaryId = entryId, tagId = tagId))
        }
        syncEntryImages(entryId, safeContent)

        clearDraftsForSavedEntry(diaryId)
        _hasUnsavedChanges.value = false

        return entryId
    }

    private suspend fun syncEntryImages(entryId: Long, content: String) {
        dao.deleteImagesForEntry(entryId)
        DiaryMediaManager.extractMediaNames(content).forEachIndexed { index, mediaName ->
            val displayFile = java.io.File(DiaryMediaManager.mediaDir(appContext), mediaName)
            val thumbFile = java.io.File(DiaryMediaManager.thumbDir(appContext), mediaName)
            dao.insertImage(
                DiaryImage(
                    entryId = entryId,
                    localPath = displayFile.absolutePath,
                    thumbPath = thumbFile.absolutePath.takeIf { thumbFile.exists() },
                    mediaName = mediaName,
                    mediaRef = DiaryMediaManager.toMediaRef(mediaName),
                    mimeType = "image/jpeg",
                    fileSize = displayFile.takeIf { it.exists() }?.length() ?: 0L,
                    sortOrder = index,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Strip Base64 data URLs from Delta JSON content.
     * Images should be stored as files, not inline Base64.
     */
    private fun stripBase64FromContent(content: String): String {
        if (!content.contains("data:image/")) return content
        return try {
            val sb = StringBuilder(content.length)
            var i = 0
            while (i < content.length) {
                val dataIdx = content.indexOf("data:image/", i)
                if (dataIdx == -1) {
                    sb.append(content, i, content.length)
                    break
                }
                sb.append(content, i, dataIdx)
                val endQuote = content.indexOf('"', dataIdx)
                if (endQuote == -1) {
                    sb.append(content, dataIdx, content.length)
                    break
                }
                sb.append("")
                i = endQuote
            }
            sb.toString()
        } catch (e: Exception) {
            content
        }
    }
}
