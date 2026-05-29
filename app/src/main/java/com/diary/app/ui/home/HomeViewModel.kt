package com.diary.app.ui.home

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class TagInfo(val name: String, val color: Color)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _tagsMap = MutableStateFlow<Map<Long, List<TagInfo>>>(emptyMap())
    val tagsMap: StateFlow<Map<Long, List<TagInfo>>> = _tagsMap

    val entryDates: StateFlow<Set<LocalDate>> = dao.getAllTimestamps()
        .map { timestamps ->
            timestamps.map { ts ->
                Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate()
            }.toSet()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val entries: StateFlow<List<DiaryEntry>> = combine(
        dao.getAllEntries(),
        _selectedDate,
        _searchQuery
    ) { entries, date, query ->
        entries.filter { entry ->
            val matchesDate = date == null || run {
                val entryDate = Instant.ofEpochMilli(entry.createdAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                entryDate == date
            }
            val matchesQuery = query.isBlank() || entry.plainText.contains(query, ignoreCase = true)
            matchesDate && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectDate(date: LocalDate?) {
        _selectedDate.value = date
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteEntry(entry: DiaryEntry) {
        viewModelScope.launch {
            dao.deleteEntry(entry)
        }
    }

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
