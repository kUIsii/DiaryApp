# 日记编辑器稳定性改造 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成日记编辑页的稳定性改造，解决滚动跳动、光标错位、图片不显示、标签布局不符合预期的问题，并加入“专注书写 / 完整编辑”模式。

**Architecture:** 保留现有 `Compose + WebView + Quill` 架构。`EditorScreen.kt` 负责页面结构、模式状态、媒体选择和标签布局；`editor.html` 负责正文内部滚动、位置记忆、列表样式和图片插入后的恢复；`EditorToolbar.kt` 负责模式切换与字号常驻。

**Tech Stack:** Kotlin、Jetpack Compose、Android WebView、Quill、JUnit4、Gradle

---

## 文件结构与职责

- `app/src/main/java/com/diary/app/ui/editor/EditorScreen.kt`
  - 编辑页整体状态
  - 键盘/工具栏/模式的集成
  - 标签区布局
  - WebView 与图片选择器集成
- `app/src/main/java/com/diary/app/ui/editor/EditorToolbar.kt`
  - 底部工具栏
  - “专注书写 / 完整编辑”切换
  - 字号调节常驻
- `app/src/main/java/com/diary/app/ui/editor/EditorUtils.kt`
  - 可抽离的模式、留白、标签文案等纯逻辑辅助函数
- `app/src/test/java/com/diary/app/ui/editor/EditorUtilsTest.kt`
  - 纯逻辑测试
- `app/src/main/assets/editor.html`
  - Quill 编辑器滚动控制
  - 光标/选区位置记忆
  - 列表样式修复
  - 图片插入与加载恢复
- `docs/build-notes.md`
  - 如有必要，补充这次验证与构建注意事项

### Task 1: 先补可测试的纯逻辑与回归测试

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/editor/EditorUtils.kt`
- Modify: `app/src/test/java/com/diary/app/ui/editor/EditorUtilsTest.kt`

- [ ] **Step 1: 先写失败测试，覆盖新逻辑入口**

```kotlin
@Test
fun `editor mode toggle label reflects current mode`() {
    assertEquals("显示编辑器", editorModeToggleLabel(isFullEditorVisible = false))
    assertEquals("专注书写", editorModeToggleLabel(isFullEditorVisible = true))
}

@Test
fun `editor bottom gap preset matches mode and keyboard state`() {
    assertEquals(180, resolveEditorBottomGap(showToolbar = false, isKeyboardVisible = false, isFullEditorVisible = false, activeCategory = -1))
    assertEquals(220, resolveEditorBottomGap(showToolbar = true, isKeyboardVisible = true, isFullEditorVisible = false, activeCategory = -1))
    assertEquals(300, resolveEditorBottomGap(showToolbar = true, isKeyboardVisible = true, isFullEditorVisible = true, activeCategory = -1))
    assertEquals(360, resolveEditorBottomGap(showToolbar = true, isKeyboardVisible = true, isFullEditorVisible = true, activeCategory = 2))
}

@Test
fun `location row label keeps selected value and falls back when blank`() {
    assertEquals("上海 · 徐汇", resolveCenteredLocationLabel("上海 · 徐汇"))
    assertEquals("位置", resolveCenteredLocationLabel(null))
    assertEquals("位置", resolveCenteredLocationLabel("   "))
}
```

- [ ] **Step 2: 运行测试，确认它们先失败**

Run: `.\gradlew.bat :app:testExperimentalDebugUnitTest --tests com.diary.app.ui.editor.EditorUtilsTest`

Expected:
- 新增测试失败
- 失败原因是新函数还不存在或返回值不匹配

- [ ] **Step 3: 在 `EditorUtils.kt` 添加最小实现**

```kotlin
internal fun editorModeToggleLabel(isFullEditorVisible: Boolean): String {
    return if (isFullEditorVisible) "专注书写" else "显示编辑器"
}

internal fun resolveEditorBottomGap(
    showToolbar: Boolean,
    isKeyboardVisible: Boolean,
    isFullEditorVisible: Boolean,
    activeCategory: Int
): Int {
    return when {
        showToolbar && isFullEditorVisible && activeCategory >= 0 -> 360
        showToolbar && isKeyboardVisible && isFullEditorVisible -> 300
        showToolbar || isKeyboardVisible -> 220
        else -> 180
    }
}

