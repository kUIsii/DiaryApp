package com.diary.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class WebViewAssetHelperTest {

    @Test
    fun `diary media file path becomes appassets url`() {
        val filePath = "/data/user/0/com.diary.app/files/diary_media/img_123.jpg"
        assertEquals("https://appassets/diary_media/img_123.jpg", WebViewAssetHelper.toWebViewUrl(filePath))
    }

    @Test
    fun `file url for diary media becomes appassets url`() {
        val fileUrl = "file:///data/user/0/com.diary.app/files/diary_media/img_123.jpg"
        assertEquals("https://appassets/diary_media/img_123.jpg", WebViewAssetHelper.toWebViewUrlFromFileUrl(fileUrl))
    }

    @Test
    fun `non media path keeps file url fallback`() {
        val filePath = "/sdcard/Pictures/other.jpg"
        assertEquals("file:///sdcard/Pictures/other.jpg", WebViewAssetHelper.toWebViewUrl(filePath))
    }
}
