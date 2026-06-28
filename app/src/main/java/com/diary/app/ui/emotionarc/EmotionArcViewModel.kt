package com.diary.app.ui.emotionarc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class EmotionArcData(
    val diaryId: Long,
    val title: String,
    val emotionPoints: List<EmotionPoint>
)

data class EmotionPoint(
    val position: Int,
    val emotion: Float,
    val label: String
)

class EmotionArcViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _arcData = MutableStateFlow<EmotionArcData?>(null)
    val arcData: StateFlow<EmotionArcData?> = _arcData

    fun loadDiary(diaryId: Long) {
        viewModelScope.launch {
            val entry = dao.getEntryById(diaryId) ?: return@launch
            
            // Simulate emotion arc based on text content
            val text = entry.plainText
            val sentences = text.split(Regex("[.!?。！？\\n]+"))
                .filter { it.isNotBlank() }
                .take(20) // Limit to 20 points
            
            val emotionPoints = sentences.mapIndexed { index, sentence ->
                // Simple emotion estimation based on keywords
                val emotion = estimateEmotion(sentence)
                EmotionPoint(
                    position = index,
                    emotion = emotion,
                    label = sentence.take(30)
                )
            }
            
            _arcData.value = EmotionArcData(
                diaryId = diaryId,
                title = entry.title.ifBlank { "无标题" },
                emotionPoints = emotionPoints
            )
        }
    }
    
    private fun estimateEmotion(text: String): Float {
        // Simple keyword-based emotion estimation
        val positiveWords = listOf("开心", "高兴", "快乐", "幸福", "满足", "感恩", "美好", "成功", "进步")
        val negativeWords = listOf("难过", "伤心", "焦虑", "压力", "疲惫", "失败", "失望", "担心", "烦恼")
        
        val positiveCount = positiveWords.count { text.contains(it) }
        val negativeCount = negativeWords.count { text.contains(it) }
        
        // Map to 0-1 scale, 0.5 is neutral
        return when {
            positiveCount > negativeCount -> 0.5f + (positiveCount * 0.1f).coerceAtMost(0.5f)
            negativeCount > positiveCount -> 0.5f - (negativeCount * 0.1f).coerceAtMost(0.5f)
            else -> 0.5f
        }
    }
}
