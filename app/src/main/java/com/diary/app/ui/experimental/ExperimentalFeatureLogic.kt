package com.diary.app.ui.experimental

import com.diary.app.data.TodoItem

data class ExperimentalFeaturesState(
    val mainScreenSwipeEnabled: Boolean = false,
    val keepCompletedItemsInPlace: Boolean = false,
    val writingMilestonesEnabled: Boolean = false,
    val aiInsightCardEnabled: Boolean = false,
    val aiAssistantEnabled: Boolean = false,
    val floatingBubbleEnabled: Boolean = false,
    val healthDataEnabled: Boolean = false,
    val diaryMapEnabled: Boolean = false,
    val aiBiographyEnabled: Boolean = false
)

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
