# Todo/Habit 系统重构设计方案

> 日期: 2026-06-10
> 分支: experiment/v2-redesign
> 当前数据库版本: 13 | 依赖: Compose BOM 2023.10, Room 2.6.1, 无 Glance/图表库

---

## 现状分析

### 当前架构

- **数据层**: `TodoItem` (单表承载 task/reminder/goal 三类) + `HabitRecord` (打卡记录)
- **UI 层**: `TodoScreen` 三 Tab (HABIT/MEMO/DEADLINE), 共享 `TodoViewModel`
- **Widget**: 基于 `RemoteViews` + `AppWidgetProvider`, XML 布局
- **提醒**: `AlarmManager` + `BroadcastReceiver`, 两个通知渠道 (待办提醒/每日摘要)
- **日记联动**: Editor 保存时调用 `autoCompleteHabitsForDiary()`, 按 linkedTagIds 匹配

### 核心问题

| 问题 | 严重程度 | 影响范围 |
|------|---------|---------|
| `TodoItem` 单表承载三种语义不同的实体, 字段空值多 | 高 | 数据层 |
| 习惯只支持"每天", 不支持灵活频率 | 高 | 习惯追踪 |
| 无自然语言日期解析, 创建待办步骤多 | 中 | UX |
| 习惯可视化仅 7 天 strip + 月历, 无 streak 激励 | 中 | UX |
| Widget 基于 RemoteViews, 无法实现复杂交互 | 中 | Widget |
| 无拖拽排序, 排序靠 sortOrder 数值 | 低 | UX |
| 备忘录仅纯文本, 无分类/置顶 | 低 | 功能 |
| 提醒无位置/行为感知 | 低 | 功能 |

---

## 一、待办系统重构

### 1.1 数据模型重构

**问题**: 当前 `TodoItem` 用 `category` 字段区分 task/reminder/goal, 但很多字段只对特定类型有意义 (如 `dueDate` 对备忘无意义, `linkedTagIds` 对任务无意义)。

**方案**: 保持单表但明确语义分区, 引入 `taskList` 概念替代 `category` 的粗粒度分类。

```
// TodoItem 新增字段
+ taskList: String = "inbox"     // inbox/today/next/someday/waiting (GTD 列表)
+ dueTime: Long? = null          // 截止时间 (精确到分钟, 与 dueDate 互补)
+ estimatedMinutes: Int? = null  // 预估耗时
+ energy: String = "any"         // low/medium/high (精力等级, GTD 场景)
+ completedSubtaskCount: Int = 0 // 冗余字段, 避免查询子任务计数
```

**数据库迁移**: `MIGRATION_13_14`, ALTER TABLE 添加新列, 默认值兼容旧数据。`category` 字段保留兼容, 新数据用 `taskList`。

### 1.2 自然语言日期解析

**问题**: 创建待办需要手动选择日期, 步骤多。

**方案**: 实现轻量级中文自然语言日期解析器, 无需外部依赖。

```kotlin
object NaturalDateParser {
    // 解析规则:
    // "明天下午3点开会" -> dueDate=明天, reminderTime=15:00
    // "每周一跑步" -> recurringType=weekly, dueDate=下周一
    // "月底前交报告" -> dueDate=本月最后一天
    // "3天后" -> dueDate=+3天
    // "下周五" -> dueDate=下周五

    fun parse(input: String): ParsedDate {
        // 1. 提取时间关键词 (今天/明天/后天/下周/月底/N天后/周X)
        // 2. 提取具体时间 (上午/下午/N点)
        // 3. 提取重复模式 (每天/每周/每月/每周X)
        // 4. 返回 ParsedDate(dueDate, reminderTime, recurringType, cleanTitle)
    }
}

data class ParsedDate(
    val dueDate: Long? = null,
    val reminderTime: Long? = null,
    val recurringType: String = "none",
    val cleanTitle: String  // 去除日期关键词后的标题
)
```

**实现位置**: `com.diary.app.ui.todo.nlp.NaturalDateParser`

