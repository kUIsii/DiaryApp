# DiaryApp 可分发给其他 AI 的任务包

> 更新时间：2026-06-26
> 用途：把当前最值得推进的工作拆成多个边界清晰、可独立执行的任务包，减少不同 AI 互相踩文件、重复理解、重复判断。
> 配套文档：
> - [docs/ai-handoff-bundle/module-status-matrix.md](/C:/Users/陈仕杰/Desktop/DiaryApp/docs/ai-handoff-bundle/module-status-matrix.md)
> - [docs/ai-handoff-bundle/ai-handoff-prompts-and-guide.md](/C:/Users/陈仕杰/Desktop/DiaryApp/docs/ai-handoff-bundle/ai-handoff-prompts-and-guide.md)

---

## 1. 分发原则

每个 AI 一次只接一个任务包，不要同时吃多个大主题。因为当前项目最容易出问题的地方不是“不会做”，而是：

1. 多个模块共享入口文件
2. 同一功能有两套设置来源
3. 很多能力已经半实现
4. 大文件太多，冲突概率高

---

## 2. 推荐分发顺序

1. `Packet-01：设置与提醒系统统一`
2. `Packet-02：首页搜索闭环`
3. `Packet-03：数据库迁移与敏感配置止血`
4. `Packet-04：周报接主路径 + 报告入口梳理`
5. `Packet-05：写作目标闭环`
6. `Packet-06：报告分享升级`
7. `Packet-07：标签层级与标签治理`
8. `Packet-08：地图真实状态校准与热力图补完`

前 3 个更偏基础治理，后 5 个更偏产品闭环。

---

## 3. 任务包详情

### Packet-01：设置与提醒系统统一

**目标**

把提醒、通知、天气预警、每日提醒等同类设置收口到统一来源，避免“用户改了但实际不生效”。

**状态更新（2026-06-26）**

- 已完成第一轮收口：新增 `ReminderSettingsRepository`，把 `SettingsScreen`、`ProfileScreen`、`ReminderReceiver`、`AchievementNotificationManager` 统一到 `AppPreferences` 背后的同一来源。
- 已补兼容迁移：旧的 `diary_reminder_prefs` / `notification_preferences` 会在启动时迁移到统一设置。
- 已补真实调度：`SettingsScreen` 切换写作提醒时会同步调度/取消提醒，不再只写设置不改运行时。
- 剩余问题：`dailyReviewPush` 目前仍只有设置项，没有对应的真实通知消费端；`ProfileScreen` 仍保留第二入口，但已与主设置同源。

**优先级**

- `P0`

**入口文件**

- [app/src/main/java/com/diary/app/ui/settings/SettingsScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/settings/SettingsScreen.kt:1)
- [app/src/main/java/com/diary/app/ui/profile/ProfileScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/profile/ProfileScreen.kt:1)
- [app/src/main/java/com/diary/app/ui/settings/AppPreferences.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/settings/AppPreferences.kt:1)
- [app/src/main/java/com/diary/app/reminder/NotificationPreferencesManager.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/reminder/NotificationPreferencesManager.kt:1)
- [app/src/main/java/com/diary/app/reminder/ReminderManager.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/reminder/ReminderManager.kt:1)
- [app/src/main/java/com/diary/app/reminder/ReminderReceiver.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/reminder/ReminderReceiver.kt:1)

**建议动作**

1. 画出现有设置项到真实消费方的映射表
2. 建立统一 facade 或 repository
3. 去掉重复入口或做同源映射
4. 验证设置切换后提醒调度/接收逻辑真实一致

**常见陷阱**

- 只改设置页，不改 Receiver
- 只统一写入，不统一读取
- 忽略 `ProfileScreen` 中第二套提醒入口

**验收标准**

1. 每个提醒类开关只有一个真实来源
2. 设置页和个人页不再互相打架
3. Receiver/Manager 不再绕过统一配置层
4. 至少有 1 组自动化测试或可重复验证脚本

**可直接复制给 AI 的提示词**