internal fun resolveCenteredLocationLabel(selectedLocation: String?): String {
    return selectedLocation?.trim().takeUnless { it.isNullOrEmpty() } ?: "位置"
}
```

- [ ] **Step 4: 再跑测试，确认通过**

Run: `.\gradlew.bat :app:testExperimentalDebugUnitTest --tests com.diary.app.ui.editor.EditorUtilsTest`

Expected:
- `BUILD SUCCESSFUL`
- `EditorUtilsTest` 全通过

- [ ] **Step 5: 提交这一小步**

```bash
git add app/src/main/java/com/diary/app/ui/editor/EditorUtils.kt app/src/test/java/com/diary/app/ui/editor/EditorUtilsTest.kt
git commit -m "test: add editor mode and layout helper coverage"
```

### Task 2: 改造工具栏，加入“专注书写 / 完整编辑”模式

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/editor/EditorToolbar.kt`
- Modify: `app/src/main/java/com/diary/app/ui/editor/EditorUtils.kt`
- Test: `app/src/test/java/com/diary/app/ui/editor/EditorUtilsTest.kt`

- [ ] **Step 1: 先补描述模式文案和切换行为的测试（如 Task 1 尚未覆盖则补齐）**

```kotlin
@Test
fun `editor mode toggle label uses chinese copy`() {
    assertEquals("显示编辑器", editorModeToggleLabel(false))
    assertEquals("专注书写", editorModeToggleLabel(true))
}
```

- [ ] **Step 2: 运行测试，确认红灯有效**

Run: `.\gradlew.bat :app:testExperimentalDebugUnitTest --tests com.diary.app.ui.editor.EditorUtilsTest`

Expected:
- 新增断言在实现前失败

- [ ] **Step 3: 对 `EditorToolbar.kt` 做最小模式化改造**

```kotlin
internal fun EditorToolbar(
    showToolbar: Boolean,
    activeCategory: Int,
    isFullEditorVisible: Boolean,
    onEditorModeToggle: () -> Unit,
    onCategoryChange: (Int) -> Unit,
    ...
)
```

```kotlin
CategoryButton(
    icon = if (isFullEditorVisible) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
    label = editorModeToggleLabel(isFullEditorVisible),
    isActive = isFullEditorVisible,
    onClick = onEditorModeToggle,
    textColor = textColor,
    activeColor = activeColor
)
```

```kotlin
if (isFullEditorVisible) {
    AnimatedVisibility(
        visible = activeCategory >= 0,
        ...
    ) { ... }
}
```

- [ ] **Step 4: 做一次编译级验证**

Run: `.\gradlew.bat :app:compileExperimentalDebugKotlin`

Expected:
- `BUILD SUCCESSFUL`
- 不出现 `EditorToolbar` 参数缺失或 Compose 调用错误

- [ ] **Step 5: 提交这一小步**

```bash
git add app/src/main/java/com/diary/app/ui/editor/EditorToolbar.kt app/src/main/java/com/diary/app/ui/editor/EditorUtils.kt app/src/test/java/com/diary/app/ui/editor/EditorUtilsTest.kt
git commit -m "feat: add writing focus editor toolbar mode"
```

### Task 3: 在 `EditorScreen.kt` 接入模式状态并调整标签布局

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/editor/EditorScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/editor/EditorUtils.kt`
- Test: `app/src/test/java/com/diary/app/ui/editor/EditorUtilsTest.kt`

- [ ] **Step 1: 若需要，先补纯逻辑测试覆盖底部留白与位置文案**

```kotlin
@Test
fun `resolve editor bottom gap keeps compact space in writing focus mode`() {
    assertEquals(220, resolveEditorBottomGap(true, true, false, -1))
}

