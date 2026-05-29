package com.diary.app.ui.map

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.DiaryEntry
import com.diary.app.ui.home.TagInfo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class MonthGroup(
    val yearMonth: YearMonth,
    val entries: List<DiaryEntry>
)

class TimelineViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    val tagsMap: StateFlow<Map<Long, List<TagInfo>>> = dao.getAllDiaryTagPairs()
        .map { pairs ->
            pairs.groupBy { it.diaryId }.mapValues { (_, tagPairs) ->
                tagPairs.map { TagInfo(it.tagId, it.name, Color(it.color)) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

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

    // Tags are now loaded reactively via getAllDiaryTagPairs() Flow in tagsMap above.
}
