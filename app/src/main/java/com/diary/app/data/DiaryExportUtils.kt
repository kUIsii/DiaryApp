package com.diary.app.data

internal const val MAX_EXPORT_INLINE_MEDIA_LENGTH = 256 * 1024

private val inlineImageRegex = Regex(
    """"image"\s*:\s*"data:image/[^"]{262144,}"""",
    setOf(RegexOption.IGNORE_CASE)
)

internal fun normalizeContentForExport(content: String): String {
    if (content.isBlank()) return content
    return inlineImageRegex.replace(content) { "\"image\":\"\"" }
}