@Test
fun `resolve centered location label falls back to default text`() {
    assertEquals("位置", resolveCenteredLocationLabel(""))
}
```

- [ ] **Step 2: 运行测试确认新增断言有效**

Run: `.\gradlew.bat :app:testExperimentalDebugUnitTest --tests com.diary.app.ui.editor.EditorUtilsTest`

Expected:
- 新增断言在实现前失败，或旧实现值不匹配

- [ ] **Step 3: 在 `EditorScreen.kt` 接入最小状态和布局改造**

```kotlin
var isFullEditorVisible by remember { mutableStateOf(false) }
```

```kotlin
LaunchedEffect(isKeyboardVisible) {
    if (isKeyboardVisible) {
        showToolbar = true
        if (activeCategory >= 0 && !isFullEditorVisible) {
            activeCategory = -1
        }
    } else if (activeCategory < 0) {
        delay(200)
        if (activeCategory < 0) showToolbar = false
    }
}
```

```kotlin
val bottomGap = resolveEditorBottomGap(
    showToolbar = showToolbar,
    isKeyboardVisible = isKeyboardVisible,
    isFullEditorVisible = isFullEditorVisible,
    activeCategory = activeCategory
)
webView?.evaluateJavascript("setEditorBottomGap($bottomGap)", null)
```

```kotlin
Row {
    MetadataChip(label = moodLabel, ...)
    MetadataChip(label = weatherLabel, ...)
    MetadataChip(label = tagLabel, ...)
}

Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Center
) {
    MetadataChip(
        label = resolveCenteredLocationLabel(selectedLocation),
        ...
    )
}
```

```kotlin
EditorToolbar(
    showToolbar = showToolbar,
    activeCategory = activeCategory,
    isFullEditorVisible = isFullEditorVisible,
    onEditorModeToggle = {
        isFullEditorVisible = !isFullEditorVisible
        if (!isFullEditorVisible) activeCategory = -1
    },
    ...
)
```

- [ ] **Step 4: 运行编译验证**

Run: `.\gradlew.bat :app:compileExperimentalDebugKotlin`

Expected:
- `BUILD SUCCESSFUL`
- 不出现 Compose 参数或布局相关编译错误

- [ ] **Step 5: 提交这一小步**

```bash
git add app/src/main/java/com/diary/app/ui/editor/EditorScreen.kt app/src/main/java/com/diary/app/ui/editor/EditorUtils.kt app/src/test/java/com/diary/app/ui/editor/EditorUtilsTest.kt
git commit -m "feat: update editor screen for writing focus layout"
```

### Task 4: 收敛 `editor.html` 滚动逻辑并加入位置记忆

**Files:**
- Modify: `app/src/main/assets/editor.html`

- [ ] **Step 1: 先让问题具备可验证目标**

手工目标：
- 普通输入时不再每次都强制平滑滚动
- 有序列表回车时只在超出安全区才滚动
- 点回编辑器时优先回到刚才光标附近

- [ ] **Step 2: 在 `editor.html` 加最小位置记忆结构**

```javascript
var editorContext = {
    selectionIndex: 0,
    selectionLength: 0,
    scrollTop: 0,
    restoreLockUntil: 0
};
```

```javascript
function saveEditorContext(range) {
    if (!range) return;
    var editor = document.querySelector('.ql-editor');
    editorContext.selectionIndex = range.index;
    editorContext.selectionLength = range.length || 0;
    editorContext.scrollTop = editor ? editor.scrollTop : 0;
}
```

```javascript
function restoreEditorContext() {
    var docLength = Math.max(quill.getLength() - 1, 0);
    var index = Math.min(editorContext.selectionIndex || 0, docLength);
    var length = Math.min(editorContext.selectionLength || 0, Math.max(docLength - index, 0));
    quill.setSelection(index, length, 'silent');
    editorContext.restoreLockUntil = Date.now() + 180;
    scrollToCursor(index, length, true);
}
```

- [ ] **Step 3: 把滚动收敛到一套安全区算法**

```javascript
function scrollToCursor(index, length, forceRestore) {
    setTimeout(function() {
        try {
            if (!forceRestore && Date.now() < editorContext.restoreLockUntil) return;
            var bounds = quill.getBounds(index, length);
            var editor = document.querySelector('.ql-editor');
            if (!editor || !bounds) return;
            var bottomGap = parseFloat(getComputedStyle(document.documentElement).getPropertyValue('--editor-bottom-gap')) || 180;
            var safeTop = editor.scrollTop + 20;
            var safeBottom = editor.scrollTop + editor.clientHeight - Math.max(bottomGap * 0.55, 110);
            var cursorTop = bounds.top;
            var cursorBottom = bounds.top + Math.max(bounds.height, 24);
            if (cursorBottom > safeBottom) {
                editor.scrollTo({ top: Math.max(cursorBottom - editor.clientHeight * 0.6, 0), behavior: 'auto' });
            } else if (cursorTop < safeTop) {
                editor.scrollTo({ top: Math.max(cursorTop - 36, 0), behavior: 'auto' });
            }
        } catch (e) {}
    }, forceRestore ? 0 : 30);
}
```

- [ ] **Step 4: 在选区、输入、聚焦处统一调用记忆逻辑**

```javascript
quill.on('selection-change', function(range) {
    if (!range) return;
    saveEditorContext(range);
    scrollToCursor(range.index, range.length, false);
});

quill.on('text-change', function() {
    var range = quill.getSelection();
    if (range) {
        saveEditorContext(range);
        scrollToCursor(range.index, range.length, false);
    }
});

function focusEditorWithRestore() {
    quill.focus();
    restoreEditorContext();
}
```

- [ ] **Step 5: 提交这一小步**

```bash
git add app/src/main/assets/editor.html
git commit -m "fix: stabilize editor scrolling and context restore"
```

### Task 5: 修复列表样式与图片插入显示

**Files:**
- Modify: `app/src/main/assets/editor.html`
- Modify: `app/src/main/java/com/diary/app/ui/editor/EditorScreen.kt`

- [ ] **Step 1: 先明确手工验证目标**

手工目标：
- 无序列表圆点和复选框与列表正文基线对齐
- 插入图片后立即能看到图片
- 图片后继续输入时不跳动

- [ ] **Step 2: 统一列表缩进和标记位置**

```css
.ql-editor ol,
.ql-editor ul:not([data-checked]),
.ql-editor ul[data-checked="true"],
.ql-editor ul[data-checked="false"] {
    margin: 0 0 8px 0 !important;
    padding-left: 1.5em !important;
}

