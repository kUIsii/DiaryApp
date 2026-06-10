package com.diary.app.ui.experimental

import com.diary.app.data.TodoItem

const val MAIN_SCREEN_SWIPE_THRESHOLD = 48f

val mainScreenRoutes = listOf("home", "timeline", "todo", "profile")

data class ExperimentalFeaturesState(
    val mainScreenSwipeEnabled: Boolean = false,
    val keepCompletedItemsInPlace: Boolean = false
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
