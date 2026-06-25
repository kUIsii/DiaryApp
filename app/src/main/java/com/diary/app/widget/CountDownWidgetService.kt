package com.diary.app.widget

import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.diary.app.R
import com.diary.app.data.CountDownItem
import com.diary.app.data.DiaryDatabase
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class CountDownWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return CountDownRemoteViewsFactory(applicationContext)
    }
}

class CountDownRemoteViewsFactory(
    private val context: android.content.Context
) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<CountDownItem> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() = runBlocking {
        try {
            val db = DiaryDatabase.getDatabase(context)
            items = db.countDownDao().getTopCountDownItems(10)
        } catch (e: Exception) {
            items = emptyList()
        }
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getItemId(position: Int): Long = items[position].id

    override fun hasStableIds(): Boolean = true

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getViewAt(position: Int): RemoteViews {
        val item = items[position]
        val views = RemoteViews(context.packageName, R.layout.widget_countdown_item)

        // Set title
        views.setTextViewText(R.id.tv_title, item.title)

        // Calculate days remaining
        val targetDate = Instant.ofEpochMilli(item.targetDate)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val today = LocalDate.now()
        val isPast = targetDate.isBefore(today)
        val days = ChronoUnit.DAYS.between(today, targetDate)

        val daysText = when {
            days == 0L -> "今天"
            isPast -> "已过 ${-days} 天"
            else -> "还有 $days 天"
        }
        views.setTextViewText(R.id.tv_days, daysText)

        // Set target date
        views.setTextViewText(
            R.id.tv_date,
            targetDate.format(DateTimeFormatter.ofPattern("M月d日"))
        )

        // Set color tint for days text (Long to Int ARGB)
        views.setTextColor(R.id.tv_days, item.color.toInt())

        return views
    }
}
