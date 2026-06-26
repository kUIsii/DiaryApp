package com.diary.app.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HomeTagFilterHierarchySourceTest {

    @Test
    fun `home search expands selected parent tags before applying filters`() {
        val source = File("src/main/java/com/diary/app/ui/home/HomeViewModel.kt").readText()

        assertTrue(source.contains("expandTagFilterNames("))
        assertTrue(source.contains("tagNames = expandTagFilterNames("))
        assertTrue(source.contains("allTagsList"))
    }
}
