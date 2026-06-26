# 执行指令文件（AI开发用）

> **用法**：新对话时告诉AI "读取 docs/EXECUTION-BRIEF.md，开始执行 Phase X"
> **详细需求**：docs/FINAL-REQUIREMENTS.md
> **完整进度**：docs/PROJECT-PROGRESS.md
> **对照表**：docs/REQUIREMENTS-CROSS-REFERENCE.md

---

## 项目信息

- 项目路径: C:\Users\陈仕杰\Desktop\DiaryApp
- 分支: experiment/v2-redesign
- 版本: v2.69.0-experimental (versionCode 26900)
- 数据库: Room v34, MIGRATION_33_34
- Kotlin源文件: 188个, 53,985行
- 编辑器: Quill.js WebView (editor.html + DiaryJsBridge)
- AI: 3个Provider (ModelScope, Agnes, Deepseek), 基于HttpURLConnection
- 地图: 高德3D地图 SDK
- 签名: debug keystore

---

## 关键约束（不可违反，每条都有原因）

1. **不压缩图片** — 用户担心质量下降，宁可占空间也不要压缩
2. **不改主题** — 用户喜欢当前14主题7色系，不要改任何主题逻辑
3. **不加版本历史** — 编辑直接覆盖，不做版本对比/diff/恢复
4. **单用户本地APP** — 不联网，不做云同步，不做社交，不做多用户
5. **AI问答替代静态模板** — 不要模板选择器UI，改为AI通过提问引导写作
6. **华为健康搁置** — Health Connect联系困难，健康模块不动
7. **已删除宠物/小岛/称号/模板** — Phase 1已全部删除，代码中不应有残留引用
8. **无视称号代码** — TitleManager/TitleChecker/TitleDao等全部忽略，只保留成就系统(AchievementRepository)
9. **不做PDF/DOCX导出** — 太复杂，只优化现有JSON/MD/HTML/PNG
10. **不做键盘快捷键** — 手机用户，不需要Ctrl+B等
11. **不做云同步/单篇加密** — 备份安全模块已删除
12. **不做Widget** — 已删除，不创建心情签到/日记预览等Widget
13. **不做无障碍/国际化** — 当前不做TalkBack和多语言
14. **不做应用锁增强** — 保持现有生物识别+4位PIN，不做图案锁/入侵检测

---

## 已完成工作（Phase 1-5，代码已验证）

### Phase 1: 死代码清理 ✅
已删除的文件（确认不存在）：
- 宠物系统: PetDao, PetModels, PetPersonality, PetStateMachine, PetAiGenerator, PetMemoryRepository
- 小岛系统: IslandDao, IslandModels, IslandRepository
- 称号系统: TitleDao, TitleChecker, TitleManager, TitleModels, TitleSeedData
- 模板系统: TemplateManager, DiaryTemplate
- 其他: AppContainer, AchievementManager, FeedbackGenerator, MoodEnvironmentMapper, EditorDialogs, EditorUtils

### Phase 2: 架构优化 ✅
- Repository层: DiaryEntryRepository(97行, 封装DiaryDao+TagDao+MediaDao+TrashDao), TodoRepository(39行, 封装TodoDao)
- DAO拆分: TagDao(132行/25+查询), TodoDao(121行/30+查询), MediaDao(37行), NotificationDao(55行), ChatDao(59行), TrashDao(34行), CountDownDao(35行), CapsuleDao(34行)
- ProGuard启用: isMinifyEnabled=true, isShrinkResources=true
- 启动优化: backfillDiaryImages用SharedPreferences标记替代COUNT查询

### Phase 3: 数据层增强 ✅
- WritingGoal Entity (writing_goals表): id, type(weekly_entries/monthly_entries/monthly_words), targetValue, currentValue, periodStart, enabled
- StreakFreeze Entity (streak_freezes表): id, usedAt, streakAtUse
- MoodCheckin Entity (mood_checkins表): id, moodLevel(1-6), note, createdAt
- EntryComment Entity (entry_comments表): id, entryId, content, createdAt
- Tag层级: parentId, usageCount字段
- MIGRATION_33_34: 创建4新表+Tag列

