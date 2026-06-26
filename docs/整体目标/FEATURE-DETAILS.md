# 功能详细说明（用户确认版）

> 基于 AI 代码审计的 38 模块 120+ 功能点，经用户 2026-06-26 逐条确认后保留的功能。
> 每个功能包含：现状分析 → 代码级缺口 → 实现方案 → 数据模型建议 → 涉及文件。
> 本文档是 EXECUTION-BRIEF.md 的详细补充，开发具体功能时按需查阅对应章节。

---

## 一、编辑器系统 (Editor)

### 1.1 语音输入

**现状：** 完全没有语音输入功能。移动端口述日记是高频需求。

**缺口：**
- 没有语音转文字入口
- 没有语音备忘录附件功能

**实现方案：**
- Android SpeechRecognizer API → 文字 → 插入编辑器光标位置
- UI：编辑器工具栏增加麦克风按钮，长按持续录音，松手转文字
- 进阶：支持语音备忘录附件（保存为 .m4a，关联 DiaryImage 表新增 mediaType=audio）

**数据模型：**
- DiaryImage 表新增 `mediaType: String = "image"` (可选值: image/audio/file)
- 数据库迁移 MIGRATION_34_35

**涉及文件：**
- `ui/editor/EditorScreen.kt` — 工具栏加麦克风按钮
- `ui/editor/EditorToolbar.kt` — 新增 VoiceInputButton 组件
- `assets/editor.html` — WebView JS 层接收文字并插入光标位置
- `DiaryJsBridge.kt` — 新增 insertTextAtCursor 方法
- `data/DiaryDatabase.kt` — 迁移 34→35
- `data/DiaryImage.kt` — 新增 mediaType 字段

**权限：** `RECORD_AUDIO`

---

### 1.2 AI问答引导替代模板

**现状：** 用户明确说不要静态模板（模板系统已删除）。用户提出"或者让AI问我一些问题我回答？"

**缺口：**
- 没有写作引导功能
- 新建日记时直接进空白编辑器，对"不知道写什么"的用户不友好

**实现方案：**
- 新建日记时，AI通过对话引导用户写作
- 引导流程：3-5轮问答 → 自动生成日记框架 → 进入编辑器
- 问题示例："今天发生了什么？""你的心情如何？""想记录哪个片段？""有没有什么特别的细节？"
- AI根据回答自动生成标题和大纲

**数据模型：**
- 无需新增表，复用现有 AiServiceManager

**涉及文件：**
- 新建 `ui/editor/AiWritingGuideDialog.kt` — 引导弹窗UI
- `ui/editor/EditorViewModel.kt` — 新增引导状态管理
- `ai/AiServiceManager.kt` — 新增 writingGuidePrompt
- `AppPreferences.kt` — 新增 aiWritingGuideEnabled 开关

---

### 1.3 编辑器附件支持

**现状：** 编辑器只支持图片插入，不支持语音备忘录和文件附件。

**缺口：**
- 没有语音备忘录录制和关联
- 没有PDF等文件附件
- 没有位置卡片嵌入

**实现方案：**
- 语音备忘录：录制 → .m4a → 存入 diary_media/ → 关联 DiaryImage 表
- 文件附件：选择文件 → 存入 diary_media/ → 在编辑器中显示附件卡片
- 位置卡片：嵌入当前地图截图作为位置可视化

**数据模型：**
- DiaryImage 表新增 `mediaType: String` (image/audio/file)
- DiaryImage 表新增 `fileSize: Long?` (文件大小)

**涉及文件：**
- `ui/editor/EditorToolbar.kt` — 新增附件按钮（长按弹出选择：语音/文件）
- 新建 `util/AudioRecorder.kt` — 录音管理
- 新建 `ui/editor/MediaPicker.kt` — 文件选择器
- `assets/editor.html` — 新增附件卡片渲染

---

### 1.4 编辑器手势

**现状：** 没有手势操作，所有操作依赖工具栏按钮。

**缺口：**
- 没有文字缩放
- 没有滑动返回
- 没有下拉保存

**实现方案：**
- 双指捏合缩放文字（在 WebView JS 层拦截 touch 事件）
- 左滑返回（带保存确认弹窗）
- 下拉保存（显示保存进度指示器）

**涉及文件：**
- `ui/editor/EditorScreen.kt` — 手势检测 (modifier.pointerInput)
- `assets/editor.html` — JS 层缩放事件处理

---

## 二、首页 (Home)

### 2.1 心情日历热力图

**现状：** CalendarView 873行，日历只显示有/无日记的圆点。没有心情颜色编码。

**缺口：** 用户无法一眼看到情绪变化趋势。