.ql-editor ul:not([data-checked]) > li::before {
    left: -1.05em !important;
    top: 0.35em !important;
}

.ql-editor ul[data-checked="true"] > li::before,
.ql-editor ul[data-checked="false"] > li::before {
    left: -1.25em !important;
    top: 0.2em !important;
}
```

- [ ] **Step 3: 稳定图片插入与加载完成后的恢复**

```javascript
function insertMedia(type, url) {
    try {
        var r = quill.getSelection(true);
        if (type === 'image') {
            quill.insertEmbed(r.index, 'image', url, 'user');
            quill.insertText(r.index + 1, '\n', 'user');
            quill.setSelection(r.index + 2, 0, 'silent');
            saveEditorContext({ index: r.index + 2, length: 0 });
            setTimeout(function() { restoreEditorContext(); }, 30);
        }
    } catch (e) {
        console.error('insertMedia error:', e);
    }
}
```

```kotlin
webView?.evaluateJavascript("insertMedia('image', '${escapeForJs(fileUrl)}')", null)
```

```kotlin
webView?.evaluateJavascript("focusEditorWithRestore()", null)
```

- [ ] **Step 4: 编译验证**

Run: `.\gradlew.bat :app:compileExperimentalDebugKotlin`

Expected:
- `BUILD SUCCESSFUL`

- [ ] **Step 5: 提交这一小步**

```bash
git add app/src/main/assets/editor.html app/src/main/java/com/diary/app/ui/editor/EditorScreen.kt
git commit -m "fix: repair editor list alignment and image rendering"
```

### Task 6: 总体验证、构建、发版

**Files:**
- Modify: `docs/build-notes.md`（仅在需要补充验证或构建注意事项时）

- [ ] **Step 1: 跑编辑器相关单测**

Run: `.\gradlew.bat :app:testExperimentalDebugUnitTest --tests com.diary.app.ui.editor.EditorUtilsTest`

Expected:
- `BUILD SUCCESSFUL`
- `EditorUtilsTest` 全通过

- [ ] **Step 2: 跑 Kotlin 编译**

Run: `.\gradlew.bat :app:compileExperimentalDebugKotlin`

Expected:
- `BUILD SUCCESSFUL`

- [ ] **Step 3: 构建 experimental release APK**

Run: `.\gradlew.bat :app:assembleExperimentalRelease`

Expected:
- `BUILD SUCCESSFUL`
- 产物位于 `app/build/outputs/apk/experimental/release/app-experimental-release.apk`

- [ ] **Step 4: 检查版本号与 release tag 是否一致**

Run: `git grep -n "2.61.10-experimental\|versionCode = 56" -- app/build.gradle.kts`

Expected:
- 当前 experimental 版本信息与计划发布一致

- [ ] **Step 5: 提交整体验证与实现**

```bash
git status --short
git add app/src/main/java/com/diary/app/ui/editor/EditorScreen.kt app/src/main/java/com/diary/app/ui/editor/EditorToolbar.kt app/src/main/java/com/diary/app/ui/editor/EditorUtils.kt app/src/test/java/com/diary/app/ui/editor/EditorUtilsTest.kt app/src/main/assets/editor.html docs/build-notes.md
git commit -m "feat: stabilize diary editor writing experience"
```

- [ ] **Step 6: 推送并发布最新 experimental release**

```bash
git push origin experiment/v2-redesign
gh release create v2.61.10-experimental app/build/outputs/apk/experimental/release/app-experimental-release.apk --title "v2.61.10-experimental" --notes-file release-notes-v2.61.10-experimental.md --target experiment/v2-redesign
```

Expected:
- 推送成功
- GitHub Release 发布成功
- 用户可以在应用内检测更新

## 自检

- 规格覆盖检查
  - 滚动稳定：Task 4
  - 位置记忆：Task 4
  - 标签布局：Task 3
  - 专注书写 / 完整编辑：Task 2 + Task 3
  - 图片显示：Task 5
  - 构建与 release：Task 6
- 占位符检查
  - 本计划未使用 TBD / TODO / “后续补充”
- 类型与命名检查
  - `editorModeToggleLabel`
  - `resolveEditorBottomGap`
  - `resolveCenteredLocationLabel`
  - `focusEditorWithRestore`
  - 上述名称在各任务中保持一致
