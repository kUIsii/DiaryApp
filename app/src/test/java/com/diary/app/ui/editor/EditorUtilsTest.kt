package com.diary.app.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class EditorUtilsTest {

    @Test
    fun `summarize selected names returns empty label when no valid names`() {
        val summary = summarizeSelectedNames(
            names = listOf("", "   "),
            emptyLabel = "标签"
        )

        assertEquals("标签", summary)
    }

    @Test
    fun `summarize selected names limits visible names and appends hidden count`() {
        val summary = summarizeSelectedNames(
            names = listOf("工作", "阅读", "散步"),
            emptyLabel = "标签"
        )

        assertEquals("工作 · 阅读 +1", summary)
    }

    @Test
    fun `editor mode toggle label reflects current mode`() {
        assertEquals("显示编辑器", editorModeToggleLabel(isFullEditorVisible = false))
        assertEquals("专注书写", editorModeToggleLabel(isFullEditorVisible = true))
    }

    @Test
    fun `editor bottom gap preset matches mode and keyboard state`() {
        assertEquals(
            180,
            resolveEditorBottomGap(
                showToolbar = false,
                isKeyboardVisible = false,
                isFullEditorVisible = false,
                activeCategory = -1
            )
        )
        assertEquals(
            220,
            resolveEditorBottomGap(
                showToolbar = true,
                isKeyboardVisible = true,
                isFullEditorVisible = false,
                activeCategory = -1
            )
        )
        assertEquals(
            300,
            resolveEditorBottomGap(
                showToolbar = true,
                isKeyboardVisible = true,
                isFullEditorVisible = true,
                activeCategory = -1
            )
        )
        assertEquals(
            360,
            resolveEditorBottomGap(
                showToolbar = true,
                isKeyboardVisible = true,
                isFullEditorVisible = true,
                activeCategory = 2
            )
        )
    }

    @Test
    fun `location row label keeps selected value and falls back when blank`() {
        assertEquals("上海 · 徐汇", resolveCenteredLocationLabel("上海 · 徐汇"))
        assertEquals("位置", resolveCenteredLocationLabel(null))
        assertEquals("位置", resolveCenteredLocationLabel("   "))
    }

    @Test
    fun `normalize editor color converts rgb values to hex`() {
        val normalized = normalizeEditorColor("rgb(231, 76, 60)")

        assertEquals("#e74c3c", normalized)
    }

    @Test
    fun `normalize editor color handles uppercase hex and alpha channels`() {
        val normalized = normalizeEditorColor("RGBA(74, 144, 217, 0.9)")

        assertEquals("#4a90d9", normalized)
    }

    @Test
    fun `draft keys to clear removes both new and current entry drafts`() {
        val keys = draftKeysToClear(42L)

        assertEquals(setOf("draft_new", "draft_42"), keys)
    }

    @Test
    fun `draft keys to clear without entry only removes new draft`() {
        val keys = draftKeysToClear(null)

        assertEquals(setOf("draft_new"), keys)
    }

    @Test
    fun `should restore draft only for new entries with non blank content`() {
        assertEquals(true, shouldRestoreDraft(diaryId = null, plainText = "hello"))
        assertEquals(false, shouldRestoreDraft(diaryId = 7L, plainText = "hello"))
        assertEquals(false, shouldRestoreDraft(diaryId = null, plainText = "   "))
    }

    @Test
    fun `normalize editor color returns null for transparent like values`() {
        assertEquals(null, normalizeEditorColor("transparent"))
        assertEquals(null, normalizeEditorColor("false"))
    }
}