**实现方案：**
- 日历每个日期格子按心情等级着色：
  - 1 (很差) = 深蓝 #1565C0
  - 2 (较差) = 浅蓝 #42A5F5
  - 3 (一般) = 灰色 #9E9E9E
  - 4 (不错) = 浅绿 #66BB6A
  - 5 (很好) = 绿色 #2E7D32
  - 6 (极好) = 金色 #F9A825
- 无日记 = 透明（保持当前）
- 月视图和周视图都需要支持

**数据模型：**
- 需要日记的 moodLevel 数据，通过新 DAO 查询获取：
```sql
SELECT date(createdAt/1000, 'unixepoch', 'localtime') as day, AVG(moodLevel) as avgMood
FROM diary_entries WHERE createdAt BETWEEN :start AND :end
GROUP BY day
```

**涉及文件：**
- `ui/components/CalendarView.kt` — 日期 Cell 颜色渲染（核心改动）
- `data/DiaryDao.kt` — 新增按日期聚合心情查询
- `ui/home/HomeViewModel.kt` — 提供日期心情Map数据

---

### 2.2 今日写作提示卡（常驻）

**现状：** AI洞察卡片35%概率随机出现，内容简单。WritingPromptCard已有81行基础。

**缺口：** 没有每日写作引导，用户打开APP不知道写什么。

**实现方案：**
- 首页顶部常驻"今日提示"卡片，每天更换
- 提示类型：
  - 回顾型："一年前的今天你写了..."
  - 引导型："今天试试记录一件让你微笑的事"
  - 季节型："春天来了，窗外有什么变化？"
  - 情绪型：检测最近心情低落 → "想聊聊让你烦恼的事吗？"
- 点击提示卡直接进入编辑器
- "换一个"按钮切换提示

**涉及文件：**
- `ui/home/WritingPromptCard.kt` — 增强为常驻卡片
- `ui/home/HomeViewModel.kt` — 增强提示选择逻辑（按类型分类）
- 提示库需从25个扩充到50+个

---

### 2.3 写作连续天数首页展示

**现状：** WritingPromptCard中有简单数字展示。StreakCalculator已实现全部计算逻辑。

**缺口：** 首页没有醒目的连续记录展示，缺乏激励。

**实现方案：**
- 首页日历上方显示火焰图标 + 连续天数
- 颜色分级（streakTier()已实现）：
  - 1-6天: 灰色
  - 7-29天: 橙色
  - 30-99天: 金色
  - 100-364天: 彩虹渐变
  - 365天+: 皇冠图标
- 达到里程碑时配合 haptic 反馈

**涉及文件：**
- `ui/home/HomeScreen.kt` — 新增 StreakDisplay 组件（日历上方）
- `ui/home/WritingPromptCard.kt` — StreakInfoBar 增强
- `util/StreakCalculator.kt` — streakTier() 已实现

---

### 2.4 智能问候语

**现状：** HomeGreeting已有基础（时间段问候）。

**缺口：** 问候语缺乏个性化，没有结合天气/心情/习惯。

**实现方案：**
- 根据以下维度生成问候：
  - 时间段（早上好/下午好/晚上好）
  - 天气（外面在下雨/今天阳光明媚）
  - 最近心情（最近心情不错/看你最近有些低落）
  - 写作习惯（你已经连续写了5天了/好几天没写了）
- 示例："周三下午好，外面在下雨，适合写点什么。你已经连续写了5天了。"

**涉及文件：**
- `ui/home/HomeViewModel.kt` — 增强 HomeGreeting 数据

---

### 2.5 首页布局自定义

**现状：** 固定布局（日历→快捷入口→AI卡片→日记列表）。

**缺口：** 不同用户偏好不同的首页信息排列。

**实现方案：**
- 卡片顺序可拖拽调整（reorderable LazyColumn）
- 卡片可显示/隐藏（设置中配置）
- 紧凑模式/宽松模式切换

**涉及文件：**
- `ui/home/HomeScreen.kt` — 较大改动，LazyColumn + reorderable
- `AppPreferences.kt` — 新增首页布局偏好字段

---

## 三、统计分析 (Stats)

### 3.1 词汇丰富度分析

**现状：** 只有词频统计（top 20高频词）。

**实现方案：**
- 词汇多样性指数：unique words / total words
- 最常用表达 TOP 10
- 写作风格演变折线图：按月统计平均句长、词汇丰富度变化
- 情绪词汇占比趋势

**涉及文件：**
- `ui/stats/StatsScreen.kt` — 新增词汇分析 Section
- `ui/stats/StatsViewModel.kt` — 新增词汇分析逻辑
- 新建 `util/TextAnalyzer.kt` — 文本分析工具（分词、统计）

---

### 3.2 时间维度深度分析

