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

    @Test
    fun `achievement rename only runs when source exists and target is missing`() {
        val statements = buildAchievementRenameStatements(
            setOf("id", "key", "name", "description", "iconEmoji")
        )

        assertEquals(
            listOf("ALTER TABLE achievements RENAME COLUMN iconEmoji TO iconName"),
            statements
        )
    }

    @Test
    fun `achievement rename becomes no-op after rename already happened`() {
        val statements = buildAchievementRenameStatements(
            setOf("id", "key", "name", "description", "iconName")
        )

        assertEquals(emptyList<String>(), statements)
    }

    @Test
    fun `achievement migration only adds still-missing columns`() {
        val statements = buildMissingAchievementColumnStatements(
            setOf("id", "key", "name", "description", "iconName", "category", "tier")
        )

        assertEquals(
            listOf(
                "ALTER TABLE achievements ADD COLUMN iconEmoji TEXT NOT NULL DEFAULT ''",
                "ALTER TABLE achievements ADD COLUMN flavorText TEXT NOT NULL DEFAULT ''",
                "ALTER TABLE achievements ADD COLUMN isHidden INTEGER NOT NULL DEFAULT 0"
            ),
            statements
        )
    }
}
