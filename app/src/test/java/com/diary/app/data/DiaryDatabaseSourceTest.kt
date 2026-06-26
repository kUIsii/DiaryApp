package com.diary.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DiaryDatabaseSourceTest {

    @Test
    fun `database no longer uses destructive migration fallback in main open path`() {
        val source = File("src/main/java/com/diary/app/data/DiaryDatabase.kt").readText()

        assertTrue(source.contains("throw DiaryDatabaseOpenException"))
        assertFalse(source.contains("Migration failed, attempting destructive recovery"))
        assertFalse(source.contains(".fallbackToDestructiveMigration()"))
    }
}