**现状：** 只有月度趋势（最近6个月）。

**实现方案：**
- 年度对比：今年 vs 去年同期写作量对比
- 季节性模式：哪个季节写得最多
- 星期几分布柱状图：周几写得最多
- 时段分布饼图：凌晨/早晨/下午/晚上/深夜

**涉及文件：**
- `ui/stats/StatsScreen.kt` — 新增时间分析 Section
- `ui/stats/StatsViewModel.kt` — 新增时间维度聚合查询
- `data/DiaryDao.kt` — 新增按小时/星期/月份聚合查询

---

### 3.3 互动式情绪时间线

**现状：** 心情趋势只有最近30天 vs 前30天平均值。

**实现方案：**
- 可缩放的情绪折线图（横轴=时间，纵轴=心情等级1-6）
- 支持按月/季/年缩放
- 点击数据点跳转到对应日记详情

**涉及文件：**
- `ui/stats/StatsScreen.kt` — 替换/增强现有心情趋势图
- 新建 `ui/components/MoodTimelineChart.kt` — 可缩放折线图组件

---

### 3.4 生活事件关联分析

**现状：** 只有心情-天气关联。

**实现方案：**
- 标签与心情关联："写了'工作'标签时平均心情3.2，写了'旅行'时4.8"
- 写作时长与心情关联："写作超过10分钟时心情平均高0.5分"
- 地点与心情关联："在家写日记时心情最好"

**涉及文件：**
- `ui/stats/StatsScreen.kt` — 新增关联分析 Section
- `ui/stats/StatsViewModel.kt` — 新增多维度关联查询
- `data/DiaryDao.kt` — 新增 Tag-Mood、Duration-Mood、Location-Mood 聚合查询

---

### 3.5 写作目标设定

**现状：** WritingGoal entity和StatsScreen中的WritingGoalSection已创建（Phase 5）。

**需要完善：**
- 目标类型：每周写作N篇 / 每月字数N字
- 首页显示目标进度环（目前只在统计页）
- 达成目标时触发庆祝动画
- 目标历史记录（已完成的目标列表）

**涉及文件：**
- `ui/stats/StatsScreen.kt` — WritingGoalSection增强
- `ui/home/HomeScreen.kt` — 新增目标进度环
- `ui/stats/StatsViewModel.kt` — 目标完成检测+庆祝触发

---

## 四、成就系统 (Achievement)

### 4.1 每周/每月挑战

**现状：** 只有永久成就，没有时效性挑战。

**实现方案：**
- 每周挑战："本周写3篇日记"、"本周尝试2种不同天气记录"
- 每月挑战："本月写一篇超过2000字的日记"、"本月使用5个新标签"
- 挑战卡片在首页和成就页展示
- 完成有特殊动画
- 每周一自动刷新挑战列表

**数据模型：**
- 新建 `Challenge` Entity：id, title, description, type(weekly/monthly), targetValue, currentValue, periodStart, periodEnd, completed
- 或复用 Achievement 框架，新增 challenge 类型

**涉及文件：**
- 新建 `data/Challenge.kt` — Entity
- 新建 `data/ChallengeDao.kt` — DAO
- 新建 `data/ChallengeManager.kt` — 挑战生成+检测逻辑
- `ui/achievement/AchievementScreen.kt` — 新增挑战Tab
- `ui/home/HomeScreen.kt` — 首页展示挑战卡片
- `data/DiaryDatabase.kt` — 迁移新增表

---

### 4.2 成就进度可视化增强

**现状：** 进度条+百分比。

**实现方案：**
- 成就树/成就地图：按类别展示解锁进度的可视化树状图
- 稀有度标识：解锁率显示（基于本地数据推算）
- 成就分享卡片：解锁时生成精美分享图

**涉及文件：**
- `ui/achievement/AchievementDetailScreen.kt` — 增强进度可视化
- 新建 `ui/achievement/AchievementShareCard.kt` — 分享卡片Canvas绘制

---

## 七、AI 系统

### 7.1 AI周报/月报增强

**现状：** 月报/年报已有基础版（MonthlyReportScreen 979行, AnnualReportScreen 1322行）。

**实现方案：**
- AI自动生成每周摘要（3-5句话概括本周生活）
- 情绪曲线解读（AI分析为什么某天心情特别好/差）
- 生活建议（基于写作模式给出温和建议）
- 周报内容：本周日记数、总字数、心情走势、高频标签、写作天数

**涉及文件：**
- 新建 `ui/stats/WeeklyReportScreen.kt`
- 新建 `ui/stats/WeeklyReportViewModel.kt`
- `ai/AiServiceManager.kt` — 新增 weeklySummary Prompt
- 可参考 `ui/stats/MonthlyReportScreen.kt` 的结构

