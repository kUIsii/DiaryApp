# 数据分析中心 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把工具区中的统计、情绪、写作、时间报告和关联图谱整合为一个统一的数据分析中心，并把首页做成真正可用的总览与洞察入口。

**Architecture:** 保持现有 Kotlin + Jetpack Compose + ViewModel + Room 架构，不新增后端。把 `StatsScreen` 从“重型单页仪表盘”拆成分析中心首页 + 共享分析逻辑 + 深挖入口层，复用现有的统计、情绪、写作、季度、月度、年度和图谱页面，但重新组织入口与职责。共享的分析数据计算和洞察文案集中到 `ui/stats` 下的新逻辑文件，避免页面之间重复计算和口径不一致。

**Tech Stack:** Kotlin, Jetpack Compose, ViewModel, Room, Flow, Coroutines, existing AI service, existing theme/design tokens.

---

### Task 1: 提炼共享分析逻辑与可测单元

**Files:**
- Create: `app/src/main/java/com/diary/app/ui/stats/AnalysisCenterModels.kt`
- Create: `app/src/main/java/com/diary/app/ui/stats/AnalysisCenterLogic.kt`
- Modify: `app/src/main/java/com/diary/app/ui/stats/StatsViewModel.kt`
- Test: `app/src/test/java/com/diary/app/ui/stats/AnalysisCenterLogicTest.kt`
- Test: `app/src/test/java/com/diary/app/ui/stats/StatsHeatmapDataTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun `analysis center summary prefers the strongest available insight`() {
    val summary = buildAnalysisCenterSummary(
        totalEntries = 42,
        currentStreak = 9,
        thisMonthEntries = 11,
        moodTrend = AnalysisMoodTrend(recent30Avg = 4.2, previous30Avg = 3.4, direction = TrendDirection.UP),
        writingHabit = AnalysisWritingHabit(avgPerWeek = 3.5, mostActiveDay = "周三", mostActiveTime = "晚上", avgWritingMinutes = 18),
        moodWeatherInsight = AnalysisMoodWeatherInsight(text = "晴天时心情最好", moodLevel = 4.4f)
    )

    assertEquals("晴天时心情最好", summary.primaryInsight.text)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testExperimentalDebugUnitTest --tests com.diary.app.ui.stats.AnalysisCenterLogicTest`
Expected: FAIL because the new models and builder do not exist yet.

- [ ] **Step 3: Write minimal shared models and summary builder**

```kotlin
data class AnalysisMoodTrend(
    val recent30Avg: Double?,
    val previous30Avg: Double?,
    val direction: TrendDirection,
)

data class AnalysisWritingHabit(
    val avgPerWeek: Double,
    val mostActiveDay: String,
    val mostActiveTime: String,
    val avgWritingMinutes: Int? = null,
)

data class AnalysisMoodWeatherInsight(
    val text: String,
    val moodLevel: Float,
)

data class AnalysisCenterInsight(
    val title: String,
    val text: String,
    val priority: Int,
)

data class AnalysisCenterSummary(
    val primaryInsight: AnalysisCenterInsight,
    val secondaryInsights: List<AnalysisCenterInsight>,
)

fun buildAnalysisCenterSummary(
    totalEntries: Int,
    currentStreak: Int,
    thisMonthEntries: Int,
    moodTrend: AnalysisMoodTrend?,
    writingHabit: AnalysisWritingHabit?,
    moodWeatherInsight: AnalysisMoodWeatherInsight?
): AnalysisCenterSummary {
    val insights = mutableListOf<AnalysisCenterInsight>()
    moodWeatherInsight?.let {
        insights += AnalysisCenterInsight("天气与心情", it.text, 0)
    }
    moodTrend?.let {
        val directionText = when (it.direction) {
            TrendDirection.UP -> "最近 30 天心情在上升"
            TrendDirection.DOWN -> "最近 30 天心情在下降"
            TrendDirection.FLAT -> "最近 30 天心情比较稳定"
        }
        insights += AnalysisCenterInsight("心情趋势", directionText, 1)
    }
    writingHabit?.let {
        insights += AnalysisCenterInsight("写作习惯", "你更常在 ${it.mostActiveTime} 写作，主要活跃在 ${it.mostActiveDay}", 2)
    }

    val fallback = AnalysisCenterInsight("写作状态", "你已经记录了 $totalEntries 篇日记，当前连续 $currentStreak 天", 99)
    val primary = insights.minByOrNull { it.priority } ?: fallback
    return AnalysisCenterSummary(primaryInsight = primary, secondaryInsights = insights.filterNot { it == primary })
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testExperimentalDebugUnitTest --tests com.diary.app.ui.stats.AnalysisCenterLogicTest`
Expected: PASS.

