package com.diary.app.ui.lockscreenquickwrite

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import org.json.JSONArray
import org.json.JSONObject

data class QuickNote(val text: String, val createdAt: Long, val category: String = "快速笔记")

enum class NoteSortMode { TIME_DESC, TIME_ASC, CATEGORY }

val NOTE_CATEGORIES = listOf("快速笔记", "灵感", "待办")

class LockScreenQuickWriteViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("quick_notes", 0)
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _notes = MutableStateFlow(loadNotes())
    val notes: StateFlow<List<QuickNote>> = _notes.asStateFlow()

    private fun loadNotes(): List<QuickNote> {
        val json = prefs.getString("notes", "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                QuickNote(o.getString("text"), o.getLong("createdAt"), o.optString("category", "快速笔记"))
            }.sortedByDescending { it.createdAt }
        } catch (_: Exception) { emptyList() }
    }

    private fun saveNotes(notes: List<QuickNote>) {
        val arr = JSONArray()
        notes.forEach { n ->
            arr.put(JSONObject().apply { put("text", n.text); put("createdAt", n.createdAt); put("category", n.category) })
        }
        prefs.edit().putString("notes", arr.toString()).apply()
    }

    fun addNote(text: String, category: String = "快速笔记") {
        if (text.isBlank()) return
        val note = QuickNote(text.trim(), System.currentTimeMillis(), category)
        val list = listOf(note) + _notes.value
        _notes.value = list
        saveNotes(list)
    }

    private val _sortMode = MutableStateFlow(NoteSortMode.TIME_DESC)
    val sortMode: StateFlow<NoteSortMode> = _sortMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSortMode(mode: NoteSortMode) { _sortMode.value = mode }
    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun deleteNote(note: QuickNote) {
        val list = _notes.value.toMutableList().apply { remove(note) }
        _notes.value = list
        saveNotes(list)
    }

    fun syncToDiary(note: QuickNote, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                dao.insertEntry(DiaryEntry(
                    title = note.text.take(50),
                    content = "{\"ops\":[{\"insert\":\"${note.text.replace("\"", "\\\"").replace("\n", "\\n")}\"}]}",
                    plainText = note.text,
                    createdAt = note.createdAt,
                    updatedAt = note.createdAt
                ))
                deleteNote(note)
                onComplete(true)
            } catch (_: Exception) { onComplete(false) }
        }
    }

    fun syncAllToDiary(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val current = _notes.value.toList()
                for (note in current) {
                    dao.insertEntry(DiaryEntry(
                        title = note.text.take(50),
                        content = "{\"ops\":[{\"insert\":\"${note.text.replace("\"", "\\\"").replace("\n", "\\n")}\"}]}",
                        plainText = note.text,
                        createdAt = note.createdAt,
                        updatedAt = note.createdAt
                    ))
                }
                _notes.value = emptyList()
                prefs.edit().putString("notes", "[]").apply()
                onComplete(true)
            } catch (_: Exception) { onComplete(false) }
        }
    }
}
