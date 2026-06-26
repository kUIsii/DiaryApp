# DiaryApp 优化与功能拓展 - 完整对接文档

## 项目信息
- 项目路径: C:\Users\陈仕杰\Desktop\DiaryApp
- 分支: experiment/v2-redesign
- 当前版本: v2.69.0-experimental
- 最新commit: 6d2f302a (Phase 4 完成)
- 数据库版本: Room v34
- 源文件: ~195 个 .kt 文件

---

## 一、所有需求汇总（用户反馈 + 原始审计）

### A. 用户明确要求的模块（按优先级）

| 模块 | 用户要求 | 状态 |
|------|---------|------|
| **编辑器增强** | 1.语音输入 2.快速记录(跳过) 3.AI内联建议 4.图片裁剪 | 待做 |
| **首页增强** | 1.心情热力图 2.写作提示 3.智能问候 4.搜索增强 | 1✅ 2✅ 3✅ 4部分完成 |
| **SharedPreferences统一** | 全部5项统一 | 已用AppPreferences替代 |
| **性能优化** | 成就全量加载优化 + 连续记录(忽略称号) | 连续记录逻辑✅待UI |
| **安全** | 跳过 | — |
| **代码质量** | 跳过 | — |
| **测试** | 全部4项 | 待做 |
| **导航重构** | 跳过 | — |
| **健康关联** | 跳过/替换 | — |
| **设置页** | 最简单版本 | ✅已完成(17项设置) |
| **EditorViewModel拆分** | 拆分职责 | 待做 |
| **大屏拆分** | 跳过 | — |
| **待办内容展示** | 做 | 待做 |
| **备份** | 跳过复杂版 | — |
| **设置功能丰富化** | 5项全做 | ✅已完成 |
| **组件库** | 跳过(重复) | — |
| **智能建议** | 跳过 | — |
| **健康数据** | 优化但不压缩图片 | 待做 |
| **宠物/小岛/模板** | 全部删除 | ✅已删除 |
| **备份恢复** | 3项全做(单用户) | 待做 |
| **备份数据** | 3项全做(单用户) | 待做 |
| **备份版本** | 不要版本历史 | — |
| **通知** | 深入优化2项 | 待做 |
| **月报** | 3项优化内容 | 待做 |
| **成就** | 3项全做 | 待做 |
| **Bug修复** | 修复 | 待做 |
| **主题** | 不改 | — |
| **连续记录** | 4项全做(冻结/最长/里程碑/排行) | 逻辑✅待UI |
| **标签** | 5项全做(层级/合并/颜色/AI/看板) | 待做 |
| **模板→AI提问** | 不要静态模板，AI提问引导 | 待做 |
| **称号→成就** | 删除称号，优化成就 | 称号✅已删 |
| **个人页** | 跳过(单用户) | — |
| **搜索** | 4项全做(历史/筛选/补全/语义) | 历史✅筛选✅ |
| **健康数据** | 搁置(华为) | — |
| **组件** | 不确定 | — |
| **导出** | 优化原有格式 | 待做 |
| **WebView编辑器** | 1.代码高亮 2.表格(跳过快捷键) | 待做 |

### B. 性能优化建议（来自审计报告）

| 优化项 | 描述 | 状态 |
|--------|------|------|
| 成就全量加载 | checkAndUnlock()加载全部日记→改数据库聚合查询 | 待做 |
| 备份全量加载 | buildBackupJson()全量加载→分批处理 | 待做 |
| 列表分页 | Paging 3替换全部加载 | 待做 |
| Room FTS | 替换LIKE搜索 | 待做 |
| backfillDiaryImages启动 | 每次启动查询COUNT(*)→改SharedPreferences标记 | 待做 |
| ProGuard | Release启用混淆 | ✅已完成 |
| OkHttp | 替换HttpURLConnection | 待做 |
| 依赖注入 | Hilt替换手动DI | 待做 |

---

## 二、已完成工作清单

### Phase 1: 死代码清理 ✅
- 删除宠物系统 (PetDao, PetModels, PetPersonality, PetStateMachine, PetAiGenerator, PetMemoryRepository)
- 删除小岛系统 (IslandDao, IslandModels, IslandRepository)
- 删除称号系统 (TitleDao, TitleChecker, TitleManager, TitleModels, TitleSeedData)
- 删除模板系统 (TemplateManager, DiaryTemplate)
- 删除 AppContainer (死代码)
- 删除 AchievementManager (与AchievementRepository重复)
- 删除 FeedbackGenerator
- 删除 MoodEnvironmentMapper
- 删除 EditorDialogs, EditorUtils (编辑器子文件)

### Phase 2: 架构优化 ✅
- Repository层: DiaryEntryRepository, TodoRepository
- DAO拆分: TagDao, TodoDao, MediaDao, NotificationDao, ChatDao, TrashDao, CountDownDao, CapsuleDao
- ProGuard启用 + 规则
- BaseHttpProvider.cleanEndpoint修复
- backfillDiaryImages启动优化

### Phase 3: 数据层增强 ✅
- 4个新Entity: EntryComment, WritingGoal, MoodCheckin, StreakFreeze
- Tag层级: parentId, usageCount
- MIGRATION_33_34 (4新表 + Tag列)
- 新DAO查询

