package com.diary.app.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.diary.app.BuildConfig
import com.diary.app.ui.components.moodLabelForLevel
import com.diary.app.ui.components.weatherLabelFor
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DiaryExporter {

    private data class ExportFile(
        val title: String,
        val content: String,
        val plainText: String,
        val moodLevel: Int?,
        val weather: String?,
        val location: String?,
        val latitude: Double?,
        val longitude: Double?,
        val createdAt: Long,
        val updatedAt: Long,
        val tags: List<String>
    )

    private data class ExportTag(
        val name: String,
        val color: Long,
        val isPreset: Boolean
    )

    private data class ExportData(
        val app: String = "DiaryApp",
        val version: String = BuildConfig.VERSION_NAME,
        val exportDate: String,
        val entries: List<ExportFile>,
        val tags: List<ExportTag>
    )

    suspend fun export(context: Context, dao: DiaryDao, tagDao: TagDao): String {
        // 分批查询避免 CursorWindow 溢出
        val entries = mutableListOf<DiaryEntry>()
        var offset = 0
        val batchSize = 50
        while (true) {
            val batch = dao.getEntriesBatchForExport(offset, batchSize)
            if (batch.isEmpty()) break
            entries.addAll(batch)
            offset += batchSize
        }

        val tags = tagDao.getAllTagsOnce()
        val allDiaryTags = tagDao.getAllDiaryTags()

        // Build diaryId -> tag names map
        val tagMap = tags.associateBy { it.id }
        val diaryTagMap = allDiaryTags.groupBy({ it.diaryId }, { tagMap[it.tagId]?.name ?: "" })

        val exportEntries = entries.map { entry ->
            ExportFile(
                title = entry.title,
                content = normalizeContentForExport(entry.content),
                plainText = entry.plainText,
                moodLevel = entry.moodLevel,
                weather = entry.weather,
                location = entry.location,
                latitude = entry.latitude,
                longitude = entry.longitude,
                createdAt = entry.createdAt,
                updatedAt = entry.updatedAt,
                tags = diaryTagMap[entry.id] ?: emptyList()
            )
        }

        val exportTags = tags.map { tag ->
            ExportTag(
                name = tag.name,
                color = tag.color,
                isPreset = tag.isPreset
            )
        }

        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
        val exportData = ExportData(
            exportDate = now,
            entries = exportEntries,
            tags = exportTags
        )

        val gson = GsonBuilder().setPrettyPrinting().create()
        val json = gson.toJson(exportData)

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val fileName = "diary_backup_$timestamp.json"

        return saveToFile(context, fileName, json)
    }

    suspend fun exportAsMarkdown(context: Context, dao: DiaryDao, tagDao: TagDao): String {
        val entries = mutableListOf<DiaryEntry>()
        var offset = 0
        val batchSize = 50
        while (true) {
            val batch = dao.getEntriesBatch(offset, batchSize)
            if (batch.isEmpty()) break
            entries.addAll(batch)
            offset += batchSize
        }
        val tags = tagDao.getAllTagsOnce()
        val allDiaryTags = tagDao.getAllDiaryTags()

        val tagMap = tags.associateBy { it.id }
        val diaryTagMap = allDiaryTags.groupBy({ it.diaryId }, { tagMap[it.tagId]?.name ?: "" })

        val sb = StringBuilder()
        sb.appendLine("# 日记本导出")
        sb.appendLine()
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        sb.appendLine("导出时间: $now")
        sb.appendLine()

        val dateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm")

        entries.forEach { entry ->
            val entryDateTime = Instant.ofEpochMilli(entry.createdAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
            val dateText = entryDateTime.format(dateFormatter)

            sb.appendLine("---")
            sb.appendLine()
            sb.appendLine("## $dateText")

            val metaParts = mutableListOf<String>()
            entry.moodLevel?.let { level ->
                metaParts.add("**心情:** ${moodLabelForLevel(level)}")
            }
            entry.weather?.let { weather ->
                metaParts.add("**天气:** ${weatherLabelFor(weather)}")
            }
            val entryTags = diaryTagMap[entry.id] ?: emptyList()
            if (entryTags.isNotEmpty()) {
                metaParts.add("**标签:** ${entryTags.joinToString(", ")}")
            }
            if (metaParts.isNotEmpty()) {
                sb.appendLine(metaParts.joinToString(" | "))
            }

            sb.appendLine()
            if (entry.plainText.isNotBlank()) {
                sb.appendLine(entry.plainText)
            }
            sb.appendLine()
        }

        val markdown = sb.toString()
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val fileName = "diary_export_$timestamp.md"

        return saveToFile(context, fileName, markdown, "text/markdown")
    }

    private fun saveToFile(context: Context, fileName: String, content: String, mimeType: String = "application/json"): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: throw Exception("无法创建文件")

            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(content.toByteArray(Charsets.UTF_8))
            } ?: throw Exception("无法写入文件")

            return "Download/$fileName"
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName)
            file.writeText(content, Charsets.UTF_8)
            return "Download/$fileName"
        }
    }

    /**
     * Export a single diary entry as a beautifully formatted image (PNG).
     * Returns the path to the saved image file.
     */
    suspend fun exportAsImage(context: Context, entry: DiaryEntry, tags: List<String> = emptyList()): String {
        val width = 1080
        val padding = 64
        val contentWidth = width - padding * 2

        // Colors
        val bgColor = 0xFFF8F6F4.toInt()
        val primaryColor = 0xFF2D2D3A.toInt()
        val secondaryColor = 0xFF6B6B80.toInt()
        val accentColor = 0xFF667EEA.toInt()
        val dividerColor = 0xFFE8E6E1.toInt()

        // Paints
        val datePaint = Paint().apply {
            color = primaryColor
            textSize = 52f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val metaPaint = Paint().apply {
            color = secondaryColor
            textSize = 32f
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = primaryColor
            textSize = 38f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            isAntiAlias = true
            letterSpacing = 0.02f
        }
        val tagPaint = Paint().apply {
            color = accentColor
            textSize = 28f
            isAntiAlias = true
        }
        val footerPaint = Paint().apply {
            color = secondaryColor
            textSize = 26f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val dividerPaint = Paint().apply {
            color = dividerColor
            strokeWidth = 2f
        }

        // Calculate content
        val entryDate = Instant.ofEpochMilli(entry.createdAt)
            .atZone(ZoneId.systemDefault()).toLocalDateTime()
        val dateText = entryDate.format(DateTimeFormatter.ofPattern("yyyy年M月d日"))
        val dayOfWeek = entryDate.format(DateTimeFormatter.ofPattern("EEEE", java.util.Locale.CHINESE))
        val timeText = entryDate.format(DateTimeFormatter.ofPattern("HH:mm"))

        val metaParts = mutableListOf("$dayOfWeek $timeText")
        entry.moodLevel?.let { metaParts.add("心情: ${moodLabelForLevel(it)}") }
        entry.weather?.let { metaParts.add("天气: ${weatherLabelFor(it)}") }
        val metaText = metaParts.joinToString("  |  ")

        // Word-wrap body text
        val bodyLines = if (entry.plainText.isNotBlank()) {
            wordWrap(entry.plainText, bodyPaint, contentWidth)
        } else {
            emptyList()
        }

        // Calculate total height
        var y = padding.toFloat()
        y += 60f // date
        y += 40f // spacing
        y += 36f // meta
        y += 40f // spacing
        y += 4f // divider
        y += 40f // spacing after divider

        // Tags
        if (tags.isNotEmpty()) {
            y += 36f
            y += 32f
        }

        // Body
        y += bodyLines.size * 56f
        y += 60f // spacing before footer

        // Divider
        y += 4f
        y += 40f

        // Footer
        y += 30f
        y += padding.toFloat()

        val totalHeight = y.toInt().coerceAtLeast(800)

        // Create bitmap
        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(bgColor)

        var currentY = padding.toFloat()

        // Date
        canvas.drawText(dateText, padding.toFloat(), currentY + 52f, datePaint)
        currentY += 60f + 40f

        // Meta line
        canvas.drawText(metaText, padding.toFloat(), currentY + 32f, metaPaint)
        currentY += 36f + 40f

        // Divider
        canvas.drawLine(
            padding.toFloat(), currentY,
            (width - padding).toFloat(), currentY,
            dividerPaint
        )
        currentY += 40f

        // Tags
        if (tags.isNotEmpty()) {
            val tagText = tags.joinToString("  ·  ") { "#$it" }
            canvas.drawText(tagText, padding.toFloat(), currentY + 28f, tagPaint)
            currentY += 36f + 32f
        }

        // Body text
        for (line in bodyLines) {
            canvas.drawText(line, padding.toFloat(), currentY + 38f, bodyPaint)
            currentY += 56f
        }

        currentY += 60f

        // Footer divider
        canvas.drawLine(
            padding.toFloat(), currentY,
            (width - padding).toFloat(), currentY,
            dividerPaint
        )
        currentY += 40f

        // Footer
        canvas.drawText("日记本", (width / 2).toFloat(), currentY + 26f, footerPaint)

        // Save to file
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val fileName = "diary_share_$timestamp.png"

        return saveBitmapToFile(context, bitmap, fileName)
    }

    fun exportSingleAsHtml(context: Context, entry: DiaryEntry, tags: List<String> = emptyList()): String {
        val entryDate = Instant.ofEpochMilli(entry.createdAt)
            .atZone(ZoneId.systemDefault()).toLocalDateTime()
        val dateText = entryDate.format(DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm"))
        val dayOfWeek = entryDate.format(DateTimeFormatter.ofPattern("EEEE", java.util.Locale.CHINESE))

        val moodLabel = entry.moodLevel?.let { moodLabelForLevel(it) }
        val weatherLabel = entry.weather?.let { weatherLabelFor(it) }
        val locationText = entry.location?.trim().takeUnless { it.isNullOrEmpty() }

        // Convert delta to HTML using the same logic as viewer.html
        val bodyHtml = deltaToHtmlForExport(entry.content)

        // Embed local images as base64
        val embeddedHtml = embedLocalImages(context, bodyHtml)

        val sb = StringBuilder()
        sb.appendLine("<!DOCTYPE html>")
        sb.appendLine("<html lang='zh-CN'>")
        sb.appendLine("<head>")
        sb.appendLine("<meta charset='UTF-8'>")
        sb.appendLine("<meta name='viewport' content='width=device-width, initial-scale=1.0'>")
        sb.appendLine("<title>${escapeHtml(dateText)}</title>")
        sb.appendLine("<style>")
        sb.appendLine("body { font-family: Georgia, 'Noto Serif SC', serif; max-width: 680px; margin: 0 auto; padding: 32px 20px; color: #2D2D3A; background: #FAFAF8; line-height: 1.9; }")
        sb.appendLine("h1 { font-size: 22px; font-weight: 600; margin-bottom: 4px; }")
        sb.appendLine(".meta { color: #6B6B80; font-size: 13px; margin-bottom: 24px; }")
        sb.appendLine(".meta span { margin-right: 12px; }")
        sb.appendLine(".content { font-size: 16px; }")
        sb.appendLine(".content img { max-width: 100%; border-radius: 8px; margin: 8px 0; }")
        sb.appendLine(".content blockquote { border-left: 3px solid #6B8DB5; padding: 12px 16px; margin: 12px 0; background: rgba(107,141,181,0.05); border-radius: 0 8px 8px 0; }")
        sb.appendLine(".content pre { background: rgba(0,0,0,0.04); padding: 12px; border-radius: 8px; overflow-x: auto; }")
        sb.appendLine(".content code { font-family: 'SF Mono', monospace; font-size: 14px; }")
        sb.appendLine(".tags { margin-top: 24px; }")
        sb.appendLine(".tag { display: inline-block; padding: 3px 10px; border-radius: 12px; font-size: 12px; margin-right: 6px; margin-bottom: 6px; }")
        sb.appendLine(".footer { margin-top: 32px; padding-top: 16px; border-top: 1px solid #E8E6E1; color: #8A96A8; font-size: 12px; text-align: center; }")
        sb.appendLine("</style>")
        sb.appendLine("</head>")
        sb.appendLine("<body>")

        sb.appendLine("<h1>${escapeHtml(dateText)}</h1>")
        sb.appendLine("<div class='meta'>")
        sb.appendLine("<span>$dayOfWeek</span>")
        moodLabel?.let { sb.appendLine("<span>心情: ${escapeHtml(it)}</span>") }
        weatherLabel?.let { sb.appendLine("<span>天气: ${escapeHtml(it)}</span>") }
        locationText?.let { sb.appendLine("<span>地点: ${escapeHtml(it)}</span>") }
        sb.appendLine("</div>")

        sb.appendLine("<div class='content'>$embeddedHtml</div>")

        if (tags.isNotEmpty()) {
            sb.appendLine("<div class='tags'>")
            tags.forEach { tag ->
                sb.appendLine("<span class='tag'>${escapeHtml(tag)}</span>")
            }
            sb.appendLine("</div>")
        }

        sb.appendLine("<div class='footer'>日记本 App</div>")
        sb.appendLine("</body>")
        sb.appendLine("</html>")

        val html = sb.toString()
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val fileName = "diary_${timestamp}.html"

        return saveToFile(context, fileName, html, "text/html")
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }

    /**
     * Delta-to-html for export. Handles all Quill formats:
     * bold, italic, underline, strike, code, link, script, color, background,
     * header, blockquote, code-block, ordered/bullet/checked/unchecked lists,
     * indent, align, divider, image, video.
     */
    private fun deltaToHtmlForExport(content: String): String {
        if (content.isBlank()) return ""
        return try {
            val gson = com.google.gson.Gson()
            val delta = gson.fromJson(content, DeltaForExport::class.java)
            if (delta?.ops == null) return escapeHtml(content)

            // First pass: build blocks
            data class Block(val html: String, val attrs: Map<String, Any>?)
            val blocks = mutableListOf<Block>()
            var currentBlock = StringBuilder()
            var blockAttrs: Map<String, Any>? = null

            for (op in delta.ops) {
                val insert = op.insert ?: continue
                if (insert is String) {
                    val text = insert.replace("\\n", "\n").replace("\\r\\n", "\n").replace("\\r", "\n")
                    val lines = text.split("\n")
                    for (j in lines.indices) {
                        if (j > 0) {
                            blocks.add(Block(currentBlock.toString(), blockAttrs))
                            currentBlock = StringBuilder()
                            blockAttrs = null
                        }
                        if (lines[j].isNotEmpty()) {
                            currentBlock.append(wrapInlineForExport(lines[j], op.attributes))
                        }
                        if (j < lines.size - 1) {
                            blockAttrs = op.attributes?.mapValues { it.value as? Any ?: "" }
                        }
                    }
                } else if (insert is Map<*, *>) {
                    val imgSrc = insert["image"]
                    val videoSrc = insert["video"]
                    val hasDivider = insert["divider"] != null
                    when {
                        hasDivider -> {
                            if (currentBlock.isNotEmpty()) {
                                blocks.add(Block(currentBlock.toString(), blockAttrs))
                                currentBlock = StringBuilder()
                                blockAttrs = null
                            }
                            blocks.add(Block("", mapOf("divider" to true)))
                        }
                        imgSrc is String -> {
                            currentBlock.append("<img src='${escapeHtml(imgSrc)}' />")
                        }
                        videoSrc is String -> {
                            currentBlock.append("<video src='${escapeHtml(videoSrc)}' controls></video>")
                        }
                    }
                }
            }
            if (currentBlock.isNotEmpty()) {
                blocks.add(Block(currentBlock.toString(), blockAttrs))
            }

            // Second pass: group consecutive list items, render other blocks
            val sb = StringBuilder()
            data class ListItem(val html: String, val type: String, val listClass: String?)
            var listBuffer = mutableListOf<ListItem>()
            var listType: String? = null

            fun flushList() {
                if (listBuffer.isEmpty()) return
                val tag = if (listType == "ordered") "ol" else "ul"
                sb.append("<$tag>")
                for (item in listBuffer) {
                    val cls = item.listClass?.let { " class=\"$it\"" } ?: ""
                    sb.append("<li$cls>${item.html}</li>")
                }
                sb.append("</$tag>")
                listBuffer = mutableListOf()
                listType = null
            }

            for (block in blocks) {
                val rawListType = block.attrs?.get("list") as? String
                val currentListType = when (rawListType) {
                    "ordered", "bullet" -> rawListType
                    "checked", "unchecked" -> "checklist"
                    else -> null
                }

                if (currentListType != null) {
                    if (listType != null && listType != currentListType) {
                        flushList()
                    }
                    listType = currentListType
                    val cls = when (rawListType) {
                        "checked" -> "task-checked"
                        "unchecked" -> "task-unchecked"
                        else -> null
                    }
                    listBuffer.add(ListItem(block.html, currentListType, cls))
                } else {
                    flushList()
                    sb.append(wrapBlockForExport(block.html, block.attrs))
                }
            }
            flushList()

            sb.toString()
        } catch (e: Exception) {
            escapeHtml(content)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun wrapInlineForExport(text: String, attrs: Map<String, Any>?): String {
        if (attrs.isNullOrEmpty()) return escapeHtml(text)
        var html = escapeHtml(text)
        // Order matches viewer.html
        if (attrs["code"] == true) html = "<code>$html</code>"
        val link = attrs["link"] as? String
        if (link != null) html = "<a href='${escapeHtml(link)}'>$html</a>"
        if (attrs["bold"] == true) html = "<strong>$html</strong>"
        if (attrs["italic"] == true) html = "<em>$html</em>"
        if (attrs["underline"] == true) html = "<u>$html</u>"
        if (attrs["strike"] == true) html = "<s>$html</s>"
        val script = attrs["script"] as? String
        if (script == "super") html = "<sup>$html</sup>"
        else if (script == "sub") html = "<sub>$html</sub>"
        val color = attrs["color"] as? String
        if (color != null) html = "<span style='color:$color'>$html</span>"
        val bg = attrs["background"] as? String
        if (bg != null) html = "<span style='background:$bg'>$html</span>"
        return html
    }

    @Suppress("UNCHECKED_CAST")
    private fun wrapBlockForExport(innerHtml: String, attrs: Map<String, Any>?): String {
        if (attrs.isNullOrEmpty()) return "<p>$innerHtml</p>"

        if (attrs["divider"] == true) return "<hr />"

        var tag = "p"
        var content = innerHtml
        var extra = ""

        val header = (attrs["header"] as? Double)?.toInt()
        if (header != null) {
            tag = "h${header.coerceIn(1, 3)}"
        } else if (attrs["code-block"] == true) {
            tag = "pre"
            content = "<code>$content</code>"
        } else if (attrs["blockquote"] == true) {
            tag = "blockquote"
        }

        val align = attrs["align"] as? String
        if (align != null) {
            extra += " class=\"align-$align\""
        }
        val indent = (attrs["indent"] as? Double)?.toInt()
        if (indent != null && indent > 0) {
            extra += " style=\"padding-left:${indent * 24}px\""
        }

        return "<$tag$extra>$content</$tag>"
    }

    /**
     * Embed local images as base64 data URIs in HTML.
     */
    private fun embedLocalImages(context: Context, html: String): String {
        val imgRegex = Regex("""src=['"](https://appassets/[^'"]+)['"]""")
        return imgRegex.replace(html) { match ->
            val webViewUrl = match.groupValues[1]
            val localPath = webViewUrl
                .removePrefix("https://appassets/")
                .let { "${context.filesDir.absolutePath}/$it" }
            try {
                val file = java.io.File(localPath)
                if (file.exists() && file.length() < 5 * 1024 * 1024) {
                    val bytes = file.readBytes()
                    val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    val mime = when {
                        localPath.endsWith(".png") -> "image/png"
                        localPath.endsWith(".webp") -> "image/webp"
                        else -> "image/jpeg"
                    }
                    "src='data:$mime;base64,$base64'"
                } else {
                    match.value
                }
            } catch (e: Exception) {
                match.value
            }
        }
    }

    private class DeltaForExport(val ops: List<OpForExport>?)
    private class OpForExport(val insert: Any?, val attributes: Map<String, Any>?)

    fun exportSingleAsMarkdown(context: Context, entry: DiaryEntry, tags: List<String> = emptyList()): String {
        val entryDate = Instant.ofEpochMilli(entry.createdAt)
            .atZone(ZoneId.systemDefault()).toLocalDateTime()
        val dateText = entryDate.format(DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm"))
        val dayOfWeek = entryDate.format(DateTimeFormatter.ofPattern("EEEE", java.util.Locale.CHINESE))

        val sb = StringBuilder()
        sb.appendLine("# $dateText")
        sb.appendLine()
        sb.appendLine("$dayOfWeek")

        val metaParts = mutableListOf<String>()
        entry.moodLevel?.let { metaParts.add("心情: ${moodLabelForLevel(it)}") }
        entry.weather?.let { metaParts.add("天气: ${weatherLabelFor(it)}") }
        if (tags.isNotEmpty()) {
            metaParts.add("标签: ${tags.joinToString(", ")}")
        }
        if (metaParts.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine(metaParts.joinToString(" | "))
        }

        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()
        if (entry.plainText.isNotBlank()) {
            sb.appendLine(entry.plainText)
        }

        val markdown = sb.toString()
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val fileName = "diary_${timestamp}.md"

        return saveToFile(context, fileName, markdown, "text/markdown")
    }

    private fun wordWrap(text: String, paint: Paint, maxWidth: Int): List<String> {
        val lines = mutableListOf<String>()
        for (paragraph in text.split("\n")) {
            if (paragraph.isBlank()) {
                lines.add("")
                continue
            }
            var remaining = paragraph
            while (remaining.isNotEmpty()) {
                val breakIndex = paint.breakText(remaining, true, maxWidth.toFloat(), null)
                if (breakIndex >= remaining.length) {
                    lines.add(remaining)
                    break
                }
                lines.add(remaining.substring(0, breakIndex))
                remaining = remaining.substring(breakIndex)
            }
        }
        return lines
    }

    private fun saveBitmapToFile(context: Context, bitmap: Bitmap, fileName: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "image/png")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/DiaryApp")
            }
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: throw Exception("无法创建图片文件")

            context.contentResolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            } ?: throw Exception("无法写入图片文件")

            return "Pictures/DiaryApp/$fileName"
        } else {
            @Suppress("DEPRECATION")
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "DiaryApp"
            )
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName)
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            return "Pictures/DiaryApp/$fileName"
        }
    }
}
