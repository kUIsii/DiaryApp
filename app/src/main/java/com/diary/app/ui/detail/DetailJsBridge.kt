package com.diary.app.ui.detail

import android.webkit.JavascriptInterface
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 详情页 WebView JS Bridge。
 * 接收图片点击事件，通知 Compose 层打开图片查看器。
 */
class DetailJsBridge {

    data class ImageClickEvent(
        val clickedUrl: String,
        val allUrls: List<String>
    )

    private val _imageClicks = MutableSharedFlow<ImageClickEvent>(extraBufferCapacity = 1)
    val imageClicks: SharedFlow<ImageClickEvent> = _imageClicks.asSharedFlow()

    @JavascriptInterface
    fun onImageClick(clickedUrl: String, allUrlsJson: String) {
        try {
            val urlsArray = org.json.JSONArray(allUrlsJson)
            val urls = (0 until urlsArray.length()).map { urlsArray.getString(it) }
            _imageClicks.tryEmit(ImageClickEvent(clickedUrl, urls))
        } catch (_: Exception) {
            _imageClicks.tryEmit(ImageClickEvent(clickedUrl, listOf(clickedUrl)))
        }
    }
}
