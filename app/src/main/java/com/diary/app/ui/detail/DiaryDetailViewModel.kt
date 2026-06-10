package com.diary.app.ui.detail

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryEntry
import com.diary.app.data.DiaryExporter
import com.diary.app.data.DiaryPreview
import com.diary.app.data.Tag
import com.diary.app.data.TrashEntry
import com.diary.app.data.normalizeContentForExport
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

    private val _relatedEntries = MutableStateFlow<List<DiaryPreview>>(emptyList())
    val relatedEntries = _relatedEntries.asStateFlow()

    private val _loadError = MutableStateFlow(false)
    val loadError = _loadError.asStateFlow()

    fun loadEntry(id: Long) {
        viewModelScope.launch {
            try {
                val loaded = dao.getEntryByIdSafe(id)
                if (loaded == null) {
                    _loadError.value = true
                    return@launch
                }
                // Sanitize content: strip Base64 data URLs to prevent OOM
                _entry.value = if (loaded.content.contains("data:image/")) {
                    loaded.copy(content = stripBase64FromDelta(loaded.content))
                } else {
                    loaded
                }
                _tags.value = dao.getTagInfoForDiary(id)

                // Load related entries from the same day in previous years (lightweight query)
                val entry = _entry.value
                if (entry != null) {
                    val entryDate = java.time.Instant.ofEpochMilli(entry.createdAt)
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()

                    val monthDayEntries = dao.getPreviewsByMonthDay(entryDate.monthValue, entryDate.dayOfMonth)
                    val related = monthDayEntries.filter { other ->
                        other.id != entry.id && run {
                            val otherDate = java.time.Instant.ofEpochMilli(other.createdAt)
                                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                            otherDate.year != entryDate.year
                        }
                    }.sortedByDescending { it.createdAt }.take(3)

                    _relatedEntries.value = related
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _loadError.value = true
            }
        }
    }

    /**
     * Strip Base64 data URLs from Delta JSON to prevent OOM.
     * Replaces data:image/... base64 strings with empty placeholder.
     */
    private fun stripBase64FromDelta(content: String): String {
        return try {
            // Use a conservative approach: replace data:image blocks with empty strings
            // The regex matches "data:image/..." patterns within JSON string values
            val sb = StringBuilder(content.length)
            var i = 0
            while (i < content.length) {
                val dataIdx = content.indexOf("data:image/", i)
                if (dataIdx == -1) {
                    sb.append(content, i, content.length)
                    break
                }
                sb.append(content, i, dataIdx)
                // Find the end of the base64 string (closing quote)
                val endQuote = content.indexOf('"', dataIdx)
                if (endQuote == -1) {
                    sb.append(content, dataIdx, content.length)
                    break
                }
                sb.append("") // replace the data URL with empty
                i = endQuote
            }
            sb.toString()
        } catch (e: Exception) {
            // If stripping fails, return content as-is but log
            e.printStackTrace()
            content
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

        // Use the stored plainText field instead of parsing delta JSON
        if (currentEntry.plainText.isNotBlank()) {
            sb.appendLine(currentEntry.plainText)
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

    suspend fun exportAsImage(context: Context): String? {
        val currentEntry = _entry.value ?: return null
        val tagNames = _tags.value.map { it.name }
        return DiaryExporter.exportAsImage(context, currentEntry, tagNames)
    }

    fun toggleFavorite() {
        val currentEntry = _entry.value ?: return
        viewModelScope.launch {
            dao.toggleFavorite(currentEntry.id, !currentEntry.isFavorite)
            _entry.value = currentEntry.copy(isFavorite = !currentEntry.isFavorite)
        }
    }

    suspend fun deleteEntry() {
        val currentEntry = _entry.value ?: return
        // Move to trash instead of permanent delete
        val trashEntry = TrashEntry(
            originalId = currentEntry.id,
            title = currentEntry.title,
            content = normalizeContentForExport(currentEntry.content),
            plainText = currentEntry.plainText,
            moodLevel = currentEntry.moodLevel,
            weather = currentEntry.weather,
            location = currentEntry.location,
            latitude = currentEntry.latitude,
            longitude = currentEntry.longitude,
            isFavorite = currentEntry.isFavorite,
            createdAt = currentEntry.createdAt,
            updatedAt = currentEntry.updatedAt
        )
        dao.insertTrashEntry(trashEntry)
        dao.deleteEntryWithTags(currentEntry)
    }

    suspend fun exportToMarkdown(context: Context): String? {
        val currentEntry = _entry.value ?: return null
        val entryTags = _tags.value
        return DiaryExporter.exportSingleAsMarkdown(context, currentEntry, entryTags.map { it.name })
    }
}
