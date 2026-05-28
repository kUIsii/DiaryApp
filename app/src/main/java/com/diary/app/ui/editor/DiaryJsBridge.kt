package com.diary.app.ui.editor

import android.webkit.JavascriptInterface
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class DiaryJsBridge {
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events: SharedFlow<String> = _events.asSharedFlow()

    @JavascriptInterface
    fun pickImage() {
        _events.tryEmit("image")
    }

    @JavascriptInterface
    fun pickVideo() {
        _events.tryEmit("video")
    }

    @JavascriptInterface
    fun pickAudio() {
        _events.tryEmit("audio")
    }
}
