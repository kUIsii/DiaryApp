package com.diary.app.ui.outline

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OutlineItem(
    val title: String,
    val level: Int,
    val charOffset: Int
)

data class OutlineData(
    val items: List<OutlineItem>,
    val totalWords: Int,
    val paragraphCount: Int,
    val estimatedReadMinutes: Int
)

class OutlineViewViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _outline = MutableStateFlow<OutlineData?>(null)
    val outline: StateFlow<OutlineData?> = _outline.asStateFlow()

    fun loadDiary(diaryId: Long) {
        viewModelScope.launch {
            val entry = dao.getEntryById(diaryId) ?: return@launch
            val text = entry.plainText
            if (text.isBlank()) {
                _outline.value = null
                return@launch
            }

            val items = mutableListOf<OutlineItem>()
            val lines = text.split("\n")
            var offset = 0

            lines.forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty()) {
                    offset += line.length + 1
                    return@forEach
                }

                // 检测标题层级
                when {
                    trimmed.startsWith("# ") -> {
                        items.add(OutlineItem(trimmed.removePrefix("# ").trim(), 0, offset))
                    }
                    trimmed.startsWith("## ") -> {
                        items.add(OutlineItem(trimmed.removePrefix("## ").trim(), 1, offset))
                    }
                    trimmed.startsWith("### ") -> {
                        items.add(OutlineItem(trimmed.removePrefix("### ").trim(), 2, offset))
                    }
                    trimmed.endsWith("：") || trimmed.endsWith(":") -> {
                        if (trimmed.length < 30) {
                            items.add(OutlineItem(trimmed, 0, offset))
                        }
                    }
                }
                offset += line.length + 1
            }

            val words = text.length
            val paragraphs = text.split(Regex("\n\\s*\n")).filter { it.isNotBlank() }.size
            val readMinutes = (words / 300).coerceAtLeast(1)

            _outline.value = OutlineData(
                items = items,
                totalWords = words,
                paragraphCount = paragraphs,
                estimatedReadMinutes = readMinutes
            )
        }
    }
}
