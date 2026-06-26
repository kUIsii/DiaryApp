package com.diary.app.ui.profile

import com.diary.app.data.Tag

internal fun filterAvailableParentTags(
    currentTag: Tag,
    allTags: List<Tag>
): List<Tag> {
    val blockedIds = collectDescendantTagIds(currentTag.id, allTags) + currentTag.id
    return allTags.filterNot { it.id in blockedIds }
}

internal fun expandTagFilterNames(
    selectedTagNames: Set<String>,
    allTags: List<Tag>
): Set<String> {
    if (selectedTagNames.isEmpty() || allTags.isEmpty()) return selectedTagNames

    val tagsById = allTags.associateBy { it.id }
    val childrenByParent = allTags
        .filter { it.parentId != null }
        .groupBy { it.parentId!! }

    val selectedIds = allTags
        .filter { it.name in selectedTagNames }
        .map { it.id }

    if (selectedIds.isEmpty()) return selectedTagNames

    val expandedIds = linkedSetOf<Long>()
    selectedIds.forEach { tagId ->
        expandedIds += tagId
        expandedIds += collectDescendantTagIds(tagId, allTags, childrenByParent)
    }

    val expandedNames = expandedIds.mapNotNullTo(linkedSetOf()) { tagId ->
        tagsById[tagId]?.name
    }

    return expandedNames + selectedTagNames
}

private fun collectDescendantTagIds(
    tagId: Long,
    allTags: List<Tag>,
    childrenByParent: Map<Long, List<Tag>> = allTags
        .filter { it.parentId != null }
        .groupBy { it.parentId!! }
): Set<Long> {
    val descendants = linkedSetOf<Long>()

    fun visit(parentId: Long) {
        childrenByParent[parentId].orEmpty().forEach { child ->
            if (descendants.add(child.id)) {
                visit(child.id)
            }
        }
    }

    visit(tagId)
    return descendants
}
