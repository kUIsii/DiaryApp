package com.diary.app.ui.navigation

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportShareSourceTest {

    @Test
    fun `nav host upgrades report sharing from text only to shared report image flow`() {
        val navSource = File("src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt").readText()

        assertTrue(navSource.contains("shareReportImage("))
        assertTrue(navSource.contains("buildWeeklyReportShareCard("))
        assertTrue(navSource.contains("buildMonthlyReportShareCard("))
        assertTrue(navSource.contains("buildAnnualReportShareCard("))
    }
}