### Phase 4: 设置页+首页增强 ✅
- AppPreferences (112行): 17项设置，5分类
  - 写作: defaultMoodLevel, defaultWeather, autoSaveInterval, defaultSortBy, defaultCalendarMode
  - 通知: writingReminderEnabled/Hour/Minute, streakBreakReminder, weatherReminder, dailyReviewPush, doNotDisturb
  - 数据: trashRetentionDays, autoCleanOrphanMedia
  - 编辑器: editorFontSize, editorToolbarCompact, autoTagSuggestion
  - 隐私: locationRecordingEnabled, aiDataUsageConsent, screenshotProtection
- SettingsScreen重写 (707行): 8个分区全部接入AppPreferences
- HomeViewModel增强 (+238行): HomeGreeting(时间问候), HomeStreakInfo(连续记录), WritingPrompt(25个提示)
- WritingPromptCard (81行): 智能问候+GlassCard提示卡+StreakInfoBar(火焰图标+颜色分级)
- WeatherDetailSheet (523行): 天气详情全页

### Phase 5: 写作目标追踪 ✅
- StatsScreen新增WritingGoalSection (+405行): 目标设置UI+进度环
- StatsViewModel新增goal逻辑 (+105行): insertGoal, updateGoal, getActiveGoals
- StreakCalculator (154行, 7个函数):
  - computeStreak: 基础连续天数
  - computeStreakWithFreezes: 冻结支持
  - computeLongestStreak: 历史最长连续+起止日期
  - detectStreakMilestone: 里程碑检测(3/7/14/30/50/100/200/365天)
  - streakTier: 视觉等级(NONE/BRONZE/SILVER/GOLD/DIAMOND/LEGENDARY)
  - computeMonthlyLeaderboard: 月度排行榜
  - computeYearlyBestStreak: 年度最佳

**注意：Hilt、OkHttp、Paging 3 均未引入（build.gradle中无相关依赖）。**

---

## 后续实施计划（Phase 6-17，按优先级排序）

---

### Phase 6: 性能优化（P0，最优先）

**6.1 成就全量加载优化**
- 现状: AchievementRepository.checkAndUnlock()调用diaryDao.getAllPreviews().first()，将全部日记加载到内存
- 改动: 改为数据库聚合查询，用SQL COUNT/GROUP BY替代内存过滤
- 文件: AchievementRepository.kt, DiaryDao.kt(新增聚合查询)
- 已有基础: AchievementRepository已存在

**6.2 备份全量加载优化**
- 现状: buildBackupJson()全量加载日记、标签、待办、胶囊、回收站等
- 改动: 大数据量表分批处理（每批100条）
- 文件: BackupManager.kt
- 已有基础: 日记已分批，其他表需改造

**6.3 列表分页 Paging 3**
- 现状: getAllPreviews()等Flow返回全部行，长期用户数千篇日记时内存压力大
- 改动: 引入androidx.paging3依赖，所有列表Screen改用LazyPagingItems
- 文件: build.gradle.kts(加依赖), HomeViewModel, StatsViewModel, 所有LazyColumn
- 需新增: DiaryDao中返回PagingSource的查询

**6.4 Room FTS全文搜索**
- 现状: searchEntries()用LIKE '%query%'，无法利用索引
- 改动: 新增FTS虚拟表(diary_entries_fts)，用FTS4/FTS5替代LIKE
- 文件: DiaryDatabase.kt(新表+迁移), DiaryDao.kt(新查询), build.gradle.kts(FTS依赖)

**6.5 OkHttp替换HttpURLConnection**
- 现状: BaseHttpProvider使用原生HttpURLConnection，无连接池/超时控制
- 改动: 引入okhttp3依赖，BaseHttpProvider改用OkHttpClient
- 文件: build.gradle.kts(加依赖), BaseHttpProvider.kt

**6.6 图片懒加载和缓存**
- 现状: Coil已引入(2.5.0)但未配置缓存策略
- 改动: 配置ImageLoader + memoryCache + diskCache
- 文件: DiaryApplication.kt(配置Coil), 各Screen的AsyncImage

**6.7 SharedPreferences → DataStore**
- 现状: 8+个Prefs文件名, 53+次调用, BiometricHelper每次方法调用都重新getSharedPreferences
- 改动: 统一迁移到Preferences DataStore，或至少合并为2-3个Prefs
- 文件: AppPreferences.kt, BiometricHelper.kt, ThemePreferences, 各处SharedPreferences调用

---

### Phase 7: 首页增强（P0）

**7.1 心情日历热力图**
- 现状: CalendarView 873行，日历只显示有/无日记的圆点
- 改动: 日期格子按心情等级着色（1=深蓝, 2=浅蓝, 3=灰, 4=浅绿, 5=绿, 6=金）
- 文件: CalendarView.kt(改造日期Cell), 需要日记的心情数据JOIN查询
- UI: 每个日期格子背景色=moodColor[moodLevel]，无日记=透明

