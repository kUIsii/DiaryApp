package com.diary.app.ui.values

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.ExtractedValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ValuesViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _values = MutableStateFlow<List<ExtractedValue>>(emptyList())
    val values: StateFlow<List<ExtractedValue>> = _values.asStateFlow()

    init {
        loadValues()
        extractValues()
    }

    fun loadValues() {
        viewModelScope.launch {
            dao.getAllExtractedValues().collect { list ->
                _values.value = list
            }
        }
    }

    private fun extractValues() {
        viewModelScope.launch {
            val entries = dao.getAllEntriesOnce()
            if (entries.size < 5) return@launch

            val existingValues = dao.getAllExtractedValues().first()
            if (existingValues.isNotEmpty()) return@launch

            // 基于标签和内容推断价值观
            val valueCategories = mapOf(
                "家庭" to listOf("家", "爸妈", "孩子", "家人", "妈妈", "爸爸", "老公", "老婆"),
                "成长" to listOf("学习", "进步", "提升", "读书", "课程", "成长", "努力"),
                "健康" to listOf("运动", "健身", "跑步", "饮食", "睡眠", "锻炼", "养生"),
                "友情" to listOf("朋友", "聚会", "聊天", "闺蜜", "兄弟", "同事"),
                "事业" to listOf("工作", "项目", "职业", "升职", "创业", "事业"),
                "兴趣" to listOf("爱好", "画画", "音乐", "旅行", "摄影", "写作")
            )

            valueCategories.forEach { (category, keywords) ->
                var mentionCount = 0
                val evidence = mutableListOf<String>()

                entries.forEach { entry ->
                    val text = entry.plainText
                    val matches = keywords.count { text.contains(it) }
                    if (matches > 0) {
                        mentionCount += matches
                        if (evidence.size < 3) {
                            evidence.add(text.take(50))
                        }
                    }
                }

                if (mentionCount > 0) {
                    val confidence = (mentionCount.toFloat() / entries.size).coerceAtMost(1f)
                    dao.insertExtractedValue(ExtractedValue(
                        category = category,
                        value = category,
                        evidence = evidence.joinToString("|"),
                        confidence = confidence
                    ))
                }
            }
        }
    }
}
