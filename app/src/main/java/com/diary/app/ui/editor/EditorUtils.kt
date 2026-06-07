package com.diary.app.ui.editor

import android.content.SharedPreferences
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.diary.app.data.TemplateCategory

/**
 * Properly unescape a JSON-encoded string returned by WebView.evaluateJavascript().
 * Uses org.json.JSONTokener for correct handling of all escape sequences.
 */
internal fun unescapeEvaluateJsResult(raw: String?): String {
    if (raw.isNullOrEmpty()) return ""
    return try {
        val value = org.json.JSONTokener(raw).nextValue()
        if (value is String) value else raw
    } catch (_: Exception) {
        // Fallback: manual unescape with correct order (\\\\ must come before \\n etc.)
        val s = if (raw.startsWith("\"") && raw.endsWith("\"")) raw.substring(1, raw.length - 1) else raw
        s.replace("\\\\", "\u0000")
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\t", "\t")
            .replace("\\r", "\r")
            .replace("\u0000", "\\")
    }
}

internal fun getEditorFontSize(prefs: SharedPreferences): Int {
    return when (prefs.getString("editor_font_size", "small")) {
        "tiny" -> 10
        "small" -> 14
        "large" -> 18
        "extra_large" -> 20
        else -> 16
    }
}

internal fun escapeForJs(input: String): String {
    return input
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
        .replace("\b", "\\b")
        .replace("\u0000", "")
}

internal fun countWords(text: String): Int {
    var count = 0
    var inWord = false
    for (ch in text) {
        if (ch.isWhitespace()) {
            inWord = false
        } else if (ch.code in 0x4E00..0x9FFF || ch.code in 0x3400..0x4DBF) {
            count++
            inWord = false
        } else {
            if (!inWord) {
                count++
                inWord = true
            }
        }
    }
    return count
}

internal fun summarizeSelectedNames(
    names: List<String>,
    emptyLabel: String,
    maxVisible: Int = 2
): String {
    val cleaned = names.map { it.trim() }.filter { it.isNotEmpty() }
    if (cleaned.isEmpty()) return emptyLabel

    val visibleNames = cleaned.take(maxVisible)
    val hiddenCount = cleaned.size - visibleNames.size
    val summary = visibleNames.joinToString(" · ")

    return if (hiddenCount > 0) {
        "$summary +$hiddenCount"
    } else {
        summary
    }
}

internal fun iconForTemplate(iconName: String): ImageVector {
    return when (iconName) {
        "today" -> Icons.Default.Today
        "favorite" -> Icons.Default.Favorite
        "mood" -> Icons.Default.Favorite
        "psychology" -> Icons.Default.Favorite
        "edit" -> Icons.Default.FormatSize
        "menu_book" -> Icons.Default.MenuBook
        "flight" -> Icons.Default.Flight
        "work" -> Icons.Default.Work
        else -> Icons.Default.Today
    }
}

internal fun templateCategoryLabel(category: TemplateCategory): String {
    return when (category) {
        TemplateCategory.DAILY -> "日常"
        TemplateCategory.EMOTIONAL -> "情感"
        TemplateCategory.CREATIVE -> "创意"
        TemplateCategory.TRAVEL -> "旅行"
        TemplateCategory.WORK -> "工作"
    }
}

internal fun getTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3_600_000 -> "${diff / 60_000}分钟前"
        diff < 86_400_000 -> "${diff / 3_600_000}小时前"
        diff < 604_800_000 -> "${diff / 86_400_000}天前"
        else -> {
            val date = java.util.Date(timestamp)
            java.text.SimpleDateFormat("MM/dd", java.util.Locale.getDefault()).format(date)
        }
    }
}
