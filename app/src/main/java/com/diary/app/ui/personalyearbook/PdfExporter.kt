package com.diary.app.ui.personalyearbook

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

object PdfExporter {

    private val pageWidth = 595
    private val pageHeight = 842
    private val margin = 48f
    private val contentWidth = pageWidth - 2 * margin

    fun export(
        context: Context,
        yearbook: YearbookData,
        onResult: (Boolean, String) -> Unit
    ) {
        try {
            val document = PdfDocument()
            val paint = createBasePaint()
            val titlePaint = createTitlePaint()
            val statValuePaint = createStatValuePaint()
            val statLabelPaint = createStatLabelPaint()
            val headerPaint = createHeaderPaint()
            val bodyPaint = createBodyPaint()

            val fileName = "diary_yearbook_${yearbook.year}.pdf"
            val file = File(context.cacheDir, fileName)

            var pageNum = 1
            page1Cover(document, paint, titlePaint, yearbook)
            pageNum++
            page2Stats(document, paint, statValuePaint, statLabelPaint, headerPaint, bodyPaint, yearbook)
            pageNum++
            page3MonthlyChart(document, paint, headerPaint, bodyPaint, yearbook)
            pageNum++
            page4MoodChart(document, paint, headerPaint, bodyPaint, yearbook)
            pageNum++
            if (yearbook.metaphor.isNotEmpty()) {
                page5Metaphor(document, paint, headerPaint, bodyPaint, yearbook)
                pageNum++
            }
            if (yearbook.arcs.isNotEmpty()) {
                page6NarrativeArcs(document, paint, headerPaint, bodyPaint, yearbook)
                pageNum++
            }
            if (yearbook.topPhotos.isNotEmpty()) {
                page7Photos(document, paint, headerPaint, bodyPaint, yearbook)
                pageNum++
            }

            document.writeTo(file.outputStream())
            document.close()

            onResult(true, file.absolutePath)
        } catch (e: Exception) {
            onResult(false, e.message ?: "导出失败")
        }
    }