---

### 7.2 AI语义搜索

**现状：** 搜索是SQL LIKE匹配，无法理解语义。

**实现方案：**
- "找找我关于旅行的日记" → AI理解语义后检索
- 实现方式：AI将自然语言查询转为结构化参数 → 再用DAO精确查询
- AI输出示例：`{keywords: ["旅行", "出游"], mood: "开心", timeRange: "最近3个月"}`
- 支持组合搜索："雨天在家写的长日记"

**涉及文件：**
- `ui/home/HomeViewModel.kt` — 搜索逻辑增加AI解析分支
- `ai/AiServiceManager.kt` — 新增 searchParsePrompt
- `data/DiaryDao.kt` — 新增多条件组合查询

---

### 7.3 AI自动标签

**现状：** 标签全靠手动添加。AppPreferences.autoTagSuggestion开关已存在。

**实现方案：**
- 保存日记时AI分析内容，建议添加标签
- UI：保存前弹出标签建议Chip列表，用户点选采纳
- 批量给历史日记打标签（后台任务）
- 标签合并建议（"旅行"和"旅游"是否合并）

**涉及文件：**
- `ui/editor/EditorViewModel.kt` — save时触发标签建议
- `ai/AiServiceManager.kt` — 新增 tagSuggestPrompt
- `ui/editor/EditorScreen.kt` — 保存前弹出建议Chip

---

### 7.4 AI写作风格分析

**现状：** 完全没有。

**实现方案：**
- 分析用户写作风格特征：简洁/详细、理性/感性、叙事/反思
- 风格演变趋势图（按月统计风格变化）
- "你的写作越来越详细了" 这类洞察
- AI分析维度：平均句长、词汇丰富度、情感词占比、第一人称使用频率

**涉及文件：**
- `ui/stats/StatsScreen.kt` — 新增风格分析Section
- `ui/stats/StatsViewModel.kt` — 新增风格分析逻辑
- `ai/AiServiceManager.kt` — 新增 styleAnalysisPrompt
- 新建 `util/TextAnalyzer.kt` — 文本特征提取

---

## 十、待办与习惯 (Todo/Habit)

### 10.1 最简精致待办

**现状：** TodoDao(121行/30+查询), TodoRepository(39行), TodoScreen(1606行) 已有完整功能。

**用户要求：** "最简单版本，精致的待办"。不需要看板、习惯统计增强、番茄钟等复杂功能。

**实现方案：**
- 精简TodoScreen（当前1606行太重），保留核心功能：
  - 添加待办（文字输入+分类选择）
  - 勾选完成（带动画）
  - 分类筛选（全部/工作/生活/学习等）
  - 到期日标记
  - 删除/编辑
- UI风格：简洁卡片式，每条待办一行

**涉及文件：**
- `ui/todo/TodoScreen.kt` — 精简重构
- `ui/todo/TodoViewModel.kt` — 精简

---

## 十一、通知与提醒

### 11.1 智能写作提醒

**现状：** 固定时间提醒（AppPreferences.writingReminderHour/Minute）。

**实现方案：**
- 基于写作习惯的智能提醒：检测到用户通常9点写 → 9点提醒
- 连续断裂提醒："你已经2天没写了"
- 心情低落时的温柔提醒："想聊聊吗？"
- 天气触发提醒："外面在下雨，适合写点什么"（需AppPreferences.weatherReminder=true）

**涉及文件：**
- `reminder/NotificationPreferencesManager.kt` — 增强智能时间检测
- `reminder/TodoReminderManager.kt` — 增强提醒逻辑
- `AppPreferences.kt` — 已有相关设置项

---

### 11.2 每日回顾推送

**现状：** 没有每日回顾功能。

**实现方案：**
- 睡前推送今日回顾："今天你写了1篇日记，心情指数4.2，连续第7天"
- 每周日推送周回顾摘要
- APP内通知横幅展示

**涉及文件：**
- `reminder/NotificationPreferencesManager.kt` — 新增每日回顾通知
- `reminder/TodoReminderManager.kt` — 新增回顾推送逻辑
- 需WorkManager定时任务

---

## 十四、月报/年报

### 14.1 周报

**现状：** 有月报(MonthlyReportScreen 979行)和年报(AnnualReportScreen 1322行)，没有周报。

**实现方案：**
- 每周日晚自动生成周报
- 内容：本周日记数、总字数、心情走势、高频标签、写作天数
- 推送通知
- 可参考月报的结构

**数据模型：**
- 无需新增表，实时计算

**涉及文件：**
- 新建 `ui/stats/WeeklyReportScreen.kt`
- 新建 `ui/stats/WeeklyReportViewModel.kt`
- `ai/AiServiceManager.kt` — 新增 weeklyInsight Prompt
- 参考 `ui/stats/MonthlyReportScreen.kt`

