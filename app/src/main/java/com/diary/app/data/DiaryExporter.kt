package com.diary.app.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.diary.app.BuildConfig
import com.google.gson.GsonBuilder
import java.io.File
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
        val entries = dao.getAllEntriesOnce()
        val tags = dao.getAllTagsOnce()
        val allDiaryTags = dao.getAllDiaryTags()

        // Build diaryId -> tag names map
        val tagMap = tags.associateBy { it.id }
        val diaryTagMap = allDiaryTags.groupBy({ it.diaryId }, { tagMap[it.tagId]?.name ?: "" })

        val exportEntries = entries.map { entry ->
            ExportFile(
                title = entry.title,
                content = entry.content,
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
        val entries = dao.getAllEntriesOnce()
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
                val moodLabel = when (level.coerceIn(1, 6)) {
                    1 -> "沮丧"
                    2 -> "低落"
                    3 -> "平静"
                    4 -> "开心"
                    5 -> "愉快"
                    6 -> "兴奋"
                    else -> "平静"
                }
                metaParts.add("**心情:** $moodLabel")
            }
            entry.weather?.let { weather ->
                val weatherLabel = when (weather) {
                    "晴", "晴天" -> "晴天"
                    "多云" -> "多云"
                    "阴", "阴天" -> "阴天"
                    "雨", "雨天" -> "雨天"
                    "雷", "雷暴" -> "雷暴"
                    "风", "大风" -> "大风"
                    else -> weather
                }
                metaParts.add("**天气:** $weatherLabel")
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
}
