package com.diary.app.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HomeSearchSourceTest {

    @Test
    fun `home search UI wires history suggestions filters and location search`() {
        val homeScreen = File("src/main/java/com/diary/app/ui/home/HomeScreen.kt").readText()
        val homeViewModel = File("src/main/java/com/diary/app/ui/home/HomeViewModel.kt").readText()
        val diaryDao = File("src/main/java/com/diary/app/data/DiaryDao.kt").readText()

        assertTrue(homeScreen.contains("SearchAssistPanel("))
        assertTrue(homeScreen.contains("HomeSearchFilters("))
        assertTrue(homeScreen.contains("onSearchSubmit = { viewModel.commitSearch(searchQuery) }"))
        assertTrue(homeViewModel.contains("private val _filterTagNames = MutableStateFlow<Set<String>>(emptySet())"))
        assertTrue(homeViewModel.contains("private val _filterWordCountRange = MutableStateFlow<SearchWordCountRange?>(null)"))
        assertTrue(diaryDao.contains("OR location LIKE '%' || :query || '%'"))
    }
}
