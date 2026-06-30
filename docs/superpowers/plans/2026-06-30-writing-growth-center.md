# 写作成长中心 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把写作相关工具重构为一个统一的写作成长中心，让灵感、训练、教练、小确幸形成可持续的闭环。

**Architecture:** 保持现有的 Compose + ViewModel + Room + SharedPreferences 架构，不引入新后端。新增一个写作成长中心入口页作为聚合层，现有 `写作工坊`、`写作灵感`、`写作教练`、`小确幸` 继续作为能力页存在，但补上跨页联动、统一状态表达和更清晰的空/完成态。

**Tech Stack:** Kotlin, Jetpack Compose, ViewModel, Room, SharedPreferences, Gson, existing AI service.

---

### Task 1: 建立写作成长中心入口

**Files:**
- Create: `app/src/main/java/com/diary/app/ui/writingcenter/WritingGrowthCenterScreen.kt`
- Create: `app/src/main/java/com/diary/app/ui/writingcenter/WritingGrowthCenterViewModel.kt`
- Modify: `app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt`
- Modify: `app/src/main/java/com/diary/app/ui/tools/ToolsScreen.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// No dedicated test harness exists for this screen yet.
// Verify manually after implementation by navigating from ToolsScreen to the new route.
```

- [ ] **Step 2: Implement the screen shell**

```kotlin
@Composable
fun WritingGrowthCenterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWritingLab: () -> Unit,
    onNavigateToWritingHint: () -> Unit,
    onNavigateToWritingCoach: () -> Unit,
    onNavigateToSmallWins: () -> Unit
) { /* 聚合入口：今日起点、成长概览、快捷入口、最近沉淀 */ }
```

- [ ] **Step 3: Wire the new route**

```kotlin
composable(Screen.WritingGrowthCenter.route) {
    WritingGrowthCenterScreen(
        onNavigateBack = { navController.popBackStack() },
        onNavigateToWritingLab = { navController.navigate(Screen.WritingLab.route) },
        onNavigateToWritingHint = { navController.navigate(Screen.WritingHint.route) },
        onNavigateToWritingCoach = { navController.navigate(Screen.WritingCoach.route) },
        onNavigateToSmallWins = { navController.navigate(Screen.SmallWins.route) }
    )
}
```

- [ ] **Step 4: Add the tools entry**

```kotlin
ToolItem(
    Icons.Default.AutoStories,
    "写作成长中心",
    "灵感·训练·教练·小确幸",
    onNavigateToWritingGrowthCenter
)
```

- [ ] **Step 5: Manually verify navigation**

Run the app and confirm the new tool entry opens the new聚合页, then all four subpages are reachable from it.

### Task 2: Refactor `写作工坊` into a clearer training hub

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/writinglab/WritingLabScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/writinglab/WritingLabViewModel.kt`

- [ ] **Step 1: Add a clear top summary and task framing**

```kotlin
// Add a small summary block above the tab row:
// - today focus
// - current experiment
// - last completed challenge
// - CTA to continue training
```

- [ ] **Step 2: Give each tab a stronger completion loop**

```kotlin
// Experiments: start -> log -> complete -> archive summary
// Style transfer: input -> rewrite -> rating -> history
// Challenge: generate -> complete/skip -> streak update
// Rhetorical: analyze -> expand -> apply/dismiss
// Templates: generate -> preview -> reuse
```

- [ ] **Step 3: Add local fallback content for every AI-backed section**

```kotlin
// When AI is unavailable or parsing fails, preserve useful local output.
// Do not leave an empty shell.
```

- [ ] **Step 4: Verify the training hub remains usable without AI**

Check that every tab still shows a useful action, explanation, and result state when AI is off.

### Task 3: Turn `写作灵感` into a reusable素材库

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/writinghint/WritingHintScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/writinghint/WritingHintViewModel.kt`

- [ ] **Step 1: Add a center-level summary strip**

```kotlin
// Show: total generated, favorite count, recently used, custom count
```

- [ ] **Step 2: Strengthen the hint lifecycle**

```kotlin
// Hint states: new -> favorite -> used -> expanded -> custom saved
// Keep history and favorites visible as separate but related pools
```

- [ ] **Step 3: Expand the refine dialog into actionable guidance**

