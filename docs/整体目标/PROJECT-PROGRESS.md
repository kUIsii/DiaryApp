# 项目进度报告（2026-06-26 最终版）

## 当前状态

| 指标 | 值 |
|------|-----|
| 分支 | experiment/v2-redesign |
| 版本 | v2.69.0-experimental (versionCode 26900) |
| 最新提交 | `310d8fc0` Phase 5 - WritingGoal tracking UI |
| Kotlin源文件 | 188个 |
| 总代码行数 | 53,985行 |
| 数据库版本 | Room v34, MIGRATION_33_34 |
| ProGuard | 已启用 (isMinifyEnabled=true) |

---

## 已完成工作（Phase 1-5，代码已验证）

### Phase 1: 死代码清理 ✅

删除的文件（全部已验证不存在）：

| 文件 | 说明 |
|------|------|
| PetDao.kt, PetModels.kt, PetPersonality.kt, PetStateMachine.kt, PetAiGenerator.kt, PetMemoryRepository.kt | 宠物系统 |
| IslandDao.kt, IslandModels.kt, IslandRepository.kt | 小岛系统 |
| TitleDao.kt, TitleChecker.kt, TitleManager.kt, TitleModels.kt, TitleSeedData.kt | 称号系统 |
| TemplateManager.kt, DiaryTemplate.kt | 模板系统 |
| AppContainer.kt | 死代码 |
| AchievementManager.kt | 与AchievementRepository重复 |
| FeedbackGenerator.kt | 死代码 |
| MoodEnvironmentMapper.kt | 死代码 |
| EditorDialogs.kt, EditorUtils.kt | 编辑器子文件 |

### Phase 2: 架构优化 ✅

| 改动 | 文件 | 行数 | 状态 |
|------|------|------|------|
| Repository层 | DiaryEntryRepository.kt | 97行 | 封装DiaryDao+TagDao+MediaDao+TrashDao |
| Repository层 | TodoRepository.kt | 39行 | 封装TodoDao |
| DAO拆分 | TagDao.kt | 132行 | 25+查询方法，含层级/合并/统计 |
| DAO拆分 | TodoDao.kt | 121行 | 30+查询方法，全生命周期 |
| DAO拆分 | MediaDao.kt | 37行 | 图片CRUD+统计 |
| DAO拆分 | NotificationDao.kt | 55行 | 通知CRUD |
| DAO拆分 | ChatDao.kt | 59行 | 聊天CRUD |
| DAO拆分 | TrashDao.kt | 34行 | 回收站操作 |
| DAO拆分 | CountDownDao.kt | 35行 | 倒数日操作 |
| DAO拆分 | CapsuleDao.kt | 34行 | 时间胶囊操作 |
| ProGuard启用 | build.gradle.kts | - | isMinifyEnabled=true, isShrinkResources=true |
| 启动优化 | DiaryApplication.kt | - | backfillDiaryImages用SharedPreferences标记 |

**注意：Hilt依赖注入、OkHttp替换、Paging 3 均未实施（build.gradle中无相关依赖）。**

### Phase 3: 数据层增强 ✅

| 改动 | 文件 | 说明 |
|------|------|------|
| WritingGoal Entity | WritingGoal.kt (28行) | writing_goals表：type/targetValue/currentValue/periodStart/enabled |
| StreakFreeze Entity | StreakFreeze.kt (21行) | streak_freezes表：usedAt/streakAtUse |
| MoodCheckin Entity | MoodCheckin.kt (23行) | mood_checkins表：moodLevel/note/createdAt |
| EntryComment Entity | EntryComment.kt (22行) | entry_comments表：entryId/content/createdAt |
| Tag层级 | Tag.kt | parentId, usageCount字段新增 |
| 数据库迁移 | DiaryDatabase.kt | MIGRATION_33_34 创建4新表+Tag列 |

### Phase 4: 设置页 + 首页增强 ✅

| 改动 | 文件 | 行数 | 说明 |
|------|------|------|------|
| 统一偏好管理 | AppPreferences.kt | 112行 | 17项设置，5分类（写作/通知/数据/编辑器/隐私） |
| 设置页重写 | SettingsScreen.kt | 707行 | 8个分区，17项设置全部接入UI |
| 智能问候 | HomeViewModel.kt | +238行 | HomeGreeting(时间问候), HomeStreakInfo, WritingPrompt(25个提示) |
| 写作提示卡 | WritingPromptCard.kt | 81行 | GlassCard提示+换一个按钮+StreakInfoBar |
| 天气详情 | WeatherDetailSheet.kt | 523行 | 天气详情全页展示 |
| 首页集成 | HomeScreen.kt | +14行 | WritingPromptCard插入LazyColumn |

**AppPreferences 17项设置清单：**
- 写作：defaultMoodLevel, defaultWeather, autoSaveInterval, defaultSortBy, defaultCalendarMode
- 通知：writingReminderEnabled/Hour/Minute, streakBreakReminder, weatherReminder, dailyReviewPush, doNotDisturb
- 数据：trashRetentionDays, autoCleanOrphanMedia
- 编辑器：editorFontSize, editorToolbarCompact, autoTagSuggestion
- 隐私：locationRecordingEnabled, aiDataUsageConsent, screenshotProtection