**UI 集成**: 输入框下方实时预览解析结果, 用户可确认或修改。

### 1.3 循环任务增强

**问题**: 当前循环任务完成时创建新副本, 但:
- 不支持"每周X" (如每周一三五)
- 不支持"每月第N天"
- 完成旧副本后副本累积

**方案**:

```kotlin
// TodoItem 新增
+ recurringDays: String = ""     // "1,3,5" 表示周一三五 (weekly 模式)
+ recurringDayOfMonth: Int? = null // 每月几号 (monthly 模式)
+ recurringEndDate: Long? = null   // 循环结束日期
```

**循环任务完成逻辑优化**:
1. 完成时不再创建新副本, 改为标记 `isCompleted` 并记录 `completedAt`
2. 新增 `getRecurringSchedule()` 方法, 计算下一次应该出现的日期
3. 新增 `RecurringScheduler` (WorkManager), 每天凌晨检查并生成当天的循环任务实例

### 1.4 子任务 UX 增强

**问题**: 子任务存在但 UI 交互弱, 无进度自动计算。

**方案**:
- 父任务完成度 = 子任务完成百分比 (自动计算, 不用手动设 progress)
- 子任务支持拖拽重排序
- 子任务展开/收起动画
- 父任务完成时可选"同时完成所有子任务"

### 1.5 拖拽排序

**方案**: 使用 Compose 内置的 `Modifier.draggable` + `LazyListState` 实现, 无需外部依赖。

```kotlin
// 核心实现思路
@Composable
fun <T> ReorderableList(
    items: List<T>,
    onReorder: (List<T>) -> Unit,
    itemContent: @Composable (T) -> Unit
) {
    // 1. 长按触发拖拽
    // 2. 拖拽时交换位置并动画
    // 3. 松手后回调 onReorder, 批量更新 sortOrder
}
```

**数据层**: 批量更新 `sortOrder` 字段, 使用事务保证一致性。

### 优先级与工作量

| 子任务 | 优先级 | 预估工时 | 依赖 |
|--------|--------|---------|------|
| 自然语言日期解析 | P1 | 3 天 | 无 |
| 循环任务增强 | P1 | 2 天 | 数据库迁移 |
| 子任务 UX | P2 | 2 天 | 无 |
| 拖拽排序 | P2 | 3 天 | 无 |
| taskList 概念 | P3 | 2 天 | 数据库迁移 |

---

## 二、习惯追踪重构

### 2.1 GitHub 风格热力图

**问题**: 当前习惯可视化仅 7 天 strip + 月历, 缺少长期趋势感。

**方案**: 新增 GitHub 贡献图风格的热力图, 展示过去 3-6 个月的打卡情况。

```kotlin
@Composable
fun HabitHeatmap(
    records: List<HabitRecord>,
    startDate: LocalDate,
    endDate: LocalDate,
    modifier: Modifier = Modifier
) {
    // 列 = 周, 行 = 星期几
    // 颜色深浅 = 打卡强度 (未打卡/手动/日记/详细记录)
    // 支持水平滚动查看历史
    // 点击格子显示当日详情
}
```

**颜色方案**: 4 级色阶
- 无记录: surface 最低透明度
- 手动打卡: secondary 色, alpha 0.3
- 日记打卡: primary 色, alpha 0.5
- 详细记录: primary 色, alpha 0.8

### 2.2 灵活频率 (每周 N 次)

**问题**: 当前习惯只有"每天", 无法表达"每周运动 3 次"。

**方案**:

```kotlin
// TodoItem 新增
+ habitFrequency: String = "daily"    // daily/weekly_n/monthly_n
+ habitTargetCount: Int = 1           // 每周期目标次数 (weekly_n 模式)
+ habitPeriodDays: Int = 7            // 周期天数 (默认 7=一周)
```

