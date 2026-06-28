package com.diary.app.ui.writinglab

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.ExperimentParticipation
import com.diary.app.data.WritingExperiment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class WritingLabViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _activeExperiment = MutableStateFlow<WritingExperiment?>(null)
    val activeExperiment: StateFlow<WritingExperiment?> = _activeExperiment.asStateFlow()

    private val _participations = MutableStateFlow<List<ExperimentParticipation>>(emptyList())
    val participations: StateFlow<List<ExperimentParticipation>> = _participations.asStateFlow()

    private val _completedExperiments = MutableStateFlow<List<WritingExperiment>>(emptyList())
    val completedExperiments: StateFlow<List<WritingExperiment>> = _completedExperiments.asStateFlow()

    init {
        loadExperiments()
        ensureActiveExperiment()
    }

    fun loadExperiments() {
        viewModelScope.launch {
            dao.getAllWritingExperiments().collect { experiments ->
                _activeExperiment.value = experiments.firstOrNull { it.status == "active" }
                _completedExperiments.value = experiments.filter { it.status == "completed" }
                val active = experiments.firstOrNull { it.status == "active" }
                if (active != null) {
                    val parts = dao.getExperimentParticipations(active.id).first()
                    _participations.value = parts
                }
            }
        }
    }

    fun logParticipation(note: String) {
        viewModelScope.launch {
            val exp = _activeExperiment.value ?: return@launch
            val dayNum = _participations.value.size + 1
            dao.insertExperimentParticipation(ExperimentParticipation(
                experimentId = exp.id,
                diaryId = null,
                dayNumber = dayNum,
                note = note
            ))
        }
    }

    private fun ensureActiveExperiment() {
        viewModelScope.launch {
            val existing = dao.getAllWritingExperiments().first()
            val now = System.currentTimeMillis()
            val hasActive = existing.any { it.status == "active" }
            if (!hasActive) {
                // 创建默认实验
                val weekStart = now
                val weekEnd = now + 7 * 24 * 60 * 60 * 1000L
                dao.insertWritingExperiment(WritingExperiment(
                    title = "三句话日记",
                    description = "每天只用三句话记录今天。限制字数，反而能激发更精炼的表达。",
                    rules = "每天写恰好三句话的日记，坚持7天。",
                    badgeName = "精炼笔者",
                    startDate = weekStart,
                    endDate = weekEnd,
                    status = "active"
                ))
            }
        }
    }
}