```text
你现在只负责 DiaryApp 的“设置与提醒系统统一”，不要顺手做别的重构。

先读：
- docs/ai-handoff-bundle/module-status-matrix.md
- docs/ai-handoff-bundle/code-audit-major-issues.md
- docs/ai-handoff-bundle/code-audit-addendum-round2.md
- docs/ai-handoff-bundle/ai-handoff-prompts-and-guide.md

然后完成：
1. 识别所有提醒/通知相关设置项的写入点、读取点、真实消费方。
2. 统一成一套配置来源，不允许同类设置在不同页面各写一套。
3. 修复提醒调度和接收端对配置的读取分裂问题。
4. 增加或更新验证，证明设置切换后真实行为一致。

输出必须包含：
- 修改文件列表
- 统一前后的映射关系
- 验收证据
```

---

### Packet-02：首页搜索闭环

**目标**

把首页搜索从“状态先有了”补成“用户真实能用”的完整闭环。

**状态更新（2026-06-26）**

- 已完成首页闭环的第一轮实现：`HomeSearchBar` 现在已经接上搜索提交、历史面板、建议面板和筛选面板。
- 已新增标签 / 地点 / 字数范围筛选，并让“只筛选不输入关键词”的结果也能直接在首页显示。
- 已把地点字段接进 `DiaryDao.search*` 查询，地点建议点击后可以形成真实结果，不再只是一层 UI。
- 剩余问题：当前搜索仍然建立在全量预览 + 内存过滤之上，性能优化、FTS 和更完整的日期选择器仍待后续阶段处理。

**优先级**

- `P0`

**入口文件**

- [app/src/main/java/com/diary/app/ui/home/HomeViewModel.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/home/HomeViewModel.kt:1)
- [app/src/main/java/com/diary/app/ui/home/HomeScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/home/HomeScreen.kt:1)
- [app/src/main/java/com/diary/app/data/DiaryDao.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/data/DiaryDao.kt:1)

**建议动作**

1. 先把历史搜索和建议词 UI 渲染出来
2. 再把标签/地点/字数筛选纳入结构
3. 最后再考虑 AI 解析如何接入同一入口

**常见陷阱**

- 继续往 `HomeViewModel` 塞状态，不补 UI
- 只做 UI，不处理筛选模型
- 把“语义搜索”误做成另一个独立入口

**验收标准**

1. 搜索框聚焦后可见历史搜索或建议
2. 选择建议后能形成实际查询
3. 筛选条件至少与现有状态模型一致
4. 搜索主流程有测试或手动验证记录

**可直接复制给 AI 的提示词**

```text
你现在只负责 DiaryApp 的“首页搜索闭环”，不要扩散到首页其他模块。

先读：
- docs/ai-handoff-bundle/module-status-matrix.md
- docs/ai-handoff-bundle/feature-expansion-report.md
- docs/ai-handoff-bundle/feature-expansion-addendum-round2.md
- docs/ai-handoff-bundle/ai-handoff-prompts-and-guide.md

任务目标：
1. 核对 HomeViewModel 里已有的搜索历史、建议词、筛选状态。
2. 补齐 HomeScreen 的搜索历史/建议 UI。
3. 让建议点击、历史点击、筛选应用形成真实查询闭环。
4. 如需新增状态，保持和现有筛选模型一致。

输出必须包含：
- 真实已有基础
- 本次补齐的 UI/逻辑闭环
- 验收方式
```

---

### Packet-03：数据库迁移与敏感配置止血

**目标**

先消除最危险的数据丢失和敏感信息存储风险。

**状态更新（2026-06-26）**

- 已完成第一轮高风险止血：
  - `DiaryDatabase` 已移除产品路径 `fallbackToDestructiveMigration()`，迁移/开库失败时会先备份数据库文件，再抛出 `DiaryDatabaseOpenException`，不再静默清库。
  - 新增 `SecureConfigStore`，AI API Key 与 PIN hash/salt/hint 已迁移到安全存储，并支持从旧 `SharedPreferences` 自动迁移。
  - 备份恢复链路已补齐媒体索引重建：恢复 zip 中的媒体文件后，会按导入后的日记内容重建 `diary_images`，修复“图片文件恢复了但媒体 UI 仍断链”的严重问题。
