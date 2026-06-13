# 首页与日历重新设计提案

> 目标分支: `experiment/v2-redesign`
> 基于当前 `HomeScreen.kt`、`CalendarView.kt`、`HomeViewModel.kt`、`TimelineScreen.kt`、`StatsScreen.kt`、`DiaryReviewScreen.kt` 的代码分析

---

## 目录

1. [当前架构概要](#1-当前架构概要)
2. [日历组件重新设计](#2-日历组件重新设计)
3. [日记列表重新设计](#3-日记列表重新设计)
4. [搜索体验](#4-搜索体验)
5. ["历史今天"增强](#5-历史今天增强)
6. [周/月摘要](#6-周月摘要)
7. [快捷操作](#7-快捷操作)
8. [首页布局策略](#8-首页布局策略)
9. [动画与过渡](#9-动画与过渡)
10. [参考应用分析](#10-参考应用分析)
11. [实施路线图](#11-实施路线图)

---

## 1. 当前架构概要

### 现状

**HomeScreen** (`HomeScreen.kt`):
- 页面标题显示"首页"+ "共 X 天有记录"副标题
- 功能菜单按钮（右上角）打开下拉菜单，包含"统计"和"倒数日"
- `CalendarView` 组件（周/月切换）
- 选中日期标题显示"今天"/"昨天"/日期
- 通过 `LazyColumn` 的 `itemsIndexed` 展示条目列表，每项包裹在 `AnimatedVisibility` 中
- 新建条目的 FAB 按钮（右下角）

**CalendarView** (`CalendarView.kt`):
- 两种模式: `CalendarMode.WEEK` 和 `CalendarMode.MONTH`
- 模式切换按钮（"周"/"月"）居中显示在导航下方
- 左右箭头导航切换月/周
- `MonthView`: 7 列网格，日期数字下方有心情色点
- `WeekView`: 7 天的单行显示
- 模式间切换使用 `AnimatedContent` 过渡（上下滑动 + 淡入淡出）
- `CalendarDay`: 圆形显示日期数字 + 心情色点，选中状态有缩放动画

**HomeViewModel** (`HomeViewModel.kt`):
- 通过 `dao.getAllPreviews()` 将所有条目加载到内存
- `selectedDate`、`searchQuery`、`selectedTagFilter`、`searchFilters` 作为 StateFlow
- `filteredEntries`: 将所有条目与日期/查询/标签/心情/天气筛选条件组合
- `selectedEntries`: 当前选中日期的条目
- `onThisDayEntries`: 往年同月同日的条目
- `reviewEntries`: 上周、上月、去年的条目
- `stats`: 总数、连续天数、本月数量
- `weeklySummary`: 条目数量、平均心情、总字数、有记录天数（最近 7 天）
- `moodTrend`: 7 天心情等级
- 搜索历史: 内存中保存最近 5 条查询
- 排序方式: NEWEST、OLDEST、BEST_MOOD、FAVORITES

**数据层**:
- `DiaryEntry` 实体包含 `plainText` 字段（无 FTS）
- 搜索使用 `LIKE '%' || :query || '%'` 在 `plainText` 上查询
- `DiaryPreview` 轻量投影（不含 `content` 字段）用于列表视图
- 数据库版本 13，8 个实体

### 已识别的关键问题

1. **日历占用过多空间** -- 月视图高约 280dp，将条目列表推到很下方
2. **首页没有搜索功能** -- 搜索仅存在于 TimelineScreen
3. **所有条目加载到内存** -- `getAllPreviews()` 加载全部数据，在 Kotlin 中过滤
4. **"历史今天"未在首页展示** -- 只能通过单独的 ReviewScreen 访问
5. **没有滑动操作** -- 长按 TODO 存在但未实现
6. **周摘要未在首页显示** -- `weeklySummary` 和 `moodTrend` 存在于 ViewModel 中但未在 HomeScreen 中渲染
7. **没有下拉刷新** -- 数据通过 Flow 响应式更新，但没有手动刷新手势
8. **日历不可滚动** -- 月/周切换仅通过箭头导航，不支持水平滑动

---

## 2. 日历组件重新设计

### 2.1 问题: 固定的日历布局

当前日历是固定高度的区块。月视图始终显示完整的 5-6 行。周/月模式切换会导致整个页面布局移动。

### 2.2 提议方案: 水平翻页日历

**方案**: 用 `HorizontalPager` 替换箭头导航，同时适用于月视图和周视图。

```
┌─────────────────────────────┐
│  ◀  2026年6月  ▀▀  [周][月] │  <- Header with mode toggle
│  日 一 二 三 四 五 六        │  <- Day of week headers
│  ─────────────────────────  │
│  1  2  3  4  5  6  7        │  <- HorizontalPager content
│  8  9  10 11 12 13 14       │    (swipeable left/right)
│  15 16 17 18 19 20 21       │
│  22 23 24 25 26 27 28       │
│  29 30                      │
│  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬  │  <- Optional: mood heatmap bar
└─────────────────────────────┘
```

**技术细节**:
- 使用 `androidx.compose.foundation.pager` 中的 `HorizontalPager`
- 月模式: 每页 = 一个 `YearMonth`，初始页 = 当前月
- 周模式: 每页 = 一周（周一至周日），初始页 = 当前周
- `PagerState` 的 `initialPage` 设为较大偏移量（如 1200），允许向前/后滚动约 100 年
- 页码键: 月份使用 `yearMonth.toEpochDay()`，周使用 `weekStart.toEpochDay()`
- 周/月模式切换时使用交叉淡入/滑动动画

**心情热力图日历**（GitHub 贡献图风格）:

```
┌──────────────────────────────────────┐
│  6月                                │
│  日 一 二 三 四 五 六                │
│        1  2  3  4  5  6  7          │
│  ░░  ░░  ██  ░░  ▓▓  ░░  ░░        │  ░░ = no entry
│  8  9  10 11 12 13 14               │  ██ = entry exists
│  ░░  ██  ██  ░░  ░░  ▓▓  ░░        │  ▓▓ = entry + good mood
│  ...                                │  Color intensity = mood level
└──────────────────────────────────────┘
```

- 每个日期单元格使用基于心情等级的背景色（不仅仅是色点）
- 色阶: 透明（无条目）-> 浅主色（有条目，无心情）-> 心情渐变（1-6）
- 用更具视觉信息量的单元格替代小色点

**优先级**: 高 | **工作量**: 3-4 天

### 2.3 周/月切换动画

当前: `AnimatedContent` 使用滑动 + 淡入淡出。这会导致生硬的布局跳动。

**提议方案**: 在日历容器上使用 `AnimatedContentSize` 修饰符:
- 周 -> 月: 平滑扩展高度，同时淡入新行
- 月 -> 周: 折叠高度，同时淡出多余行
- 在 Column 外层使用 `animateContentSize()` 配合 `tween(300)`
- Pager 内部内容使用交叉淡入淡出

**优先级**: 中 | **工作量**: 1 天

### 2.4 日期选择器交互

当前: 点击日期进行选择。无法跳转到很久以前的特定日期。

**提议方案**:
- 长按月/年标签打开原生 `DatePickerDialog`
- 或: 点击年份标签显示年份滚轮，然后是月份滚轮
- 滚动离开当前月时显示"今天"按钮（类似 Google 日历的浮动按钮）

**优先级**: 低 | **工作量**: 1-2 天

---

## 3. 日记列表重新设计

### 3.1 问题: 卡片布局效率低

当前 `EntryCard` 是全宽 GlassCard，包含时间、标题、预览（3 行）、心情/天气和标签。在手机屏幕上，一次只能看到 2-3 张卡片。加上日历区域，用户需要大量滚动。

### 3.2 提议方案: 紧凑行布局选项

**当前卡片**（预估高度: ~140dp）:
```
┌─────────────────────────────────┐
│ 14:30                           │
│ Today's Title                   │
│ This is the preview text that   │
│ takes up to three lines and...  │
│                                 │
│ [mood] [weather]      [tag1][t2]│
└─────────────────────────────────┘
```

**提议的紧凑行**（预估高度: ~72dp）:
```
┌─────────────────────────────────┐
│ 14:30  Today's Title      [mood]│
│        This is the preview t... │
│        [tag1] [tag2]            │
└─────────────────────────────────┘
```

**实现方式**:
- 添加 `ViewMode` 枚举: `CARD`、`COMPACT`、`TIMELINE`（timeline = 当前 TimelineScreen 样式）
- 在 `DataStore` 中持久化偏好设置
- 紧凑模式: 单行标题 + 单行预览，心情图标在右侧
- 时间线模式: 复用 TimelineScreen 的 `DayGroupCard`

**优先级**: 中 | **工作量**: 2-3 天

### 3.3 滑动操作

**提议方案**: 在每个条目卡片上滑动显示操作按钮。

```
┌─────────────────────────────────┐
│ [收藏]  ←──── card ────→  [删除]│
└─────────────────────────────────┘
```

**技术细节**:
- 使用 Material3 的 `SwipeToDismissBox`（或自定义 `Modifier.swipeable`）
- 左滑: 显示"收藏"（切换收藏状态）操作
- 右滑: 显示"删除"（移至回收站）操作，带确认提示
- 背景: 带图标的彩色操作指示器
- 在 `LazyColumn` 项目上使用 `animateItemPlacement()` 实现平滑重排

**优先级**: 高 | **工作量**: 2 天

### 3.4 下拉刷新

数据已通过 Room Flow 实现响应式更新，下拉刷新主要用于让用户安心。

**实现方式**:
- 用 `PullToRefreshBox`（Material3）包裹内容
- 刷新时: 触发 `dao.getAllPreviews()` 重新评估（已通过 Flow 自动发生）
- 完成时显示简洁的对勾动画

**优先级**: 低 | **工作量**: 0.5 天

### 3.5 无限滚动 vs 当前方案

**当前问题**: `getAllPreviews()` 将所有条目加载到内存。对于拥有 1000+ 条目的用户，这会导致:
- 初始加载缓慢
- 内存占用高
- 所有过滤在 Kotlin 中执行（而非 SQL）

**提议方案**: 使用 Room 的 `PagingSource` 配合 Paging 3 库。

```kotlin
@Query("SELECT ... FROM diary_entries WHERE ... ORDER BY createdAt DESC")
fun getPreviewsPaged(filters: ...): PagingSource<Int, DiaryPreview>
```

- `LazyColumn` -> `LazyPagingItems`
- 每页大小: 20-30 条目
- 过滤下推到 SQL（添加心情、天气、标签、日期范围的 WHERE 子句）
- 搜索使用 FTS 表（见第 4 节）

**优先级**: 高 | **工作量**: 3-4 天（包含 SQL 过滤重构）

---

## 4. 搜索体验

### 4.1 问题: 基础的 LIKE 搜索

当前 `DiaryDao` 中的搜索:
```sql
WHERE plainText LIKE '%' || :query || '%'
```

存在的问题:
- 大数据集上速度慢（全表扫描）
- 无排名/相关性排序
- 无匹配高亮
- 无搜索建议

### 4.2 提议方案: FTS5 全文搜索

**数据库迁移 (v13 -> v14)**:

```sql
CREATE VIRTUAL TABLE diary_entries_fts USING fts5(
    title,
    plainText,
    content='diary_entries',
    content_rowid='id'
);

-- Populate FTS table
INSERT INTO diary_entries_fts(rowid, title, plainText)
    SELECT id, title, plainText FROM diary_entries;

-- Triggers to keep FTS in sync
CREATE TRIGGER diary_entries_ai AFTER INSERT ON diary_entries BEGIN
    INSERT INTO diary_entries_fts(rowid, title, plainText)
    VALUES (new.id, new.title, new.plainText);
END;

CREATE TRIGGER diary_entries_ad AFTER DELETE ON diary_entries BEGIN
    INSERT INTO diary_entries_fts(diary_entries_fts, rowid, title, plainText)
    VALUES ('delete', old.id, old.title, old.plainText);
END;

CREATE TRIGGER diary_entries_au AFTER UPDATE ON diary_entries BEGIN
    INSERT INTO diary_entries_fts(diary_entries_fts, rowid, title, plainText)
    VALUES ('delete', old.id, old.title, old.plainText);
    INSERT INTO diary_entries_fts(rowid, title, plainText)
    VALUES (new.id, new.title, new.plainText);
END;
```

**新的 DAO 查询**:
```sql
SELECT d.id, d.title, d.plainText, ...
FROM diary_entries d
JOIN diary_entries_fts fts ON d.id = fts.rowid
WHERE diary_entries_fts MATCH :query
ORDER BY rank
```

**优势**:
- SQLite FTS5 在大数据集上比 LIKE 快 10-100 倍
- 内置排名算法（BM25）
- 支持短语搜索、前缀搜索、布尔运算符
- `snippet()` 函数可返回带匹配高亮的文本

### 4.3 搜索建议

**提议方案**:
- 最近搜索（已存在于内存中，持久化到 DataStore）
- 热门标签作为搜索建议
- 标题词汇自动补全

```
┌─────────────────────────────────┐
│  🔍 搜索日记内容...              │
│  ─────────────────────────────  │
│  最近搜索                        │
│  旅行  工作  心情                │
│  热门标签                        │
│  [生活] [工作] [旅行] [学习]     │
└─────────────────────────────────┘
```

**优先级**: 中 | **工作量**: 1-2 天

### 4.4 搜索结果高亮

使用 FTS5 的 `snippet()` 函数:
```sql
SELECT snippet(diary_entries_fts, 1, '<b>', '</b>', '...', 32) as highlighted
FROM diary_entries_fts
WHERE diary_entries_fts MATCH :query
```

在 Compose 中将 `<b>` 标签渲染为带粗体样式的 `AnnotatedString`。

**优先级**: 中 | **工作量**: 1 天

### 4.5 筛选面板设计

当前: HomeScreen 没有筛选 UI。TimelineScreen 有筛选面板但独立存在。

**提议方案**: 从 HomeScreen 可访问的统一筛选底部面板。

```
┌─────────────────────────────────┐
│  筛选条件                    [清除]│
│  ─────────────────────────────  │
│  心情                          │
│  [沮丧] [低落] [平静] [开心]    │
│  [快乐] [兴奋]                 │
│  ─────────────────────────────  │
│  天气                          │
│  [晴天] [多云] [阴天] [雨天]    │
│  ─────────────────────────────  │
│  时间范围                       │
│  [最近7天] [最近30天] [自定义]  │
│  ─────────────────────────────  │
│  标签                          │
│  [生活] [工作] [旅行] ...       │
│  ─────────────────────────────  │
│  排序                          │
│  [最新] [最早] [心情最好] [收藏]│
│                                 │
│         [应用筛选]              │
└─────────────────────────────────┘
```

**优先级**: 高 | **工作量**: 2-3 天

---

## 5. "历史今天"增强

### 5.1 问题: 功能未被充分利用

`onThisDayEntries` 存在于 ViewModel 中，但仅在单独的 `DiaryReviewScreen` 上显示。用户很少访问。

### 5.2 提议方案: 首页内嵌"历史今天"卡片

当存在"历史今天"条目时，在首页显示一张精美的卡片。

```
┌─────────────────────────────────────┐
│  ◈ X年前的今天                       │
│  ─────────────────────────────────  │
│  ┌───────────────────────────────┐  │
│  │ 2023年6月10日          3年前  │  │
│  │ Title of the old entry        │  │
│  │ Preview text of the entry...  │  │
│  │ [mood] [weather]              │  │
│  └───────────────────────────────┘  │
│  ┌───────────────────────────────┐  │
│  │ 2021年6月10日          5年前  │  │
│  │ Another old entry title       │  │
│  │ Preview text...               │  │
│  └───────────────────────────────┘  │
│                                     │
│  [查看所有历史今天]                  │
└─────────────────────────────────────┘
```

**技术细节**:
- 将此卡片显示在日历和选中日期条目之间
- 仅在 `onThisDayEntries` 非空时显示
- 每个条目卡片可点击（跳转到详情页）
- 年份标签: 根据 `now.year - entryDate.year` 计算得出"X年前"
- 多年条目时支持水平滚动

### 5.3 时间线对比: 过去 vs 现在

```
┌─────────────────────────────────────┐
│  3年前的你 vs 现在的你              │
│  ─────────────────────────────────  │
│  当时心情: 😊 开心    现在: 😌 平静 │
│  当时天气: ☀️ 晴天    现在: 🌧 雨天 │
│  当时字数: 320字      现在: 150字   │
│  当时标签: [旅行]     现在: [工作]  │
└─────────────────────────────────────┘
```

这是一个延伸目标 -- 需要跨年份比较元数据。

**优先级**: 高（内嵌卡片）/ 低（对比功能） | **工作量**: 1-2 天 / 3-4 天

---

## 6. 周/月摘要

### 6.1 问题: 摘要数据未在首页展示

`weeklySummary` 和 `moodTrend` 存在于 `HomeViewModel` 中，但未在 `HomeScreen` 中渲染。这是浪费的潜力。

### 6.2 提议方案: 摘要仪表盘卡片

在首页显示紧凑的摘要卡片，类似 iOS 的屏幕使用时间小组件。

```
┌─────────────────────────────────────┐
│  本周摘要                           │
│  ┌──────┐ ┌──────┐ ┌──────┐       │
│  │  5   │ │ 3.8  │ │ 1280 │       │
│  │ 篇   │ │ 平均 │ │ 字   │       │
│  │ 日记 │ │ 心情 │ │ 总计 │       │
│  └──────┘ └──────┘ └──────┘       │
│  ─────────────────────────────────  │
│  7天心情趋势                        │
│  😊 ─ 😌 ─ 😊 ─ 😢 ─ 😊 ─ 😌 ─ 😊  │
│  一   二   三   四   五   六   日  │
│  ─────────────────────────────────  │
│  连续记录: 12天 🔥                  │
└─────────────────────────────────────┘
```

**技术细节**:
- 使用 `weeklySummary`（已在 ViewModel 中计算）
- 使用 `moodTrend`（已计算）绘制 7 天心情折线
- 连续天数: 使用 `stats.streak`（已计算）
- 显示为可折叠区域: 点击标题展开/折叠
- 默认状态: 新用户展开，回访用户折叠

### 6.3 月度摘要（新增）

在 ViewModel 中添加月度摘要计算:

```kotlin
data class MonthlySummary(
    val entryCount: Int,
    val avgMood: Float?,
    val totalWords: Int,
    val daysWithEntries: Int,
    val mostUsedTag: String?,
    val moodTrend: TrendDirection
)
```

- 显示在周摘要下方
- "本月: 15篇日记, 平均心情 4.2, 总字数 5600"

**优先级**: 高 | **工作量**: 2-3 天（包含心情趋势可视化）

---

## 7. 快捷操作

### 7.1 长按上下文菜单

当前代码中有一个 TODO:
```kotlin
onLongClick = {
    haptic.click()
    // TODO: Implement multi-select mode
}
```

**提议方案**: 长按时显示上下文菜单。

```
┌─────────────────────────────┐
│  编辑                        │
│  收藏 / 取消收藏             │
│  分享                        │
│  删除                        │
│  多选                        │
└─────────────────────────────┘
```

**实现方式**:
- 使用 `DropdownMenu` 锚定到长按的卡片
- 或使用 `ModalBottomSheet` 获得更原生的移动端体验
- 操作: 编辑、收藏切换、分享（导出为文本/图片）、删除（-> 回收站）、进入多选

**优先级**: 高 | **工作量**: 1-2 天

### 7.2 多选模式

**提议方案**: 通过长按 -> "多选"或专用按钮进入多选模式。

```
┌─────────────────────────────────────┐
│  [X] 已选 3 篇          [全选] [删除]│
│  ─────────────────────────────────  │
│  ☑ 14:30  Entry Title 1     [mood]  │
│  ☑ 14:30  Entry Title 2     [mood]  │
│  ☐ 14:30  Entry Title 3     [mood]  │
│  ☑ 14:30  Entry Title 4     [mood]  │
└─────────────────────────────────────┘
```

**技术细节**:
- 在 ViewModel 中添加 `multiSelectMode: Boolean` 和 `selectedIds: Set<Long>`
- 顶栏变为显示选中计数 + 操作按钮
- 点击卡片切换选中状态（而非导航）
- 批量操作: 删除所有选中项、切换所有选中项的收藏状态、导出选中项

**优先级**: 中 | **工作量**: 2-3 天

### 7.3 批量操作

多选模式下:
- **批量删除**: 将所有选中项移至回收站，单次确认
- **批量收藏**: 切换所有选中项的收藏状态
- **批量标签**: 为所有选中项添加/移除标签
- **批量导出**: 将选中条目导出为 JSON/Markdown

**优先级**: 中 | **工作量**: 2 天

---

## 8. 首页布局策略

### 8.1 问题: 扁平列表布局

当前首页是扁平的 `LazyColumn`: 标题 -> 日历 -> 日期标题 -> 条目。所有内容处于同一层级，缺乏视觉层次。

### 8.2 方案 A: 吸顶日历头部

```
┌─────────────────────────────────────┐
│  首页                    [≡] [🔍]   │
│  ─────────────────────────────────  │
│  [周 view - always visible]         │  <- Sticky: stays at top when scrolling
│  ─────────────────────────────────  │
│  今天 · 3篇日记                     │  <- Date header
│  ┌───────────────────────────────┐  │
│  │ Entry 1                       │  │
│  └───────────────────────────────┘  │
│  ┌───────────────────────────────┐  │
│  │ Entry 2                       │  │
│  └───────────────────────────────┘  │
│  ...                                │
└─────────────────────────────────────┘
```

**优点**: 日历始终可访问，快速切换日期
**缺点**: 永久占用约 100dp 垂直空间

### 8.3 方案 B: 可折叠日历（底部面板）

```
┌─────────────────────────────────────┐
│  首页                    [≡] [🔍]   │
│  ─────────────────────────────────  │
│  本周摘要 (compact)                 │  <- Summary card
│  ─────────────────────────────────  │
│  今天 · 3篇日记                     │
│  ┌───────────────────────────────┐  │
│  │ Entry 1                       │  │
│  └───────────────────────────────┘  │
│  ┌───────────────────────────────┐  │
│  │ Entry 2                       │  │
│  └───────────────────────────────┘  │
│  ...                                │
│  ┌───────────────────────────────┐  │
│  │  [Calendar peek]              │  │  <- Bottom sheet peek
│  │  [Swipe up for full calendar] │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

**实现方式**:
- 使用 `BottomSheetScaffold` 或 `ModalBottomSheet`
- 预览状态: 仅显示周视图（约 80dp）
- 半屏状态: 显示月视图（约 280dp）
- 全屏状态: 月视图 + 日期选择器 + 筛选器
- 日历从底部滑上，条目保持不动

**优点**: 条目空间最大化，日历可访问但不侵入
**缺点**: 访问完整日历需要额外手势

### 8.4 方案 C: 灵动岛式摘要卡片（推荐）

```
┌─────────────────────────────────────┐
│  首页                    [≡] [🔍]   │
│  ┌───────────────────────────────┐  │
│  │  ◈ 今天 · 3篇    🔥 12天连续  │  │  <- Dynamic summary card
│  │  本周: 5篇 · 心情 3.8 · 1280字│  │    (always visible, compact)
│  └───────────────────────────────┘  │
│  ─────────────────────────────────  │
│  6月10日 周二 · 3篇日记            │  <- Selected date
│  ┌───────────────────────────────┐  │
│  │ Entry 1                       │  │
│  └───────────────────────────────┘  │
│  ┌───────────────────────────────┐  │
│  │ Entry 2                       │  │
│  └───────────────────────────────┘  │
│  ...                                │
│                                     │
│  ┌───────────────────────────────┐  │
│  │ ◈ 3年前的今天                 │  │  <- "On This Day" card
│  │ Entry from 2023...            │  │    (only if entries exist)
│  └───────────────────────────────┘  │
│  ...more entries...                 │
│                                     │
│  [FAB]                              │
│  ┌───────────────────────────────┐  │
│  │  [Week calendar peek]         │  │  <- Bottom bar calendar
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

**推荐方案**: 结合方案 B（底部面板日历）和顶部动态摘要卡片。

**布局结构**:
1. **顶栏**: "首页" + 功能菜单 + 搜索按钮
2. **摘要卡片**: 紧凑的胶囊形态，显示连续天数、周统计
3. **选中日期标题**: "6月10日 周二 · 3篇日记"
4. **条目列表**: 可滚动，支持滑动操作
5. **"历史今天"卡片**: 内嵌在条目之间（如适用）
6. **底部日历栏**: 周视图预览，点击展开为月视图
7. **FAB**: 悬浮在右下角

**优先级**: 高 | **工作量**: 4-5 天

---

## 9. 动画与过渡

### 9.1 当前动画状态

应用已具备:
- `AnimationConfig` 含 spring/tween 预设
- `staggeredListItem` 修饰符用于级联条目动画
- `GlassCard` 带按压缩放动画
- `CalendarDay` 带选中缩放动画
- `AnimatedContent` 用于日历模式切换
- `AnimatedVisibility` 用于条目卡片入场

### 9.2 提议的动画

**页面过渡**:
- 首页 -> 详情页: 条目卡片的共享元素过渡（标题文本、心情图标）
- 首页 -> 编辑器: FAB 变形为编辑器工具栏
- 使用 Compose 的 `SharedTransitionLayout` + `sharedElement()` 修饰符

**列表项动画**:
- 条目卡片: `animateItemPlacement()` 用于筛选/排序变更时的平滑重排
- 滑动删除: `AnimatedVisibility` 配合 `slideOutHorizontally` + `fadeOut`
- 新条目插入: 从顶部滑入，带弹性物理效果

**日历展开/折叠**:
- 周 -> 月: `animateContentSize(expandFrom = Alignment.TopCenter)`
- 月 -> 周: 反向
- 日期选择: 从日期单元格中心扩展的涟漪效果
- 月份导航（pager）: 带视差效果的水平滑动

**摘要卡片**:
- 数字: `AnimatedCounter`（代码库中已存在）用于计数变更
- 心情趋势线: 使用 `animateFloatAsState` 在路径进度上绘制动画
- 连续记录火焰图标: 微妙的脉冲动画

**优先级**: 中 | **工作量**: 3-4 天

---

## 10. 参考应用分析

### 10.1 Day One -- "历史今天"

**优秀之处**:
- 精美的全屏"历史今天"展示
- 突出显示过去条目中的照片
- "X年前的今天"使用醒目的排版
- 点击查看该日期跨年份的所有条目
- 与 Apple 小组件系统集成，提供每日提醒

**可借鉴之处**:
- 将"历史今天"作为一等功能，而非隐藏在菜单中
- 使用大号、富有情感的排版显示年份数
- 展示条目预览时提供足够的上下文使其有意义
- 考虑推送通知: "X年前的今天，你写了..."

### 10.2 Journey -- 时间线

**优秀之处**:
- 带日期标记的垂直时间线
- 照片作为时间线中的视觉锚点
- 地图视图上的位置标记
- 日历热力图显示写作一致性
- "今天"浮动按钮跳转回当前日期

**可借鉴之处**:
- 时间线布局（已存在于 TimelineScreen）可作为首页的视图模式选项
- 时间线条目中的照片缩略图
- 地图视图作为浏览条目的替代方式

### 10.3 Diaro -- 网格视图

**优秀之处**:
- 网格视图（2-3 列）显示带照片的条目卡片
- 紧凑卡片: 照片缩略图 + 标题 + 日期
- 列表/网格/时间线视图切换
- 基于文件夹/标签的组织方式

**可借鉴之处**:
- 网格视图作为第三种布局选项（在卡片/紧凑之后）
- 含图片条目的照片优先卡片
- 头部的视图模式切换

### 10.4 Momento -- 自动导入

**优秀之处**:
- 从社交媒体自动导入（Twitter、Instagram、Facebook）
- 从签到、照片、帖子创建条目
- 从多个来源聚合"时刻"

**可借鉴之处**（延伸目标）:
- 从微信导入（已通过微信桥接部分实现）
- 根据今天拍摄的照片自动创建条目
- 基于位置的条目建议

### 10.5 所有应用的共同模式

1. **日历是辅助功能** -- 这些应用都没有将日历放在首页的核心位置。它始终可访问但不占主导。
2. **内容优先** -- 条目列表/内容是主要焦点。日历和统计是辅助元素。
3. **视觉层次** -- "今天"内容和"浏览"内容之间有清晰的分隔。
4. **快速记录** -- 所有应用都有醒目的"新建条目"按钮，通常带有模板。

---

## 11. 实施路线图

### 阶段 1: 核心体验改进（2-3 周）

| 任务 | 优先级 | 工作量 | 依赖 |
|------|--------|--------|------|
| HorizontalPager 日历 | 高 | 3-4 天 | 无 |
| 条目卡片滑动操作 | 高 | 2 天 | 无 |
| 内嵌"历史今天"卡片 | 高 | 1-2 天 | 无 |
| 长按上下文菜单 | 高 | 1-2 天 | 无 |
| 首页摘要仪表盘卡片 | 高 | 2-3 天 | 无 |
| 灵动岛布局 | 高 | 4-5 天 | 摘要卡片 |

### 阶段 2: 搜索与筛选（1-2 周）

| 任务 | 优先级 | 工作量 | 依赖 |
|------|--------|--------|------|
| FTS5 全文搜索 | 高 | 3-4 天 | 数据库迁移 |
| 筛选底部面板 | 高 | 2-3 天 | 无 |
| 搜索建议 | 中 | 1-2 天 | FTS5 |
| 搜索结果高亮 | 中 | 1 天 | FTS5 |

### 阶段 3: 数据层优化（1-2 周）

| 任务 | 优先级 | 工作量 | 依赖 |
|------|--------|--------|------|
| Paging 3 集成 | 高 | 3-4 天 | FTS5 |
| SQL 层级过滤 | 高 | 2-3 天 | Paging 3 |
| 搜索历史持久化 | 中 | 0.5 天 | 无 |

### 阶段 4: 打磨与动画（1 周）

| 任务 | 优先级 | 工作量 | 依赖 |
|------|--------|--------|------|
| 日历展开/折叠动画 | 中 | 1 天 | 翻页日历 |
| 列表项动画 | 中 | 2 天 | 无 |
| 多选模式 | 中 | 2-3 天 | 上下文菜单 |
| 批量操作 | 中 | 2 天 | 多选模式 |
| 共享元素过渡 | 低 | 2-3 天 | 无 |

### 阶段 5: 高级功能（延伸）

| 任务 | 优先级 | 工作量 | 依赖 |
|------|--------|--------|------|
| 紧凑/网格视图模式 | 低 | 2-3 天 | 无 |
| 心情热力图日历 | 低 | 2 天 | 翻页日历 |
| 月度摘要 | 低 | 1-2 天 | 周摘要 |
| 时间线对比 | 低 | 3-4 天 | 历史今天 |
| 日期选择器跳转 | 低 | 1-2 天 | 无 |

### 总预估工作量: 单人开发 8-12 周

### 建议首个 PR: "首页布局 v2"

范围:
1. 将日历移至底部面板（周视图预览）
2. 顶部添加摘要卡片
3. 添加内嵌"历史今天"卡片
4. 保持现有条目列表不变

这是一个可见的改进，风险最小。不改变数据层或导航，只是重新组织布局。

---

## 附录: 技术说明

### 数据库迁移 v13 -> v14

```sql
-- FTS5 table for full-text search
CREATE VIRTUAL TABLE IF NOT EXISTS diary_entries_fts USING fts5(
    title,
    plainText,
    content='diary_entries',
    content_rowid='id'
);

-- Populate
INSERT INTO diary_entries_fts(rowid, title, plainText)
    SELECT id, title, plainText FROM diary_entries;

-- Triggers
CREATE TRIGGER IF NOT EXISTS diary_fts_ai AFTER INSERT ON diary_entries BEGIN
    INSERT INTO diary_entries_fts(rowid, title, plainText)
    VALUES (new.id, new.title, new.plainText);
END;

CREATE TRIGGER IF NOT EXISTS diary_fts_ad AFTER DELETE ON diary_entries BEGIN
    INSERT INTO diary_entries_fts(diary_entries_fts, rowid, title, plainText)
    VALUES ('delete', old.id, old.title, old.plainText);
END;

CREATE TRIGGER IF NOT EXISTS diary_fts_au AFTER UPDATE ON diary_entries BEGIN
    INSERT INTO diary_entries_fts(diary_entries_fts, rowid, title, plainText)
    VALUES ('delete', old.id, old.title, old.plainText);
    INSERT INTO diary_entries_fts(rowid, title, plainText)
    VALUES (new.id, new.title, new.plainText);
END;
```

### 新增依赖

```kotlin
// Paging 3
implementation("androidx.paging:paging-runtime-ktx:3.2.1")
implementation("androidx.paging:paging-compose:3.2.1")

// (Optional) HorizontalPager is in foundation, already included
// (Optional) Material3 bottom sheet
```

### 需要创建的关键 Composable

| Composable | 用途 |
|-----------|------|
| `SummaryCard` | 周统计 + 连续天数展示 |
| `OnThisDayCard` | 内嵌"历史今天"区域 |
| `SwipeableEntryCard` | 带滑动显示操作的条目卡片 |
| `FilterBottomSheet` | 统一筛选面板 |
| `SearchBar` | 首页搜索及建议 |
| `BottomCalendarBar` | 底部周视图预览 |
| `MoodTrendLine` | 7 天心情迷你折线图 |
| `ContextMenu` | 长按下拉菜单 |
| `MultiSelectTopBar` | 多选模式顶栏 |
