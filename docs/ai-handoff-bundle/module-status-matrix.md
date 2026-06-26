# DiaryApp 模块真实状态矩阵

> 更新时间：2026-06-26
> 用途：把当前仓库各模块的真实状态压缩成一张可交接矩阵，帮助后续 AI/开发者快速判断“已完成、半实现、残留矛盾、下一步动作”。
> 配套文档：
> - [docs/ai-handoff-bundle/code-audit-major-issues.md](/C:/Users/陈仕杰/Desktop/DiaryApp/docs/ai-handoff-bundle/code-audit-major-issues.md)
> - [docs/ai-handoff-bundle/code-audit-addendum-round2.md](/C:/Users/陈仕杰/Desktop/DiaryApp/docs/ai-handoff-bundle/code-audit-addendum-round2.md)
> - [docs/ai-handoff-bundle/feature-expansion-report.md](/C:/Users/陈仕杰/Desktop/DiaryApp/docs/ai-handoff-bundle/feature-expansion-report.md)
> - [docs/ai-handoff-bundle/feature-expansion-addendum-round2.md](/C:/Users/陈仕杰/Desktop/DiaryApp/docs/ai-handoff-bundle/feature-expansion-addendum-round2.md)
> - [docs/ai-handoff-bundle/ai-handoff-prompts-and-guide.md](/C:/Users/陈仕杰/Desktop/DiaryApp/docs/ai-handoff-bundle/ai-handoff-prompts-and-guide.md)

---

## 1. 使用说明

阅读这张矩阵时，不要只看“有没有页面/有没有类”。必须同时看 4 件事：

1. 逻辑层是否真的存在
2. UI 是否真的闭环
3. 导航/入口是否接通
4. 设置/偏好/运行时消费方是否一致

很多模块当前不是“没做”，而是“做了一半但没有闭环”。

---

## 2. 模块状态矩阵