- 已补最小验证：
  - `BackupMediaIndexUtilsTest`
  - `DiaryDatabaseSourceTest`
  - `SecureConfigStoreSourceTest`
  - `BiometricHelperSourceTest`
  - `BackupManagerUtilsTest`
  - `BackupCoverageSourceTest`
  - `DiaryDatabaseMigrationUtilsTest`
- 剩余问题：
  - 数据库打开失败后的用户可见恢复/修复提示页还没做。
  - AI endpoint/model 仍在普通偏好中，不属于本轮最高风险止血范围。
  - 备份恢复仍缺少“导入前预检摘要”和更明确的失败引导。

**优先级**

- `P0`

**入口文件**

- [app/src/main/java/com/diary/app/data/DiaryDatabase.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/data/DiaryDatabase.kt:1)
- [app/src/main/java/com/diary/app/biometric/BiometricHelper.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/biometric/BiometricHelper.kt:1)
- [app/src/main/java/com/diary/app/ai/AiConfigStore.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ai/AiConfigStore.kt:1)

**建议动作**

1. 去掉 release 主路径 destructive migration
2. 设计迁移失败时的安全降级策略
3. 评估 PIN 与 AI Key 的安全存储迁移方案
4. 至少先把 API Key 从普通 SharedPreferences 收口

**常见陷阱**

- 只删 fallback，不给失败路径
- 只改 PIN，不改 AI 配置
- 忽略旧数据迁移兼容

**验收标准**

1. release 路径不再无提示 destructive migration
2. 敏感配置不再直接明文落普通 SharedPreferences
3. 有明确迁移或兼容说明
4. 有最小验证证据

**可直接复制给 AI 的提示词**

```text
你现在只负责 DiaryApp 的“数据库迁移与敏感配置止血”。

先读：
- docs/ai-handoff-bundle/module-status-matrix.md
- docs/ai-handoff-bundle/code-audit-major-issues.md
- docs/ai-handoff-bundle/ai-handoff-prompts-and-guide.md
- docs/整体目标/FINAL-REQUIREMENTS.md

任务目标：
1. 处理数据库 destructive migration 的产品风险。
2. 处理 PIN / AI Key 的不安全持久化问题。
3. 给出兼容旧数据的最小可行方案。

要求：
- 优先处理高风险问题，不要顺手大改所有存储层。
- 给出验证证据，不要只改代码不证明路径。
```

---

### Packet-04：周报接主路径 + 报告入口梳理

**目标**

把已存在的周报页面纳入正式导航和用户主体验路径，并顺手校准报告入口结构。

**状态更新（2026-06-26）**

- 已完成主路径接入：
  - `DiaryNavHost` 已新增 `WeeklyReport` 正式 route。
  - `StatsScreen` 原本只有单独月报入口，现已整理为周报 / 月报 / 年报三张报告入口卡。
  - `NotificationScreen` 里的 `WeeklySummaryNotification` 现在可以直接进入周报页面，不再只是展示通知。
- 已补最小验证：
  - `WeeklyReportSourceTest`
- 剩余问题：
  - 周报目前还没有独立分享能力。
  - 报告系统虽然入口更清楚了，但周报/月报/年报的共享渲染与分享层还没有统一。

**优先级**

- `P1`

**入口文件**

- [app/src/main/java/com/diary/app/ui/weeklyreport/WeeklyReportScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/weeklyreport/WeeklyReportScreen.kt:1)
- [app/src/main/java/com/diary/app/ui/weeklyreport/WeeklyReportViewModel.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/weeklyreport/WeeklyReportViewModel.kt:1)
- [app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt:1)
- [app/src/main/java/com/diary/app/ui/stats/StatsScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/stats/StatsScreen.kt:1)

**建议动作**

1. 确认周报应该从哪里进入
2. 接通正式 route
3. 避免和月报/年报入口逻辑冲突
4. 补一份报告入口说明

**常见陷阱**

- 只加 route，不加用户入口
- 只加入口，不梳理返回路径
- 顺手重写整个报告系统

**验收标准**

