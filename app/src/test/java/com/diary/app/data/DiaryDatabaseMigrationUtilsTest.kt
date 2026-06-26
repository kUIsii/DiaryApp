package com.diary.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DiaryDatabaseMigrationUtilsTest {

    @Test
    fun `tag migration only adds columns that are still missing`() {
        val statements = buildMissingTagColumnStatements(setOf("id", "name", "color", "isPreset", "parent_id"))

        assertEquals(
            listOf("ALTER TABLE tags ADD COLUMN usage_count INTEGER NOT NULL DEFAULT 0"),
            statements
        )
    }

    @Test
    fun `tag migration becomes a no-op when both new columns already exist`() {
        val statements = buildMissingTagColumnStatements(
            setOf("id", "name", "color", "isPreset", "parent_id", "usage_count")
        )

        assertEquals(emptyList<String>(), statements)
    }
}
