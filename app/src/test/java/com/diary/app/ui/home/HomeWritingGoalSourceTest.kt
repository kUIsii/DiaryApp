package com.diary.app.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HomeWritingGoalSourceTest {

    @Test
    fun `home screen shows writing goal card backed by shared goal progress model`() {
        val homeScreen = File("src/main/java/com/diary/app/ui/home/HomeScreen.kt").readText()
        val homeViewModel = File("src/main/java/com/diary/app/ui/home/HomeViewModel.kt").readText()
        val goalUtils = File("src/main/java/com/diary/app/ui/stats/WritingGoalProgressUtils.kt").readText()

        assertTrue(homeScreen.contains("HomeWritingGoalCard("))
        assertTrue(homeViewModel.contains("val goalProgress"))
        assertTrue(homeViewModel.contains("computeGoalProgress("))
        assertTrue(goalUtils.contains("data class GoalProgress"))
    }
}