1. 周报可通过正式路径进入
2. 报告入口至少不再让用户迷路
3. 基础导航/打开路径有验证

**可直接复制给 AI 的提示词**

```text
你现在只负责“周报接主路径 + 报告入口梳理”。

先读：
- docs/ai-handoff-bundle/module-status-matrix.md
- docs/ai-handoff-bundle/code-audit-addendum-round2.md
- docs/ai-handoff-bundle/feature-expansion-addendum-round2.md

任务目标：
1. 核对周报现有真实能力，不要重复造页面。
2. 为周报增加正式导航和用户入口。
3. 校准它和统计/月报/年报之间的入口关系。
4. 保持改动最小，不要一口气重写报告系统。
```

---

### Packet-05：写作目标闭环

**目标**

把已有的 `WritingGoal` 从数据层能力补成用户可感知的首页/统计闭环。

**状态更新（2026-06-26）**

- 已完成首页/统计闭环：
  - 统计页原有目标区保留，但目标进度计算已抽到共享的 `WritingGoalProgressUtils`，不再只埋在 `StatsViewModel`。
  - 首页已新增轻量 `HomeWritingGoalCard`，展示当前主目标的本周/本月进度，并直接引导进入统计页查看完整目标。
  - 首页和统计页现在消费同一套 `GoalProgress` 模型，避免两边各算一套口径。
- 已补最小验证：
  - `HomeWritingGoalSourceTest`
- 剩余问题：
  - 首页目前只展示主目标摘要，不支持直接在首页编辑目标。
  - 目标完成后的提醒/通知联动还没有纳入本轮范围。

**优先级**

- `P0`

**入口文件**

- [app/src/main/java/com/diary/app/data/WritingGoal.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/data/WritingGoal.kt:1)
- [app/src/main/java/com/diary/app/data/DiaryDao.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/data/DiaryDao.kt:1)
- [app/src/main/java/com/diary/app/ui/home/HomeScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/home/HomeScreen.kt:1)
- [app/src/main/java/com/diary/app/ui/stats/StatsScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/stats/StatsScreen.kt:1)

**建议动作**

1. 确认当前 Goal 数据查询能力
2. 设计一个轻量首页展示
3. 在统计页增加目标总览或完成进度
4. 保持目标类型先少量闭环，不要先做大全套

**常见陷阱**

- 目标种类加太多
- 首页展示做得太重
- 只展示，不验证进度计算

**验收标准**

1. 用户能看到当前写作目标进度
2. 进度来源真实可计算
3. 首页和统计页展示口径一致

**可直接复制给 AI 的提示词**

```text
你现在只负责“写作目标闭环”。

先读：
- docs/ai-handoff-bundle/module-status-matrix.md
- docs/ai-handoff-bundle/feature-expansion-report.md
- docs/ai-handoff-bundle/ai-handoff-prompts-and-guide.md

任务目标：
1. 基于已有 WritingGoal 和 DAO 能力，补齐首页/统计的目标展示。
2. 先做最小闭环，不要扩成完整任务系统。
3. 保证展示和进度计算口径一致。
```

---

### Packet-06：报告分享升级

**目标**

把月报/年报当前偏文本的分享能力升级为更像成品的分享卡或分享图。

**状态更新（2026-06-26，2.69.0+ 稳定性回归修复后）**

- 进入 `Packet-06` 前，已先处理一轮 2.69.0 之后真实影响使用的回归问题，避免在不稳定底座上继续扩分享功能：
  - 成就解锁不再偷偷创建“里程碑日记”，修正了 `AchievementRepository.checkAndUnlock()` 的错误副作用。
  - 备份导入读取链已补齐统一解析入口，`Documents/DiaryApp`、应用内部备份目录、Downloads 三条读取路径现在走同一套 `resolveReadableBackupFile()`，修复“列表里看得到但新版读不出来”的兼容问题。
  - 全量备份包已额外收集磁盘上实际存在的媒体文件名，不再只依赖当前索引/正文引用，降低“图片没被打进备份包”的风险。
  - 通知页已增加遗留宠物/小岛通知过滤，避免被已弃用系统的历史通知污染。
  - 成就详情页已改为复用主题化 `AchievementArtwork`，不再直接拿大图资源硬铺，修复模糊和风格割裂问题。
  - 天气详情页已改为 `Scaffold + pinned top bar`，修复顶部空白区和标题不贴顶问题。
  - 首页搜索框已从过透明玻璃条调整为更稳定的主题 Surface 容器。
