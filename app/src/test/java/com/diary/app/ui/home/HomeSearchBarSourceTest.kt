package com.diary.app.ui.home

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSearchBarSourceTest {

    @Test
    fun `home search bar uses solid themed container instead of transparent glass strip`() {
        val source = File("src/main/java/com/diary/app/ui/home/HomeScreen.kt").readText()

        assertTrue(source.contains("MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)"))
        assertTrue(source.contains(".border("))
    }
}