**streak 计算逻辑调整**:
- `daily`: 连续每天打卡 (现有逻辑)
- `weekly_n`: 在一个 7 天周期内完成 N 次即算达标, 连续达标周期数 = streak
- `monthly_n`: 同理, 30 天周期

```kotlin
fun calculateFlexibleStreak(
    records: List<HabitRecord>,
    frequency: String,
    targetCount: Int,
    periodDays: Int,
    today: LocalDate
): Int {
    // 1. 按周期分组记录
    // 2. 每个周期内计数是否达标
    // 3. 从当前周期往回数连续达标周期数
}
```

### 2.3 习惯链 (Habit Stacking)

**问题**: 习惯之间无关联, 无法表达"做完 A 之后做 B"。

**方案**: 引入 `habitChainId` 字段, 同一链的习惯按顺序排列, UI 上显示为连续步骤。

```kotlin
// TodoItem 新增
+ habitChainId: String? = null   // 习惯链 ID
+ habitChainOrder: Int = 0       // 在链中的顺序
```

**UI 表现**: 同一链的习惯卡片之间有连接线, 完成前一个后下一个高亮提示。

### 2.4 习惯分类

**方案**: 使用现有 `tags` 字段或新增 `habitCategory` 字段对习惯分组。

```kotlin
// TodoItem 新增
+ habitCategory: String = ""  // 健康/学习/生活/工作 (用户自定义)
```

**UI 表现**: 习惯 Tab 顶部增加分类筛选 (横向 chip), 热力图按分类着色。

### 优先级与工作量

| 子任务 | 优先级 | 预估工时 | 依赖 |
|--------|--------|---------|------|
| 热力图组件 | P1 | 4 天 | 无 |
| 灵活频率 | P1 | 3 天 | 数据库迁移 + streak 逻辑重写 |
| 习惯分类 | P2 | 1 天 | 数据库迁移 |
| 习惯链 | P3 | 3 天 | 数据库迁移 |

---

## 三、打卡系统增强

### 3.1 快速打卡 Widget

**问题**: 打卡需要进入 App -> 切到打卡 Tab -> 点击记录, 路径长。

**方案**: 
1. **通知栏快速打卡**: 每日摘要通知下方增加"一键打卡"按钮, 展开显示各习惯的打卡 action
2. **桌面 Widget**: 专门的习惯打卡 Widget (见第六节)
3. **App 内快捷入口**: 首页顶部增加"今日打卡"横幅, 一键展开打卡面板

### 3.2 Streak 奖励与庆祝

**问题**: 连续打卡无正向反馈, 缺乏激励。

**方案**:

```kotlin
object StreakMilestones {
    val milestones = listOf(
        3 to StreakMilestone(3, "三天起步", "坚持 3 天了"),
        7 to StreakMilestone(7, "一周达人", "连续一周"),
        21 to StreakMilestone(21, "习惯养成", "21 天定律"),
        30 to StreakMilestone(30, "月度冠军", "整整一个月"),
        100 to StreakMilestone(100, "百日坚持", "100 天里程碑"),
        365 to StreakMilestone(365, "年度传奇", "整整一年")
    )
}

// 打卡成功时检查是否达到里程碑
// 达到时播放庆祝动画 (Compose Animation)
// 非弹窗, 而是在打卡卡片内展开动画效果
```

**动画效果**: 
- 普通打卡: 勾选动画 + 轻微震动
- 里程碑: 卡片展开庆祝文字 + 彩色粒子效果 (Compose Canvas)
- 不使用 emoji, 用文字 + 色彩表达

### 3.3 习惯分析页

**方案**: 习惯详情弹窗升级为独立分析页, 包含:
- 热力图 (过去 3-6 个月)
- 月度完成率折线图
- 最长 streak 记录
- 打卡时间分布 (几点打卡最多)
- 打卡来源分布 (手动/日记/详细)

```kotlin
@Composable
fun HabitAnalyticsScreen(habitId: Long) {
    // 1. 热力图
    // 2. 月度趋势图 (过去 12 个月)
    // 3. 数据统计卡片 (总打卡天数/当前streak/最长streak/完成率)
    // 4. 打卡时间分布 (柱状图)
}
```

