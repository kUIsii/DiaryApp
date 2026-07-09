package com.diary.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DiaryDatabaseSourceTest {

    @Test
    fun `database open path keeps getDatabase lazy and recovers safely on background thread`() {
        val source = File("src/main/java/com/diary/app/data/DiaryDatabase.kt").readText()

        // 不使用 Room 的静默全量破坏性迁移 API（数据丢失无感知）
        assertFalse(source.contains(".fallbackToDestructiveMigration()"))
        // 主线程可调用的 getDatabase 不应强制打开数据库（避免 ANR）
        assertFalse(source.contains(".also { it.openHelper.writableDatabase }"))
        // 后台入口 openSafely 负责真正打开 + 失败时备份并重建
        assertTrue(source.contains("fun openSafely("))
        assertTrue(source.contains("backupDatabaseFiles"))
        assertTrue(source.contains("deleteDatabase(\"diary_database\")"))
        // 仍保留自定义异常类型用于彻底失败时上报
        assertTrue(source.contains("DiaryDatabaseOpenException"))
    }
}