---

### 14.2 报告分享

**现状：** 报告只能在APP内查看。

**实现方案：**
- 生成精美报告分享图（类似Spotify Wrapped风格）
- 10+精美卡片模板（极简/复古/水彩/杂志/手写体风格）
- 自动提取心情图标、天气图标、标签作为装饰
- 支持自定义背景色/字体
- 多页滑动卡片
- 一键分享到微信/朋友圈

**涉及文件：**
- 新建 `ui/stats/ReportShareRenderer.kt` — Canvas绘制分享图
- `ui/stats/MonthlyReportScreen.kt` — 新增分享按钮
- `ui/stats/AnnualReportScreen.kt` — 新增分享按钮

---

### 14.3 跨年对比

**现状：** 年报只展示当年数据。

**实现方案：**
- 今年 vs 去年对比视图
- 多年趋势图（如果有多于1年的数据）
- 对比维度：日记数、总字数、平均心情、常用标签变化

**涉及文件：**
- `ui/stats/AnnualReportScreen.kt` — 新增对比Tab
- `ui/stats/AnnualReportViewModel.kt` — 新增历史年份数据查询

---

## 十五、详情页 (DiaryDetail)

### 15.1 日记关联推荐

**现状：** 每篇日记独立，没有关联。

**实现方案：**
- "相关日记"区域：基于相同标签+相近时间+相似内容推荐
- 手动链接：用户可将两篇日记关联
- 链接关系可视化（时间线上的连线）

**数据模型：**
- EntryComment Entity已创建，可用于批注/反思
- 新建 `EntryLink` Entity：id, entryId1, entryId2, createdAt（手动关联）
- 或用 `linkedEntryIds: String?` 字段（JSON数组）

**涉及文件：**
- `ui/diary/DiaryDetailScreen.kt` — 新增相关日记Section
- `data/DiaryDao.kt` — 新增关联查询（同标签+时间邻近）
- 可能需要新Entity+迁移

---

## 十六、地图模块 (Map)

### 16.1 旅行路线

**现状：** 高德地图展示散点标记。

**实现方案：**
- 按时间顺序连接日记位置点，形成旅行路线
- 路线动画播放（从第一个点到最后一个点渐显）
- 旅行统计：去过N个城市、N个省、N个国家
- 路线颜色按时间渐变

**涉及文件：**
- `ui/map/DiaryMapScreen.kt` — 增强路线渲染
- 新建 `ui/map/TravelRouteRenderer.kt` — 路线绘制逻辑
- 高德SDK Polyline API

---

### 16.2 地点记忆

**现状：** 只展示点，无聚合分析。

**实现方案：**
- 地点聚类：同一位置的多篇日记聚合为一个气泡（数字标注）
- "你去过这个地方3次了"提示
- 常去地点排行
- 地点详情页：该位置的所有日记列表
- 地点-心情关联："在咖啡馆写的日记平均心情4.5"

**涉及文件：**
- `ui/map/DiaryMapScreen.kt` — 聚类渲染+点击展开
- `data/DiaryDao.kt` — 新增地点聚合+排行查询

---

### 16.3 热力图模式

**现状：** 只有标记点。

**实现方案：**
- 热力图层：颜色深浅表示日记密度
- 时间滑块：查看不同时期的活动范围
- 高德SDK HeatMap API

**涉及文件：**
- `ui/map/DiaryMapScreen.kt` — 新增热力图层+时间滑块
- 高德SDK HeatMapOverlay

---

## 十七、搜索系统

### 17.1 搜索历史持久化

**现状：** HomeViewModel中_recentSearches是内存MutableStateFlow<List<String>>，最多5条，重启丢失。

**实现方案：**
- 存储到SharedPreferences（简单方案）
- 最近10条搜索历史
- 搜索框下方显示历史记录列表
- 支持单条删除和清空

**涉及文件：**
- `ui/home/HomeViewModel.kt` — 改存储方式
- `AppPreferences.kt` — 新增searchHistory字段

---

### 17.2 高级筛选面板

**现状：** 只有文字搜索。

**实现方案：**
- 搜索框右侧筛选图标，点击弹出底部Sheet
- 筛选项（所有条件AND组合）：
  - 日期范围：开始-结束日期选择器
  - 心情等级：多选（1-6）
  - 天气类型：多选（晴/阴/雨/雪等）
  - 标签：多选
  - 字数范围：短篇(<500)/中篇(500-2000)/长篇(>2000)
  - 收藏/非收藏
- 筛选结果数量显示
- 一键清除所有筛选

