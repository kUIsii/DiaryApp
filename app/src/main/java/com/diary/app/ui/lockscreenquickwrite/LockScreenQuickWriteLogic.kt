package com.diary.app.ui.lockscreenquickwrite

data class SmartLinkSuggestion(
    val quickWriteId: Long,
    val linkedEntryId: Long,
    val message: String
)

fun buildDiaryContentFromQuickWrite(content: String): String {
    val escaped = content
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
    return "{\"ops\":[{\"insert\":\"$escaped\"}]}"
}