### Phase 5: 写作目标追踪 ✅

| 改动 | 文件 | 说明 |
|------|------|------|
| 目标UI | StatsScreen.kt (+405行) | WritingGoalSection组件，目标设置+进度环 |
| 目标逻辑 | StatsViewModel.kt (+105行) | WritingGoalGoalState, insertGoal, updateGoal, getActiveGoals |
| 连续记录工具 | StreakCalculator.kt (154行) | 7个函数：computeStreak, computeStreakWithFreezes, computeLongestStreak, detectStreakMilestone, streakTier, computeMonthlyLeaderboard, computeYearlyBestStreak |

**StreakCalculator已实现的能力（仅需UI接入）：**
- 基础连续天数计算
- 冻结支持的连续计算
- 历史最长连续+起止日期
- 里程碑检测（3/7/14/30/50/100/200/365天）
- 视觉等级（NONE/BRONZE/SILVER/GOLD/DIAMOND/LEGENDARY）
- 月度/年度排行榜

---

## 完整后续工作清单

> 基于 `FINAL-REQUIREMENTS.md` 中用户确认的所有需求，按实施阶段划分。
> 每个Phase预估工作量，标注已有代码基础的任务。

### Phase 6: 性能优化（P0，优先做）

| # | 任务 | 难度 | 已有基础 |
|---|------|------|---------|
| 6.1 | 成就全量加载优化 → 数据库聚合查询 | 中 | AchievementRepository已有 |
| 6.2 | 备份全量加载优化 → 分批处理 | 中 | BackupManager已有 |
| 6.3 | 列表分页 Paging 3 | 中 | 需引入依赖 |
| 6.4 | Room FTS全文搜索 | 低 | 需引入FTS虚拟表 |
| 6.5 | backfillDiaryImages启动优化 | 低 | 已用SharedPreferences标记 |
| 6.6 | OkHttp替换HttpURLConnection | 中 | 需引入依赖 |
| 6.7 | Hilt依赖注入 | 高 | 需引入依赖+改造全部ViewModel |
| 6.8 | 内存优化 | 中 | 需profiler分析 |
| 6.9 | 数据库查询优化 | 低 | 索引+查询重写 |
| 6.10 | 图片懒加载和缓存 | 低 | Coil已引入，需配置 |
| 6.11 | Compose UI渲染优化 | 中 | 需逐Screen优化 |
| 6.12 | SharedPreferences统一 → DataStore | 中 | 当前8+文件53+调用 |

### Phase 7: 首页增强（P0）

| # | 任务 | 难度 | 已有基础 |
|---|------|------|---------|
| 7.1 | 心情日历热力图 | 低 | CalendarView已有873行，需加颜色编码 |
| 7.2 | 今日写作提示卡 | 低 | WritingPromptCard已有，需增强为常驻卡片 |
| 7.3 | 写作连续天数首页展示 | 低 | StreakCalculator已有全部计算逻辑 |
| 7.4 | 智能问候语 | 低 | HomeGreeting已有基础 |
| 7.5 | 首页布局自定义 | 中 | 需拖拽排序框架 |

### Phase 8: 搜索增强（P0）

| # | 任务 | 难度 | 已有基础 |
|---|------|------|---------|
| 8.1 | 搜索历史持久化 | 低 | 当前内存MutableStateFlow，需改SharedPreferences |
| 8.2 | 高级筛选面板 | 中 | 需新建筛选UI+DAO查询 |
| 8.3 | 搜索建议/自动补全 | 中 | 需标签+地点数据源 |
| 8.4 | 搜索历史Room表（或Prefs） | 低 | 可用SearchHistory Entity或Prefs |

### Phase 9: 编辑器增强（P1）

| # | 任务 | 难度 | 已有基础 |
|---|------|------|---------|
| 9.1 | 语音输入 | 中 | Android SpeechRecognizer API |
| 9.2 | AI问答引导替代模板 | 中 | AI Provider已接入，需设计问答流程 |
| 9.3 | 编辑器附件支持（语音/文件） | 中 | 需DiaryImage表扩展mediaType |
| 9.4 | 编辑器手势（缩放/滑动/下拉） | 低 | WebView手势处理 |
| 9.5 | Prism.js代码高亮 | 低 | 需在editor.html引入Prism.js |
| 9.6 | 表格支持 | 低 | 需引入quill-better-table插件 |
| 9.7 | EditorViewModel拆分 | 高 | 当前432行，需拆为子管理器 |

### Phase 10: AI增强（P1）

| # | 任务 | 难度 | 已有基础 |
|---|------|------|---------|
| 10.1 | AI自动标签 | 低 | AI Provider+标签系统已有 |
| 10.2 | 语义搜索 | 中 | 需AI将查询转关键词组合 |
| 10.3 | 写作风格分析 | 中 | 需新AI Prompt |
| 10.4 | 周报生成 | 低 | 月报/年报已有基础 |
| 10.5 | AI问答写作引导 | 中 | 替代静态模板 |

### Phase 11: 连续记录系统（P1）

