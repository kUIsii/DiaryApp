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

    suspend fun export(context: Context, dao: DiaryDao): String {
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

        val tags = dao.getAllTagsOnce()
        val allDiaryTags = dao.getAllDiaryTags()

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

    suspend fun exportAsMarkdown(context: Context, dao: DiaryDao): String {
        val entries = mutableListOf<DiaryEntry>()
        var offset = 0
        val batchSize = 50
        while (true) {
            val batch = dao.getEntriesBatch(offset, batchSize)
            if (batch.isEmpty()) break
            entries.addAll(batch)
            offset += batchSize
        }
        val tags = dao.getAllTagsOnce()
        val allDiaryTags = dao.getAllDiaryTags()

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
