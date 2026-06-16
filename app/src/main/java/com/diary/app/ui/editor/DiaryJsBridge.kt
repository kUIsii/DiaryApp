package com.diary.app.ui.editor

import android.webkit.JavascriptInterface
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class DiaryJsBridge {
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _contentChanges = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val contentChanges: SharedFlow<String> = _contentChanges.asSharedFlow()

    @JavascriptInterface
    fun onContentChange(text: String) {
        _contentChanges.tryEmit(text)
    }

    private val _formatChanges = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val formatChanges: SharedFlow<String> = _formatChanges.asSharedFlow()

    @JavascriptInterface
    fun onFormatChange(formatJson: String) {
        _formatChanges.tryEmit(formatJson)
    }

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

    private val _linkInsertRequest = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val linkInsertRequest: SharedFlow<Unit> = _linkInsertRequest.asSharedFlow()

    @JavascriptInterface
    fun requestInsertLink() {
        _linkInsertRequest.tryEmit(Unit)
    }

    // Selected text for AI assistant
    private val _selectedText = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val selectedText: SharedFlow<String> = _selectedText.asSharedFlow()

    @JavascriptInterface
    fun onSelectedText(text: String) {
        _selectedText.tryEmit(text)
    }
}
