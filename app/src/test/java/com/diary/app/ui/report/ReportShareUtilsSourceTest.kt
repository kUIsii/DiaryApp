package com.diary.app.ui.report

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportShareUtilsSourceTest {

    @Test
    fun `report share utils save share cards and expose file provider uri`() {
        val source = File("src/main/java/com/diary/app/ui/report/ReportShareUtils.kt").readText()

        assertTrue(source.contains("FileProvider.getUriForFile"))
        assertTrue(source.contains("buildWeeklyReportShareCard"))
        assertTrue(source.contains("buildMonthlyReportShareCard"))
        assertTrue(source.contains("buildAnnualReportShareCard"))
        assertTrue(source.contains("Bitmap.createBitmap"))
    }
}