**7.2 今日写作提示卡常驻**
- 现状: WritingPromptCard已有81行，AI洞察卡片35%概率随机出现
- 改动: 改为首页常驻，每天更换提示。类型：回顾型/引导型/季节型/情绪型
- 文件: WritingPromptCard.kt(增强), HomeViewModel.kt(新增prompt选择逻辑)
- 提示库: 已有25个WritingPrompt，需扩充到50+并分类

**7.3 写作连续天数首页展示**
- 现状: StreakCalculator已实现全部计算逻辑(7个函数)，但首页只有WritingPromptCard中的简单数字
- 改动: 首页日历上方显示火焰图标+连续天数，7天橙色/30天金色/100天彩虹色
- 文件: HomeScreen.kt(新增StreakDisplay组件), 颜色分级逻辑已在streakTier()中
- 配合: haptic反馈(连续天数变化时震动)

**7.4 智能问候语增强**
- 现状: HomeGreeting已有基础（时间段问候）
- 改动: 加入天气+最近心情+写作习惯信息。如"周三下午好，外面在下雨，适合写点什么。你已经连续写了5天了。"
- 文件: HomeViewModel.kt(增强HomeGreeting), 已有HomeStreakInfo数据

**7.5 首页布局自定义**
- 现状: 固定布局（日历→快捷入口→AI卡片→日记列表）
- 改动: 卡片顺序可拖拽调整、卡片可显示/隐藏、紧凑/宽松模式
- 文件: HomeScreen.kt(较大改动), AppPreferences.kt(新增布局偏好)
- 实现: 可用LazyColumn + reorderable modifier

---

### Phase 8: 搜索增强（P0）

**8.1 搜索历史持久化**
- 现状: _recentSearches是内存MutableStateFlow<List<String>>，最多5条，重启丢失
- 改动: 存储到SharedPreferences或Room表
- 文件: HomeViewModel.kt(改存储方式), AppPreferences.kt(新增searchHistory字段)
- 保留: 最近10条，显示在搜索框下方

**8.2 高级筛选面板**
- 现状: 只有文字搜索
- 改动: 新建筛选Sheet，支持组合条件AND查询
- 筛选项: 日期范围、心情等级多选(1-6)、天气类型多选、标签多选、字数范围(短/中/长)、收藏/非收藏
- 文件: 新建SearchFilterSheet.kt, HomeViewModel.kt(新增filterState), DiaryDao.kt(新增组合查询)
- UI: 搜索框右侧筛选图标，点击弹出底部Sheet

**8.3 搜索建议/自动补全**
- 现状: 输入时无建议
- 改动: 输入时显示标签名+地点名+热门搜索词补全
- 文件: HomeScreen.kt(搜索框下方SuggestionsList), HomeViewModel.kt(新增suggestions逻辑)
- 数据源: TagDao.getAllTags()获取标签名, 日记位置字段获取地点名

---

### Phase 9: 编辑器增强（P1）

**9.1 语音输入**
- 现状: 完全没有语音输入
- 改动: Android SpeechRecognizer API → 文字 → 插入编辑器光标位置
- 文件: EditorScreen.kt(工具栏加麦克风按钮), EditorToolbar.kt(新增VoiceInputButton), WebView JS层(接收文字插入)
- UI: 长按录音，松手转文字。录音中显示波形动画
- 权限: RECORD_AUDIO

**9.2 AI问答引导替代模板**
- 现状: 不要静态模板选择器
- 改动: 新建日记时，AI通过对话引导用户写作。"今天发生了什么？""你的心情如何？""想记录哪个片段？"
- 文件: 新建AiWritingGuideDialog.kt, EditorViewModel.kt(新增引导逻辑), AiServiceManager(新增引导Prompt)
- 流程: 点击新建→弹出AI问答→3-5轮对话→自动生成日记框架→进入编辑器

**9.3 编辑器附件支持**
- 现状: 只支持图片
- 改动: 支持语音备忘录(.m4a)和文件附件(PDF等)
- 文件: DiaryImage表扩展mediaType字段(image/audio/file), EditorToolbar.kt(新增附件按钮), 新建MediaPicker.kt
- 数据库: MIGRATION_34_35新增mediaType列

