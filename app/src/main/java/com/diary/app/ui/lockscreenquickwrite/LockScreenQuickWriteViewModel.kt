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
import org.json.JSONArray
import org.json.JSONObject

data class QuickNote(val text: String, val createdAt: Long)

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
                QuickNote(o.getString("text"), o.getLong("createdAt"))
            }.sortedByDescending { it.createdAt }
        } catch (_: Exception) { emptyList() }
    }

    private fun saveNotes(notes: List<QuickNote>) {
        val arr = JSONArray()
        notes.forEach { n ->
            arr.put(JSONObject().apply { put("text", n.text); put("createdAt", n.createdAt) })
        }
        prefs.edit().putString("notes", arr.toString()).apply()
    }

    fun addNote(text: String) {
        if (text.isBlank()) return
        val note = QuickNote(text.trim(), System.currentTimeMillis())
        val list = listOf(note) + _notes.value
        _notes.value = list
        saveNotes(list)
    }

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