- [ ] **Step 5: Keep existing heatmap regression coverage intact**

Run: `./gradlew testExperimentalDebugUnitTest --tests com.diary.app.ui.stats.StatsHeatmapDataTest`
Expected: PASS and still confirms same-day multiple entries are counted.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/diary/app/ui/stats/AnalysisCenterModels.kt app/src/main/java/com/diary/app/ui/stats/AnalysisCenterLogic.kt app/src/main/java/com/diary/app/ui/stats/StatsViewModel.kt app/src/test/java/com/diary/app/ui/stats/AnalysisCenterLogicTest.kt app/src/test/java/com/diary/app/ui/stats/StatsHeatmapDataTest.kt
git commit -m "feat: add shared analysis center logic"
```

### Task 2: 重做分析中心首页 UI 与入口结构

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/stats/StatsScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/tools/ToolsScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt`
- Test: `app/src/test/java/com/diary/app/ui/stats/AnalysisCenterLogicTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun `analysis center home exposes the correct section order`() {
    val home = buildAnalysisCenterHome(
        summary = buildAnalysisCenterSummary(
            totalEntries = 42,
            currentStreak = 9,
            thisMonthEntries = 11,
            moodTrend = null,
            writingHabit = null,
            moodWeatherInsight = null
        ),
        totalEntries = 42,
        currentStreak = 9,
        thisMonthEntries = 11,
        topInsights = emptyList(),
        deepDiveEntries = listOf("月度报告", "季度回顾", "年度报告")
    )

    assertEquals(listOf("摘要", "关键指标", "洞察", "深挖入口"), home.sectionOrder)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testExperimentalDebugUnitTest --tests com.diary.app.ui.stats.AnalysisCenterLogicTest`
Expected: FAIL because `buildAnalysisCenterHome` does not exist yet.

- [ ] **Step 3: Implement a compact homepage model**

```kotlin
data class AnalysisCenterHome(
    val summary: AnalysisCenterSummary,
    val sectionOrder: List<String>,
    val keyMetrics: List<Pair<String, String>>,
    val deepDiveEntries: List<String>,
)

fun buildAnalysisCenterHome(
    summary: AnalysisCenterSummary,
    totalEntries: Int,
    currentStreak: Int,
    thisMonthEntries: Int,
    topInsights: List<AnalysisCenterInsight>,
    deepDiveEntries: List<String>
): AnalysisCenterHome {
    return AnalysisCenterHome(
        summary = summary,
        sectionOrder = listOf("摘要", "关键指标", "洞察", "深挖入口"),
        keyMetrics = listOf(
            "总记录" to totalEntries.toString(),
            "连续天数" to currentStreak.toString(),
            "本月记录" to thisMonthEntries.toString(),
        ),
        deepDiveEntries = deepDiveEntries
    )
}
```

- [ ] **Step 4: Rebuild `StatsScreen` into a cleaner center page**

```kotlin
StatsScreen(
    onNavigateToDetail = onNavigateToDetail,
    onNavigateToMonthlyReport = onNavigateToMonthlyReport,
    onNavigateToQuarterlyReview = onNavigateToQuarterlyReview,
    onNavigateToPersonalYearbook = onNavigateToPersonalYearbook,
    onNavigateToAnnualReport = onNavigateToAnnualReport,
    viewModel = viewModel()
)
```