**9.4 编辑器手势**
- 现状: 没有手势操作
- 改动: 双指缩放文字、左滑返回(带保存确认)、下拉保存
- 文件: EditorScreen.kt(手势处理), WebView层(缩放支持)
- 实现: modifier.pointerInput + detectTransformGestures

**9.5 Prism.js代码高亮**
- 现状: Quill.js支持代码块但无语法高亮
- 改动: editor.html引入Prism.js CDN，支持多语言，主题跟随APP
- 文件: editor.html(引入Prism.js CSS/JS), DiaryJsBridge.kt(代码块语言选择)
- 主题: 浅色→PrismOkaidia, 深色→PrismTomorrow

**9.6 表格支持**
- 现状: Quill.js默认不支持表格
- 改动: 引入quill-better-table插件
- 文件: editor.html(引入插件JS/CSS), EditorToolbar.kt(新增表格按钮), 工具栏增加插入表格/行列操作
- 样式: 表格边框颜色跟随主题

**9.7 EditorViewModel拆分**
- 现状: 432行，职责过多（草稿管理/日记保存/标签管理/AI标题/写作计时/媒体同步/Base64清理/预设标签种子）
- 改动: 拆分为EditorSaveManager, DraftManager, WritingTimer等子管理器
- 文件: EditorViewModel.kt(拆分), 新建3-4个Manager类

---

### Phase 10: AI增强（P1）

**10.1 AI自动标签**
- 现状: 标签全靠手动添加
- 改动: 保存日记时AI分析内容，建议标签。"你可能想添加：旅行、美食"
- 文件: EditorViewModel.kt(save时触发), AiServiceManager.kt(新增tagSuggest Prompt)
- UI: 保存前弹出标签建议Chip列表，用户点选采纳
- AppPreferences.autoTagSuggestion已有开关

**10.2 语义搜索**
- 现状: SQL LIKE匹配
- 改动: "找找我关于旅行的日记" → AI理解语义后返回关键词+情感+时间范围组合 → 再查数据库
- 文件: HomeViewModel.kt(搜索逻辑), AiServiceManager.kt(新增searchParse Prompt)
- 实现: AI将自然语言转为结构化查询参数，再用DAO精确查询

**10.3 写作风格分析**
- 现状: 没有
- 改动: 分析用户写作风格特征（简洁/详细、理性/感性、叙事/反思），风格演变趋势图
- 文件: StatsScreen.kt(新增风格分析Section), StatsViewModel.kt, AiServiceManager.kt(新增styleAnalysis Prompt)
- 展示: 统计页新增"写作风格"卡片，显示风格标签+趋势折线

**10.4 周报生成**
- 现状: 有月报和年报，没有周报
- 改动: 每周日晚自动生成周报。内容：本周日记数、总字数、心情走势、高频标签、写作天数
- 文件: 新建WeeklyReportScreen.kt, WeeklyReportViewModel.kt, MonthlyReportScreen.kt可参考
- 触发: WorkManager定时任务 或 打开APP时检测

---

### Phase 11: 连续记录系统（P1）

**说明: StreakCalculator已实现全部计算逻辑，此Phase只需做UI接入。**

**11.1 连续冻结UI**
- 已有: StreakFreeze Entity已创建, computeStreakWithFreezes()已实现, computeStreakWithTodayFreeze()已实现
- 需做: UI展示冻结次数+使用按钮。连续天数旁显示冰块图标+剩余次数
- 文件: WritingPromptCard.kt(增强StreakInfoBar), 或新建StreakSection组件
- 逻辑: 每月赠送1次冻结，可通过成就额外获得

**11.2 最长连续记录展示**
- 已有: computeLongestStreak()返回最长天数+起止日期
- 需做: 统计页显示"最长连续 X 天 (YYYY.MM.DD - YYYY.MM.DD)"
- 文件: StatsScreen.kt(新增最长连续卡片)

**11.3 连续里程碑视觉庆祝**
- 已有: detectStreakMilestone()检测3/7/14/30/50/100/200/365天, streakTier()返回等级
- 需做: 达到里程碑时弹出庆祝动画。7天→橙色火焰, 30天→金色+徽章, 100天→彩虹渐变+全屏, 365天→皇冠
- 文件: 新建StreakCelebrationDialog.kt, HomeScreen.kt(触发检测)

**11.4 月/年排行榜展示**
- 已有: computeMonthlyLeaderboard()和computeYearlyBestStreak()已实现
- 需做: 统计页展示"本月最佳连续"、"本年最佳连续"
- 文件: StatsScreen.kt(新增排行榜Section)