### 优先级与工作量

| 子任务 | 优先级 | 预估工时 | 依赖 |
|--------|--------|---------|------|
| Streak 里程碑动画 | P1 | 2 天 | 无 |
| 快速打卡入口 | P1 | 2 天 | 无 |
| 习惯分析页 | P2 | 5 天 | 热力图组件 |
| 通知栏快速打卡 | P2 | 2 天 | 通知系统 |

---

## 四、备忘录增强

### 4.1 置顶备忘

**问题**: `TodoItem` 已有 `isPinned` 字段, 但备忘 Tab 未使用。

**方案**: 
- 长按备忘项显示"置顶"选项
- 置顶项显示在列表顶部, 有视觉区分 (左侧色条或背景色)
- 排序: 置顶 -> 未完成 -> 已完成

### 4.2 备忘分类

**方案**: 使用 `tags` 字段为备忘添加分类标签。

```kotlin
// UI: 备忘 Tab 顶部增加标签筛选横排
// 创建备忘时可选标签
// 默认显示全部
```

### 4.3 快速捕获

**方案**: 通知栏增加"快速记录"通知, 点击展开输入框, 输入后直接保存为备忘。

```kotlin
// QuickCaptureService (Android 11+ 支持内联回复)
// 或: 常驻通知 + RemoteInput
// 输入内容保存为 TodoItem(category = "memo")
```

### 优先级与工作量

| 子任务 | 优先级 | 预估工时 | 依赖 |
|--------|--------|---------|------|
| 置顶备忘 | P1 | 0.5 天 | 无 (字段已存在) |
| 备忘分类筛选 | P2 | 1 天 | 无 |
| 快速捕获通知 | P3 | 2 天 | 通知系统 |

---

## 五、待办与日记联动

### 5.1 当前联动分析

**现有机制**: `EditorScreen` 保存日记时调用 `autoCompleteHabitsForDiary(diaryTagIds, diaryEntryId)`, 匹配 `linkedTagIds` 自动创建 `HabitRecord`。

**问题**:
- 只能通过标签匹配, 不支持关键词/内容匹配
- 联动是单向的 (日记 -> 习惯), 反向不可见
- 无法在编辑器中看到今日待办

### 5.2 编辑器内"今日待办"面板

**方案**: 在日记编辑器底部工具栏增加"待办"按钮, 展开显示今日待办列表, 可直接勾选完成。

```kotlin
// EditorScreen 新增组件
@Composable
fun EditorTodoPanel(
    todayTodos: List<TodoItem>,
    onToggle: (TodoItem) -> Unit,
    onAddToEditor: (TodoItem) -> Unit  // 将待办内容插入编辑器
) {
    // 底部抽屉, 显示今日待办
    // 点击可完成
    // 长按可将待办内容作为文字插入日记
}
```

### 5.3 双向联动增强

**方案**:
- 习惯详情中显示关联的日记条目列表 (已有 `diaryEntryId`, 需补充 UI)
- 日记详情中显示该日记自动完成的习惯列表
- 编辑器中选择标签时, 预览哪些习惯会被自动打卡

```kotlin
// EditorScreen 选择标签时
@Composable
fun TagHabitPreview(selectedTagIds: Set<Long>, habits: List<TodoItem>) {
    val matchedHabits = habits.filter { habit ->
        val linkedIds = TodoItem.getLinkedTagIds(habit.linkedTagIds)
        linkedIds.any { it in selectedTagIds }
    }
    if (matchedHabits.isNotEmpty()) {
        // 显示提示: "保存后将自动打卡: 运动, 早睡"
    }
}
```

### 5.4 日记模板与待办关联

**方案**: 创建日记时可选择模板, 模板预填关联标签和待办提示。

### 优先级与工作量

