# DiaryApp 全局代码审计补遗（第二轮深挖）

> 更新时间：2026-06-26
> 用途：补充 [docs/ai-handoff-bundle/code-audit-major-issues.md](/C:/Users/陈仕杰/Desktop/DiaryApp/docs/ai-handoff-bundle/code-audit-major-issues.md) 中未展开或第二轮才进一步确认的高价值问题。

---

## 1. 本轮新增结论总览

第二轮继续下钻后，确认了 5 类非常关键、且容易被误判的问题：

1. 设置系统已经分裂成多套来源，用户修改设置不一定真实生效。
2. 周报、地图、AI、搜索都不是“没做”，但存在大量“半实现未闭环”状态。
3. 报告分享目前更接近文本分享，不是完整的报告成品分享。
4. 地图热力图目前主要是模式状态和按钮外观，不是真正的热力图层。
5. 首页搜索状态模型已经存在，但 UI 没把这些状态真正展示出来。

---

## 2. 新增高优问题

### A1 设置体系分裂，用户改了设置不一定真的生效

**严重级别**

- `P1`

**问题描述**

当前“提醒/通知/天气预警/成就通知/每日提醒/隐私开关”等设置，没有统一通过一套配置来源驱动，而是至少分成：

- `AppPreferences`
- `NotificationPreferencesManager`
- `ReminderManager`

这不是单纯的代码风格问题，而是会直接导致用户设置和真实行为不一致。

**关键证据**

- 设置页写入的是 `AppPreferences.writingReminderEnabled`、`weatherReminder`、`dailyReviewPush` 等  
  证据：[app/src/main/java/com/diary/app/ui/settings/SettingsScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/settings/SettingsScreen.kt:250)
- `ReminderReceiver` 读取的是 `NotificationPreferencesManager.isDailyReminderEnabled(context)`  
  证据：[app/src/main/java/com/diary/app/reminder/ReminderReceiver.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/reminder/ReminderReceiver.kt:15)
- 个人页每日提醒直接走 `ReminderManager.scheduleReminder(...)`  
  证据：[app/src/main/java/com/diary/app/ui/profile/ProfileScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/profile/ProfileScreen.kt:212)
- 个人页天气预警、成就提醒用的是 `NotificationPreferencesManager`  
  证据：[app/src/main/java/com/diary/app/ui/profile/ProfileScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/profile/ProfileScreen.kt:503)

**影响**

- 用户会遇到“设置里开了，实际不提醒”。
- 不同页面的同类开关可能互相不一致。
- 后续 AI 如果先做“智能提醒”等增强，会建立在错误基础上。

**建议处理**

1. 建统一 Notification/Reminder settings facade。
2. 清理 `ProfileScreen` 与 `SettingsScreen` 的重复提醒入口。
3. 每个设置项都标明真实消费方。

**状态更新（2026-06-26，Packet-01 后）**

- 上述“多源分裂”判断已部分过时：当前代码已新增 `ReminderSettingsRepository` 作为统一 facade，`ReminderManager`、`NotificationPreferencesManager`、`ReminderReceiver`、`AchievementNotificationManager` 现已消费同一套配置。
- `SettingsScreen` 的写作提醒也已接入真实调度/取消，不再只是写 `AppPreferences`。
- 仍未完全闭环的点：`dailyReviewPush` 暂无真实消费端，`ProfileScreen` 仍保留第二入口但已不再和设置页打架。

---

### A2 周报不是空白，但还没有接入正式主路径

**严重级别**

- `P2`

**问题描述**

周报不是“需求文档里有，代码里没有”，而是已经有独立页面与聚合逻辑，但没有被纳入正式导航主路径。

**关键证据**

- 周报 ViewModel 已存在，能按一周时间范围聚合条目、标签、字数、心情、时长  
  证据：[app/src/main/java/com/diary/app/ui/weeklyreport/WeeklyReportViewModel.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/weeklyreport/WeeklyReportViewModel.kt:25)
- 周报 Screen 已存在  
  证据：[app/src/main/java/com/diary/app/ui/weeklyreport/WeeklyReportScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/weeklyreport/WeeklyReportScreen.kt:49)
- `DiaryNavHost` 中没有 `Screen.WeeklyReport` 路由  
  证据：[app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt:195)

**影响**

- 后续 AI 可能误判“周报未开始”，重复造轮子。
- 用户无法通过正式主路径稳定触达这项能力。

