package com.diary.app.ui.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryEntry
import com.diary.app.data.DiaryTag
import com.diary.app.data.Tag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditorViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _currentEntry = MutableStateFlow<DiaryEntry?>(null)
    val currentEntry = _currentEntry.asStateFlow()

    private val _allTags = MutableStateFlow<List<Tag>>(emptyList())
    val allTags = _allTags.asStateFlow()

    private val _selectedTagIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTagIds = _selectedTagIds.asStateFlow()

    init {
        viewModelScope.launch {
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

    fun saveEntry(
        title: String,
        content: String,
        plainText: String,
        diaryId: Long?,
        moodLevel: Int?,
        weather: String?
    ): Long {
        return kotlinx.coroutines.runBlocking {
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

            entryId
        }
    }
}
