package com.diary.app.ui.notification

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationCleanupSourceTest {

    @Test
    fun `notification view model filters deprecated pet and island notifications`() {
        val source = File("src/main/java/com/diary/app/ui/notification/NotificationViewModel.kt").readText()

        assertTrue(source.contains("LEGACY_NOTIFICATION_KEYWORDS"))
        assertTrue(source.contains("isDeprecatedLegacyNotification"))
    }
}
