package com.diary.app.ui.todo

import com.diary.app.data.TodoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class MobileTaskOpsTest {

    @Test
    fun `parseMobileCapture creates tasks from multiline mobile text`() {
        val baseDate = LocalDate.of(2026, 7, 4)

        val parsed = parseMobileCapture(
            text = """
                !! 今天 18:30 完成 App 快速捕获 #app #todo
                ! 明天 整理同步协议 #sync
                记得买咖啡
            """.trimIndent(),
            baseDate = baseDate,
            zoneId = ZoneId.of("Asia/Shanghai")
        )

        assertEquals(3, parsed.size)
        assertEquals("完成 App 快速捕获", parsed[0].title)
        assertEquals(2, parsed[0].priority)
        assertEquals(listOf("app", "todo"), parsed[0].tags)
        assertEquals(TodoItem.CATEGORY_TASK, parsed[0].category)
        assertEquals(baseDate.atTime(18, 30).atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli(), parsed[0].dueDate)
        assertEquals("整理同步协议", parsed[1].title)
        assertEquals(1, parsed[1].priority)
        assertEquals(baseDate.plusDays(1).atTime(20, 0).atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli(), parsed[1].dueDate)
        assertEquals("记得买咖啡", parsed[2].title)
    }

    @Test
    fun `buildTodayThree picks urgent focused and actionable tasks`() {
        val now = LocalDate.of(2026, 7, 4).atTime(8, 0).atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()
        val todayNoon = LocalDate.of(2026, 7, 4).atTime(12, 0).atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()
        val tomorrow = LocalDate.of(2026, 7, 5).atTime(20, 0).atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()

        val result = buildTodayThree(
            tasks = listOf(
                TodoItem(id = 1, title = "普通备忘", category = TodoItem.CATEGORY_TASK),
                TodoItem(id = 2, title = "今天交付", priority = 1, dueDate = todayNoon, category = TodoItem.CATEGORY_TASK),
                TodoItem(id = 3, title = "明天检查", priority = 2, dueDate = tomorrow, category = TodoItem.CATEGORY_TASK),
                TodoItem(id = 4, title = "已完成", isCompleted = true, priority = 2, dueDate = todayNoon, category = TodoItem.CATEGORY_TASK)
            ),
            now = now
        )

        assertEquals(listOf(2L, 3L, 1L), result.map { it.task.id })
        assertTrue(result.first().reason.contains("今天"))
    }

    @Test
    fun `extractTodoDraftsFromDiary keeps drafts confirmable`() {
        val drafts = extractTodoDraftsFromDiary(
            text = "今天要完成移动端捕获，还需要整理同步协议。晚上记得复盘体验。",
            sourceId = 42L
        )

        assertEquals(3, drafts.size)
        assertEquals("完成移动端捕获", drafts[0].title)
        assertEquals(42L, drafts[0].sourceDiaryId)
        assertTrue(drafts[0].confidence >= 0.6f)
    }

    @Test
    fun `buildDesktopSyncPayload includes version device and task counts`() {
        val payload = buildDesktopSyncPayload(
            tasks = listOf(
                TodoItem(id = 1, title = "任务一", isCompleted = false, tags = "app, todo"),
                TodoItem(id = 2, title = "任务二", isCompleted = true)
            ),
            deviceId = "phone-1",
            exportedAt = 1783100000000L
        )

        assertEquals(1, payload.version)
        assertEquals("phone-1", payload.syncMeta.deviceId)
        assertEquals(2, payload.tasks.size)
        assertEquals(1, payload.summary.completed)
        assertEquals(listOf("app", "todo"), payload.tasks[0].tags)
    }
}