| 子任务 | 优先级 | 预估工时 | 依赖 |
|--------|--------|---------|------|
| 编辑器今日待办面板 | P1 | 3 天 | 无 |
| 标签选择时习惯预览 | P1 | 1 天 | 无 |
| 习惯详情关联日记列表 | P2 | 2 天 | 无 |
| 日记模板 | P3 | 3 天 | 无 |

---

## 六、Widget 重构

### 6.1 当前 Widget 问题

- 基于 `RemoteViews` + XML 布局, 视觉效果受限
- `TodoWidgetService` 使用 `RemoteViewsFactory`, 列表交互有限
- 不支持复杂手势 (滑动、长按)
- 主题与 App 内 UI 不一致

### 6.2 Glance API 迁移

**方案**: 迁移到 Jetpack Glance, 使用 Compose 风格编写 Widget。

```kotlin
// 依赖
implementation("androidx.glance:glance-appwidget:1.1.1")
implementation("androidx.glance:glance-material3:1.1.1")

// Widget 实现
class TodoGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            TodoWidgetContent()
        }
    }
}

@Composable
fun TodoWidgetContent() {
    GlanceTheme {
        Column(modifier = GlanceModifier.fillMaxSize().padding(16.dp)) {
            // 标题 + 待办计数
            // 今日待办列表 (可勾选)
            // 习惯打卡快捷按钮
            // "添加" 按钮
        }
    }
}
```

### 6.3 Widget 类型规划

| Widget | 尺寸 | 功能 | 优先级 |
|--------|------|------|--------|
| 今日概览 | 4x3 | 今日待办 + 习惯完成度 | P1 |
| 习惯打卡 | 4x2 | 习惯列表 + 快速打卡 | P1 |
| 待办列表 | 4x4 | 可滚动待办列表 + 勾选 | P2 |
| 倒计时 (已有) | 2x2 | 保持现有 | - |

### 6.4 Widget 交互增强

- **内联打卡**: Widget 内直接点击打卡, 无需打开 App
- **快速添加**: Widget 底部输入框, 直接添加待办
- **深色模式**: 自动适配系统深色模式
- **动态主题**: 取色自适应壁纸

### 优先级与工作量

| 子任务 | 优先级 | 预估工时 | 依赖 |
|--------|--------|---------|------|
| Glance 迁移 (今日概览) | P1 | 4 天 | Glance 依赖 |
| 习惯打卡 Widget | P1 | 3 天 | Glance 迁移 |
| 待办列表 Widget | P2 | 3 天 | Glance 迁移 |
| 倒计时 Widget 迁移 | P3 | 2 天 | Glance 迁移 |

---

## 七、提醒系统增强

### 7.1 当前提醒系统分析

**现有**:
- `AlarmManager` 精确提醒 (Android 12+ 需 `SCHEDULE_EXACT_ALARM` 权限)
- 两个通知渠道: `todo_reminder` (待办提醒) / `todo_summary` (每日摘要)
- 支持延迟 15 分钟 / 标记完成
- 每日 8:00 AM 摘要

**问题**:
- 无位置提醒
- 延迟固定 15 分钟, 不灵活
- 无智能提醒 (基于用户行为)
- 通知渠道缺少细分

### 7.2 通知渠道细分

```kotlin
object NotificationChannels {
    const val TODO_REMINDER = "todo_reminder"       // 已有
    const val TODO_SUMMARY = "todo_summary"         // 已有
    const val HABIT_REMINDER = "habit_reminder"     // 新增: 习惯打卡提醒
    const val HABIT_MILESTONE = "habit_milestone"   // 新增: 习惯里程碑
    const val QUICK_CAPTURE = "quick_capture"       // 新增: 快速记录
}
```

### 7.3 灵活延迟选项

**方案**: 替换固定 15 分钟延迟, 增加多选项。

```kotlin
// 通知 Action 改为展开式
val snoozeOptions = listOf(
    5 * 60 * 1000L to "5 分钟后",
    15 * 60 * 1000L to "15 分钟后",
    30 * 60 * 1000L to "30 分钟后",
    60 * 60 * 1000L to "1 小时后",
    // Android 7+ 支持 RemoteInput, 可自定义时间
)
```

