package com.diary.app.ui.todo

import com.diary.app.data.TodoItem
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class MobileCaptureTask(
    val title: String,
    val priority: Int = 0,
    val dueDate: Long? = null,
    val tags: List<String> = emptyList(),
    val category: String = TodoItem.CATEGORY_TASK
)

data class TodayThreeItem(
    val task: TodoItem,
    val reason: String
)

data class TodoDraft(
    val title: String,
    val reason: String,
    val confidence: Float,
    val sourceDiaryId: Long? = null
)

data class DesktopSyncPayload(
    val version: Int,
    val syncMeta: SyncMeta,
    val summary: SyncSummary,
    val tasks: List<SyncTask>
)

data class SyncMeta(
    val deviceId: String,
    val exportedAt: Long,
    val source: String = "android-app"
)

data class SyncSummary(
    val total: Int,
    val completed: Int,
    val active: Int
)

data class SyncTask(
    val id: Long,
    val title: String,
    val description: String,
    val completed: Boolean,
    val priority: Int,
    val dueDate: Long?,
    val tags: List<String>,
    val category: String,
    val updatedAt: Long
)

fun parseMobileCapture(
    text: String,
    baseDate: LocalDate = LocalDate.now(),
    zoneId: ZoneId = ZoneId.systemDefault()
): List<MobileCaptureTask> {
    return text.lineSequence()
        .map { parseCaptureLine(it, baseDate, zoneId) }
        .filterNotNull()
        .toList()
}

fun buildTodayThree(tasks: List<TodoItem>, now: Long = System.currentTimeMillis()): List<TodayThreeItem> {
    val zone = ZoneId.systemDefault()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
    val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

    return tasks
        .filter { !it.isCompleted && it.parentId == null && it.category != TodoItem.CATEGORY_GOAL }
        .sortedWith(
            compareByDescending<TodoItem> { it.dueDate != null && it.dueDate in start until end }
                .thenByDescending { it.isPinned }
                .thenByDescending { it.priority }
                .thenBy { it.dueDate ?: Long.MAX_VALUE }
                .thenByDescending { it.createdAt }
        )
        .take(3)
        .map { task -> TodayThreeItem(task = task, reason = todayThreeReason(task, now, start, end)) }
}

fun extractTodoDraftsFromDiary(text: String, sourceId: Long? = null): List<TodoDraft> {
    if (text.isBlank()) return emptyList()
    val triggers = listOf("要", "需要", "记得", "别忘", "完成", "整理", "准备", "复盘")
    return text
        .split("。", "！", "？", "；", ";", "\n", "，", ",")
        .map { it.trim() }
        .filter { part -> part.isNotBlank() && triggers.any { part.contains(it) } }
        .take(6)
        .mapIndexed { index, part ->
            TodoDraft(
                title = cleanupDraftTitle(part),
                reason = "从日志中的行动语气识别，确认后写入待办。",
                confidence = (0.64f + index * 0.06f).coerceAtMost(0.92f),
                sourceDiaryId = sourceId
            )
        }
}

fun buildDesktopSyncPayload(
    tasks: List<TodoItem>,
    deviceId: String,
    exportedAt: Long = System.currentTimeMillis()
): DesktopSyncPayload {
    val syncTasks = tasks.map { task ->
        SyncTask(
            id = task.id,
            title = task.title,
            description = task.description,
            completed = task.isCompleted,
            priority = task.priority,
            dueDate = task.dueDate,
            tags = TodoItem.getTagList(task.tags),
            category = task.category,
            updatedAt = task.completedAt ?: task.createdAt
        )
    }
    return DesktopSyncPayload(
        version = 1,
        syncMeta = SyncMeta(deviceId = deviceId, exportedAt = exportedAt),
        summary = SyncSummary(
            total = tasks.size,
            completed = tasks.count { it.isCompleted },
            active = tasks.count { !it.isCompleted }
        ),
        tasks = syncTasks
    )
}

private fun parseCaptureLine(line: String, baseDate: LocalDate, zoneId: ZoneId): MobileCaptureTask? {
    var working = line.trim()
    if (working.isBlank()) return null

    val priority = when {
        working.startsWith("!!") -> 2
        working.startsWith("!") -> 1
        else -> 0
    }
    working = working.replace(Regex("^!!?\\s*"), "")

    val tags = Regex("#([\\p{L}\\p{N}_-]+)")
        .findAll(working)
        .map { it.groupValues[1] }
        .toList()
    working = working.replace(Regex("#[\\p{L}\\p{N}_-]+"), "").trim()

    val due = parseDue(working, baseDate, zoneId)
    working = due.cleanedTitle
    if (working.isBlank()) return null

    return MobileCaptureTask(
        title = working,
        priority = priority,
        dueDate = due.dueDate,
        tags = tags
    )
}

private data class ParsedDue(val cleanedTitle: String, val dueDate: Long?)

private fun parseDue(text: String, baseDate: LocalDate, zoneId: ZoneId): ParsedDue {
    var working = text.trim()
    val dayToken = Regex("^(今天|明天|后天)\\s*").find(working)
    val timeToken = Regex("\\b([01]?\\d|2[0-3]):([0-5]\\d)\\b").find(working)
    val date = when (dayToken?.groupValues?.get(1)) {
        "今天" -> baseDate
        "明天" -> baseDate.plusDays(1)
        "后天" -> baseDate.plusDays(2)
        else -> null
    }

    val dueDate = date?.let {
        val time = if (timeToken != null) {
            LocalTime.of(timeToken.groupValues[1].toInt(), timeToken.groupValues[2].toInt())
        } else {
            LocalTime.of(20, 0)
        }
        it.atTime(time).atZone(zoneId).toInstant().toEpochMilli()
    }
    if (dayToken != null) working = working.removeRange(dayToken.range).trim()
    if (timeToken != null) working = working.replace(timeToken.value, "").trim()
    return ParsedDue(cleanedTitle = working.replace(Regex("\\s+"), " "), dueDate = dueDate)
}

private fun todayThreeReason(task: TodoItem, now: Long, start: Long, end: Long): String {
    return when {
        task.dueDate != null && task.dueDate in start until end -> "今天截止，适合放入今日三件事。"
        task.dueDate != null && task.dueDate < now -> "已经逾期，需要优先处理。"
        task.isPinned -> "已置顶，说明它是当前关注事项。"
        task.priority >= 2 -> "高优先级任务，建议今天推进。"
        task.priority == 1 -> "重要任务，适合安排一个明确下一步。"
        else -> "当前可执行，作为低阻力推进项。"
    }
}

private fun cleanupDraftTitle(text: String): String {
    return text
        .removePrefix("今天")
        .removePrefix("还")
        .removePrefix("并且")
        .removePrefix("晚上")
        .removePrefix("要")
        .removePrefix("需要")
        .removePrefix("记得")
        .removePrefix("别忘")
        .trim()
}