**建议处理**

1. 先确定周报是归到统计页、通知页还是报告页。
2. 接通导航。
3. 再做分享与 AI 洞察增强。

---

### A3 地图热力图目前名不副实

**严重级别**

- `P2`

**问题描述**

地图模块已经不是普通散点图，实际上已有：

- 路线模式
- 路线统计
- 地点聚类
- 常去地点统计

但热力图模式目前更像“状态切换 + 按钮高亮”，未看到真实热力图地图层。

**关键证据**

- 路线/聚类逻辑存在  
  证据：[app/src/main/java/com/diary/app/ui/map/MapViewModel.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/map/MapViewModel.kt:125)  
  证据：[app/src/main/java/com/diary/app/ui/map/MapViewModel.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/map/MapViewModel.kt:223)
- `isHeatmapMode` 只是状态  
  证据：[app/src/main/java/com/diary/app/ui/map/MapViewModel.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/map/MapViewModel.kt:132)
- `DiaryMapScreen` 里有 Marker 和 Polyline，但没有 HeatMapOverlay  
  证据：[app/src/main/java/com/diary/app/ui/map/DiaryMapScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/map/DiaryMapScreen.kt:519)  
  证据：[app/src/main/java/com/diary/app/ui/map/DiaryMapScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/map/DiaryMapScreen.kt:538)

**影响**

- 容易误判成“只差样式优化”。
- 文档层面若不澄清，后续 AI 会低估工作量。

**建议处理**

1. 把地图能力区分为逻辑层和渲染层。
2. 把“热力图模式”单独标记为未闭环功能。

---

### A4 AI 能力已经散点落地，但没有统一体验层

**严重级别**

- `P2`

**问题描述**

AI 相关功能并不是空白，当前至少已有：

- 多服务商路由
- 请求缓存
- 标签推荐
- 搜索解析
- 风格分析
- AI 传记

但这些能力是散点存在，不是统一的 AI 产品层。

**关键证据**

- `AiServiceManager`  
  证据：[app/src/main/java/com/diary/app/ai/AiServiceManager.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ai/AiServiceManager.kt:14)
- `BiographyViewModel`  
  证据：[app/src/main/java/com/diary/app/ui/biography/BiographyViewModel.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/biography/BiographyViewModel.kt:64)
- `StatsViewModel.analyzeWritingStyle()`  
  证据：[app/src/main/java/com/diary/app/ui/stats/StatsViewModel.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/stats/StatsViewModel.kt:372)

**进一步发现**

- `aiDataUsageConsent` 几乎只停留在设置页，没有清晰的全链路消费证据。  
  证据：[app/src/main/java/com/diary/app/ui/settings/SettingsScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/settings/SettingsScreen.kt:297)

**影响**

- AI 功能会越做越多，但用户和开发者都很难理解“AI 在这个 App 里到底承担什么角色”。

**建议处理**

1. 建立 AI capability map。
2. 对每项能力标记：
   - UI 是否存在
   - 导航是否接通
   - 设置是否接线
   - 测试是否存在

---

### A5 首页搜索状态已存在，但 UI 提示层没有真正闭环

**严重级别**

- `P2`

**问题描述**

- `HomeViewModel` 已维护 `recentSearches` 和 `searchSuggestions`
- 但 `HomeSearchBar` 仍主要只是输入框本体，没有建议列表、历史列表或浮层渲染

**关键证据**

- `HomeViewModel`  
  证据：[app/src/main/java/com/diary/app/ui/home/HomeViewModel.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/home/HomeViewModel.kt:141)  
  证据：[app/src/main/java/com/diary/app/ui/home/HomeViewModel.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/home/HomeViewModel.kt:157)
- `HomeSearchBar`  
  证据：[app/src/main/java/com/diary/app/ui/home/HomeScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/home/HomeScreen.kt:1386)

**影响**

- 会出现“状态先有了，用户却看不见”的半实现状态。
- 这是后续最容易被误判为“已经做过”的坑。

**建议处理**

1. 搜索能力按层拆开：
   - 输入层
   - 建议层
   - 筛选层
   - AI 解析层
2. 先把历史/建议 UI 补出来，再继续往下扩。

**状态更新（2026-06-26，Packet-02 后）**