- 已补最小回归验证：
  - `AchievementRepositorySourceTest`
  - `BackupManagerSourceTest`
  - `AchievementDetailSourceTest`
  - `HomeSearchBarSourceTest`
  - `WeatherDetailSourceTest`
  - `NotificationCleanupSourceTest`
- `Packet-06` 已完成第一轮主功能实现：
  - 月报 / 年报导航层不再只分享文本，现已接入统一的 `ReportShareUtils`，优先生成可分享图片，再以原有文本分享兜底。
  - 已新增 `buildMonthlyReportShareCard()`、`buildAnnualReportShareCard()` 和 `shareReportImage()`，复用同一套图片输出链。
  - 已补最小验证：
    - `ReportShareSourceTest`
    - `ReportShareUtilsSourceTest`
    - `MonthlyReportShareUtilsTest`
    - `AnnualReportShareUtilsTest`
- 当前仍未完成的点：
  - 周报仍未进入统一分享链。
  - 当前分享图为第一版单张成品卡，不是多页长图或逐卡导出系统。
  - 备份恢复虽然已修正主要读取兼容问题，并已补上“导入前摘要 + 图片缺失风险提示”，但“更强失败引导 / 更完整旧包真机兼容说明”仍要继续补。

**优先级**

- `P1`

**入口文件**

- [app/src/main/java/com/diary/app/ui/monthlyreport/MonthlyReportScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/monthlyreport/MonthlyReportScreen.kt:1)
- [app/src/main/java/com/diary/app/ui/annualreport/AnnualReportScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/annualreport/AnnualReportScreen.kt:1)

**建议动作**

1. 先确认当前分享输出是文本还是位图
2. 设计第一版单张分享图
3. 尽量复用现有数据聚合，不先动指标层

**常见陷阱**

- 一上来做多页炫技系统
- 先重构全报告体系
- 忽略导出/分享稳定性

**验收标准**

1. 至少一类报告可输出可分享的成品图
2. 现有文本分享不被破坏
3. 分享内容与报告页面指标一致

**当前验收结果（2026-06-26）**

1. 月报与年报都已可输出可分享图片。
2. 文本分享仍保留为兜底路径，未被破坏。
3. 第一版分享卡使用对应报告对象中的真实指标构建，已满足最小闭环。

---

### Packet-07：标签层级与标签治理

**目标**

把已有标签父子结构做成真实可用的管理与治理能力。

**状态更新（2026-06-26）**

- 已完成第一轮真实闭环，而不是只停留在“有树形页面”：
  - `TagManagementScreen` 的树形展开状态现已支持多层级共享，不再只有根节点能展开、子节点永远是折叠死状态。
  - 标签行里的“移除父标签（设为顶级）”现在会真实写回 `parentId = null`，不再只是关闭对话框。
  - 父标签选择已新增层级防护：不能把自己或自己的后代重新设为父标签，避免形成循环层级。
  - 首页搜索中的标签筛选现在会先展开父标签对应的全部后代标签，再参与过滤，修复“选了父标签但搜不到子标签内容”的口径断裂。
- 已补最小验证：
  - `TagHierarchyUtilsTest`
  - `TagHierarchySourceTest`
  - `HomeTagFilterHierarchySourceTest`
- 剩余问题：
  - 标签合并虽然已有入口和 DAO helper，但相似标签检测、批量治理建议仍未做。
  - 当前首页标签筛选仍基于标签名展开，不是基于稳定 tagId 的查询层下推。

**优先级**

- `P1`

**入口文件**

- [app/src/main/java/com/diary/app/data/Tag.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/data/Tag.kt:1)
- [app/src/main/java/com/diary/app/data/TagDao.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/data/TagDao.kt:1)
- [app/src/main/java/com/diary/app/ui/profile/TagManagementScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/profile/TagManagementScreen.kt:1)

