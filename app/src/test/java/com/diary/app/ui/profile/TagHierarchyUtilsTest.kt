package com.diary.app.ui.profile

import com.diary.app.data.Tag
import org.junit.Assert.assertEquals
import org.junit.Test

class TagHierarchyUtilsTest {

    private val root = Tag(id = 1, name = "旅行", color = 0xFF667EEA)
    private val child = Tag(id = 2, name = "海边", color = 0xFF4E8EF7, parentId = 1)
    private val grandChild = Tag(id = 3, name = "日落", color = 0xFF3AAFA9, parentId = 2)
    private val siblingRoot = Tag(id = 4, name = "工作", color = 0xFF6FB98F)
    private val allTags = listOf(root, child, grandChild, siblingRoot)

    @Test
    fun `available parent tags exclude self and descendants`() {
        val availableParents = filterAvailableParentTags(root, allTags)

        assertEquals(listOf(siblingRoot), availableParents)
    }

    @Test
    fun `expanding selected parent tag names includes all descendants`() {
        val expanded = expandTagFilterNames(setOf("旅行"), allTags)

        assertEquals(linkedSetOf("旅行", "海边", "日落"), expanded)
    }

    @Test
    fun `expanding unknown tag names keeps original names`() {
        val expanded = expandTagFilterNames(setOf("未分类"), allTags)

        assertEquals(setOf("未分类"), expanded)
    }
}
