package com.diary.app.ui.viewer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ViewerAssetSourceTest {

    @Test
    fun `continuous preview lines do not add fake paragraph spacing`() {
        val html = File("src/main/assets/viewer.html").readText()

        assertTrue(html.contains("#content p { margin: 0; }"))
        assertTrue(html.contains("#content p:empty::before"))
        assertFalse(html.contains("#content p { margin-bottom: 16px; }"))
    }
}
