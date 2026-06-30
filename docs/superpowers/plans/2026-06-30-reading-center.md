# 阅读中心 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把沉浸阅读、专注模式、大纲视图和封面主题重构为统一的阅读中心，让阅读、专注、复盘和视觉空间形成真实可用的闭环。

**Architecture:** 保持现有 Compose + ViewModel + Room + SharedPreferences 架构，不引入新后端。新增 `readingcenter` 作为聚合层，并引入统一 `ReadingSession` 逻辑，负责在阅读中心、沉浸阅读、专注模式、阅读复盘和阅读主题之间共享上下文。现有四个页面继续存在，但会被重新接线和收敛职责。

**Tech Stack:** Kotlin, Jetpack Compose, ViewModel, Room, SharedPreferences, Gson, existing AI service.

---

### Task 1: 建立阅读中心入口与统一阅读会话

**Files:**
- Create: `app/src/main/java/com/diary/app/ui/readingcenter/ReadingCenterLogic.kt`
- Create: `app/src/main/java/com/diary/app/ui/readingcenter/ReadingCenterViewModel.kt`
- Create: `app/src/main/java/com/diary/app/ui/readingcenter/ReadingCenterScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt`
- Modify: `app/src/main/java/com/diary/app/ui/tools/ToolsScreen.kt`
- Test: `app/src/test/java/com/diary/app/ui/readingcenter/ReadingCenterLogicTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun `reading center content exposes continue reading quick action first`() {
    val content = buildReadingCenterContent(
        session = ReadingSessionSnapshot(
            diaryId = 12L,
            title = "昨夜的地铁",
            pageIndex = 3,
            totalPages = 8,
            themeName = "暖纸纹",
            lastReadAt = 1_000L,
            hasActiveFocus = false
        ),
        recentEntries = listOf("清晨散步", "书店角落"),
        completedFocusSessions = 2
    )

    assertEquals("继续阅读", content.heroActions.first().label)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testExperimentalDebugUnitTest --tests com.diary.app.ui.readingcenter.ReadingCenterLogicTest`
Expected: FAIL because `ReadingSessionSnapshot` / `buildReadingCenterContent` do not exist yet.

- [ ] **Step 3: Write minimal reading center logic**

```kotlin
data class ReadingSessionSnapshot(
    val diaryId: Long? = null,
    val title: String? = null,
    val pageIndex: Int = 0,
    val totalPages: Int = 0,
    val themeName: String? = null,
    val lastReadAt: Long? = null,
    val hasActiveFocus: Boolean = false
)

data class ReadingCenterHeroAction(
    val label: String,
    val description: String,
    val target: ReadingCenterTarget
)

fun buildReadingCenterContent(
    session: ReadingSessionSnapshot,
    recentEntries: List<String>,
    completedFocusSessions: Int
): ReadingCenterContent {
    val hero = if (session.diaryId != null) {
        listOf(
            ReadingCenterHeroAction("继续阅读", "回到 ${session.title}", ReadingCenterTarget.IMMERSIVE_READER),
            ReadingCenterHeroAction("进入专注", "围绕当前内容开始专注", ReadingCenterTarget.FOCUS_MODE)
        )
    } else {
        listOf(
            ReadingCenterHeroAction("开始阅读", "从最近内容里选一篇开始", ReadingCenterTarget.IMMERSIVE_READER)
        )
    }

    return ReadingCenterContent(heroActions = hero)
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testExperimentalDebugUnitTest --tests com.diary.app.ui.readingcenter.ReadingCenterLogicTest`
Expected: PASS

- [ ] **Step 5: Add the center route and tools entry**

```kotlin
object ReadingCenter : Screen("reading_center", "阅读中心", Icons.Default.LibraryBooks)
```

```kotlin
ToolItem(Icons.Default.MenuBook, "阅读中心", "继续阅读·专注·复盘·主题", onNavigateToReadingCenter)
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/diary/app/ui/readingcenter app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt app/src/main/java/com/diary/app/ui/tools/ToolsScreen.kt app/src/test/java/com/diary/app/ui/readingcenter/ReadingCenterLogicTest.kt
git commit -m "feat: add reading center hub and session model"
```