---

### Phase 12: 标签系统增强（P1-P2）

**12.1 标签层级UI**
- 已有: Tag.parentId字段, TagDao.rootTags()/childTags()查询
- 需做: 标签管理页支持树形展示，展开/折叠父标签
- 文件: TagManagementScreen.kt(改造为树形), TagDao.kt(已有查询)

**12.2 标签合并/去重**
- 已有: TagDao.reassignTags()方法
- 需做: 检测相似标签（"旅行"vs"旅游"、"工作"vs"Work"），一键合并
- 文件: TagManagementScreen.kt(新增合并UI), 新建TagMergeHelper.kt(相似度算法)

**12.3 标签颜色自动建议**
- 改动: 基于标签名语义推荐颜色（"旅行"→橙色, "学习"→蓝色, "健康"→绿色）
- 文件: 新建TagColorSuggester.kt(语义映射表), TagManagementScreen.kt(新增建议按钮)

**12.4 AI标签推荐**
- 已有: AppPreferences.autoTagSuggestion开关
- 改动: 编辑器保存时AI分析内容推荐标签（与10.1共用逻辑）
- 文件: (同10.1)

**12.5 标签看板视图**
- 改动: 按标签分组的看板视图，每个标签一列，显示最近日记
- 文件: 新建TagBoardScreen.kt, 导航路由新增

---

### Phase 13: 成就系统优化（P1）

**13.1 每周/月挑战**
- 现状: 只有永久成就
- 改动: 每周挑战("本周写3篇"/"尝试2种不同天气")、每月挑战("写一篇超过2000字"/"使用5个新标签")
- 文件: AchievementRepository.kt(新增挑战逻辑), 新建ChallengeManager.kt, 首页/成就页展示挑战卡片

**13.2 进度可视化增强**
- 现状: 进度条+百分比
- 改动: 成就树/成就地图(按类别展示解锁进度), 稀有度标识(解锁率), 解锁时生成分享卡片
- 文件: AchievementDetailScreen.kt(增强)

---

### Phase 14: 地图模块增强（P2）

**14.1 旅行路线**
- 现状: 高德地图展示散点
- 改动: 按时间顺序连接日记位置点形成路线，路线动画播放，旅行统计(N个城市/省/国家)
- 文件: DiaryMapScreen.kt(增强), 新建TravelRouteRenderer.kt

**14.2 地点聚类+常去排行**
- 改动: 同一位置多篇日记聚合为气泡(数字标注)，地点详情页显示该位置所有日记，常去地点排行
- 文件: DiaryMapScreen.kt(增强), DiaryDao.kt(新增地点聚合查询)

**14.3 热力图模式**
- 改动: 热力图层(颜色深浅表示日记密度)，时间滑块查看不同时期活动范围
- 文件: DiaryMapScreen.kt(新增HeatMap图层), 高德SDK热力图API

---

### Phase 15: 月报/年报增强（P2）

**15.1 周报**
- 已有: 月报/年报框架可参考
- 改动: 每周日晚自动生成，内容：日记数、总字数、心情走势、高频标签、写作天数
- 文件: 新建WeeklyReportScreen.kt, WeeklyReportViewModel.kt

**15.2 报告分享图**
- 改动: 生成精美报告分享图(Spotify Wrapped风格)，多页滑动卡片，一键分享
- 文件: 新建ReportShareRenderer.kt(Canvas绘制), MonthlyReportScreen.kt/AnnualReportScreen.kt(新增分享按钮)

**15.3 跨年对比**
- 改动: 今年vs去年对比视图，多年趋势图
- 文件: AnnualReportScreen.kt(新增对比Tab), AnnualReportViewModel.kt(新增历史数据查询)

---

### Phase 16: 其余项（P2-P3）

**16.1 日记关联推荐**
- 改动: 详情页底部显示"相关日记"：基于相同标签+相近时间+相似内容推荐
- 文件: DiaryDetailScreen.kt(新增相关日记Section), DiaryDao.kt(新增关联查询)

**16.2 导出格式优化**
- 已有: DiaryExporter 723行，支持JSON/Markdown/HTML/PNG
- 改动: 优化导出质量、HTML导出样式美化、PNG导出排版优化
- 文件: DiaryExporter.kt(优化), DiaryExportUtils.kt

