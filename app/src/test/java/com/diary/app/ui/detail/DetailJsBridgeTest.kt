package com.diary.app.ui.detail

import org.junit.Assert.assertEquals
import org.junit.Test

class DetailJsBridgeTest {

    @Test
    fun `image click payload keeps clicked url and all urls`() {
        val bridge = DetailJsBridge()
        val event = bridge.parseImageClickEvent(
            "https://appassets/diary_media/img_2.jpg",
            """["https://appassets/diary_media/img_1.jpg","https://appassets/diary_media/img_2.jpg"]"""
        )

        assertEquals("https://appassets/diary_media/img_2.jpg", event.clickedUrl)
        assertEquals(
            listOf(
                "https://appassets/diary_media/img_1.jpg",
                "https://appassets/diary_media/img_2.jpg"
            ),
            event.allUrls
        )
    }

    @Test
    fun `invalid payload falls back to clicked url only`() {
        val bridge = DetailJsBridge()
        val event = bridge.parseImageClickEvent("https://appassets/diary_media/img_2.jpg", "not-json")

        assertEquals(listOf("https://appassets/diary_media/img_2.jpg"), event.allUrls)
    }
}
