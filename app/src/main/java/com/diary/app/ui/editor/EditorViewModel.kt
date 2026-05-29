package com.diary.app.ui.editor

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryEntry
import com.diary.app.data.DiaryTag
import com.diary.app.data.Tag
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DraftData(
    val content: String,
    val plainText: String,
    val title: String,
    val moodLevel: Int?,
    val weather: String?,
    val tagIds: Set<Long>,
    val timestamp: Long
)

class EditorViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()
    private val prefs = application.getSharedPreferences("editor_drafts", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _currentEntry = MutableStateFlow<DiaryEntry?>(null)
    val currentEntry = _currentEntry.asStateFlow()

    private val _allTags = MutableStateFlow<List<Tag>>(emptyList())
    val allTags = _allTags.asStateFlow()

    private val _selectedTagIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTagIds = _selectedTagIds.asStateFlow()

    // Auto-save and word count state
    private val _autoSaveVisible = MutableStateFlow(false)
    val autoSaveVisible = _autoSaveVisible.asStateFlow()

    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges = _hasUnsavedChanges.asStateFlow()

    // Latest content cache for auto-save
    private var latestContent: String = ""
    private var latestPlainText: String = ""
    private var latestTitle: String = ""

    fun markContentChanged() {
        _hasUnsavedChanges.value = true
    }

    fun updateLatestContent(content: String, plainText: String, title: String) {
        latestContent = content
        latestPlainText = plainText
        latestTitle = title
    }

    fun performAutoSave(diaryId: Long?, moodLevel: Int?, weather: String?) {
        if (latestContent.isEmpty() && latestPlainText.isEmpty()) return
        saveDraft(latestContent, latestPlainText, diaryId, latestTitle, moodLevel, weather)
        _autoSaveVisible.value = true
        _hasUnsavedChanges.value = false
    }

    fun hideAutoSaveIndicator() {
        _autoSaveVisible.value = false
    }

    fun onManualSaveCompleted(diaryId: Long?) {
        clearDraft(diaryId)
        _hasUnsavedChanges.value = false
    }

    // Draft management
    private fun draftKey(diaryId: Long?): String {
        return if (diaryId != null) "draft_$diaryId" else "draft_new"
    }

    fun saveDraft(
        content: String, plainText: String, diaryId: Long?,
        title: String, moodLevel: Int?, weather: String?
    ) {
        val data = DraftData(content, plainText, title, moodLevel, weather, _selectedTagIds.value, System.currentTimeMillis())
        prefs.edit().putString(draftKey(diaryId), gson.toJson(data)).apply()
    }

    fun loadDraft(diaryId: Long?): DraftData? {
        val json = prefs.getString(draftKey(diaryId), null) ?: return null
        return try { gson.fromJson(json, DraftData::class.java) } catch (_: Exception) { null }
    }

    fun clearDraft(diaryId: Long?) {
        prefs.edit().remove(draftKey(diaryId)).apply()
    }

    init {
        viewModelScope.launch {
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
            _currentEntry.value = dao.getEntryById(id)
            val tags = dao.getTagsForDiary(id)
            _selectedTagIds.value = tags.map { it.tagId }.toSet()
        }
    }

    fun toggleTag(tagId: Long) {
        val current = _selectedTagIds.value.toMutableSet()
        if (tagId in current) current.remove(tagId) else current.add(tagId)
        _selectedTagIds.value = current
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
        weather: String?
    ): Long {
        val entryId = if (diaryId != null) {
            val existing = dao.getEntryById(diaryId)
            if (existing != null) {
                val updated = existing.copy(
                    title = title,
                    content = content,
                    plainText = plainText,
                    moodLevel = moodLevel,
                    weather = weather,
                    updatedAt = System.currentTimeMillis()
                )
                dao.updateEntry(updated)
                diaryId
            } else {
                dao.insertEntry(
                    DiaryEntry(
                        title = title, content = content, plainText = plainText,
                        moodLevel = moodLevel, weather = weather
                    )
                )
            }
        } else {
            dao.insertEntry(
                DiaryEntry(
                    title = title, content = content, plainText = plainText,
                    moodLevel = moodLevel, weather = weather
                )
            )
        }

        // Save tag associations
        dao.deleteTagsForDiary(entryId)
        _selectedTagIds.value.forEach { tagId ->
            dao.insertDiaryTag(DiaryTag(diaryId = entryId, tagId = tagId))
        }

        clearDraft(diaryId)
        _hasUnsavedChanges.value = false

        return entryId
    }
}
