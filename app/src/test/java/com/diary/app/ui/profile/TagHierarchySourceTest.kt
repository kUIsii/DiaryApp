package com.diary.app.ui.profile

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TagHierarchySourceTest {

    @Test
    fun `tag management wires nested expansion parent removal and cycle safe parent selection`() {
        val source = File("src/main/java/com/diary/app/ui/profile/TagManagementScreen.kt").readText()

        assertTrue(source.contains("expandedTags = expandedTags"))
        assertTrue(source.contains("isExpanded = expandedTags[child.id] ?: false"))
        assertTrue(source.contains("onToggleExpand = {"))
        assertTrue(source.contains("onSetParent(null)"))
        assertTrue(source.contains("val availableParents = filterAvailableParentTags(currentTag, allTags)"))
    }
}
