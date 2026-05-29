package com.diary.app.ui.map

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryEntry
import com.diary.app.ui.home.TagInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class MonthGroup(
    val yearMonth: YearMonth,
    val entries: List<DiaryEntry>
)

class TimelineViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _tagsMap = MutableStateFlow<Map<Long, List<TagInfo>>>(emptyMap())
    val tagsMap: StateFlow<Map<Long, List<TagInfo>>> = _tagsMap

    val monthGroups: StateFlow<List<MonthGroup>> = dao.getAllEntries()
        .map { entries ->
            entries
                .groupBy { entry ->
                    Instant.ofEpochMilli(entry.createdAt)
                        .atZone(ZoneId.systemDefault())
                        .let { YearMonth.of(it.year, it.monthValue) }
                }
                .toSortedMap(compareByDescending { it })
                .map { (yearMonth, monthEntries) ->
                    MonthGroup(yearMonth, monthEntries)
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadTagsForEntries(entries: List<DiaryEntry>) {
        viewModelScope.launch {
            val map = mutableMapOf<Long, List<TagInfo>>()
            for (entry in entries) {
                val tags = dao.getTagInfoForDiary(entry.id)
                if (tags.isNotEmpty()) {
                    map[entry.id] = tags.map { TagInfo(it.name, Color(it.color)) }
                }
            }
            _tagsMap.value = map
        }
    }
}