**实现**: Android 12+ 使用 `NotificationCompat.Builder.addAction()` 的展开式按钮, 或使用 `RemoteInput` 让用户输入自定义时间。

### 7.4 习惯打卡提醒

**方案**: 每日定时提醒用户打卡 (可自定义时间)。

```kotlin
// TodoItem 新增
+ habitReminderTime: String? = null  // "22:00" 表示每天 22:00 提醒打卡
```

**实现**: 使用 `AlarmManager` 每日定时触发, 通知内容显示今日未打卡习惯列表。

### 7.5 位置提醒 (P3)

**方案**: 使用 `GeofencingAPI` 在进入/离开特定位置时触发提醒。

```kotlin
// TodoItem 新增
+ locationReminderLat: Double? = null
+ locationReminderLng: Double? = null
+ locationReminderRadius: Float? = null  // 米
+ locationReminderType: String? = null   // "enter" / "exit"
```

**注意**: 需要 `ACCESS_FINE_LOCATION` + `ACCESS_BACKGROUND_LOCATION` 权限, 用户授权门槛高, 建议 P3。

### 优先级与工作量

| 子任务 | 优先级 | 预估工时 | 依赖 |
|--------|--------|---------|------|
| 通知渠道细分 | P1 | 0.5 天 | 无 |
| 灵活延迟选项 | P1 | 1 天 | 无 |
| 习惯打卡提醒 | P2 | 2 天 | 习惯频率系统 |
| 位置提醒 | P3 | 4 天 | 权限处理 |

---

## 八、数据可视化

### 8.1 依赖选型

**方案**: 使用 Compose Canvas 自绘, 不引入外部图表库。

理由:
- 图表需求简单 (折线图、柱状图、饼图)
- 避免依赖膨胀 (当前依赖已精简)
- 自绘可完美匹配 App 设计语言
- Compose Canvas 足够满足需求

### 8.2 习惯完成率图表

```kotlin
@Composable
fun CompletionRateChart(
    monthlyRates: List<MonthRate>,  // 过去 12 个月
    modifier: Modifier = Modifier
) {
    // 折线图, X轴=月份, Y轴=完成率 0-100%
    // 点击节点显示详情
    // 颜色: primary 色系, 渐变填充
}

data class MonthRate(val yearMonth: YearMonth, val rate: Float)
```

### 8.3 Streak 日历

(见第二节热力图, 复用同一组件)

### 8.4 生产力评分

**方案**: 综合待办完成率、习惯打卡率、日记频率计算每日/每周评分。

```kotlin
fun calculateProductivityScore(
    completedTodos: Int,
    totalTodos: Int,
    habitsCompleted: Int,
    totalHabits: Int,
    diaryWritten: Boolean
): Int {
    // 待办完成率 * 40 + 习惯完成率 * 40 + 日记 * 20
    // 返回 0-100 分
}
```

### 8.5 周/月报告

**方案**: 每周/每月自动生成报告, 包含:
- 本周/月习惯完成率
- streak 变化
- 待办完成统计
- 与上周/上月对比

**展示**: Profile 页新增"报告"入口, 或每周一推送通知摘要。

### 优先级与工作量

| 子任务 | 优先级 | 预估工时 | 依赖 |
|--------|--------|---------|------|
| 基础图表组件 (Canvas) | P1 | 3 天 | 无 |
| 习惯完成率图表 | P1 | 2 天 | 图表组件 |
| 生产力评分 | P2 | 2 天 | 数据聚合逻辑 |
| 周/月报告 | P3 | 4 天 | 评分系统 + 通知 |

---

## 九、参考优秀 App 分析

### 9.1 Todoist

**值得借鉴**:
- 自然语言日期输入 (最核心竞争力)
- 项目/标签双重分类
- Karma 积分系统 (激励机制)
- 多级优先级 (4 级)

