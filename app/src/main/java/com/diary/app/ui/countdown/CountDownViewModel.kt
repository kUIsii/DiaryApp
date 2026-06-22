package com.diary.app.ui.countdown

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.CountDownItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class CountDownViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    val items: StateFlow<List<CountDownItem>> = dao.getAllCountDownItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog

    private val _editingItem = MutableStateFlow<CountDownItem?>(null)
    val editingItem: StateFlow<CountDownItem?> = _editingItem

    fun showAddDialog() {
        _editingItem.value = null
        _showDialog.value = true
    }

    fun showEditDialog(item: CountDownItem) {
        _editingItem.value = item
        _showDialog.value = true
    }

    fun hideDialog() {
        _showDialog.value = false
        _editingItem.value = null
    }

    fun saveItem(
        title: String,
        targetDate: LocalDate,
        isCountUp: Boolean,
        color: Long,
        isRepeatYearly: Boolean,
        isPinned: Boolean
    ) {
        viewModelScope.launch {
            val existingItem = _editingItem.value
            val targetDateMillis = targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            if (existingItem != null) {
                dao.updateCountDownItem(
                    existingItem.copy(
                        title = title,
                        targetDate = targetDateMillis,
                        isCountUp = isCountUp,
                        color = color,
                        isRepeatYearly = isRepeatYearly,
                        isPinned = isPinned
                    )
                )
            } else {
                dao.insertCountDownItem(
                    CountDownItem(
                        title = title,
                        targetDate = targetDateMillis,
                        isCountUp = isCountUp,
                        color = color,
                        isRepeatYearly = isRepeatYearly,
                        isPinned = isPinned
                    )
                )
            }
            hideDialog()
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            dao.deleteCountDownItem(id)
        }
    }

    fun togglePin(item: CountDownItem) {
        viewModelScope.launch {
            dao.updateCountDownItem(item.copy(isPinned = !item.isPinned))
        }
    }

    fun getDaysRemaining(item: CountDownItem): Long {
        val targetDate = java.time.Instant.ofEpochMilli(item.targetDate)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val today = LocalDate.now()

        return if (item.isRepeatYearly) {
            // Calculate days to next occurrence
            var nextTarget = try {
                targetDate.withYear(today.year)
            } catch (e: java.time.DateTimeException) {
                // Feb 29 in non-leap year → use Feb 28
                targetDate.withDayOfMonth(28).withYear(today.year)
            }
            if (nextTarget.isBefore(today)) {
                nextTarget = try {
                    nextTarget.plusYears(1)
                } catch (e: java.time.DateTimeException) {
                    targetDate.withDayOfMonth(28).withYear(today.year + 1)
                }
            }
            ChronoUnit.DAYS.between(today, nextTarget)
        } else {
            val days = ChronoUnit.DAYS.between(today, targetDate)
            if (item.isCountUp) -days else days
        }
    }
}
