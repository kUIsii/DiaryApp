package com.diary.app.ui.editor

data class EditorChromeState(
    val showToolbar: Boolean = true,
    val activeCategory: Int = -1,
    val isToolbarManuallyHidden: Boolean = false,
    val keepToolbarOpen: Boolean = false,
    val isToolbarLocked: Boolean = false
)

internal fun onEditorKeyboardVisibilityChanged(
    state: EditorChromeState,
    isKeyboardVisible: Boolean
): EditorChromeState {
    if (state.isToolbarLocked) return state
    return if (isKeyboardVisible) {
        if (state.isToolbarManuallyHidden) {
            state
        } else {
            state.copy(
                showToolbar = true,
                activeCategory = if (state.activeCategory >= 0) -1 else state.activeCategory
            )
        }
    } else if (shouldAutoHideToolbarOnKeyboardHidden(state.activeCategory, state.keepToolbarOpen)) {
        state.copy(showToolbar = false)
    } else {
        state
    }
}

internal fun onEditorToolbarVisibilityToggled(state: EditorChromeState): EditorChromeState {
    val nextVisible = !state.showToolbar
    return state.copy(
        showToolbar = nextVisible,
        isToolbarManuallyHidden = !nextVisible,
        keepToolbarOpen = nextVisible,
        isToolbarLocked = true
    )
}

internal fun onEditorToolbarCategoryTapped(
    state: EditorChromeState,
    category: Int
): EditorChromeState {
    return if (state.activeCategory == category) {
        state.copy(
            activeCategory = -1,
            keepToolbarOpen = true,
            isToolbarLocked = false
        )
    } else {
        state.copy(
            activeCategory = category,
            keepToolbarOpen = true
        )
    }
}

internal fun onEditorToolbarHiddenByUser(state: EditorChromeState): EditorChromeState {
    return state.copy(
        showToolbar = false,
        isToolbarManuallyHidden = true,
        keepToolbarOpen = false
    )
}
