package com.diary.app.data

import androidx.sqlite.db.SupportSQLiteDatabase

internal fun buildMissingTagColumnStatements(existingColumns: Set<String>): List<String> {
    val statements = mutableListOf<String>()
    if ("parent_id" !in existingColumns) {
        statements += "ALTER TABLE tags ADD COLUMN parent_id INTEGER"
    }
    if ("usage_count" !in existingColumns) {
        statements += "ALTER TABLE tags ADD COLUMN usage_count INTEGER NOT NULL DEFAULT 0"
    }
    return statements
}

internal fun buildAchievementRenameStatements(existingColumns: Set<String>): List<String> {
    return if ("iconEmoji" in existingColumns && "iconName" !in existingColumns) {
        listOf("ALTER TABLE achievements RENAME COLUMN iconEmoji TO iconName")
    } else {
        emptyList()
    }
}

internal fun buildMissingAchievementColumnStatements(existingColumns: Set<String>): List<String> {
    val statements = mutableListOf<String>()
    if ("category" !in existingColumns) {
        statements += "ALTER TABLE achievements ADD COLUMN category TEXT NOT NULL DEFAULT 'writing'"
    }
    if ("tier" !in existingColumns) {
        statements += "ALTER TABLE achievements ADD COLUMN tier INTEGER NOT NULL DEFAULT 1"
    }
    if ("iconEmoji" !in existingColumns) {
        statements += "ALTER TABLE achievements ADD COLUMN iconEmoji TEXT NOT NULL DEFAULT ''"
    }
    if ("flavorText" !in existingColumns) {
        statements += "ALTER TABLE achievements ADD COLUMN flavorText TEXT NOT NULL DEFAULT ''"
    }
    if ("isHidden" !in existingColumns) {
        statements += "ALTER TABLE achievements ADD COLUMN isHidden INTEGER NOT NULL DEFAULT 0"
    }
    return statements
}

internal fun SupportSQLiteDatabase.getColumnNames(tableName: String): Set<String> {
    val columns = linkedSetOf<String>()
    query("PRAGMA table_info($tableName)").use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            if (nameIndex >= 0) {
                columns += cursor.getString(nameIndex)
            }
        }
    }
    return columns
}
