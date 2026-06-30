package com.diary.app.ui.readingcenter

import android.content.Context
import com.diary.app.data.DiaryPreview
import com.diary.app.ui.components.cleanPreviewText
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReadingSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _session = MutableStateFlow(loadSession())
    val session: StateFlow<ReadingSessionSnapshot> = _session.asStateFlow()

    fun update(transform: (ReadingSessionSnapshot) -> ReadingSessionSnapshot) {
        val updated = transform(_session.value)
        _session.value = updated
        saveSession(updated)
    }

    fun setEntry(preview: DiaryPreview, requestedPage: Int = 0) {
        val safeTitle = preview.title.ifBlank {
            cleanPreviewText(preview.plainText).take(18).ifBlank { "未命名内容" }
        }
        val estimatedPages = estimatePageCount(preview.plainText)
        update { current ->
            current.copy(
                diaryId = preview.id,
                title = safeTitle,
                previewText = cleanPreviewText(preview.plainText).take(96),
                pageIndex = requestedPage.coerceIn(0, estimatedPages - 1),
                totalPages = estimatedPages,
                paragraphIndex = requestedPage.coerceIn(0, estimatedPages - 1),
                lastReadAt = System.currentTimeMillis()
            )
        }
    }

    fun setTheme(themeName: String?) {
        update { current ->
            current.copy(
                themeName = themeName,
                lastReadAt = System.currentTimeMillis()
            )
        }
    }

    fun setFocusActive(active: Boolean) {
        update { current ->
            current.copy(
                hasActiveFocus = active,
                lastReadAt = System.currentTimeMillis()
            )
        }
    }

    fun updatePage(requestedPage: Int, totalPages: Int? = null) {
        update { current ->
            val prepared = if (totalPages != null && totalPages > 0) {
                current.copy(totalPages = totalPages)
            } else {
                current
            }
            updateReadingSessionPage(prepared, requestedPage)
        }
    }

    fun setParagraph(paragraphIndex: Int) {
        update { current ->
            current.copy(
                paragraphIndex = paragraphIndex.coerceAtLeast(0),
                lastReadAt = System.currentTimeMillis()
            )
        }
    }

    fun toggleBookmark(paragraphIndex: Int) {
        update { current ->
            val normalized = paragraphIndex.coerceAtLeast(0)
            val updated = current.bookmarkParagraphs.toMutableList().apply {
                if (contains(normalized)) remove(normalized) else add(normalized)
            }.sorted()
            current.copy(
                bookmarkParagraphs = updated,
                lastReadAt = System.currentTimeMillis()
            )
        }
    }

    private fun loadSession(): ReadingSessionSnapshot {
        val raw = prefs.getString(KEY_SESSION, null) ?: return ReadingSessionSnapshot()
        return runCatching { gson.fromJson(raw, ReadingSessionSnapshot::class.java) }
            .getOrDefault(ReadingSessionSnapshot())
    }

    private fun saveSession(snapshot: ReadingSessionSnapshot) {
        prefs.edit().putString(KEY_SESSION, gson.toJson(snapshot)).apply()
    }

    companion object {
        private const val PREFS_NAME = "reading_center_session"
        private const val KEY_SESSION = "reading_session_snapshot"

        fun estimatePageCount(text: String): Int {
            val cleanedLength = cleanPreviewText(text).length
            return (cleanedLength / 360).coerceAtLeast(1)
        }
    }
}
