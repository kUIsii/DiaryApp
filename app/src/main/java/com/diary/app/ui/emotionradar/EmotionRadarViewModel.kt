package com.diary.app.ui.emotionradar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.EmotionRadar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EmotionRadarViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()
    
    val radars: StateFlow<List<EmotionRadar>> = dao.getAllEmotionRadars()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    
    private val _currentRadar = MutableStateFlow<EmotionRadar?>(null)
    val currentRadar: StateFlow<EmotionRadar?> = _currentRadar
    
    // 当前编辑中的五维情绪值
    private val _vitality = MutableStateFlow(0.5f)
    val vitality: StateFlow<Float> = _vitality
    
    private val _calmness = MutableStateFlow(0.5f)
    val calmness: StateFlow<Float> = _calmness
    
    private val _happiness = MutableStateFlow(0.5f)
    val happiness: StateFlow<Float> = _happiness
    
    private val _gratitude = MutableStateFlow(0.5f)
    val gratitude: StateFlow<Float> = _gratitude
    
    private val _socialConnection = MutableStateFlow(0.5f)
    val socialConnection: StateFlow<Float> = _socialConnection
    
    fun setVitality(v: Float) { _vitality.value = v }
    fun setCalmness(v: Float) { _calmness.value = v }
    fun setHappiness(v: Float) { _happiness.value = v }
    fun setGratitude(v: Float) { _gratitude.value = v }
    fun setSocialConnection(v: Float) { _socialConnection.value = v }
    
    fun saveRadar(diaryId: Long) {
        viewModelScope.launch {
            val radar = EmotionRadar(
                diaryId = diaryId,
                vitality = _vitality.value,
                calmness = _calmness.value,
                happiness = _happiness.value,
                gratitude = _gratitude.value,
                socialConnection = _socialConnection.value,
                createdAt = System.currentTimeMillis()
            )
            dao.insertEmotionRadar(radar)
            _currentRadar.value = radar
        }
    }
    
    fun loadRadarForDiary(diaryId: Long) {
        viewModelScope.launch {
            val existing = dao.getEmotionRadarForDiary(diaryId)
            if (existing != null) {
                _currentRadar.value = existing
                _vitality.value = existing.vitality
                _calmness.value = existing.calmness
                _happiness.value = existing.happiness
                _gratitude.value = existing.gratitude
                _socialConnection.value = existing.socialConnection
            }
        }
    }
    
    // 获取最近N天的平均情绪数据
    fun getRecentAverage(days: Int = 7): Map<String, Float> {
        val recentRadars = radars.value.take(days)
        if (recentRadars.isEmpty()) return emptyMap()
        return mapOf(
            "活力" to recentRadars.map { it.vitality }.average().toFloat(),
            "平静" to recentRadars.map { it.calmness }.average().toFloat(),
            "快乐" to recentRadars.map { it.happiness }.average().toFloat(),
            "感恩" to recentRadars.map { it.gratitude }.average().toFloat(),
            "社交" to recentRadars.map { it.socialConnection }.average().toFloat()
        )
    }
}
