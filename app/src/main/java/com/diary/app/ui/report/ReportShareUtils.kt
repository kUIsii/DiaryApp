package com.diary.app.ui.report

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import androidx.core.content.FileProvider
import com.diary.app.data.TagUsage
import com.diary.app.ui.annualreport.AnnualReport
import com.diary.app.ui.monthlyreport.MonthlyReport
import com.diary.app.ui.weeklyreport.WeeklyReport
import java.io.File
import java.io.FileOutputStream

data class ReportShareCard(
    val title: String,
    val subtitle: String,
    val highlight: String,
    val metrics: List<Pair<String, String>>,
    val footer: String
)

fun buildMonthlyReportShareCard(report: MonthlyReport): ReportShareCard {
    val activeDays = report.dailyWordCounts.count { it > 0 }
    val topTags = report.tags.take(3).joinToString(" · ") { "#${it.name}" }.ifBlank { "继续记录你的关键词" }
    val moodText = report.avgMood?.let { "平均心情 ${String.format("%.1f", it)}" } ?: "本月持续留下了记录"
    return ReportShareCard(
        title = "${report.year}年${report.month}月报告",
        subtitle = "这个月一共写了 ${report.totalEntries} 篇日记",
        highlight = "${report.totalWords} 字",
        metrics = listOf(
            "活跃天数" to "$activeDays 天",
            "累计时长" to "${report.totalDurationMinutes} 分钟",
            "高频标签" to topTags
        ),
        footer = moodText
    )
}

fun buildWeeklyReportShareCard(report: WeeklyReport): ReportShareCard {
    val topTags = report.tags.take(3).joinToString(" · ") { "#${it.name}" }.ifBlank { "继续记录这一周的关键词" }
    val moodText = report.avgMood?.let { "平均心情 ${String.format("%.1f", it)}" } ?: "这一周继续留下了记录"
    return ReportShareCard(
        title = "${report.year}年第${report.weekNumber}周周报",
        subtitle = "${report.startDate} - ${report.endDate}",
        highlight = "${report.totalWords} 字",
        metrics = listOf(
            "周记篇数" to "${report.totalEntries} 篇",
            "活跃天数" to "${report.activeDays} 天",
            "高频标签" to topTags
        ),
        footer = if (report.totalDurationMinutes > 0) {
            "$moodText · 累计写作 ${report.totalDurationMinutes} 分钟"
        } else {
            moodText
        }
    )
}

fun buildAnnualReportShareCard(report: AnnualReport): ReportShareCard {
    val topTags = summarizeAnnualTags(report.topTags)
    return ReportShareCard(
        title = "${report.year} 年度报告",
        subtitle = "这一年你写下了 ${report.totalEntries} 篇日记",
        highlight = "${report.totalWords} 字",
        metrics = listOf(
            "最长连续记录" to "${report.longestStreak} 天",
            "高频写作时段" to report.mostActiveTime,
            "高频标签" to topTags
        ),
        footer = "最常在「${report.mostActiveDay}」动笔"
    )
}

fun shareReportImage(
    context: Context,
    card: ReportShareCard?,
    chooserTitle: String,
    emptyMessage: String
): Boolean {
    if (card == null) {
        android.widget.Toast.makeText(context, emptyMessage, android.widget.Toast.LENGTH_SHORT).show()
        return false
    }

    val file = saveShareCardBitmap(context, card)
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    shareImageUri(context, uri, chooserTitle)
    return true
}

private fun shareImageUri(context: Context, uri: Uri, chooserTitle: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

private fun saveShareCardBitmap(context: Context, card: ReportShareCard): File {
    val bitmap = Bitmap.createBitmap(1080, 1440, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f,
            0f,
            1080f,
            1440f,
            intArrayOf(0xFFFFF8F7.toInt(), 0xFFF8E6E3.toInt(), 0xFFFFEFED.toInt()),
            null,
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, 1080f, 1440f, backgroundPaint)

    val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xF2FFFFFF.toInt()
    }
    val panel = RectF(72f, 120f, 1008f, 1320f)
    canvas.drawRoundRect(panel, 42f, 42f, panelPaint)

    val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFC48880.toInt()
    }
    canvas.drawRoundRect(RectF(120f, 180f, 360f, 198f), 12f, 12f, accentPaint)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF422A2A.toInt()
        textSize = 56f
        isFakeBoldText = true
    }
    val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF72585A.toInt()
        textSize = 34f
    }
    val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFC48880.toInt()
        textSize = 92f
        isFakeBoldText = true
    }
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF72585A.toInt()
        textSize = 28f
    }
    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF422A2A.toInt()
        textSize = 38f
        isFakeBoldText = true
    }

    canvas.drawText(card.title, 120f, 280f, titlePaint)
    canvas.drawText(card.subtitle, 120f, 338f, subtitlePaint)
    canvas.drawText(card.highlight, 120f, 470f, highlightPaint)

    var metricTop = 580f
    card.metrics.forEach { (label, value) ->
        val itemRect = RectF(120f, metricTop, 960f, metricTop + 152f)
        val itemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFDF6F5.toInt() }
        canvas.drawRoundRect(itemRect, 28f, 28f, itemPaint)
        canvas.drawText(label, 156f, metricTop + 58f, labelPaint)
        drawMultilineText(canvas, value, 156f, metricTop + 112f, 760f, valuePaint)
        metricTop += 176f
    }

    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF72585A.toInt()
        textSize = 32f
    }
    drawMultilineText(canvas, card.footer, 120f, 1220f, 840f, footerPaint)

    val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFB08AA8.toInt()
        textSize = 26f
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("DiaryApp", 540f, 1288f, brandPaint)

    val file = File(context.cacheDir, "report_share_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { stream ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    }
    return file
}

private fun drawMultilineText(
    canvas: Canvas,
    text: String,
    startX: Float,
    baselineY: Float,
    maxWidth: Float,
    paint: Paint
) {
    val words = text.chunked(1)
    var line = ""
    var currentY = baselineY
    for (word in words) {
        val candidate = line + word
        if (paint.measureText(candidate) > maxWidth && line.isNotEmpty()) {
            canvas.drawText(line, startX, currentY, paint)
            line = word
            currentY += paint.textSize + 14f
        } else {
            line = candidate
        }
    }
    if (line.isNotEmpty()) {
        canvas.drawText(line, startX, currentY, paint)
    }
}

private fun summarizeAnnualTags(tags: List<TagUsage>): String {
    val summary = tags.take(3).joinToString(" · ") { "#${it.name}" }
    return summary.ifBlank { "继续留下属于你的关键词" }
}