```kotlin
LazyColumn {
    item { AnalysisCenterHeader(summary = state.centerSummary) }
    item { KeyMetricsRow(metrics = state.centerHome.keyMetrics) }
    item { InsightCards(insights = state.centerHome.insights) }
    item { DeepDiveEntryGrid(entries = state.centerHome.deepDiveEntries) }
    item { ExistingStatsSections(state = state) }
}
```

- [ ] **Step 5: Update tools entry copy to match the new center定位**

```kotlin
ToolItem(Icons.Default.BarChart, "数据分析中心", "总览·洞察·深挖", onNavigateToStats)
```

- [ ] **Step 6: Wire the stats route explicitly in navigation if needed and keep the bottom bar behavior unchanged**

```kotlin
composable(Screen.Stats.route) {
    StatsScreen(
        onNavigateToDetail = { diaryId -> navController.navigate(Screen.Detail.createRoute(diaryId)) },
        onNavigateToMonthlyReport = { /* existing route */ },
        onNavigateToQuarterlyReview = { navController.navigate(Screen.QuarterlyReview.route) },
        onNavigateToPersonalYearbook = { navController.navigate(Screen.PersonalYearbook.route) },
        onNavigateToAnnualReport = { navController.navigate(Screen.AnnualReport.route) }
    )
}
```

- [ ] **Step 7: Run focused verification**

Run: `./gradlew :app:compileExperimentalDebugKotlin`
Expected: PASS with the new center UI wiring.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/diary/app/ui/stats/StatsScreen.kt app/src/main/java/com/diary/app/ui/tools/ToolsScreen.kt app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt
git commit -m "feat: redesign analysis center home"
```

### Task 3: 整合时间报告与深挖入口逻辑

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/monthlyreport/MonthlyReportScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/quarterlyreview/QuarterlyReviewScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/annualreport/AnnualReportScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/stats/StatsScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt`
- Test: `app/src/test/java/com/diary/app/ui/stats/AnalysisCenterLogicTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun `deep dive groups keep time reports together`() {
    val groups = buildDeepDiveGroups()
    assertEquals(listOf("时间报告", "心情洞察", "文本洞察", "结构关联"), groups.map { it.title })
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testExperimentalDebugUnitTest --tests com.diary.app.ui.stats.AnalysisCenterLogicTest`
Expected: FAIL because `buildDeepDiveGroups` does not exist.

- [ ] **Step 3: Implement grouped deep-dive model**

```kotlin
data class DeepDiveGroup(
    val title: String,
    val entries: List<String>
)

fun buildDeepDiveGroups(): List<DeepDiveGroup> = listOf(
    DeepDiveGroup("时间报告", listOf("月度报告", "季度回顾", "年度报告")),
    DeepDiveGroup("心情洞察", listOf("情绪分析", "情绪雷达", "情绪预报")),
    DeepDiveGroup("文本洞察", listOf("写作分析", "文字显微镜")),
    DeepDiveGroup("结构关联", listOf("条目图谱"))
)
```

- [ ] **Step 4: Make the report pages visually consistent without removing their current depth**

```kotlin
GlassCard(
    cornerRadius = 20.dp,
    innerPadding = 20.dp,
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
) {
    Column {
        PageHeader(title = "月度报告", onNavigateBack = onNavigateBack)
        // keep existing charts and summaries, but align spacing/typography with the center
    }
}
```

- [ ] **Step 5: Ensure time report navigation remains reachable from the center and Tools page**

```kotlin
ToolItem(Icons.Default.CalendarMonth, "月度报告", "最近一个月的变化", onNavigateToMonthlyReport)
ToolItem(Icons.Default.CalendarMonth, "季度回顾", "阶段变化和对比", onNavigateToQuarterlyReview)
ToolItem(Icons.Default.BarChart, "年度报告", "长期趋势和年度故事", onNavigateToAnnualReport)
```