**不适合引入**:
- 过于复杂的项目层级 (对日记 App 来说太重)
- 付费墙功能 (协作、日历集成)

### 9.2 Habitica (游戏化)

**值得借鉴**:
- RPG 游戏化概念 (但需简化)
- 连续打卡奖励机制
- 社交功能 (队伍)

**不适合引入**:
- 完整 RPG 系统 (太复杂)
- 虚拟货币经济系统
- 宠物/装备系统

**可轻量引入**: 简化的等级系统, 每次打卡获得经验值, 升级时解锁主题/图标。

### 9.3 Streaks (极简)

**值得借鉴**:
- 最多 24 个习惯的限制 (强制精简)
- 极简的 UI, 一个习惯一个大圆环
- 灵活频率 (每周 N 次)
- Apple Watch 快速打卡

**不适合引入**:
- iOS 专属设计语言

**核心理念**: 少即是多, 习惯数量限制比无限添加更好。建议软提示"建议不超过 10 个活跃习惯"。

### 9.4 Loop Habit Tracker (开源)

**值得借鉴**:
- 完全开源, 数据本地化 (与本 App 理念一致)
- 丰富的图表: 热力图、折线图、柱状图、频率表
- 灵活频率 (每天/每周 N 次/每月 N 次)
- Habit 分数算法 (加权频率)
- 无网络权限, 无广告

**核心算法参考**:
```kotlin
// Loop 的 Habit Score 算法
// 分数 = Σ(频率权重 * 时间衰减)
// 最近的打卡权重更高, 逐步衰减
fun calculateHabitScore(records: List<HabitRecord>, today: LocalDate): Float {
    var score = 0f
    var weight = 1f
    val decay = 0.97f  // 每天衰减 3%
    var cursor = today
    val recordDays = records.map { it.recordDate }.toSet()

    repeat(365) {
        if (cursor.toEpochDay() in recordDays) {
            score += weight
        }
        weight *= decay
        cursor = cursor.minusDays(1)
    }
    return score
}
```

### 9.5 TickTick (日历集成)

**值得借鉴**:
- 日历视图整合待办
- 番茄钟内置
- 四象限视图 (Eisenhower Matrix)

**不适合引入**:
- 过于复杂的日历集成
- 内置番茄钟 (可作为 P3 扩展)

**可轻量引入**: 待办 Tab 增加"日历视图"切换, 在月历上显示待办分布。

---

## 十、实施路线图

### Phase 1: 核心体验提升 (2-3 周)

目标: 解决最痛的问题, 提升日常使用体验。

| 任务 | 工时 | 文件变更 |
|------|------|---------|
| 自然语言日期解析 | 3 天 | 新增 `NaturalDateParser.kt`, 修改 `TodoScreen.kt` |
| 循环任务增强 | 2 天 | 修改 `TodoItem.kt`, `TodoViewModel.kt`, 数据库迁移 |
| Streak 里程碑动画 | 2 天 | 修改 `TodoScreen.kt`, `HabitUiStateBuilder.kt` |
| 置顶备忘 | 0.5 天 | 修改 `TodoScreen.kt` (排序逻辑) |
| 热力图组件 | 4 天 | 新增 `HabitHeatmap.kt`, 修改 `TodoScreen.kt` |
| 编辑器今日待办面板 | 3 天 | 修改 `EditorScreen.kt`, `EditorViewModel.kt` |

### Phase 2: 数据与可视化 (2-3 周)

目标: 让用户看到习惯的长期价值。

| 任务 | 工时 | 文件变更 |
|------|------|---------|
| 灵活频率系统 | 3 天 | 修改 `TodoItem.kt`, `TodoViewModel.kt`, streak 逻辑 |
| 基础图表组件 | 3 天 | 新增 `Charts.kt` (Canvas 组件) |
| 习惯分析页 | 5 天 | 新增 `HabitAnalyticsScreen.kt` |
| 习惯完成率图表 | 2 天 | 复用图表组件 |
| 生产力评分 | 2 天 | 修改 `TodoViewModel.kt`, Profile 页 |