**建议动作**

1. 先把父子关系可视化
2. 再做父标签包含子标签的筛选逻辑
3. 最后再考虑合并去重

**常见陷阱**

- 先做复杂拖拽，忽略基础展示
- 只改 UI，不改筛选消费逻辑
- 在脏标签基础上继续做 AI 推荐

**验收标准**

1. 用户能看见并理解标签层级
2. 父子标签在使用场景中口径一致
3. 不破坏现有标签数据

**当前验收结果（2026-06-26）**

1. 标签管理页已支持多层树形展开与真实父级移除。
2. 父标签筛选已自动覆盖全部子标签。
3. 已补循环层级防护，避免把标签层级结构改坏。

---

### Packet-08：地图真实状态校准与热力图补完

**目标**

先校准地图模块的真实能力边界，再决定是否补热力图渲染。

**状态更新（2026-06-26）**

- 已确认旧判断的真伪：
  - “地图是空白模块”这条判断是错的，当前代码真实已有路线模式、地点聚类、常去地点统计、位置日记列表。
  - “热力图已经完成”这条判断也是错的，原实现只有 `isHeatmapMode` 状态和按钮样式，没有真实地图热力层。
- 已完成第一轮热力图闭环：
  - 新增 `MapHeatmapUtils`，把位置点先聚类再映射成主题化热力点，形成可测试的最小热力模型。
  - `DiaryMapScreen` 现在在热力图模式下会真实往高德地图添加 `CircleOptions` overlay，不再只是按钮高亮。
  - 热力图模式仍保留 marker 锚点，保证用户能看到密度分布同时还能点回具体日记。
  - 顺手修正了一条真实旧 bug：`extractCityFromLocation()` 对两段地点字符串（如 `Shibuya, Tokyo`）原本会提错城市，现已改正。
- 已补最小验证：
  - `MapHeatmapUtilsTest`
  - `MapHeatmapSourceTest`
  - 既有 `MapViewModelTest`
- 剩余问题：
  - 当前热力图是第一版 circle overlay，不是更高级的瓦片级热力 renderer。
  - 热力图强度分级目前来自本地聚类计数，没有时间维度、心情维度等更细分析。

**优先级**

- `P2`

**入口文件**

- [app/src/main/java/com/diary/app/ui/map/MapViewModel.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/map/MapViewModel.kt:1)
- [app/src/main/java/com/diary/app/ui/map/DiaryMapScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/map/DiaryMapScreen.kt:1)

**建议动作**

1. 列出当前已实现的路线/聚类/地点统计能力
2. 明确热力图是否只有模式状态
3. 如果补热力图，先做最小 overlay 闭环

**常见陷阱**

- 把地图当成空白模块重写
- 不区分逻辑层和渲染层
- 一上来做太多视觉炫技

**验收标准**

1. 地图能力边界文档化
2. 若补热力图，用户可见且真实渲染
3. 不破坏现有路线与聚类功能

**当前验收结果（2026-06-26）**

1. 地图真实能力已重新校准，确认不是空白模块。
2. 热力图模式已具备真实 overlay 渲染。
3. 路线模式、聚类统计与列表入口均保持可用。

---

## 4. 多个 AI 并行时的避让规则

### 不要同时派给两个 AI 的组合

1. `Packet-01` 和任何大规模设置改造任务
2. `Packet-02` 和“首页整体重构”
3. `Packet-04` 和“报告系统统一重写”

### 可以相对独立并行的组合

1. `Packet-03` 和 `Packet-07`
2. `Packet-05` 和 `Packet-08`
3. `Packet-01` 完成后，再开 `Packet-06`

---

## 5. 交付回写模板

每个 AI 完成任务包后，至少要回写以下内容：

1. 当前包修复/补齐了什么
2. 哪些原始判断被证实或被推翻
3. 哪些文档需要更新
4. 哪个任务包现在变成下一优先级

可直接使用下面模板：

```text
任务包：Packet-XX

本次完成：
- 

确认的真实状态：
- 

修正的过时判断：
- 

更新的文档：
- 

建议下一包：
- 
```