- [ ] **Step 6: Run focused verification**

Run: `./gradlew :app:compileExperimentalDebugKotlin`
Expected: PASS and no broken references to report routes.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/diary/app/ui/monthlyreport/MonthlyReportScreen.kt app/src/main/java/com/diary/app/ui/quarterlyreview/QuarterlyReviewScreen.kt app/src/main/java/com/diary/app/ui/annualreport/AnnualReportScreen.kt app/src/main/java/com/diary/app/ui/stats/StatsScreen.kt app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt
git commit -m "feat: align time reports with analysis center"
```

### Task 4: 强化检测、验收与发布准备

**Files:**
- Modify: `app/src/main/java/com/diary/app/ui/stats/StatsScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/stats/StatsViewModel.kt`
- Modify: `release-notes-v2.78.00-experimental.md`
- Modify: version / release metadata files used by the project
- Test: `app/src/test/java/com/diary/app/ui/stats/AnalysisCenterLogicTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun `analysis center summary falls back cleanly when optional insights are missing`() {
    val summary = buildAnalysisCenterSummary(
        totalEntries = 0,
        currentStreak = 0,
        thisMonthEntries = 0,
        moodTrend = null,
        writingHabit = null,
        moodWeatherInsight = null
    )

    assertEquals("写作状态", summary.primaryInsight.title)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testExperimentalDebugUnitTest --tests com.diary.app.ui.stats.AnalysisCenterLogicTest`
Expected: FAIL before fallback logic is finalized.

- [ ] **Step 3: Finalize fallback logic, empty states, and loading states**

```kotlin
if (state.totalEntries == 0) {
    EmptyState(
        icon = Icons.Default.SelfImprovement,
        title = "还没有统计内容",
        subtitle = "开始记录几篇日记后，这里会出现总览、洞察和深挖入口",
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)
    )
}
```

- [ ] **Step 4: Verify the actual app compiles and tests pass**

Run:

```bash
./gradlew testExperimentalDebugUnitTest --tests com.diary.app.ui.stats.AnalysisCenterLogicTest
./gradlew testExperimentalDebugUnitTest --tests com.diary.app.ui.stats.StatsHeatmapDataTest
./gradlew :app:compileExperimentalDebugKotlin
```

Expected: all PASS / exit 0.

- [ ] **Step 5: Update release notes with the analysis center release summary**

```markdown
- 数据分析中心重构：把统计、情绪、写作、时间报告和关联图谱统一为一条清晰的分析路径。
- 首页从重型仪表盘改为总览 + 洞察 + 深挖入口结构。
- 时间报告与深挖页统一了入口与视觉节奏，减少页面割裂感。
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/diary/app/ui/stats/StatsScreen.kt app/src/main/java/com/diary/app/ui/stats/StatsViewModel.kt release-notes-v2.78.00-experimental.md
git commit -m "feat: harden analysis center and prepare release"
```

---

## Self-Review Checklist

- [ ] Spec coverage: every section in `2026-06-30-data-analysis-center-design.md` maps to at least one task above.
- [ ] Placeholder scan: no `TBD`, `TODO`, or vague test instructions remain.
- [ ] Type consistency: `AnalysisCenterSummary`, `AnalysisCenterInsight`, `AnalysisCenterHome`, and `DeepDiveGroup` names match across all tasks.
- [ ] Route consistency: `Screen.Stats`, `Screen.MonthlyReport`, `Screen.QuarterlyReview`, `Screen.AnnualReport`, `Screen.EmotionRadar`, `Screen.EmotionForecast`, `Screen.WritingFingerprint`, `Screen.TextMicroscope`, and `Screen.EntryGraph` are used consistently.
- [ ] Verification commands are exact and tied to the touched files.

---

Plan complete and saved to `docs/superpowers/plans/2026-06-30-data-analysis-center.md`. Two execution options:

1. Subagent-Driven (recommended) - I dispatch a fresh subagent per task, review between tasks, fast iteration
2. Inline Execution - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