**涉及文件：**
- 新建 `ui/home/SearchFilterSheet.kt` — 筛选底部Sheet
- `ui/home/HomeViewModel.kt` — 新增filterState + 组合查询
- `data/DiaryDao.kt` — 新增多条件组合查询方法
- `ui/home/HomeScreen.kt` — 搜索框集成筛选图标

---

### 17.3 搜索建议/自动补全

**现状：** 输入时无建议。

**实现方案：**
- 输入时显示：
  - 标签名补全（从TagDao.getAllTags()获取）
  - 地点名补全（从日记位置字段获取）
  - 热门搜索词（基于搜索历史频率）
- 建议列表在搜索框下方浮层显示
- 点击建议直接执行搜索

**涉及文件：**
- `ui/home/HomeScreen.kt` — 搜索框下方SuggestionsList
- `ui/home/HomeViewModel.kt` — 新增suggestions逻辑
- `data/TagDao.kt` — getAllTags()已有
- `data/DiaryDao.kt` — 新增获取所有不重复位置的查询

---

## 十八、连续记录系统

### 18.1 连续冻结

**现状：** StreakFreeze Entity已创建。StreakCalculator.computeStreakWithFreezes()已实现。断一天就归零。

**实现方案：**
- 每月赠送1次冻结机会
- 使用冻结后连续天数不中断
- 冻结次数可通过成就/挑战额外获得
- UI：连续天数旁显示冰块图标 + 剩余次数

**涉及文件：**
- `ui/home/WritingPromptCard.kt` — StreakInfoBar新增冻结图标
- `util/StreakCalculator.kt` — computeStreakWithFreezes()已实现
- 新建 `ui/home/FreezeDialog.kt` — 冻结使用确认弹窗

---

### 18.2 最长连续记录

**现状：** StreakCalculator.computeLongestStreak()已实现，返回最长天数+起止日期。

**实现方案：**
- 统计页显示"最长连续 X 天 (YYYY.MM.DD - YYYY.MM.DD)"
- "打破纪录"时触发庆祝动画

**涉及文件：**
- `ui/stats/StatsScreen.kt` — 新增最长连续卡片
- `ui/stats/StatsViewModel.kt` — 调用computeLongestStreak()

---

### 18.3 连续里程碑视觉

**现状：** 连续天数只是数字。

**实现方案：**
- StreakCalculator.detectStreakMilestone()已实现里程碑检测
- streakTier()已实现视觉等级
- UI里程碑效果：
  - 3天: 灰色火焰
  - 7天: 橙色火焰 + 小动画
  - 14天: 红色火焰
  - 30天: 金色火焰 + 徽章弹出
  - 50天: 金色+光效
  - 100天: 彩虹渐变 + 全屏庆祝动画
  - 200天: 彩虹+粒子效果
  - 365天: 皇冠图标 + 永久特殊标识

**涉及文件：**
- 新建 `ui/home/StreakCelebrationDialog.kt` — 庆祝动画弹窗
- `ui/home/HomeScreen.kt` — 里程碑检测触发

---

### 18.4 连续排行榜（本地）

**现状：** 没有排行榜。

**实现方案：**
- StreakCalculator.computeMonthlyLeaderboard()和computeYearlyBestStreak()已实现
- 统计页展示"本月最佳连续"、"本年最佳连续"

**涉及文件：**
- `ui/stats/StatsScreen.kt` — 新增排行榜Section
- `ui/stats/StatsViewModel.kt` — 调用已有计算函数

---

## 十九、标签系统 (Tag)

### 19.1 标签层级（父子标签）

**现状：** 扁平标签列表。Tag表已有parentId字段。TagDao.rootTags()/childTags()已实现。

**实现方案：**
- 支持父子关系：工作 > 会议、工作 > 项目A
- 标签管理页支持树形展示（展开/折叠父标签）
- 筛选父标签时自动包含所有子标签的日记
- 树形缩进展示（子标签缩进显示）

**涉及文件：**
- `ui/profile/TagManagementScreen.kt` — 改造为树形展示
- `data/TagDao.kt` — rootTags()/childTags()已实现

---

### 19.2 标签合并/去重

**现状：** 没有合并功能。TagDao.reassignTags()方法已存在。

**实现方案：**
- 检测相似标签："旅行" vs "旅游"、"工作" vs "Work"
- 相似度算法：编辑距离 + 包含关系 + 拼音相似
- 一键合并：选择保留哪个，其他标签的日记自动迁移
- 批量重命名

**涉及文件：**
- `ui/profile/TagManagementScreen.kt` — 新增合并UI
- 新建 `util/TagMergeHelper.kt` — 相似度算法
- `data/TagDao.kt` — reassignTags()已实现

---

### 19.3 标签颜色自动建议

