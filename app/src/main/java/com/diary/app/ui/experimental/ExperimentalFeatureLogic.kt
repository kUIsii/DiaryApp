package com.diary.app.ui.experimental

import com.diary.app.data.TodoItem

const val MAIN_SCREEN_SWIPE_THRESHOLD = 48f

val mainScreenRoutes = listOf("home", "timeline", "todo", "profile")

data class ExperimentalFeaturesState(
    val mainScreenSwipeEnabled: Boolean = false,
    val keepCompletedItemsInPlace: Boolean = false,
    // AI 默契功能
    val aiEnabled: Boolean = false,           // AI功能总开关
    val aiSilentTitle: Boolean = false,       // 静默标题
    val aiMemoryEcho: Boolean = false,        // 记忆回响
    val aiOnThisDay: Boolean = false,         // 今日回顾
    val aiMoodTrend: Boolean = false,         // 情绪天气图
    val aiWritingRhythm: Boolean = false,     // 写作节奏
    val aiTagIntuition: Boolean = false,      // 标签直觉
    val aiMilestones: Boolean = false         // 安静的里程碑
)

fun resolveMainScreenSwipeTarget(
    currentRoute: String?,
    totalDrag: Float,
    enabled: Boolean,
    threshold: Float = MAIN_SCREEN_SWIPE_THRESHOLD
): String? {
    if (!enabled || currentRoute == null) return null
    val currentIndex = mainScreenRoutes.indexOf(currentRoute)
    if (currentIndex == -1) return null

    return when {
        totalDrag <= -threshold && currentIndex < mainScreenRoutes.lastIndex -> {
            mainScreenRoutes[currentIndex + 1]
        }

        totalDrag >= threshold && currentIndex > 0 -> {
            mainScreenRoutes[currentIndex - 1]
        }

        else -> null
    }
}

fun orderTodoItemsForDisplay(
    items: List<TodoItem>,
    keepCompletedInPlace: Boolean
): List<TodoItem> {
    if (keepCompletedInPlace) return items
    val activeItems = items.filter { !it.isCompleted }
    val completedItems = items.filter { it.isCompleted }
    return activeItems + completedItems
}

fun orderMemoItemsForDisplay(
    items: List<TodoItem>,
    keepCompletedInPlace: Boolean
): List<TodoItem> {
    if (!keepCompletedInPlace) {
        return orderTodoItemsForDisplay(items, keepCompletedInPlace = false)
    }

    return items.sortedWith(
        compareByDescending<TodoItem> { it.isPinned }
            .thenByDescending { it.priority }
            .thenBy { it.sortOrder }
            .thenByDescending { it.createdAt }
    )
}