**16.3 孤立媒体清理**
- 已有: StorageViewModel已有基础
- 改动: 扫描diary_media/目录对比diary_images表，列出孤立文件，一键清理
- 文件: StorageScreen.kt(新增清理UI), StorageViewModel.kt(新增清理逻辑)

**16.4 重复图片检测**
- 改动: 基于文件哈希检测完全相同的图片，展示重复组，用户选择保留
- 文件: StorageScreen.kt(新增检测UI), 新建DuplicateImageDetector.kt

**16.5 DB VACUUM**
- 改动: 数据库压缩+完整性检查
- 文件: StorageScreen.kt(新增按钮), DiaryDatabase.kt(新增vacuum方法)

**16.6 通知深度优化**
- 已有: 智能提醒+每日回顾逻辑在AppPreferences中
- 改动: 实现基于写作习惯的智能时间检测、连续断裂温柔提醒
- 文件: NotificationPreferencesManager.kt(增强), TodoReminderManager.kt(增强)

**16.7 待办最简版**
- 已有: TodoDao(121行/30+查询), TodoRepository(39行)
- 改动: 简洁精致的待办列表UI，支持勾选完成、添加、分类
- 文件: TodoScreen.kt(已1606行，需精简重构), TodoViewModel.kt

**16.8 Bug修复**
- 根据实际测试发现的Bug逐一修复

---

### Phase 17: 架构优化（P3，长期）

**17.1 EditorViewModel拆分**
- 现状: 432行，职责过多
- 改动: 拆为EditorSaveManager, DraftManager, WritingTimer等
- (同9.7)

**17.2 大Screen拆分**
- 现状: EditorScreen 1815行, StatsScreen 2050行, TodoScreen 1606行, HomeScreen 1504行等
- 改动: 每个大Screen拆为3-5个子组件(Header/Body/Dialogs)

**17.3 Hilt依赖注入**
- 现状: ViewModels通过getApplication<DiaryApplication>()直接访问数据库
- 改动: 引入Hilt，替换手动DI
- 文件: build.gradle.kts(加Hilt依赖), 所有ViewModel, DiaryApplication

**17.4 测试补充**
- 现状: 214源文件22测试文件，覆盖率~10%
- 改动: 核心路径覆盖>60%（备份、导入导出、成就、StreakCalculator、搜索）

---

## 文件结构快速参考

```
app/src/main/java/com/diary/app/
├── ai/                    # AI系统 (AgnesProvider, DeepseekProvider, AiServiceManager...)
├── biometric/             # 生物识别 (BiometricHelper)
├── data/                  # 数据层
│   ├── repository/        # Repository (DiaryEntryRepository, TodoRepository)
│   ├── Achievement.kt, AchievementDao.kt, AchievementModels.kt, AchievementRepository.kt
│   ├── DiaryDao.kt        # 主DAO (仍较大，待继续拆分)
│   ├── DiaryDatabase.kt   # 数据库+迁移
│   ├── DiaryExporter.kt   # 导出 (JSON/MD/HTML/PNG)
│   ├── DiaryImporter.kt   # 导入
│   ├── Tag.kt, TagDao.kt  # 标签 (含层级/合并)
│   ├── TodoDao.kt, TodoRepository.kt  # 待办
│   ├── WritingGoal.kt, StreakFreeze.kt, MoodCheckin.kt, EntryComment.kt  # 新Entity
│   └── ...
├── reminder/              # 通知提醒
├── ui/                    # UI层
│   ├── achievement/       # 成就
│   ├── backup/            # 备份
│   ├── editor/            # 编辑器 (EditorScreen 1815行, EditorToolbar 900行)
│   ├── home/              # 首页 (HomeScreen 1504行, WritingPromptCard)
│   ├── stats/             # 统计 (StatsScreen 2050行, StatsViewModel)
│   ├── settings/          # 设置 (SettingsScreen 707行, AppPreferences)
│   ├── components/        # 通用组件
│   └── ...
├── util/                  # 工具类 (StreakCalculator等)
└── widget/                # Widget (已废弃)
```

---

## 每次开发的推荐流程

1. 读取本文件（EXECUTION-BRIEF.md）获取当前Phase任务
2. 读取相关源文件了解现有代码
3. 实现改动，保持项目现有风格
4. 每个Phase完成后git commit
5. 如需查完整需求，读FINAL-REQUIREMENTS.md对应章节
6. 如需查某个功能是否确认过，读REQUIREMENTS-CROSS-REFERENCE.md

---
最后更新：2026-06-26