**现状：** 用户手动选择颜色。

**实现方案：**
- 基于标签名语义推荐颜色：
  - 旅行/出行 → 橙色
  - 学习/教育 → 蓝色
  - 健康/运动 → 绿色
  - 工作/商务 → 灰色
  - 情感/心情 → 粉色
  - 创意/灵感 → 紫色
  - 美食/餐厅 → 红色
- 基于关联日记的心情推荐颜色

**涉及文件：**
- 新建 `util/TagColorSuggester.kt` — 语义映射表+推荐算法
- `ui/profile/TagManagementScreen.kt` — 新增"自动配色"按钮

---

### 19.4 AI智能标签推荐

**现状：** 手动添加。AppPreferences.autoTagSuggestion开关已有。

**实现方案：**
- 编辑器保存时AI根据内容推荐标签
- "你可能想添加：旅行、美食"
- 基于关键词匹配 + AI语义分析

**涉及文件：**
- `ui/editor/EditorViewModel.kt` — save时触发
- `ai/AiServiceManager.kt` — 新增tagSuggestPrompt
- (与7.3 AI自动标签共用逻辑)

---

### 19.5 标签看板视图

**现状：** 标签只用于筛选。

**实现方案：**
- 按标签分组的看板视图
- 每个标签一列，显示最近N篇该标签的日记
- 适合"工作日记"、"个人日记"分场景查看
- 可左右滑动切换标签列

**涉及文件：**
- 新建 `ui/tag/TagBoardScreen.kt` — 看板主界面
- 新建 `ui/tag/TagBoardViewModel.kt`
- 新建导航路由

---

## 二十、通知系统

### 20.1 通知类型丰富化

**现状：** 主要是成就通知和系统通知。

**缺失类型：**
- 时间胶囊到期通知
- 写作提醒通知
- 连续记录断裂提醒
- 每周/月度回顾通知
- 挑战到期/完成通知

**涉及文件：**
- `reminder/NotificationPreferencesManager.kt` — 新增通知类型
- `reminder/TodoReminderManager.kt` — 扩展通知触发

---

### 20.2 通知优先级

**现状：** 所有通知同等对待。

**实现方案：**
- 通知优先级（高/中/低）
- 高优先级通知可突破勿扰模式
- 通知分组（按类型分组展示）

**涉及文件：**
- `reminder/NotificationPreferencesManager.kt` — 通知渠道分级
- `data/NotificationDao.kt` — 已有

---

## 二十一、存储管理

### 21.1 孤立媒体清理

**现状：** StorageViewModel计算大小，但不检测孤立文件。

**实现方案：**
- 扫描 diary_media/ 目录中所有文件
- 对比 diary_images 表中的 mediaName
- 列出孤立文件（磁盘有但数据库无记录）
- 一键清理，预估可释放空间

**涉及文件：**
- `ui/storage/StorageScreen.kt` — 新增清理UI
- `ui/storage/StorageViewModel.kt` — 新增孤立检测逻辑

---

### 21.2 重复图片检测

**现状：** 没有。

**实现方案：**
- 基于文件哈希(MD5/SHA256)检测完全相同的图片
- 展示重复组，用户选择保留哪个
- 预估可释放空间

**涉及文件：**
- `ui/storage/StorageScreen.kt` — 新增检测UI
- 新建 `util/DuplicateImageDetector.kt` — 哈希比对逻辑

---

### 21.3 数据库优化

**现状：** 没有。

**实现方案：**
- VACUUM操作（压缩数据库文件）
- 数据库完整性检查
- 迁移历史清理

**涉及文件：**
- `ui/storage/StorageScreen.kt` — 新增优化按钮
- `data/DiaryDatabase.kt` — 新增vacuum方法

---

## 二十二、WebView编辑器

### 22.1 编辑器性能优化

**现状：** Quill.js Delta JSON存储在content字段，大文档可能卡顿。

**实现方案：**
- 图片延迟加载（IntersectionObserver懒加载）
- 长文档分段渲染
- 减少不必要的Delta更新

**涉及文件：**
- `assets/editor.html` — 懒加载+性能优化JS
- `assets/editor.css` — 优化样式渲染

---

### 22.2 代码块语法高亮

**现状：** Quill.js支持代码块但没有语法高亮。

**实现方案：**
- 集成Prism.js（轻量级语法高亮库）
- 支持语言选择：Python/JS/Kotlin/SQL/Java/HTML/CSS等
- 代码块主题跟随APP主题（浅色/深色）

**涉及文件：**
- `assets/editor.html` — 引入Prism.js CSS/JS
- `DiaryJsBridge.kt` — 代码块语言选择回调
- `ui/editor/EditorToolbar.kt` — 工具栏新增语言选择

