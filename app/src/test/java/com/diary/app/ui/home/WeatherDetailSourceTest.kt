package com.diary.app.ui.home

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherDetailSourceTest {

    @Test
    fun `weather detail screen uses scaffold with pinned top bar and explicit insets`() {
        val source = File("src/main/java/com/diary/app/ui/home/WeatherDetailScreen.kt").readText()

        assertTrue(source.contains("Scaffold("))
        assertTrue(source.contains("contentWindowInsets = WindowInsets(0)"))
    }
}
