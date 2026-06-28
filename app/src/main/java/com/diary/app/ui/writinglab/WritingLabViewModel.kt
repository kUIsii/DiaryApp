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

data class ExperimentPreset(val title: String, val description: String, val rules: String, val badgeName: String, val days: Int)

val experimentPresets = listOf(
    ExperimentPreset("三句话日记", "每天只用三句话记录今天", "严格限制三句话，不超过150字", "精炼笔者", 7),
    ExperimentPreset("感恩日记", "每天记录三件感恩的事", "写下今天让你感恩的三件事，可长可短", "感恩之心", 7),
    ExperimentPreset("观察日记", "每天仔细观察一个事物", "描述你今天注意到的一个细节：一片叶子、一个人的表情、一杯茶的颜色", "敏锐之眼", 5),
    ExperimentPreset("情绪日记", "记录今天的情绪波动", "今天经历了哪些情绪？什么触发了它们？对自己诚实。", "情绪洞察", 7),
    ExperimentPreset("回忆日记", "每天回忆一个过去的片段", "写下一段过去的记忆，可以是很久以前的，也可以是昨天的", "时光旅人", 5)
)

class WritingLabViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _activeExperiment = MutableStateFlow<WritingExperiment?>(null)
    val activeExperiment: StateFlow<WritingExperiment?> = _activeExperiment.asStateFlow()

    private val _participations = MutableStateFlow<List<ExperimentParticipation>>(emptyList())
    val participations: StateFlow<List<ExperimentParticipation>> = _participations.asStateFlow()

    private val _completedExperiments = MutableStateFlow<List<WritingExperiment>>(emptyList())
    val completedExperiments: StateFlow<List<WritingExperiment>> = _completedExperiments.asStateFlow()

    private val _showPresetPicker = MutableStateFlow(false)
    val showPresetPicker: StateFlow<Boolean> = _showPresetPicker.asStateFlow()

    init { loadExperiments() }

    fun loadExperiments() {
        viewModelScope.launch {
            dao.getAllWritingExperiments().collect { experiments ->
                val active = experiments.firstOrNull { it.status == "active" }
                _activeExperiment.value = active
                _completedExperiments.value = experiments.filter { it.status == "completed" || it.status == "expired" }
                if (active != null) {
                    _participations.value = dao.getExperimentParticipations(active.id).first()
                }
                if (experiments.none { it.status == "active" || it.status == "upcoming" }) {
                    _showPresetPicker.value = true
                } else {
                    _showPresetPicker.value = false
                }
            }
        }
    }

    fun startExperiment(preset: ExperimentPreset) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            dao.insertWritingExperiment(WritingExperiment(
                title = preset.title, description = preset.description,
                rules = preset.rules, badgeName = preset.badgeName,
                startDate = now, endDate = now + preset.days * 24 * 60 * 60 * 1000L,
                status = "active"
            ))
            _showPresetPicker.value = false
        }
    }

    fun logParticipation(note: String) {
        viewModelScope.launch {
            val exp = _activeExperiment.value ?: return@launch
            val dayNum = _participations.value.size + 1
            dao.insertExperimentParticipation(ExperimentParticipation(
                experimentId = exp.id, diaryId = null,
                dayNumber = dayNum, note = note
            ))
            if (dayNum >= (experimentPresets.find { it.title == exp.title }?.days ?: 7)) {
                val updated = dao.getActiveWritingExperiment()?.takeIf { it.id == exp.id }
                if (updated != null) {
                    dao.updateWritingExperiment(updated.copy(status = "completed", completedAt = System.currentTimeMillis()))
                }
            }
        }
    }

    fun dismissPresetPicker() { _showPresetPicker.value = false }
}
