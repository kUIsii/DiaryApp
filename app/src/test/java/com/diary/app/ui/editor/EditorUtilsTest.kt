package com.diary.app.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

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
    fun `metadata tag summary shows selected names without field prefix`() {
        val summary = metadataTagSummary(listOf("工作", "阅读", "散步"))

        assertEquals("工作 · 阅读…", summary)
    }

    @Test
    fun `metadata location summary keeps single line compact fallback`() {
        assertEquals("位置", metadataLocationSummary(null))
        assertEquals("南京西路咖啡馆", metadataLocationSummary("南京西路咖啡馆"))
    }

    @Test
    fun `toolbar visibility icon reflects current state`() {
        assertEquals("工具栏已显示", toolbarVisibilityDescription(isToolbarVisible = true))
        assertEquals("工具栏已隐藏", toolbarVisibilityDescription(isToolbarVisible = false))
    }

    @Test
    fun `editor bottom gap only reserves a compact cursor safety area`() {
        assertEquals(
            20,
            resolveEditorBottomGap(
                showToolbar = false,
                activeCategory = -1
            )
        )
        assertEquals(
            20,
            resolveEditorBottomGap(
                showToolbar = false,
                activeCategory = -1
            )
        )
        assertEquals(
            36,
            resolveEditorBottomGap(
                showToolbar = true,
                activeCategory = -1
            )
        )
        assertEquals(
            56,
            resolveEditorBottomGap(
                showToolbar = true,
                activeCategory = 2
            )
        )
    }

    @Test
    fun `editor layout reserves ime space whenever keyboard is visible`() {
        assertEquals(
            true,
            shouldApplyImePaddingToEditorLayout(isKeyboardVisible = true)
        )
        assertEquals(
            false,
            shouldApplyImePaddingToEditorLayout(isKeyboardVisible = false)
        )
    }

    @Test
    fun `editor asset uses viewport backed memo paper instead of fake scroll padding`() {
        val html = File("src/main/assets/editor.html").readText()

        assertTrue(html.contains("min-height: 100%"))
        assertFalse(html.contains("60vh"))
        assertFalse(html.contains("repeating-linear-gradient"))
    }

    @Test
    fun `keyboard closing does not auto hide a manually opened toolbar`() {
        assertEquals(
            true,
            shouldAutoHideToolbarOnKeyboardHidden(activeCategory = -1, keepToolbarOpen = false)
        )
        assertEquals(
            false,
            shouldAutoHideToolbarOnKeyboardHidden(activeCategory = -1, keepToolbarOpen = true)
        )
        assertEquals(
            false,
            shouldAutoHideToolbarOnKeyboardHidden(activeCategory = 0, keepToolbarOpen = false)
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
    fun `draft list item key is namespaced consistently`() {
        assertEquals("draft_item_abc123", draftListItemKey("abc123"))
    }

    @Test
    fun `pause autosave is skipped when editor is intentionally exiting`() {
        assertEquals(true, shouldPersistDraftOnPause(hasUnsavedChanges = true, isExitingEditor = false))
        assertEquals(false, shouldPersistDraftOnPause(hasUnsavedChanges = false, isExitingEditor = false))
        assertEquals(false, shouldPersistDraftOnPause(hasUnsavedChanges = true, isExitingEditor = true))
    }

    @Test
    fun `should restore draft when snapshot has any meaningful content`() {
        assertEquals(
            true,
            shouldRestoreDraft(
                EditorSnapshot(
                    plainText = "hello",
                    defaultTitle = "2026年6月9日"
                )
            )
        )
        assertEquals(
            true,
            shouldRestoreDraft(
                EditorSnapshot(
                    moodLevel = 3,
                    defaultTitle = "2026年6月9日"
                )
            )
        )
        assertEquals(
            true,
            shouldRestoreDraft(
                EditorSnapshot(
                    location = "上海 徐汇",
                    defaultTitle = "2026年6月9日"
                )
            )
        )
        assertEquals(false, shouldRestoreDraft(EditorSnapshot()))
    }

    @Test
    fun `normalize editor color returns null for transparent like values`() {
        assertEquals(null, normalizeEditorColor("transparent"))
        assertEquals(null, normalizeEditorColor("false"))
    }

    @Test
    fun `editor content dirty detects title metadata and body changes`() {
        val base = EditorSnapshot(
            title = "title",
            plainText = "body",
            moodLevel = 3,
            weather = "rain",
            tagIds = setOf(1L),
            location = "home"
        )

        assertEquals(false, isEditorDirty(base, base))
        assertEquals(true, isEditorDirty(base, base.copy(title = "new title")))
        assertEquals(true, isEditorDirty(base, base.copy(plainText = "new body")))
        assertEquals(true, isEditorDirty(base, base.copy(moodLevel = 4)))
        assertEquals(true, isEditorDirty(base, base.copy(tagIds = setOf(1L, 2L))))
        assertEquals(true, isEditorDirty(base, base.copy(location = "office")))
    }

    @Test
    fun `editor content dirty ignores trailing whitespace and blank default title`() {
        assertEquals(
            false,
            isEditorDirty(
                EditorSnapshot(title = "2026年6月8日", plainText = "hello\n"),
                EditorSnapshot(title = "", plainText = "hello", defaultTitle = "2026年6月8日")
            )
        )
    }

    @Test
    fun `draft is meaningful when it has text title or metadata`() {
        assertEquals(false, isMeaningfulDraft(EditorSnapshot()))
        assertEquals(true, isMeaningfulDraft(EditorSnapshot(plainText = "hello")))
        assertEquals(true, isMeaningfulDraft(EditorSnapshot(title = "A thought")))
        assertEquals(true, isMeaningfulDraft(EditorSnapshot(moodLevel = 2)))
        assertEquals(true, isMeaningfulDraft(EditorSnapshot(tagIds = setOf(7L))))
    }

    @Test
    fun `editor asset restores a usable cursor on focus`() {
        val html = File("src/main/assets/editor.html").readText()

        assertTrue(html.contains("function ensureSelection("))
        assertTrue(html.contains("function focusEditorAtEnd()"))
    }

    @Test
    fun `editor asset vertically centers unordered list bullets`() {
        val html = File("src/main/assets/editor.html").readText()

        assertTrue(html.contains("top: 50% !important;"))
        assertTrue(html.contains("transform: translateY(-50%) !important;"))
    }

    @Test
    fun `editor asset accepts explicit viewport metrics for cursor docking`() {
        val html = File("src/main/assets/editor.html").readText()

        assertTrue(html.contains("--viewport-height"))
        assertTrue(html.contains("function setViewportMetrics("))
        assertFalse(html.contains("function setBottomObstruction("))
    }

    @Test
    fun `editor screen no longer sends duplicate bottom obstruction to web content`() {
        val source = File("src/main/java/com/diary/app/ui/editor/EditorScreen.kt").readText()

        assertFalse(source.contains("setBottomObstruction("))
    }

    @Test
    fun `webview asset helper uses the same appassets domain as inserted media urls`() {
        val source = File("src/main/java/com/diary/app/ui/components/WebViewAssetHelper.kt").readText()

        assertTrue(source.contains("setDomain(AUTHORITY)"))
        assertTrue(source.contains("https://$"))
        assertTrue(source.contains("AUTHORITY"))
    }

    @Test
    fun `editor asset clear formatting removes inline styles at caret`() {
        val html = File("src/main/assets/editor.html").readText()

        assertTrue(html.contains("quill.format('bold', false);"))
        assertTrue(html.contains("quill.format('italic', false);"))
        assertTrue(html.contains("quill.format('underline', false);"))
        assertTrue(html.contains("quill.format('strike', false);"))
    }

    @Test
    fun `editor asset gives dark mode dividers stronger contrast`() {
        val html = File("src/main/assets/editor.html").readText()

        assertTrue(html.contains(".theme-dark hr {"))
        assertTrue(html.contains("rgba(255,255,255,0.34)"))
    }

    @Test
    fun `editor asset avoids nested internal scrolling in quill`() {
        val html = File("src/main/assets/editor.html").readText()

        assertTrue(html.contains("height: auto !important;"))
        assertTrue(html.contains("overflow-y: visible !important;"))
    }

    @Test
    fun `editor asset scrolls caret into view immediately after selection changes`() {
        val html = File("src/main/assets/editor.html").readText()

        assertTrue(html.contains("function scrollSelectionIntoView(force)"))
        assertTrue(html.contains("requestAnimationFrame(function() {"))
        assertTrue(html.contains("scrollSelectionIntoView(false);"))
        assertTrue(html.contains("function scrollToCurrentCursor(forceRestore) { scrollSelectionIntoView"))
    }

    @Test
    fun `editor asset uses explicit webview viewport height instead of keyboard math`() {
        val html = File("src/main/assets/editor.html").readText()

        assertTrue(html.contains("var explicitViewportHeight = parseFloat(getComputedStyle(document.documentElement).getPropertyValue('--viewport-height')) || 0;"))
        assertTrue(html.contains("var viewportHeight = explicitViewportHeight > 0"))
        assertFalse(html.contains("window.visualViewport ? window.visualViewport.height"))
    }

    @Test
    fun `editor asset rechecks cursor visibility right after enter`() {
        val html = File("src/main/assets/editor.html").readText()

        assertTrue(html.contains("if (e.key === 'Enter')"))
        assertTrue(html.contains("scrollSelectionIntoView(true);"))
    }

    @Test
    fun `editor asset stops listening to viewport scroll events that cause self triggered jumps`() {
        val html = File("src/main/assets/editor.html").readText()

        assertTrue(html.contains("function handleViewportChange()"))
        assertTrue(html.contains("window.visualViewport.addEventListener('resize', handleViewportChange);"))
        assertFalse(html.contains("window.visualViewport.addEventListener('scroll', handleViewportChange);"))
        assertFalse(html.contains("var viewportSyncTimer = null;"))
    }

    @Test
    fun `editor asset does not treat forced visibility checks as unconditional scrolling`() {
        val html = File("src/main/assets/editor.html").readText()

        assertFalse(html.contains("if (force || caretBottom > visibleBottom)"))
        assertFalse(html.contains("var bottomThreshold = visibleBottom - (force ? 6 : 18);"))
        assertTrue(html.contains("if (caretBottom > bottomThreshold)"))
    }

    @Test
    fun `editor asset keeps bottom gap separate from viewport occlusion checks`() {
        val html = File("src/main/assets/editor.html").readText()

        assertTrue(html.contains("var visibleBottom = Math.max(viewportHeight, visibleTop + 2);"))
        assertFalse(html.contains("editorBottomGap"))
    }

    @Test
    fun `editor asset keeps obscured caret tight to keyboard or toolbar`() {
        val html = File("src/main/assets/editor.html").readText()

        assertTrue(html.contains("var bottomThreshold = visibleBottom - 2;"))
        assertFalse(html.contains("+ (force ? 6 : 12)"))
    }

    @Test
    fun `editor asset converts quill bounds into viewport coordinates before scrolling`() {
        val html = File("src/main/assets/editor.html").readText()

        assertTrue(html.contains("var editorRect = quill.root.getBoundingClientRect();"))
        assertTrue(html.contains("var caretTop = editorRect.top + bounds.top;"))
        assertTrue(html.contains("var caretBottom = caretTop + bounds.height;"))
    }

    @Test
    fun `editor asset clamps cursor visibility to the webview viewport before keyboard edge`() {
        val html = File("src/main/assets/editor.html").readText()

        assertTrue(html.contains("var editorVisibleBottom = Math.min(visibleBottom, editorRect.bottom);"))
        assertTrue(html.contains("var editorVisibleTop = Math.max(visibleTop, editorRect.top);"))
        assertTrue(html.contains("var bottomThreshold = editorVisibleBottom - 2;"))
        assertTrue(html.contains("var topThreshold = editorVisibleTop + (force ? 2 : 0);"))
    }

    @Test
    fun `editor image insert uses generic content picker for wider device compatibility`() {
        val source = File("src/main/java/com/diary/app/ui/editor/EditorScreen.kt").readText()

        assertTrue(source.contains("ActivityResultContracts.GetContent()) { uri: Uri? ->"))
        assertTrue(source.contains("imageLauncher.launch(\"image/*\")"))
        assertFalse(source.contains("PickVisualMediaRequest"))
    }

    @Test
    fun `editor media insertion reports success or failure to kotlin`() {
        val html = File("src/main/assets/editor.html").readText()

        assertTrue(html.contains("function insertMedia(type, url)"))
        assertTrue(html.contains("return true;"))
        assertTrue(html.contains("return false;"))
    }

    @Test
    fun `editor queues selected images until webview is ready`() {
        val source = File("src/main/java/com/diary/app/ui/editor/EditorScreen.kt").readText()

        assertTrue(source.contains("pendingImageWebUrl"))
        assertTrue(source.contains("LaunchedEffect(isWebViewReady, pendingImageWebUrl)"))
    }
}
