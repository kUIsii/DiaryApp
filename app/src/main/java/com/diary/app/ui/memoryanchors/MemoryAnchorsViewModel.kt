package com.diary.app.ui.memoryanchors

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.AnchorRelation
import com.diary.app.data.DiaryEntry
import com.diary.app.data.MemoryAnchor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AnchorWithDetails(
    val anchor: MemoryAnchor,
    val relatedCount: Int,
    val diaryEntry: DiaryEntry?
)

class MemoryAnchorsViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _anchors = MutableStateFlow<List<AnchorWithDetails>>(emptyList())
    val anchors: StateFlow<List<AnchorWithDetails>> = _anchors.asStateFlow()

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    init {
        loadAnchors()
    }

    fun loadAnchors() {
        viewModelScope.launch {
            dao.getAllMemoryAnchors().collect { anchorList ->
                val details = anchorList.map { anchor ->
                    val entry = dao.getEntryById(anchor.diaryId)
                    val relations = dao.getAnchorRelations(anchor.id).first()
                    AnchorWithDetails(anchor, relations.size, entry)
                }
                _anchors.value = details
            }
        }
    }

    fun addAnchor(topic: String, description: String, diaryId: Long) {
        viewModelScope.launch {
            val anchor = MemoryAnchor(
                diaryId = diaryId,
                topic = topic,
                description = description
            )
            val id = dao.insertMemoryAnchor(anchor)
            findRelatedDiaries(id, topic)
            _showAddDialog.value = false
        }
    }

    fun deleteAnchor(anchorId: Long) {
        viewModelScope.launch {
            dao.deleteMemoryAnchor(anchorId)
        }
    }

    fun setShowAddDialog(show: Boolean) {
        _showAddDialog.value = show
    }

    private suspend fun findRelatedDiaries(anchorId: Long, topic: String) {
        val allEntries = dao.getAllEntriesOnce()
        val keywords = topic.split(" ", ",", "，", "、").filter { it.length > 1 }
        allEntries.forEach { entry ->
            val matches = keywords.count { kw ->
                entry.plainText.contains(kw, ignoreCase = true)
            }
            if (matches > 0 && entry.id != dao.getEntryById(entry.id)?.id) {
                val score = matches.toFloat() / keywords.size.toFloat()
                dao.insertAnchorRelation(
                    AnchorRelation(
                        anchorId = anchorId,
                        diaryId = entry.id,
                        relevanceScore = score
                    )
                )
            }
        }
    }
}