### Phase 4: 设置页 + 首页增强 ✅
- AppPreferences: 17项设置, 5分类
- SettingsScreen重写: 8个分区, 17项设置全部接入UI
  - 写作(默认心情/天气/自动保存/排序/日历)
  - 通知(写作提醒+时间/连续提醒/天气提醒/每日回顾/免打扰)
  - 数据管理(回收站保留/自动清理/清除缓存)
  - 编辑器(字体大小/精简工具栏/智能标签)
  - 外观(主题)
  - 隐私(应用锁/位置/AI授权/截屏保护)
  - 备份(备份+标签管理)
  - 关于(版本/更新/日志)
- HomeViewModel增强: HomeGreeting(时间问候), HomeStreakInfo(连续记录信息), WritingPrompt(25个写作提示), HomeNewState
- WritingPromptCard组件: 智能问候 + 写作提示 + 连续记录信息栏
- HomeScreen集成: WritingPromptCard插入LazyColumn

---

## 三、剩余工作清单（按优先级）

### Phase 5: 统计增强
- [ ] 写作目标追踪 (WritingGoal entity已创建，需接入Stats)
- [ ] 词汇分析增强
- [ ] 时间深度分析
- [ ] 情绪时间线
- [ ] 生活关联分析

### Phase 6: 编辑器增强
- [ ] 语音输入集成
- [ ] AI内联建议
- [ ] 图片裁剪支持
- [ ] EditorViewModel拆分

### Phase 7: AI增强
- [ ] 语义搜索
- [ ] 自动标签
- [ ] 写作风格分析
- [ ] 周报生成
- [ ] AI问答写作引导(替代静态模板)

### Phase 8: 通知增强
- [ ] 智能提醒(基于写作习惯)
- [ ] 每日回顾通知

### Phase 9: 标签增强
- [ ] 层级UI(父子展示)
- [ ] 合并/去重相似标签
- [ ] 颜色自动建议
- [ ] AI标签推荐
- [ ] 看板视图

### Phase 10: 连续记录UI
- [ ] 冻结机制UI(StreakFreeze entity已创建)
- [ ] 最长记录展示
- [ ] 里程碑视觉庆祝
- [ ] 月/年排行展示

### Phase 11: 搜索增强
- [ ] 搜索自动补全(标签/地点)
- [ ] 语义搜索(AI)

### Phase 12: 存储 + 性能
- [ ] 孤立媒体清理
- [ ] 重复检测
- [ ] DB VACUUM
- [ ] 成就全量加载优化(改数据库聚合查询)
- [ ] Paging 3列表分页
- [ ] Room FTS全文搜索
- [ ] OkHttp替换HttpURLConnection
- [ ] Hilt依赖注入

### Phase 13: 成就优化
- [ ] 周/月挑战
- [ ] 进度可视化

### Phase 14: WebView增强
- [ ] Prism.js代码高亮
- [ ] 表格支持

### Phase 15: 剩余项
- [ ] 待办内容展示
- [ ] 通知深度优化
- [ ] 月报内容优化
- [ ] 备份格式优化
- [ ] 导出格式改进
- [ ] Bug修复

### Phase 16: 测试补充
- [ ] 单元测试(核心逻辑)
- [ ] 集成测试

---

## 四、关键约束（不可违反）

1. **不压缩图片** - 用户担心质量下降
2. **不改主题** - 用户喜欢当前主题
3. **不加版本历史** - 担心空间占用
4. **不联网** - 单用户本地APP
5. **AI问答替代静态模板** - 用户不喜欢静态模板
6. **华为健康搁置** - Health Connect联系困难
7. **删除宠物/小岛/称号/模板** - 已完成
8. **不要截图** - view_image会导致崩溃

---

## 五、关键文件路径

`
app/src/main/java/com/diary/app/
├── DiaryApplication.kt
├── MainActivity.kt
├── ai/              (AI系统)
├── data/            (Entity/DAO/Repository)
├── ui/
│   ├── home/        (首页: HomeScreen, HomeViewModel, WritingPromptCard)
│   ├── editor/      (编辑器: EditorScreen, EditorViewModel, EditorToolbar)
│   ├── stats/       (统计: StatsScreen, StatsViewModel, WordCloudView)
│   ├── settings/    (设置: SettingsScreen, AppPreferences)
│   ├── detail/      (详情页)
│   ├── todo/        (待办)
│   ├── notification/(通知)
│   ├── achievement/ (成就)
│   ├── map/         (地图)
│   └── components/  (通用组件)
├── util/            (StreakCalculator等)
├── weather/         (天气)
└── widget/          (桌面小组件)
`

---

## 六、手交指令

复制以下内容到新会话：

> 我在做 DiaryApp 优化（路径: C:\Users\陈仕杰\Desktop\DiaryApp, 分支 experiment/v2-redesign）。
> 读取 docs/DIARYAPP-REDESIGN-PLAN.md 了解完整状态。
> 已完成 Phase 1-4（commit 6d2f302a），需要从 Phase 5 开始继续。
> 关键约束：不压缩图片、不改主题、不加版本历史、单用户本地APP、宠物/小岛/称号已删除、华为健康搁置。
> 请直接开始 Phase 5（统计增强 - WritingGoal追踪）的工作。