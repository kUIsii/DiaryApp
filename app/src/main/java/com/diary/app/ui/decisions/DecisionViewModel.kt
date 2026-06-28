package com.diary.app.ui.decisions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.Decision
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DecisionViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _decisions = MutableStateFlow<List<Decision>>(emptyList())
    val decisions: StateFlow<List<Decision>> = _decisions.asStateFlow()

    init {
        loadDecisions()
        scanForDecisions()
    }

    fun loadDecisions() {
        viewModelScope.launch {
            dao.getAllDecisions().collect { list ->
                _decisions.value = list
            }
        }
    }

    fun addDecision(title: String, context: String, diaryId: Long) {
        viewModelScope.launch {
            dao.insertDecision(Decision(
                diaryId = diaryId,
                title = title,
                context = context,
                madeAt = System.currentTimeMillis()
            ))
        }
    }

    fun updateDecision(decision: Decision) {
        viewModelScope.launch {
            dao.updateDecision(decision)
        }
    }

    private fun scanForDecisions() {
        viewModelScope.launch {
            val entries = dao.getAllEntriesOnce()
            val existing = dao.getAllDecisions().let { flow ->
                var result = emptyList<Decision>()
                flow.collect { result = it }
                result
            }
            val existingDiaryIds = existing.map { it.diaryId }.toSet()

            val decisionKeywords = listOf("决定", "选择", "考虑", "纠结", "最终", "下定决心", "想了很久")

            entries.forEach { entry ->
                if (entry.id in existingDiaryIds) return@forEach
                val text = entry.plainText
                if (text.isBlank()) return@forEach

                val matchedKeyword = decisionKeywords.firstOrNull { text.contains(it) }
                if (matchedKeyword != null && text.length > 50) {
                    val title = text.take(30).replace("\n", " ") + "..."
                    dao.insertDecision(Decision(
                        diaryId = entry.id,
                        title = title,
                        context = text.take(200),
                        madeAt = entry.createdAt
                    ))
                }
            }
        }
    }
}