### Task 2: 重构沉浸阅读为主阅读页

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/immersive/ImmersiveReaderScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/immersive/ImmersiveReaderViewModel.kt`
- Modify: `app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt`
- Test: `app/src/test/java/com/diary/app/ui/readingcenter/ReadingCenterLogicTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun `session update keeps current page within total page bounds`() {
    val updated = updateReadingSessionPage(
        session = ReadingSessionSnapshot(pageIndex = 2, totalPages = 5),
        requestedPage = 8
    )

    assertEquals(4, updated.pageIndex)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testExperimentalDebugUnitTest --tests com.diary.app.ui.readingcenter.ReadingCenterLogicTest`
Expected: FAIL because `updateReadingSessionPage` does not exist.

- [ ] **Step 3: Implement bounded page update and real swipe behavior**

```kotlin
fun updateReadingSessionPage(
    session: ReadingSessionSnapshot,
    requestedPage: Int
): ReadingSessionSnapshot {
    val bounded = requestedPage.coerceIn(0, (session.totalPages - 1).coerceAtLeast(0))
    return session.copy(pageIndex = bounded)
}
```

```kotlin
.pointerInput(currentPage) {
    var dragTotal = 0f
    detectHorizontalDragGestures(
        onDragStart = { dragTotal = 0f },
        onHorizontalDrag = { change, dragAmount ->
            dragTotal += dragAmount
            change.consume()
        },
        onDragEnd = {
            when {
                dragTotal <= -48f -> onSwipeLeft()
                dragTotal >= 48f -> onSwipeRight()
            }
        }
    )
}
```

- [ ] **Step 4: Add reader actions that are actually reachable**

```kotlin
ImmersiveReaderScreen(
    onNavigateBack = { navController.popBackStack() },
    onNavigateToFocusMode = { navController.navigate(Screen.FocusMode.route) },
    onNavigateToOutlineView = { navController.navigate(Screen.OutlineView.createRoute(currentDiaryId)) },
    onNavigateToReadingCenter = { navController.navigate(Screen.ReadingCenter.route) }
)
```

- [ ] **Step 5: Run focused verification**

Run: `./gradlew :app:compileExperimentalDebugKotlin`
Expected: PASS and no unresolved navigation arguments.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/diary/app/ui/immersive/ImmersiveReaderScreen.kt app/src/main/java/com/diary/app/ui/immersive/ImmersiveReaderViewModel.kt app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt app/src/test/java/com/diary/app/ui/readingcenter/ReadingCenterLogicTest.kt
git commit -m "feat: turn immersive reader into session-aware reading page"
```

### Task 3: 让专注模式真正承接阅读上下文

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/focus/FocusModeScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/focus/FocusModeViewModel.kt`
- Modify: `app/src/main/java/com/diary/app/ui/readingcenter/ReadingCenterViewModel.kt`
- Test: `app/src/test/java/com/diary/app/ui/readingcenter/ReadingCenterLogicTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun `focus summary prefers active reading title when session exists`() {
    val summary = buildReadingFocusSummary(
        session = ReadingSessionSnapshot(title = "雨后的公园", pageIndex = 1, totalPages = 4, hasActiveFocus = true),
        selectedDuration = 25
    )

    assertTrue(summary.contains("雨后的公园"))
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testExperimentalDebugUnitTest --tests com.diary.app.ui.readingcenter.ReadingCenterLogicTest`
Expected: FAIL because `buildReadingFocusSummary` does not exist.

- [ ] **Step 3: Implement contextual focus summary**

```kotlin
fun buildReadingFocusSummary(
    session: ReadingSessionSnapshot,
    selectedDuration: Int
): String {
    val title = session.title ?: "当前阅读内容"
    val progress = if (session.totalPages > 0) "${session.pageIndex + 1}/${session.totalPages}" else "未开始"
    return "围绕《$title》专注 ${selectedDuration} 分钟，当前进度 $progress。"
}
```

- [ ] **Step 4: Surface reading context in the focus UI**

```kotlin
GlassCard(modifier = Modifier.fillMaxWidth()) {
    Column {
        Text("当前阅读", fontWeight = FontWeight.Medium)
        Text(focusSummary)
        Text("结束后将返回当前阅读内容")
    }
}
```

- [ ] **Step 5: Verify focus start / pause / stop still work**

Run: `./gradlew testExperimentalDebugUnitTest`
Expected: PASS with no regressions in focus session persistence.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/diary/app/ui/focus/FocusModeScreen.kt app/src/main/java/com/diary/app/ui/focus/FocusModeViewModel.kt app/src/main/java/com/diary/app/ui/readingcenter/ReadingCenterViewModel.kt app/src/test/java/com/diary/app/ui/readingcenter/ReadingCenterLogicTest.kt
git commit -m "feat: connect focus mode to reading context"
```

### Task 4: 把大纲视图收敛为阅读复盘

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/outline/OutlineViewScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/outline/OutlineViewViewModel.kt`
- Modify: `app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt`
- Test: `app/src/test/java/com/diary/app/ui/readingcenter/ReadingCenterLogicTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun `review summary emphasizes structure first for single entry`() {
    val summary = buildReadingReviewSummary(
        totalWords = 1200,
        paragraphCount = 7,
        headingCount = 3
    )

    assertTrue(summary.startsWith("这篇内容共有"))
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testExperimentalDebugUnitTest --tests com.diary.app.ui.readingcenter.ReadingCenterLogicTest`
Expected: FAIL because `buildReadingReviewSummary` does not exist.

- [ ] **Step 3: Parameterize the route so single-entry review is real**

```kotlin
object OutlineView : Screen("outline_view?diaryId={diaryId}", "阅读复盘", Icons.Default.Article) {
    fun createRoute(diaryId: Long? = null): String =
        if (diaryId != null) "outline_view?diaryId=$diaryId" else "outline_view"
}
```

- [ ] **Step 4: Re-balance the UI toward mobile reading review**

```kotlin
// Change the screen to prioritize:
// 1. outline + paragraph jump
// 2. review summary
// 3. tags / emotion / word frequency
// Move time-range and theme-wide analysis behind secondary actions.
```

- [ ] **Step 5: Run targeted compile verification**

Run: `./gradlew :app:compileExperimentalDebugKotlin`
Expected: PASS and `OutlineViewScreen` receives optional `diaryId`.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/diary/app/ui/outline/OutlineViewScreen.kt app/src/main/java/com/diary/app/ui/outline/OutlineViewViewModel.kt app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt app/src/test/java/com/diary/app/ui/readingcenter/ReadingCenterLogicTest.kt
git commit -m "feat: refactor outline view into reading review"
```

### Task 5: 把封面主题升级为阅读主题空间

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/covertheme/CoverThemeScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/covertheme/CoverThemeViewModel.kt`
- Modify: `app/src/main/java/com/diary/app/ui/readingcenter/ReadingCenterViewModel.kt`
- Test: `app/src/test/java/com/diary/app/ui/readingcenter/ReadingCenterLogicTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun `reading theme preview description mentions active theme and reading space`() {
    val description = buildReadingThemePreviewDescription(
        themeName = "暖纸纹",
        isDefault = true
    )

    assertEquals("暖纸纹 · 当前默认阅读空间", description)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testExperimentalDebugUnitTest --tests com.diary.app.ui.readingcenter.ReadingCenterLogicTest`
Expected: FAIL because `buildReadingThemePreviewDescription` does not exist.

- [ ] **Step 3: Replace generic preview with real reading preview**

```kotlin
Box(
    modifier = modifier
        .clip(RoundedCornerShape(DesignTokens.CornerLarge))
        .background(previewTexture)
) {
    Column {
        Text("昨夜的地铁", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = previewAccent)
        Text("这一页预览的是阅读正文，而不是泛化封面。", lineHeight = 24.sp)
    }
}
```

- [ ] **Step 4: Make theme application feed back into reading session**

```kotlin
fun applyThemeToReadingSession(themeName: String) {
    val updated = currentSession.copy(themeName = themeName)
    saveReadingSession(updated)
}
```

- [ ] **Step 5: Verify theme apply is visible**

Run: `./gradlew :app:compileExperimentalDebugKotlin`
Expected: PASS and reading preview uses selected theme data.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/diary/app/ui/covertheme/CoverThemeScreen.kt app/src/main/java/com/diary/app/ui/covertheme/CoverThemeViewModel.kt app/src/main/java/com/diary/app/ui/readingcenter/ReadingCenterViewModel.kt app/src/test/java/com/diary/app/ui/readingcenter/ReadingCenterLogicTest.kt
git commit -m "feat: turn cover theme into reading space theme"
```

### Task 6: 全链路验证与说明更新

**Files:**
- Modify: `release-notes-v2.77.00-experimental.md`

- [ ] **Step 1: Run full build and test suite**

```powershell
./gradlew :app:compileExperimentalDebugKotlin
./gradlew testExperimentalDebugUnitTest
```

- [ ] **Step 2: Manually verify the end-to-end reading flow**

Run these checks in the app:
- Tools -> 阅读中心 -> 继续阅读
- 沉浸阅读中滑动翻页、打开目录、切换主题、进入专注
- 专注模式结束后回到阅读
- 从阅读进入阅读复盘并定位重点段落
- 在阅读主题中应用主题并返回阅读页确认视觉变化

Expected:
- 所有链路都真实可点击、可返回、可恢复
- 没有明显布局挤压、状态丢失或空壳卡片

- [ ] **Step 3: Update release note**

```markdown
## 第三大类：阅读中心

- 新增阅读中心聚合入口
- 沉浸阅读、专注模式、阅读复盘、阅读主题接入统一阅读会话
- 修复翻页、路由参数、状态恢复和主题预览等关键问题
```

- [ ] **Step 4: Commit**

```bash
git add release-notes-v2.77.00-experimental.md
git commit -m "docs: add reading center release notes"
```

## Self-Review

- Spec coverage: the plan covers the reading center hub, unified session model, immersive reader, contextual focus mode, reading review, reading theme space, and full verification.
- Placeholder scan: all tasks have concrete files, commands, and implementation direction; no TODO/TBD markers remain.
- Type consistency: `ReadingSessionSnapshot`, `ReadingCenterContent`, `ReadingCenterTarget`, and route names are used consistently across tasks.