| # | 任务 | 难度 | 已有基础 |
|---|------|------|---------|
| 11.1 | 连续冻结UI | 低 | StreakFreeze Entity+StreakCalculator已有 |
| 11.2 | 最长连续记录展示 | 低 | computeLongestStreak已实现 |
| 11.3 | 连续里程碑视觉庆祝 | 中 | detectStreakMilestone已实现，需动画 |
| 11.4 | 月/年排行榜展示 | 低 | computeMonthlyLeaderboard/YearlyBest已实现 |

### Phase 12: 标签系统增强（P1-P2）

| # | 任务 | 难度 | 已有基础 |
|---|------|------|---------|
| 12.1 | 标签层级UI | 低 | TagDao.rootTags/childTags已实现 |
| 12.2 | 标签合并/去重 | 中 | TagDao.reassignTags已实现 |
| 12.3 | 标签颜色自动建议 | 低 | 需语义映射表 |
| 12.4 | AI标签推荐 | 低 | AI Provider+保存时触发 |
| 12.5 | 标签看板视图 | 中 | 需新建Screen |

### Phase 13: 成就系统优化（P1）

| # | 任务 | 难度 | 已有基础 |
|---|------|------|---------|
| 13.1 | 每周/月挑战 | 中 | 成就框架已有 |
| 13.2 | 进度可视化增强 | 低 | 当前有进度条，需增强 |

### Phase 14: 地图模块增强（P2）

| # | 任务 | 难度 | 已有基础 |
|---|------|------|---------|
| 14.1 | 旅行路线 | 中 | 高德地图已接入 |
| 14.2 | 地点聚类+常去排行 | 中 | 需新DAO查询 |
| 14.3 | 热力图模式 | 中 | 需HeatMap图层 |

### Phase 15: 月报/年报增强（P2）

| # | 任务 | 难度 | 已有基础 |
|---|------|------|---------|
| 15.1 | 周报 | 低 | 月报框架已有 |
| 15.2 | 报告分享图 | 中 | 需Canvas绘制 |
| 15.3 | 跨年对比 | 中 | 需历史数据聚合 |

### Phase 16: 详情页 + 导出 + 其他（P2-P3）

| # | 任务 | 难度 | 已有基础 |
|---|------|------|---------|
| 16.1 | 日记关联推荐 | 中 | 需标签+时间+内容相似度计算 |
| 16.2 | 导出格式优化（JSON/MD/HTML/PNG） | 低 | DiaryExporter已有723行 |
| 16.3 | 存储：孤立媒体清理 | 低 | StorageViewModel已有 |
| 16.4 | 存储：重复检测 | 中 | 需文件哈希比对 |
| 16.5 | 存储：DB VACUUM | 低 | 一条SQL |
| 16.6 | 通知深度优化 | 中 | 智能提醒+每日回顾 |
| 16.7 | 待办系统最简版 | 低 | TodoDao/Repository已有 |
| 16.8 | Bug修复 | - | - |

### Phase 17: 架构优化（P3，长期）

| # | 任务 | 难度 | 说明 |
|---|------|------|------|
| 17.1 | DiaryDao继续拆分 | 中 | TagDao/TodoDao等已拆出，DiaryDao仍较大 |
| 17.2 | EditorViewModel拆分 | 高 | 432行，需拆为子管理器 |
| 17.3 | 大Screen拆分 | 高 | EditorScreen 1815行、StatsScreen 2050行等 |
| 17.4 | Hilt依赖注入 | 高 | 替换手动DI |
| 17.5 | 测试补充 | 高 | 当前覆盖率~10%，目标>60% |

---

## 关键约束（不可违反）

1. **不压缩图片** - 用户担心质量下降
2. **不改主题** - 用户喜欢当前14主题
3. **不加版本历史** - 担心空间占用
4. **不联网** - 单用户本地APP
5. **AI问答替代静态模板** - 用户不喜欢静态模板
6. **华为健康搁置** - Health Connect联系困难
7. **已删除宠物/小岛/称号/模板** - Phase 1已完成
8. **无视称号相关代码** - 只保留成就系统
9. **不做图片压缩** - 担心质量下降
10. **不做PDF/DOCX导出** - 太复杂
11. **不做键盘快捷键** - 手机用户
12. **不做云同步/单篇加密** - 备份与安全模块删除

---

## 需求文档索引

| 文件 | 用途 |
|------|------|
| `docs/FINAL-REQUIREMENTS.md` | 最终确认版需求（用户逐条确认，19大类+性能+架构） |
| `docs/REQUIREMENTS-CROSS-REFERENCE.md` | 38模块×用户确认 逐条对照表（含12个遗漏项追踪） |
| `docs/DIARYAPP-REDESIGN-PLAN.md` | 详细设计和实施计划（Phase 1-5已完成记录） |
| `docs/所有需求.txt` | AI审计的原始38模块120+功能点 |

---

## 下一步行动

**立即开始 Phase 6（性能优化）或 Phase 7（首页增强），由用户决定。**

建议顺序：Phase 6 → Phase 7 → Phase 8 → Phase 9 → ...

---
更新时间：2026-06-26