```kotlin
// Output should include:
// - 2-3 concrete questions
// - 2-3 angles
// - 1-2 lines of "how to start writing"
```

- [ ] **Step 4: Make custom hints first-class**

```kotlin
// Allow user-created hints to live alongside AI-generated and history-derived hints.
```

- [ ] **Step 5: Verify AI-off fallback remains strong**

Check that local hints still span the intended categories and are useful on first launch.

### Task 4: Upgrade `写作教练` into a growth dashboard

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/writingcoach/WritingCoachScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/writingcoach/WritingCoachViewModel.kt`

- [ ] **Step 1: Surface the key growth metrics first**

```kotlin
// Show at top:
// - writing frequency
// - current streak or goal progress
// - word count trend
// - best writing time
```

- [ ] **Step 2: Tie goals to the analytics output**

```kotlin
// Current goals should not sit as isolated settings.
// Show progress toward daily words and weekly writing days on the dashboard.
```

- [ ] **Step 3: Turn AI output into concrete next steps**

```kotlin
// Add "next action" cards:
// - write more sensory details
// - shorten sentence length
// - reduce repeated words
// - keep a consistent writing window
```

- [ ] **Step 4: Keep cached AI analysis and local analysis in sync**

```kotlin
// Cache stays, but it must never override the latest local stats.
```

- [ ] **Step 5: Verify all states**

Check loading, no-data, AI-on, and AI-off states each show a meaningful dashboard.

### Task 5: Make `小确幸` a source of writing material

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/smallwins/SmallWinsScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/smallwins/SmallWinsViewModel.kt`

- [ ] **Step 1: Add a cross-link from small wins to writing**

```kotlin
// Include a CTA to send today’s small wins into writing inspiration / growth review.
```

- [ ] **Step 2: Keep summary and sharing but improve usefulness**

```kotlin
// The share text should highlight not only counts, but also themes and streak context.
```

- [ ] **Step 3: Make AI summary feed back into the growth center**

```kotlin
// The summary should be reusable by the center-level overview, not just a one-off paragraph.
```

- [ ] **Step 4: Verify the page still works as a standalone recorder**

Confirm add/edit/delete, history, stats, and AI summary still work after the cross-link changes.

### Task 6: Route the data flow across the writing suite

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt`
- Modify: `app/src/main/java/com/diary/app/ui/tools/ToolsScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/writinghint/WritingHintViewModel.kt`
- Modify: `app/src/main/java/com/diary/app/ui/writingcoach/WritingCoachViewModel.kt`

- [ ] **Step 1: Add shared navigation hooks**

```kotlin
// New hooks should let the growth center jump into the existing four subpages.
```

- [ ] **Step 2: Normalize wording across the tools page**

```kotlin
// Update the labels and subtitles to reflect the writing growth center vocabulary.
```

- [ ] **Step 3: Keep all AI fallbacks local**

```kotlin
// Any AI-driven feature must still provide a local result if AI is unavailable.
```

- [ ] **Step 4: Manual navigation sweep**

Walk through Tools -> Growth Center -> each child page -> back navigation.

### Task 7: Verification and release note

**Files:**
- Modify: `release-notes-v2.77.00-experimental.md`

- [ ] **Step 1: Run build and target tests**

```powershell
./gradlew :app:compileExperimentalDebugKotlin
./gradlew testExperimentalDebugUnitTest
```

- [ ] **Step 2: Fix any compile or test failures**

```kotlin
// Repair imports, route arguments, state flows, or broken previews as needed.
```

- [ ] **Step 3: Write the release note**

```markdown
## 第二大类：写作成长中心

- 说明统一入口和四个子页的重构内容
- 说明 AI 兜底和本地闭环
- 说明完成后用户能获得什么
```

- [ ] **Step 4: Commit and push**

```bash
git add docs/superpowers/plans/2026-06-30-writing-growth-center.md release-notes-v2.77.00-experimental.md ...
git commit -m "v2.77.00-experimental: writing growth center redesign"
git push
```

## Self-Review

- Spec coverage: the plan covers the growth center entry, writing lab, writing hints, writing coach, small wins, routing, verification, and release note.
- Placeholder scan: no TODO/TBD markers remain in the plan.
- Type consistency: route names and screen names match the current codebase vocabulary.

