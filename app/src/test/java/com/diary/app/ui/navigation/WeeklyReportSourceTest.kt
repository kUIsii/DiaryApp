package com.diary.app.ui.navigation

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WeeklyReportSourceTest {

    @Test
    fun `weekly report is wired into nav host stats entry and notifications`() {
        val navSource = File("src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt").readText()
        val statsSource = File("src/main/java/com/diary/app/ui/stats/StatsScreen.kt").readText()
        val notificationSource = File("src/main/java/com/diary/app/ui/notification/NotificationScreen.kt").readText()

        assertTrue(navSource.contains("object WeeklyReport : Screen(\"weekly_report\""))
        assertTrue(navSource.contains("WeeklyReportScreen("))
        assertTrue(navSource.contains("onNavigateToWeeklyReport = { navController.navigate(Screen.WeeklyReport.route) }"))
        assertTrue(statsSource.contains("onNavigateToWeeklyReport: () -> Unit = {}"))
        assertTrue(notificationSource.contains("onNavigateToWeeklyReport: () -> Unit = {}"))
        assertTrue(notificationSource.contains("is WeeklySummaryNotification -> onNavigateToWeeklyReport()"))
    }
}
