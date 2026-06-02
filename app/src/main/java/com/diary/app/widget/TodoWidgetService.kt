package com.diary.app.widget

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.diary.app.R
import com.diary.app.data.DiaryDatabase
import com.diary.app.data.TodoItem
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TodoWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TodoRemoteViewsFactory(applicationContext, intent)
    }
}

class TodoRemoteViewsFactory(
    private val context: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private var todos: List<TodoItem> = emptyList()
    private val dateFormatter = DateTimeFormatter.ofPattern("M月d日")

    override fun onCreate() {
        // Initialize
    }

    override fun onDataSetChanged() {
        // Load todos on the background thread
        runBlocking {
            try {
                val db = DiaryDatabase.getDatabase(context)
                todos = db.diaryDao().getTopPendingTodos(20)
            } catch (e: Exception) {
                todos = emptyList()
            }
        }
    }

    override fun onDestroy() {
        todos = emptyList()
    }

    override fun getCount(): Int = todos.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position < 0 || position >= todos.size) {
            return RemoteViews(context.packageName, R.layout.widget_todo_item)
        }

        val todo = todos[position]
        val views = RemoteViews(context.packageName, R.layout.widget_todo_item)

        // Set title
        views.setTextViewText(R.id.tv_title, todo.title)

        // Set checkbox state
        views.setBoolean(R.id.cb_todo, "setChecked", todo.isCompleted)

        // Set title strikethrough if completed
        if (todo.isCompleted) {
            views.setInt(R.id.tv_title, "setPaintFlags", android.graphics.Paint.STRIKE_THRU_TEXT_FLAG)
        } else {
            views.setInt(R.id.tv_title, "setPaintFlags", 0)
        }

        // Set priority indicator
        if (todo.priority > 0) {
            views.setViewVisibility(R.id.view_priority, View.VISIBLE)
            val priorityColor = when (todo.priority) {
                1 -> context.getColor(R.color.widget_priority_medium)
                2 -> context.getColor(R.color.widget_priority_high)
                else -> context.getColor(R.color.widget_priority_low)
            }
            views.setInt(R.id.view_priority, "setBackgroundColor", priorityColor)
        } else {
            views.setViewVisibility(R.id.view_priority, View.GONE)
        }

        // Set category tag
        if (todo.category != TodoItem.CATEGORY_TASK) {
            views.setViewVisibility(R.id.tv_category, View.VISIBLE)
            views.setTextViewText(R.id.tv_category, TodoItem.categoryLabel(todo.category))
        } else {
            views.setViewVisibility(R.id.tv_category, View.GONE)
        }

        // Set due date
        todo.dueDate?.let { due ->
            val dueDate = Instant.ofEpochMilli(due).atZone(ZoneId.systemDefault()).toLocalDate()
            val isOverdue = dueDate.isBefore(LocalDate.now()) && !todo.isCompleted
            views.setViewVisibility(R.id.tv_due_date, View.VISIBLE)
            views.setTextViewText(R.id.tv_due_date, dueDate.format(dateFormatter))
            if (isOverdue) {
                views.setTextColor(R.id.tv_due_date, context.getColor(R.color.widget_priority_high))
            } else {
                views.setTextColor(R.id.tv_due_date, context.getColor(R.color.widget_text_secondary))
            }
        } ?: run {
            views.setViewVisibility(R.id.tv_due_date, View.GONE)
        }

        // Set up fill-in intent for click handling (entire row)
        val fillInIntent = Intent().apply {
            putExtra(TodoWidgetProvider.EXTRA_TODO_ID, todo.id)
        }
        views.setOnClickFillInIntent(R.id.ll_item_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_todo_item)
    }

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        return if (position >= 0 && position < todos.size) {
            todos[position].id
        } else {
            position.toLong()
        }
    }

    override fun hasStableIds(): Boolean = true
}
