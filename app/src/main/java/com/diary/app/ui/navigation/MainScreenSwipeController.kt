package com.diary.app.ui.navigation

const val MAIN_SCREEN_SWIPE_THRESHOLD = 48f

val mainScreenRoutes = listOf("home", "timeline", "todo", "profile")

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
