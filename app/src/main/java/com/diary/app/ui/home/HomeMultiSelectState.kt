package com.diary.app.ui.home

data class HomeMultiSelectState(
    val isEnabled: Boolean = false,
    val selectedIds: Set<Long> = emptySet()
) {
    val selectedCount: Int
        get() = selectedIds.size

    fun toggleSelection(id: Long): HomeMultiSelectState {
        val nextIds = selectedIds.toMutableSet().apply {
            if (!add(id)) {
                remove(id)
            }
        }
        return copy(selectedIds = nextIds)
    }

    fun clearSelection(): HomeMultiSelectState = HomeMultiSelectState()

    companion object {
        fun startSelection(id: Long): HomeMultiSelectState {
            return HomeMultiSelectState(
                isEnabled = true,
                selectedIds = setOf(id)
            )
        }
    }
}