---

### 22.3 表格支持

**现状：** Quill.js默认不支持表格。

**实现方案：**
- 集成quill-better-table插件
- 支持插入/编辑/删除表格
- 支持添加/删除行和列
- 表格样式跟随主题（边框颜色、背景色）

**涉及文件：**
- `assets/editor.html` — 引入quill-better-table JS/CSS
- `ui/editor/EditorToolbar.kt` — 新增表格操作按钮（插入表格/添加行/删除行等）

---

## 二十三、数据导出优化

### 23.1 优化现有导出模式

**现状：** DiaryExporter 723行，支持JSON/Markdown/HTML/PNG。

**优化方向：**
- HTML导出CSS美化（更好的排版、字体、间距）
- PNG导出排版优化（更精美的Canvas绘制）
- Markdown导出格式规范化
- 导出进度显示

**涉及文件：**
- `data/DiaryExporter.kt` — 优化各格式导出质量
- `data/DiaryExportUtils.kt` — Base64内联图片清理

---

## 二十四、数据层缺口

### 24.1 DiaryEntry 缺失字段（按需添加）

**根据后续功能需要，可能需要：**
- `mediaType: String?` — 来源（手动/语音/Widget）
- `linkedEntryIds: String?` — 关联日记ID（JSON数组，用于日记关联功能）

### 24.2 缺失DAO查询（按需添加）

- 按情绪范围查询：`WHERE moodLevel BETWEEN :min AND :max`
- 按字数范围查询：`WHERE LENGTH(plainText) BETWEEN :min AND :max`
- 按时间段聚合：`GROUP BY strftime('%Y-%m', createdAt/1000, 'unixepoch')`
- 按日期聚合心情（用于热力图）
- 地点聚合查询（用于地图聚类）

### 24.3 缺失Entity（按需添加）

- `Challenge` — 每周/月挑战
- `EntryLink` — 手动日记关联
- 搜索历史可用SharedPreferences替代

---

## 二十五、架构优化（长期）

### 25.1 EditorViewModel拆分

**现状：** 432行，职责过多：草稿管理、日记保存、标签管理、AI标题建议、写作计时、媒体同步、Base64清理、预设标签种子初始化。

**拆分方案：**
- `EditorSaveManager` — 保存/草稿逻辑
- `DraftManager` — 草稿自动保存
- `WritingTimer` — 写作计时
- `EditorViewModel` — 精简为协调层

### 25.2 大Screen拆分

**现状行数：**
- StatsScreen: 2050行
- EditorScreen: 1815行
- TodoScreen: 1606行
- HomeScreen: 1504行
- TimelineScreen: 1405行
- AnnualReportScreen: 1322行
- ProfileScreen: 1230行

**拆分方案：** 每个大Screen拆为 Header + Body sections(3-5个) + Dialogs + State holders

### 25.3 Hilt依赖注入

**现状：** ViewModels通过getApplication<DiaryApplication>()直接访问数据库。

**方案：** 引入Hilt，所有ViewModel通过@Inject获取依赖。

### 25.4 测试补充

**现状：** 214源文件22测试文件，覆盖率~10%。
**目标：** 核心路径覆盖率>60%（备份、导入导出、成就、StreakCalculator、搜索）。

---

## 二十六、性能优化（详细）

### 26.1 成就全量加载优化
- `AchievementRepository.checkAndUnlock()`调用`diaryDao.getAllPreviews().first()`
- 改为SQL聚合查询：`SELECT tagId, COUNT(*) FROM diary_tags GROUP BY tagId`

### 26.2 备份全量加载优化
- `buildBackupJson()`全量加载所有表
- 大表分批处理：每批100条，用LIMIT/OFFSET

### 26.3 Paging 3
- 引入`androidx.paging3`依赖
- 所有LazyColumn改用`LazyPagingItems`
- DAO返回`PagingSource`而非`Flow<List<T>>`

### 26.4 Room FTS
- 新建FTS虚拟表`diary_entries_fts`
- 迁移时从diary_entries复制数据
- 搜索查询改用`MATCH`替代`LIKE`

### 26.5 OkHttp
- 引入`okhttp3`依赖
- BaseHttpProvider改用OkHttpClient
- 配置连接池、超时、拦截器

### 26.6 图片懒加载
- Coil已引入(2.5.0)
- 配置ImageLoader + memoryCache(maxSize=50MB) + diskCache(maxSize=500MB)
- AsyncImage使用placeholder+error+fallback

### 26.7 SharedPreferences → DataStore
- 当前8+文件53+调用
- 迁移到Preferences DataStore（异步、线程安全）
- 或至少合并为2-3个Prefs文件

---
最后更新：2026-06-26