| 模块 | 当前判断 | 已实现基础 | 半实现/未闭环 | 残留矛盾/历史包袱 | 关键文件 | 建议下一步 | 优先级 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 首页总入口 | 功能很重，结构压力大 | 日历、统计摘要、搜索入口、提示/问候等已有基础 | 仍依赖全量预览数据，状态多、职责混杂 | 首页越来越像“控制台”，不是轻入口 | `ui/home/HomeScreen.kt` `ui/home/HomeViewModel.kt` | 先做职责拆分，改为摘要查询驱动 | P0 |
| 首页搜索 | `Packet-02` 已完成首页闭环 | 搜索历史持久化、标签建议、AI 解析入口原本已存在；现已补上历史/建议面板、筛选面板、地点查询、提交历史 | 仍然基于全量预览 + 内存过滤，性能治理和 FTS 还没做；日期筛选目前是快捷范围，不是完整日期选择器 | “搜索历史只是内存态”的旧判断已经过时 | `ui/home/HomeViewModel.kt` `ui/home/HomeScreen.kt` `ui/home/HomeSearchUiUtils.kt` `data/DiaryDao.kt` | 下一步在 `Packet-03` 后继续处理 FTS / 查询驱动化，避免首页继续依赖全量缓存 | P0 |
| 编辑器核心 | 基础成熟，但单文件过重 | 富文本编辑、图片插入、AI 引导入口已有 | AI 引导仍沿用旧模板语义；附件体系不完整 | 模板功能已删除，但命名与写入路径残留 | `ui/editor/EditorScreen.kt` `ui/editor/EditorViewModel.kt` | 收口模板语义，拆块式附件路线 | P1 |
| AI 写作引导 | 有基础，不是空白 | 对话框、生成结果注入已存在 | 仍偏一次性，不是多轮引导闭环 | 与“删除模板、改 AI 引导”的目标仍有过渡态冲突 | `ui/editor/EditorScreen.kt` | 改为提问流/草稿流双模式 | P1 |
| 统计主页 | 功能很多，维护成本高 | 词云、热力相关、风格分析、各类统计卡已有 | 代码体积过大，很多能力堆在同一屏 | 和周报/月报/年报边界不清 | `ui/stats/StatsScreen.kt` `ui/stats/StatsViewModel.kt` | 拆 shared stats/report capability | P1 |
| 周报 | 主路径与分享链都已接通 | 周维度统计、标签、心情、字数等已有；已接入 `DiaryNavHost` 正式 route，并从统计页报告入口、通知页周报通知进入；现已补上和月报/年报一致的分享入口、分享图构建和文本兜底 | 当前分享图仍是第一版单张卡，不是多页周报导出；报告 renderer 仍可继续统一 | “周报还没进正式导航主链”与“周报还没有分享能力”这两条旧判断都已过时 | `ui/weeklyreport/WeeklyReportScreen.kt` `ui/weeklyreport/WeeklyReportViewModel.kt` `ui/weeklyreport/WeeklyReportShareUtils.kt` `ui/report/ReportShareUtils.kt` `ui/navigation/DiaryNavHost.kt` `ui/stats/StatsScreen.kt` `ui/notification/NotificationScreen.kt` | 后续继续统一报告 renderer 即可，不需要再把周报当成孤立模块补路由 | P1 |
| 月报/年报 | `Packet-06` 已完成第一轮分享升级 | 报告页面、文本分享能力原本已存在；现已新增统一 `ReportShareUtils`，月报/年报优先输出可分享图片，文本分享保留兜底 | 周报仍未接入同一分享链；当前分享图是第一版单张卡，不是多页体系 | “分享更偏文本摘要，不是成品卡片/图片”的旧判断对月报/年报已经过时 | `ui/monthlyreport/*` `ui/annualreport/*` `ui/report/ReportShareUtils.kt` `ui/navigation/DiaryNavHost.kt` | 下一步把周报也纳入统一分享层，并继续收敛 renderer | P1 |
| 地图与位置 | `Packet-08` 已完成第一轮热力图闭环 | 路线、聚类、常去地点、统计本来就有；现已补上真实热力图 circle overlay 渲染，地图能力边界已校准 | 当前热力图仍是第一版 overlay，不是更高级的瓦片 renderer；密度算法还没有时间/心情等更细维度 | “地图是空白模块”与“热力图已完成”这两条旧判断都已被代码事实修正 | `ui/map/MapViewModel.kt` `ui/map/DiaryMapScreen.kt` `ui/map/MapHeatmapUtils.kt` | 后续继续优化热力图 renderer 和统计维度，但不需要再把地图误判为空白重写 | P2 |
| 标签系统 | `Packet-07` 已完成第一轮层级治理闭环 | `parentId`、父子查询、管理页、颜色建议原本已存在；现已补上多层树形展开、真实移除父标签、父级成环防护，以及首页父标签自动包含子标签的搜索闭环 | 相似标签检测、批量治理建议、基于 tagId 的更稳搜索查询仍未做 | “有树形 UI 就算完成”这条误判已被修正，之前其实只完成了壳子 | `data/Tag.kt` `data/TagDao.kt` `ui/profile/TagManagementScreen.kt` `ui/profile/TagHierarchyUtils.kt` `ui/home/HomeViewModel.kt` | 下一步如果继续做标签治理，优先做相似标签识别与更稳的查询层下推，而不是继续加新花样 | P1 |
| AI 平台层 | 能力散点存在 | 多 provider、缓存、标签建议、搜索解析、风格分析、AI 传记已存在 | 缺统一能力地图和体验分层 | 设置、同意项、具体消费方未统一梳理 | `ai/AiServiceManager.kt` `ai/AiConfigStore.kt` `ui/biography/BiographyViewModel.kt` `ui/stats/StatsViewModel.kt` | 先做 capability map，再决定扩展顺序 | P1 |
| 设置/提醒/通知 | `Packet-01` 已完成主链统一 | `ReminderSettingsRepository` 已把设置页、个人页、ReminderReceiver、AchievementNotificationManager 收口到同一来源 | `dailyReviewPush` 仍只有设置项，尚未接入真实通知消费端；提醒入口仍有 Settings/Profile 双入口 | 历史 `AppPreferences`、`NotificationPreferencesManager`、`ReminderManager` 分裂已通过兼容迁移收口 | `ui/settings/SettingsScreen.kt` `ui/profile/ProfileScreen.kt` `reminder/ReminderSettingsRepository.kt` `reminder/ReminderReceiver.kt` `reminder/NotificationPreferencesManager.kt` | 后续补每日回顾通知真实消费链，并评估是否继续保留 Profile 的重复入口 | P0 |
| 安全配置 | `Packet-03` 已完成第一轮止血 | PIN、AI 配置、隐私相关设置均有；现已新增 `SecureConfigStore`，AI API Key 与 PIN hash/salt/hint 已迁入安全存储并支持旧值迁移 | AI endpoint/model 仍是普通偏好；安全配置入口仍较分散 | “PIN / AI Key 仍直接明文落普通 SharedPreferences”的旧判断已过时，但安全体系仍未完全统一 | `security/SecureConfigStore.kt` `biometric/BiometricHelper.kt` `ai/AiConfigStore.kt` | 后续继续统一敏感配置访问层，评估 endpoint 等是否也要纳入安全层 | P0 |
| 数据库与迁移 | `Packet-03` 已完成高风险止血 | 有备份尝试、迁移逻辑、完整数据库工具；现已移除产品路径 destructive fallback，迁移失败时保留数据库文件并抛出显式异常 | 仍缺面向用户的恢复/修复提示页；数据库打开失败后的 UI 降级体验还没做 | “失败后仍可能 fallback destructive migration”的旧判断已过时 | `data/DiaryDatabase.kt` | 下一步补数据库打开失败时的可见恢复引导，而不是静默异常 | P0 |
| 详情页与关联回顾 | 有部分基础 | 相关日记、同日回顾、同标签线索已有雏形 | 还不是完整的“记忆网络” | 相关逻辑定义仍较弱 | `ui/detail/DiaryDetailScreen.kt` `ui/detail/DiaryDetailViewModel.kt` | 明确相关性规则，再决定是否建链接模型 | P2 |
| 成就/挑战/连续记录 | 不是空白，但叙事未统一 | 挑战、成就、冻结等基础存在；成就详情现已回到主题化视觉，避免原始大图模糊直铺 | 首页、成就页、目标页未形成统一激励层；成就解锁后的系统内激励仍偏散 | 旧实现曾在解锁成就时偷偷创建“里程碑日记”，该错误副作用已修正 | `data/ChallengeManager.kt` `data/StreakFreeze.kt` `data/AchievementRepository.kt` `ui/achievement/AchievementScreen.kt` `ui/achievement/AchievementDetailScreen.kt` | 设计 ProgressSystem 概念层，并继续考虑成就与通知/主页的统一表达 | P2 |
| 写作目标 | `Packet-05` 已完成首页/统计闭环 | `WritingGoal`、DAO、统计页目标区原本已存在；现已抽出共享目标进度模型，并在首页新增轻量目标卡，首页和统计页口径一致 | 首页目前只展示主目标摘要，不是完整目标管理页；更深的提醒/通知联动还没做 | “写作目标只在数据层，用户几乎无感知”的旧判断已部分过时 | `data/WritingGoal.kt` `data/DiaryDao.kt` `ui/stats/WritingGoalProgressUtils.kt` `ui/home/HomeViewModel.kt` `ui/home/HomeScreen.kt` `ui/stats/StatsScreen.kt` | 后续可继续考虑目标完成通知与更细的目标类型，但先不要扩成任务系统 | P0 |
| 待办系统 | 功能过多，方向漂移 | 任务、提醒、习惯、周期能力都已存在 | 与“轻待办”目标不一致 | 模块复杂度和主产品目标不匹配 | `ui/todo/TodoScreen.kt` `ui/todo/TodoViewModel.kt` | 先做产品裁剪，不再叠功能 | P1 |
| 存储治理 | 已具备务实基础 | 孤立文件扫描、重复图片分组、数据库体检已有 | 用户侧解释、清理流程、预览体验仍可加强 | 这是低风险高价值的治理方向 | `ui/storage/StorageViewModel.kt` `ui/storage/StorageScreen.kt` `data/DiaryDatabase.kt` | 先补预览和删除保护 | P1 |
| 备份/导入/导出 | 高风险主链已基本收口 | JSON/Markdown/HTML/PNG 导出、全量备份、导入链路都存在；恢复媒体文件后会重建 `diary_images`；读取导入统一经过 `resolveReadableBackupFile()`，`Documents/DiaryApp` / 内部目录 / Downloads 兼容性更稳定；全量包会额外收集磁盘上真实存在的媒体文件名；导入前确认框现在会显示备份来源类型、图片数量、缺失媒体风险，不再让旧 JSON 和完整备份包混在一起误导用户 | 旧包兼容仍需继续真机核验；失败后的更强引导文案和冲突策略还可以继续增强 | “新版能扫到备份文件但不一定真能读出来”的风险已修正；“导入前看不出这个包有没有图片”这条体验断点也已在本轮修正 | `data/DiaryExporter.kt` `data/BackupManager.kt` `data/DiaryImporter.kt` `ui/backup/BackupScreen.kt` | 后续继续做旧包真机兼容核验和更细的失败引导，但当前主链已比 2.69.0 之后稳定得多 | P1 |
| 通知中心 | 主链可用，但历史残留要持续清理 | 分类、回收站、周报入口通知已存在；本轮已过滤宠物/小岛遗留通知污染 | 通知生产与长期治理仍偏耦合；每日回顾等部分设置未接真实消费端 | 已弃用系统的历史通知内容曾继续出现在通知页，这条旧残留现在已被 UI 层过滤 | `ui/notification/NotificationViewModel.kt` `ui/notification/NotificationScreen.kt` `data/NotificationEntity.kt` | 后续继续清理通知来源并把每日回顾等开关接上真实生产链 | P1 |
| 天气详情 | 页面存在且主路径可进 | 当前天气、逐小时、未来几天、详情卡片均存在；本轮已改成 `Scaffold + pinned top bar` | 数据结构仍偏页面内聚合，和首页天气摘要未完全形成共享 UI 模型 | 旧实现存在顶部空白和标题不贴顶问题，现已修正 | `ui/home/WeatherDetailScreen.kt` `weather/WeatherManager.kt` | 后续再做更细的摘要/详情共享与真机视觉校准 | P2 |
| 更新系统 | 已有完整雏形 | GitHub Release 检查、下载、安装、更新说明存在 | 需要再核查 release 行为在真机上的稳定性 | 与本轮“闪退问题”可形成单独排查专题 | `update/UpdateChecker.kt` `update/ApkInstaller.kt` `update/ChangelogScreen.kt` | 单独做 release 路径核验 | P1 |
| 健康模块 | 需求已搁置，但残留明显 | 页面、开关、应用级状态仍存在 | 与当前产品路线不一致 | 继续保留会误导后续开发 | `DiaryApplication.kt` `ui/health/HealthScreen.kt` | 评估删除或彻底隐藏 | P0 |
| Widget 模块 | 需求已不需要，但代码仍完整 | Provider/Service 仍在 | 非当前主路线能力 | 增加维护面和误判风险 | `widget/TodoWidgetProvider.kt` `widget/CountDownWidgetProvider.kt` | 评估彻底移除 | P0 |

---

## 3. 当前最值得优先接手的闭环主题

按“投入产出比”和“对后续所有功能的基础价值”排序，最值得优先处理的是：

1. 数据库迁移与安全存储止血
2. 首页搜索闭环
3. 写作目标接入首页/统计
4. 周报接主路径
5. 报告分享升级

---

## 4. 给后续 AI 的判读规则

### 看到这些情况，不要直接判定“已完成”

- 只有 Screen 文件
- 只有开关 UI
- 只有 ViewModel 状态
- 只有数据结构
- 只有实验入口

### 至少满足下面 5 点，才可以接近“已闭环”

1. 正式入口存在
2. 运行时消费链路真实打通
3. 设置来源唯一或已被统一映射
4. 用户可见 UI 已完成
5. 有基础验证或测试

---

## 5. 推荐与本矩阵配套使用的文档顺序

1. 先读本文件，建立全局状态图
2. 再读 [docs/ai-handoff-bundle/ai-task-packets.md](/C:/Users/陈仕杰/Desktop/DiaryApp/docs/ai-handoff-bundle/ai-task-packets.md)，按任务包拆给其他 AI
3. 真正实现前，再回读对应专题的审计/拓展文档