    fun getShareUri(context: Context, filePath: String): Uri {
        val file = File(filePath)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    private fun page1Cover(
        document: PdfDocument,
        paint: Paint,
        titlePaint: Paint,
        yearbook: YearbookData
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        document.startPage(pageInfo).apply {
            canvas.drawColor(Color.parseColor("#FFF8F0"))

            titlePaint.textSize = 72f
            titlePaint.textAlign = Paint.Align.CENTER
            canvas.drawText(yearbook.year.toString(), pageWidth / 2f, 280f, titlePaint)

            titlePaint.textSize = 36f
            titlePaint.color = Color.parseColor("#5D4037")
            canvas.drawText("个人年鉴", pageWidth / 2f, 340f, titlePaint)

            if (yearbook.metaphor.isNotEmpty()) {
                paint.textSize = 18f
                paint.textAlign = Paint.Align.CENTER
                paint.color = Color.parseColor("#8D6E63")
                canvas.drawText(yearbook.metaphor, pageWidth / 2f, 400f, paint)
            }

            paint.textSize = 16f
            paint.textAlign = Paint.Align.CENTER
            paint.color = Color.parseColor("#8D6E63")
            val subtitle = "${yearbook.stats.totalEntries} 篇日记 · ${"%,d".format(yearbook.stats.totalWords)} 字"
            canvas.drawText(subtitle, pageWidth / 2f, 440f, paint)

            finishPage(this)
        }
    }

    private fun page2Stats(
        document: PdfDocument,
        paint: Paint,
        statValuePaint: Paint,
        statLabelPaint: Paint,
        headerPaint: Paint,
        bodyPaint: Paint,
        yearbook: YearbookData
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
        document.startPage(pageInfo).apply {
            canvas.drawColor(Color.WHITE)

            headerPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("年度统计概览", margin, 60f, headerPaint)

            paint.color = Color.parseColor("#E0E0E0")
            canvas.drawLine(margin, 72f, pageWidth - margin, 72f, paint)

            val stats = listOf(
                "总日记数" to "${yearbook.stats.totalEntries} 篇",
                "总字数" to "%,d 字".format(yearbook.stats.totalWords),
                "最佳月份" to yearbook.stats.bestMonth,
                "最长连续" to "${yearbook.stats.longestStreak} 天",
                "最常情绪" to moodLabel(yearbook.stats.topMood)
            )

            var y = 110f
            for ((label, value) in stats) {
                statLabelPaint.textAlign = Paint.Align.LEFT
                canvas.drawText(label, margin + 12f, y + 32f, statLabelPaint)
                statValuePaint.textAlign = Paint.Align.RIGHT
                canvas.drawText(value, pageWidth - margin - 12f, y + 32f, statValuePaint)

                paint.color = Color.parseColor("#F5F5F5")
                paint.style = Paint.Style.FILL
                canvas.drawRoundRect(margin, y, pageWidth - margin, y + 52f, 8f, 8f, paint)

                paint.color = Color.parseColor("#E0E0E0")
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawRoundRect(margin, y, pageWidth - margin, y + 52f, 8f, 8f, paint)

                y += 64f
            }

            finishPage(this)
        }
    }

    private fun page3MonthlyChart(
        document: PdfDocument,
        paint: Paint,
        headerPaint: Paint,
        bodyPaint: Paint,
        yearbook: YearbookData
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 3).create()
        document.startPage(pageInfo).apply {
            canvas.drawColor(Color.WHITE)

            headerPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("月度写作分布", margin, 60f, headerPaint)

            paint.color = Color.parseColor("#E0E0E0")
            canvas.drawLine(margin, 72f, pageWidth - margin, 72f, paint)

            val maxVal = max(yearbook.stats.monthlyDistribution.maxOrNull() ?: 1, 1)
            val chartLeft = margin + 30f
            val chartTop = 110f
            val chartBottom = 550f
            val chartHeight = chartBottom - chartTop
            val barCount = 12
            val barSpacing = (contentWidth - 60f) / barCount
            val barWidth = barSpacing * 0.6f
            val monthLabels = listOf("1月","2月","3月","4月","5月","6月","7月","8月","9月","10月","11月","12月")
            val barColors = listOf(
                "#EF9A9A","#FFAB91","#FFCC80","#FFF59D","#C5E1A5","#A5D6A7",
                "#80CBC4","#81D4FA","#90CAF9","#B39DDB","#CE93D8","#F48FB1"
            )

            paint.color = Color.parseColor("#E0E0E0")
            for (i in 0..4) {
                val y = chartBottom - (chartHeight * i / 4f)
                canvas.drawLine(chartLeft, y, pageWidth - margin, y, paint)
                bodyPaint.textAlign = Paint.Align.RIGHT
                bodyPaint.textSize = 10f
                val label = (maxVal * i / 4).toString()
                canvas.drawText(label, chartLeft - 6f, y + 4f, bodyPaint)
            }

            for (i in 0 until 12) {
                val count = yearbook.stats.monthlyDistribution.getOrElse(i) { 0 }
                val barHeight = if (maxVal > 0) (count.toFloat() / maxVal) * chartHeight else 0f
                val left = chartLeft + i * barSpacing + (barSpacing - barWidth) / 2f
                val top = chartBottom - barHeight
                val right = left + barWidth

                paint.color = Color.parseColor(barColors[i])
                paint.style = Paint.Style.FILL
                canvas.drawRoundRect(left, top, right, chartBottom, 4f, 4f, paint)

                bodyPaint.textAlign = Paint.Align.CENTER
                bodyPaint.textSize = 9f
                bodyPaint.color = Color.parseColor("#666666")
                canvas.drawText(monthLabels[i], left + barWidth / 2f, chartBottom + 16f, bodyPaint)

                if (count > 0) {
                    bodyPaint.textSize = 9f
                    bodyPaint.color = Color.parseColor("#333333")
                    canvas.drawText(count.toString(), left + barWidth / 2f, top - 6f, bodyPaint)
                }
            }

            finishPage(this)
        }
    }

