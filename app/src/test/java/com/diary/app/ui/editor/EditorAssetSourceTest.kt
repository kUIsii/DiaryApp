package com.diary.app.ui.editor

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class EditorAssetSourceTest {

    @Test
    fun `editor html keeps media insertion contract`() {
        val html = File("src/main/assets/editor.html").readText()

        assertTrue(html.contains("function insertMedia(type, url)"))
        assertTrue(html.contains("data-loading"))
        assertTrue(html.contains("finalizeMediaInsert"))
        assertTrue(html.contains("setContentBase64"))
    }
}