### Phase 3: Widget 与通知 (2 周)

目标: 减少打开 App 的频率, 提升便利性。

| 任务 | 工时 | 文件变更 |
|------|------|---------|
| Glance 迁移 | 4 天 | 新增 Glance Widget, 保留旧 Widget 兼容 |
| 习惯打卡 Widget | 3 天 | 新增 `HabitGlanceWidget.kt` |
| 通知渠道细分 + 灵活延迟 | 1.5 天 | 修改 `TodoReminderManager.kt` |
| 习惯打卡提醒 | 2 天 | 修改 `TodoReminderManager.kt` |
| 快速捕获通知 | 2 天 | 新增 `QuickCaptureService.kt` |

### Phase 4: 高级功能 (按需)

| 任务 | 工时 | 优先级 |
|------|------|--------|
| 拖拽排序 | 3 天 | P2 |
| 习惯链 | 3 天 | P3 |
| 位置提醒 | 4 天 | P3 |
| 周/月报告 | 4 天 | P3 |
| 日历视图 | 3 天 | P3 |
| 习惯分类 | 1 天 | P2 |

---

## 十一、数据库迁移规划

```
MIGRATION_13_14 (Phase 1):
  ALTER TABLE todo_items ADD COLUMN taskList TEXT NOT NULL DEFAULT 'inbox'
  ALTER TABLE todo_items ADD COLUMN dueTime INTEGER
  ALTER TABLE todo_items ADD COLUMN estimatedMinutes INTEGER
  ALTER TABLE todo_items ADD COLUMN energy TEXT NOT NULL DEFAULT 'any'

MIGRATION_14_15 (Phase 1 循环任务):
  ALTER TABLE todo_items ADD COLUMN recurringDays TEXT NOT NULL DEFAULT ''
  ALTER TABLE todo_items ADD COLUMN recurringDayOfMonth INTEGER
  ALTER TABLE todo_items ADD COLUMN recurringEndDate INTEGER

MIGRATION_15_16 (Phase 2 灵活频率):
  ALTER TABLE todo_items ADD COLUMN habitFrequency TEXT NOT NULL DEFAULT 'daily'
  ALTER TABLE todo_items ADD COLUMN habitTargetCount INTEGER NOT NULL DEFAULT 1
  ALTER TABLE todo_items ADD COLUMN habitPeriodDays INTEGER NOT NULL DEFAULT 7
  ALTER TABLE todo_items ADD COLUMN habitCategory TEXT NOT NULL DEFAULT ''
  ALTER TABLE todo_items ADD COLUMN habitReminderTime TEXT
```

**兼容策略**: 所有新字段都有默认值, 旧数据自动兼容。`category` 字段保留, 新代码优先使用 `taskList`。

---

## 十二、技术风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| Glance API 兼容性 | Widget 在旧设备上不显示 | 保留旧 RemoteViews Widget 作为 fallback |
| 数据库迁移失败 | 用户数据丢失 | 每次迁移前备份, 测试覆盖所有迁移路径 |
| 自然语言解析准确率 | 用户输入被误解析 | 始终显示解析结果预览, 用户可手动覆盖 |
| 位置权限被拒 | 位置提醒不工作 | 位置提醒作为可选功能, 不影响核心流程 |
| Canvas 图表性能 | 大量数据点卡顿 | 限制数据点数量, 使用 `remember` 缓存计算结果 |

---

## 十三、设计原则

1. **渐进增强**: 每个 Phase 都是独立可发布的, 不依赖后续 Phase
2. **向后兼容**: 所有数据迁移都支持旧数据, 不丢数据
3. **保持精简**: 不引入外部图表/日期解析库, 用自绘/自实现控制包体积
4. **用户选择**: 新功能都是可选的, 不强制改变现有使用方式
5. **一致性**: 所有新 UI 组件复用 `GlassCard`, `GradientBackground` 等现有组件