    private fun page4MoodChart(
        document: PdfDocument,
        paint: Paint,
        headerPaint: Paint,
        bodyPaint: Paint,
        yearbook: YearbookData
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 4).create()
        document.startPage(pageInfo).apply {
            canvas.drawColor(Color.WHITE)

            headerPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("情绪分布", margin, 60f, headerPaint)

            paint.color = Color.parseColor("#E0E0E0")
            canvas.drawLine(margin, 72f, pageWidth - margin, 72f, paint)

            val moodLabels = mapOf(
                1 to "沮丧", 2 to "低落", 3 to "一般",
                4 to "不错", 5 to "开心", 6 to "兴奋"
            )
            val moodColors = mapOf(
                1 to "#90CAF9", 2 to "#81D4FA", 3 to "#A5D6A7",
                4 to "#FFF59D", 5 to "#FFCC80", 6 to "#EF9A9A"
            )

            val total = yearbook.stats.moodDistribution.values.sum().toFloat()
            var startAngle = -90f
            val cx = pageWidth / 2f
            val cy = 300f
            val radius = 150f

            val sortedMoods = yearbook.stats.moodDistribution.entries.sortedBy { it.key }
            for ((mood, count) in sortedMoods) {
                val sweep = if (total > 0) (count / total) * 360f else 0f
                paint.color = Color.parseColor(moodColors[mood] ?: "#E0E0E0")
                paint.style = Paint.Style.FILL
                canvas.drawArc(cx - radius, cy - radius, cx + radius, cy + radius,
                    startAngle, sweep, true, paint)
                startAngle += sweep
            }

            if (total == 0f) {
                paint.color = Color.parseColor("#E0E0E0")
                canvas.drawCircle(cx, cy, radius, paint)
                bodyPaint.textAlign = Paint.Align.CENTER
                bodyPaint.textSize = 16f
                bodyPaint.color = Color.parseColor("#999999")
                canvas.drawText("暂无数据", cx, cy + 6f, bodyPaint)
            }

            paint.color = Color.WHITE
            paint.style = Paint.Style.FILL
            canvas.drawCircle(cx, cy, radius * 0.5f, paint)

            var legendY = 430f
            for ((mood, count) in sortedMoods) {
                val label = moodLabels[mood] ?: "未知"
                val pct = if (total > 0) (count / total) * 100 else 0f

                paint.color = Color.parseColor(moodColors[mood] ?: "#E0E0E0")
                paint.style = Paint.Style.FILL
                canvas.drawRoundRect(margin + 40f, legendY, margin + 60f, legendY + 14f, 3f, 3f, paint)

                bodyPaint.textAlign = Paint.Align.LEFT
                bodyPaint.textSize = 12f
                bodyPaint.color = Color.parseColor("#333333")
                canvas.drawText(label, margin + 70f, legendY + 12f, bodyPaint)

                bodyPaint.textAlign = Paint.Align.RIGHT
                bodyPaint.color = Color.parseColor("#666666")
                canvas.drawText("${count} 次 (${"%.1f".format(pct)}%)", pageWidth - margin - 40f, legendY + 12f, bodyPaint)

                legendY += 28f
            }

            finishPage(this)
        }
    }

    private fun page5Metaphor(
        document: PdfDocument,
        paint: Paint,
        headerPaint: Paint,
        bodyPaint: Paint,
        yearbook: YearbookData
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 5).create()
        document.startPage(pageInfo).apply {
            canvas.drawColor(Color.parseColor("#FFF8F0"))

            headerPaint.textAlign = Paint.Align.CENTER
            headerPaint.textSize = 28f
            headerPaint.color = Color.parseColor("#5D4037")
            canvas.drawText("年度隐喻", pageWidth / 2f, 120f, headerPaint)

            headerPaint.textSize = 22f
            headerPaint.color = Color.parseColor("#3E2723")
            canvas.drawText(yearbook.metaphor, pageWidth / 2f, 200f, headerPaint)

            if (yearbook.metaphorEvolution.isNotEmpty()) {
                var y = 300f
                paint.color = Color.parseColor("#E0E0E0")
                canvas.drawLine(margin, y - 20f, pageWidth - margin, y - 20f, paint)

                for (phase in yearbook.metaphorEvolution) {
                    bodyPaint.textAlign = Paint.Align.LEFT
                    bodyPaint.textSize = 16f
                    bodyPaint.color = Color.parseColor("#8D6E63")
                    bodyPaint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    canvas.drawText(phase.period, margin + 20f, y + 10f, bodyPaint)

                    bodyPaint.textSize = 14f
                    bodyPaint.color = Color.parseColor("#555555")
                    bodyPaint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                    canvas.drawText(phase.description, margin + 100f, y + 10f, bodyPaint)

                    paint.color = Color.parseColor("#F5F5F5")
                    paint.style = Paint.Style.FILL
                    canvas.drawRoundRect(margin, y - 20f, pageWidth - margin, y + 40f, 8f, 8f, paint)

                    y += 80f
                }
            }

            finishPage(this)
        }
    }

    private fun page6NarrativeArcs(
        document: PdfDocument,
        paint: Paint,
        headerPaint: Paint,
        bodyPaint: Paint,
        yearbook: YearbookData
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 6).create()
        document.startPage(pageInfo).apply {
            canvas.drawColor(Color.WHITE)

            headerPaint.textAlign = Paint.Align.LEFT
            headerPaint.textSize = 24f
            canvas.drawText("叙事脉络", margin, 60f, headerPaint)

            paint.color = Color.parseColor("#E0E0E0")
            canvas.drawLine(margin, 72f, pageWidth - margin, 72f, paint)

            var y = 100f
            for (arc in yearbook.arcs) {
                paint.color = Color.parseColor("#3E2723")
                paint.style = Paint.Style.FILL
                canvas.drawRoundRect(margin, y, pageWidth - margin, y + 80f, 8f, 8f, paint)
                paint.color = Color.parseColor("#8D6E63")
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawRoundRect(margin, y, pageWidth - margin, y + 80f, 8f, 8f, paint)

                bodyPaint.textAlign = Paint.Align.LEFT
                bodyPaint.textSize = 16f
                bodyPaint.color = Color.parseColor("#3E2723")
                bodyPaint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
                canvas.drawText(arc.title, margin + 16f, y + 28f, bodyPaint)

                bodyPaint.textSize = 12f
                bodyPaint.color = Color.parseColor("#666666")
                bodyPaint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                canvas.drawText("${arc.entries.size} 篇 - ${arc.summary}", margin + 16f, y + 52f, bodyPaint)

                y += 92f
                if (y > 750f) break
            }

            finishPage(this)
        }
    }

    private fun page7Photos(
        document: PdfDocument,
        paint: Paint,
        headerPaint: Paint,
        bodyPaint: Paint,
        yearbook: YearbookData
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 7).create()
        document.startPage(pageInfo).apply {
            canvas.drawColor(Color.WHITE)

            headerPaint.textAlign = Paint.Align.LEFT
            headerPaint.textSize = 24f
            canvas.drawText("年度照片精选", margin, 60f, headerPaint)

            paint.color = Color.parseColor("#E0E0E0")
            canvas.drawLine(margin, 72f, pageWidth - margin, 72f, paint)

            bodyPaint.textAlign = Paint.Align.CENTER
            bodyPaint.textSize = 14f
            bodyPaint.color = Color.parseColor("#999999")
            canvas.drawText("共 ${yearbook.topPhotos.size} 张精选照片", pageWidth / 2f, 100f, bodyPaint)

            val cols = 3
            val cellSize = (contentWidth - (cols - 1) * 8f) / cols
            var x = margin
            var y = 120f
            var col = 0

            for (i in 0 until minOf(yearbook.topPhotos.size, 12)) {
                paint.color = Color.parseColor("#F5F5F5")
                paint.style = Paint.Style.FILL
                canvas.drawRoundRect(x, y, x + cellSize, y + cellSize, 4f, 4f, paint)

                bodyPaint.textSize = 10f
                bodyPaint.color = Color.parseColor("#BBBBBB")
                bodyPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("照片 ${i + 1}", x + cellSize / 2f, y + cellSize / 2f + 4f, bodyPaint)

                col++
                if (col >= cols) {
                    col = 0
                    x = margin
                    y += cellSize + 8f
                } else {
                    x += cellSize + 8f
                }
            }

            finishPage(this)
        }
    }

    private fun createBasePaint() = Paint().apply {
        isAntiAlias = true
        isDither = true
        isFilterBitmap = true
    }

    private fun createTitlePaint() = Paint().apply {
        isAntiAlias = true
        color = Color.parseColor("#3E2723")
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }

    private fun createStatValuePaint() = Paint().apply {
        isAntiAlias = true
        textSize = 20f
        color = Color.parseColor("#3E2723")
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }

    private fun createStatLabelPaint() = Paint().apply {
        isAntiAlias = true
        textSize = 14f
        color = Color.parseColor("#8D6E63")
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }

    private fun createHeaderPaint() = Paint().apply {
        isAntiAlias = true
        textSize = 24f
        color = Color.parseColor("#3E2723")
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }

    private fun createBodyPaint() = Paint().apply {
        isAntiAlias = true
        textSize = 12f
        color = Color.parseColor("#555555")
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }

    private fun moodLabel(level: Int?): String = when (level) {
        1 -> "沮丧"
        2 -> "低落"
        3 -> "一般"
        4 -> "不错"
        5 -> "开心"
        6 -> "兴奋"
        else -> "暂无数据"
    }

    private fun finishPage(page: PdfDocument.Page) {
        try {
            page.javaClass.getMethod("finish").invoke(page)
        } catch (_: Exception) {
        }
    }
}
