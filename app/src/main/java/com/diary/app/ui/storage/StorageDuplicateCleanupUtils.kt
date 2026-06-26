package com.diary.app.ui.storage

import java.io.File

internal fun duplicateFilesToRemove(groups: List<List<File>>): List<File> {
    val removals = linkedSetOf<File>()
    groups.forEach { group ->
        if (group.size > 1) {
            group.drop(1).forEach(removals::add)
        }
    }
    return removals.toList()
}
