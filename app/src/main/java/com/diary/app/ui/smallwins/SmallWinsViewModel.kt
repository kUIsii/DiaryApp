package com.diary.app.ui.smallwins

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.SmallWin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class SmallWinsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val dao = (application as DiaryApplication).database.diaryDao()
    
    // 获取所有小确幸记录
    val allSmallWins: StateFlow<List<SmallWin>> = dao.getAllSmallWins()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    
    // 获取今天的小确幸
    private val todayDate = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
    val todaySmallWins: StateFlow<List<SmallWin>> = dao.getSmallWinsByDate(todayDate)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    
    // 输入框内容
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText
    
    fun setInputText(text: String) {
        _inputText.value = text
    }
    
    // 添加小确幸
    fun addSmallWin(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val smallWin = SmallWin(
                content = content.trim(),
                recordDate = todayDate,
                createdAt = System.currentTimeMillis()
            )
            dao.insertSmallWin(smallWin)
            _inputText.value = ""
        }
    }
    
    // 删除小确幸
    fun deleteSmallWin(id: Long) {
        viewModelScope.launch {
            dao.deleteSmallWin(id)
        }
    }
    
    // 获取今天的小确幸数量
    fun getTodayCount(): StateFlow<Int> {
        return dao.getSmallWinsByDate(todayDate)
            .map { it.size }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    }
}
