package com.diary.app

fun resolveExternalNavigation(
    action: String?,
    navigateTo: String?,
    requestedAction: String?
): String? {
    return when {
        action == "com.diary.app.NEW_DIARY" -> "editor"
        action == "com.diary.app.QUICK_TODO" -> "todo_add"
        navigateTo == "todo" && requestedAction == "add" -> "todo_add"
        else -> navigateTo
    }
}
