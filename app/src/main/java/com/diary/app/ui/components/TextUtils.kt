package com.diary.app.ui.components

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Pre-compiled regex patterns for cleanPreviewText
private val CHECKBOX_REGEX = Regex("[☐☑✓✔✕✖✗✘❎✅❌]")
private val LIST_SYMBOL_REGEX = Regex("^[•·‣⁃]\\s*", RegexOption.MULTILINE)
private val MULTI_NEWLINE_REGEX = Regex("\\n{3,}")

/**
 * Format entry timestamp to time string (HH:mm)
 */
fun formatEntryTime(timestamp: Long): String {
    val dateTime = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    return dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
}

/**
 * Format entry timestamp to date+time string (M月d日 HH:mm)
 */
fun formatEntryDateTime(timestamp: Long): String {
    val dateTime = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    return dateTime.format(DateTimeFormatter.ofPattern("M月d日 HH:mm"))
}

/**
 * Clean preview text by removing editor symbols
 */
fun cleanPreviewText(text: String): String {
    return text
        .replace("\\n", "\n")
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .replace(CHECKBOX_REGEX, "")
        .replace(LIST_SYMBOL_REGEX, "")
        .replace(MULTI_NEWLINE_REGEX, "\n\n")
        .trim()
}

/**
 * Format word count for display (e.g., 1.2万)
 */
fun formatWordCount(count: Int): String {
    return when {
        count >= 10000 -> String.format("%.1f万", count / 10000.0)
        count >= 1000 -> String.format("%.1fk", count / 1000.0)
        else -> "$count"
    }
}

/**
 * Format word count with Chinese unit (e.g., 1万字)
 */
fun formatWordCountWithUnit(count: Int): String = when {
    count >= 10000 -> "${count / 10000}万字"
    count >= 1000 -> "${count / 1000}千字"
    else -> "${count}字"
}
