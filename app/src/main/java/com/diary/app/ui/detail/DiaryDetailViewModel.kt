package com.diary.app.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryEntry
import com.diary.app.data.Tag
import com.diary.app.ui.components.moodLabelForLevel
import com.diary.app.ui.components.weatherLabelFor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DiaryDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _entry = MutableStateFlow<DiaryEntry?>(null)
    val entry = _entry.asStateFlow()

    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags = _tags.asStateFlow()

    fun loadEntry(id: Long) {
        viewModelScope.launch {
            _entry.value = dao.getEntryById(id)
            _tags.value = dao.getTagInfoForDiary(id)
        }
    }

    fun getShareText(): String? {
        val currentEntry = _entry.value ?: return null
        val currentTags = _tags.value

        val entryDate = java.time.Instant.ofEpochMilli(currentEntry.createdAt)
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        val entryTime = java.time.Instant.ofEpochMilli(currentEntry.createdAt)
            .atZone(java.time.ZoneId.systemDefault()).toLocalTime()
        val dateText = "${entryDate.year}年${entryDate.monthValue}月${entryDate.dayOfMonth}日"
        val timeText = entryTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))

        val moodLabel = currentEntry.moodLevel?.let { moodLabelForLevel(it) }

        val weatherLabel = currentEntry.weather?.let { weatherLabelFor(it) }

        val sb = StringBuilder()
        sb.appendLine("$dateText $timeText")

        val metaLine = listOfNotNull(
            moodLabel?.let { "心情: $it" },
            weatherLabel?.let { "天气: $it" }
        ).joinToString(" | ")
        if (metaLine.isNotEmpty()) {
            sb.appendLine(metaLine)
        }

        sb.appendLine()

        // Strip HTML tags for plain text
        val plainContent = currentEntry.content
            .replace(Regex("<[^>]*>"), "")
            .replace(Regex("&[a-zA-Z]+;"), "")
            .trim()
        if (plainContent.isNotBlank()) {
            sb.appendLine(plainContent)
        }

        sb.appendLine()
        sb.appendLine("---")
        if (currentTags.isNotEmpty()) {
            sb.appendLine("标签: ${currentTags.joinToString(", ") { it.name }}")
        }
        sb.append("来自 日记本 App")

        return sb.toString()
    }

    fun getDateTitle(): String {
        val currentEntry = _entry.value ?: return ""
        val entryDate = java.time.Instant.ofEpochMilli(currentEntry.createdAt)
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        return "${entryDate.year}年${entryDate.monthValue}月${entryDate.dayOfMonth}日"
    }
}