- 上述“UI 提示层没有真正闭环”的判断已过时：当前首页已新增历史搜索面板、建议面板、筛选面板，并把提交历史真正接入 `commitSearch()`。
- 标签、地点、字数范围筛选已进入同一套过滤逻辑，地点字段也已进入 `DiaryDao.search*` 查询。
- 仍然成立的剩余问题：搜索实现还依赖 `HomeViewModel` 中的全量预览缓存和内存过滤，性能与查询分层问题没有在本包内解决。

---

## 3. 第二轮后的模块结论更新

### 首页

- 不只是“首页太重”，而且已经出现了“ViewModel 状态先行、UI 闭环滞后”的问题。

### 统计与报告

- 不只是“报告体系分散”，而且周报已经半接通，月报/年报分享仍停在文本层。

### 地图

- 地图不是空白模块，路线和聚类已有不少基础。
- 真正欠缺的是热力图和更完整的渲染层能力。

### 设置与提醒

- 这是当前最被低估的问题之一。
- 它不是“后面可以顺手重构”，而是多个后续功能的基础前提。

### AI

- AI 能力不弱，但缺统一定位。
- 再继续散点加功能，会迅速失控。

---

## 4. 本轮建议优先级更新

### 现在最该先做的不是新功能，而是

1. 统一设置/提醒/通知配置来源
2. 明确周报/地图/搜索这些半实现能力的真实状态
3. 清理残留与错位命名

### 之后最值得接的闭环主题

1. 首页搜索闭环
2. 周报接入主路径
3. 报告分享从文本升级到图片/卡片
4. AI 能力统一编排

---

## 5. Packet 进展补记（2026-06-26）

### Packet-03 已完成的事实修正

1. “数据库仍会在产品路径 silent destructive migration”这条判断已经过时。
   - 当前实现已移除 `fallbackToDestructiveMigration()`。
   - 开库失败时会先保留数据库备份，再抛出显式异常。

2. “AI Key / PIN 仍直接明文落普通 SharedPreferences”这条判断已经部分过时。
   - 当前已新增 `SecureConfigStore`。
   - AI API Key 与 PIN hash/salt/hint 已迁移到安全存储，并带旧值自动迁移。

3. “备份恢复只恢复文件，不保证媒体链路恢复”这条风险已被代码事实修正。
   - 当前恢复 zip 媒体后，会按导入结果重建 `diary_images`。
   - 这修复了严重的恢复后图片/媒体 UI 断链问题。

### Packet-03 剩余边界

1. 数据库打开失败后的用户可见修复引导还没有完成。
2. 备份恢复仍缺少导入前摘要和更明确的失败说明。
3. AI endpoint/model 尚未纳入安全存储，本轮只优先止住最高风险项。

### 2.69.0+ 回归问题补记（2026-06-26）

1. “成就系统只会改成就状态，不会产生额外日记副作用”这条默认判断在本轮前是错的。
   - `AchievementRepository.checkAndUnlock()` 之前会调用 `createMilestoneDiary()`，真实插入一篇里程碑日记。
   - 当前这条错误副作用已被移除，成就解锁不再偷偷造日记。

2. “备份导入读不出来主要只是媒体索引恢复问题”这条判断也不完整。
   - 除了 `diary_images` 重建外，2.69.0+ 还存在备份扫描路径和真实读取路径不一致的问题。
   - 备份列表能扫到 `Documents/DiaryApp` 文件，但导入读取原先可能只查内部目录 / Downloads。
   - 当前已新增统一的 `resolveReadableBackupFile()` 解析链，修正该兼容问题。

3. “全量备份包理论上会带图片，所以图片缺失更可能是恢复阶段问题”这条判断过于乐观。
   - 旧实现主要依赖数据库索引和正文引用收集媒体文件名。
   - 对索引缺失、引用脏数据或历史残留图片的容错不够。
   - 当前已额外扫描磁盘上真实存在的媒体文件，降低“导出包里没有图片”的风险。

4. “宠物/小岛系统既然已弃用，通知页也不会再看到相关内容”这条默认判断在本轮前不成立。
   - 历史通知实体仍可能残留宠物/小岛相关文案或类型。
   - 当前通知页已新增遗留关键字过滤，先从消费层止血。

5. “成就详情页已有图片资源，所以视觉上大体没问题”这条判断已被代码事实修正。
   - 旧实现直接用原始 drawable 大图铺详情，造成模糊且和当前主题风格脱节。
   - 现已改为复用 `AchievementArtwork`，与当前成就系统视觉语言保持一致。
