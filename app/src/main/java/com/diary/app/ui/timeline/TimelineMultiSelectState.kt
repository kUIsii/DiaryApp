package com.diary.app.ui.timeline

data class TimelineMultiSelectState(
    val isEnabled: Boolean = false,
    val selectedIds: Set<Long> = emptySet()
) {
    val selectedCount: Int
        get() = selectedIds.size

    fun toggleSelection(id: Long): TimelineMultiSelectState {
        val nextIds = selectedIds.toMutableSet().apply {
            if (!add(id)) {
                remove(id)
            }
        }
        return copy(selectedIds = nextIds)
    }

    fun clearSelection(): TimelineMultiSelectState = TimelineMultiSelectState()

    companion object {
        fun startSelection(id: Long): TimelineMultiSelectState {
            return TimelineMultiSelectState(
                isEnabled = true,
                selectedIds = setOf(id)
            )
        }
    }
}
