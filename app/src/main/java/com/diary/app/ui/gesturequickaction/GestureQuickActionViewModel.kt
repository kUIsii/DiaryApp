package com.diary.app.ui.gesturequickaction

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GestureQuickActionViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("gesture_actions", 0)

    private val _mappings = MutableStateFlow(loadMappings())
    val mappings: StateFlow<Map<String, String>> = _mappings.asStateFlow()

    val gestureOptions = listOf("双指点击", "首页下拉", "长按日期", "左滑条目", "右滑条目")
    val actionOptions = listOf("新建日记", "快速签到", "打开那年今日", "收藏", "打开搜索", "打开统计", "随机回顾", "无操作")

    private fun loadMappings(): Map<String, String> {
        val m = mutableMapOf<String, String>()
        gestureOptions.forEach { gesture ->
            m[gesture] = prefs.getString("gesture_$gesture", defaultAction(gesture)) ?: defaultAction(gesture)
        }
        return m.toMap()
    }

    private fun defaultAction(gesture: String): String = when (gesture) {
        "双指点击" -> "新建日记"
        "首页下拉" -> "打开那年今日"
        "长按日期" -> "快速签到"
        "左滑条目" -> "收藏"
        "右滑条目" -> "删除"
        else -> "无操作"
    }

    fun setAction(gesture: String, action: String) {
        prefs.edit().putString("gesture_$gesture", action).apply()
        _mappings.value = _mappings.value + (gesture to action)
    }
}
