# DiaryApp 全局代码审计：重大问题与矛盾点

> 更新时间：2026-06-26
> 目标：给后续 AI/开发者一个可直接接手的高价值问题清单，只保留影响稳定性、需求一致性、维护成本和真实用户体验的重大问题。
> 证据原则：以下结论全部基于当前仓库代码与现有文档，不基于口头猜测。

---

## 1. 审计结论总览

当前项目已经具备很强的功能密度，但也出现了典型的“功能增长快于结构治理”的状态。最需要优先处理的问题不是再堆新功能，而是先解决下面四类高风险矛盾：

1. 需求已经否定或搁置的模块，代码和入口仍然残留，继续拉高理解成本和误触成本。
2. 首页、统计、编辑器、待办等核心模块仍以超大 Screen / ViewModel 承载多重职责，后续任何改动都容易连带破坏。
3. 搜索、统计、备份、通知、成就等关键链路仍存在“全量加载、内存过滤、跨模块硬耦合”的性能隐患。
4. 安全与数据层虽然补了部分迁移和 release 问题，但仍保留 SharedPreferences 明文敏感数据、数据库兜底 destructive migration 等硬风险。

---

## 2. P0 问题：必须优先治理

### P0-1 需求已删除/搁置的模块仍在代码和入口中存活

**问题描述**

用户已经明确在 [docs/整体目标/FINAL-REQUIREMENTS.md](/C:/Users/陈仕杰/Desktop/DiaryApp/docs/整体目标/FINAL-REQUIREMENTS.md) 中删除或搁置以下方向：

- 模板系统删除，改为 AI 问答引导
- 健康数据搁置
- 桌面小组件不需要

但当前代码中这些方向仍然有明显残留，且部分仍可被用户感知：

- 编辑器仍然把 AI 引导结果通过 `setTemplate(...)` 注入 WebView，语义上仍沿用“模板写入”路径  
  证据：[app/src/main/java/com/diary/app/ui/editor/EditorScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/editor/EditorScreen.kt:922)
- 健康数据仍保留完整模块、实验开关和应用级状态  
  证据：[app/src/main/java/com/diary/app/DiaryApplication.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/DiaryApplication.kt:176)  
  证据：[app/src/main/java/com/diary/app/ui/health/HealthScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/health/HealthScreen.kt:1)
- 工具页仍把“实验性功能”作为公开入口，实验功能页中仍保留地图等未完全收敛的开关体系  
  证据：[app/src/main/java/com/diary/app/ui/tools/ToolsScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/tools/ToolsScreen.kt:409)  
  证据：[app/src/main/java/com/diary/app/ui/experimental/ExperimentalFeaturesScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/experimental/ExperimentalFeaturesScreen.kt:173)
- Widget 相关 Provider / Service 依旧完整存在  
  证据：[app/src/main/java/com/diary/app/widget/TodoWidgetProvider.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/widget/TodoWidgetProvider.kt:25)  
  证据：[app/src/main/java/com/diary/app/widget/CountDownWidgetProvider.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/widget/CountDownWidgetProvider.kt:23)

**影响**

- 后续 AI 很容易把“已删功能”误判为待完善功能，继续错误投资。
- 用户层面会出现“需求说不要，但代码里还在”的体验矛盾。
- 残留分支会持续扩大测试面和回归风险。

**建议动作**

- 建立“需求已废弃能力清单”，先做代码侧收口。
- 明确分为三类处理：
  - 彻底删除：Widget、健康入口、旧模板语义
  - 内部保留但不暴露：仅保留迁移兼容代码
  - 重命名去歧义：把 `setTemplate` 一类旧命名替换为 `insertGuideDraft` / `applyGuideResult`

---

### P0-2 数据库在迁移异常时仍允许 destructive migration，存在真实数据损失风险

**问题描述**

当前数据库初始化逻辑在第一次建库/开库失败后，会 fallback 到 `fallbackToDestructiveMigration()`：

证据：[app/src/main/java/com/diary/app/data/DiaryDatabase.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/data/DiaryDatabase.kt:796)  
证据：[app/src/main/java/com/diary/app/data/DiaryDatabase.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/data/DiaryDatabase.kt:820)

虽然代码在 fallback 之前尝试备份数据库文件，但这仍然不是用户可感知、可恢复、可确认的安全流程。

**影响**

- 升级异常时，用户可能在无感知情况下丢失本地全部数据。
- 这和“单机日记应用”的核心信任模型冲突，是比普通崩溃更重的问题。
- 任何未来 schema 变更都会继续继承这个风险。

**建议动作**

- 将 destructive migration 从主产品路径移除，只允许在 debug/internal build 中启用。
- 迁移失败后改为：
  - 锁定进入只读/修复模式
  - 展示可见错误提示
  - 引导导出数据库文件或触发恢复流程
- 为每次迁移建立“升级样本库”测试，而不是依赖运行时兜底。

**状态更新（2026-06-26，Packet-03 后）**

- 上述“主产品路径仍允许 destructive migration”的判断已过时：当前 `DiaryDatabase.getDatabase()` 已移除 `fallbackToDestructiveMigration()`。
- 现状变为：开库失败时先备份数据库文件到 `filesDir/db_backup`，然后抛出 `DiaryDatabaseOpenException`，不再静默清库。
- 剩余问题从“静默丢库”转为“失败时用户可见恢复体验不足”。

---

### P0-3 安全敏感信息仍保存在 SharedPreferences，和目标中的安全加固要求冲突

**问题描述**

当前至少两类高敏感信息仍以 SharedPreferences 持久化：

- PIN 哈希、盐、提示语、失败次数、锁定时间  
  证据：[app/src/main/java/com/diary/app/biometric/BiometricHelper.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/biometric/BiometricHelper.kt:12)
- AI 服务商 API Key / Endpoint / Model  
  证据：[app/src/main/java/com/diary/app/ai/AiConfigStore.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ai/AiConfigStore.kt:16)

而需求文档已经明确提出：

- PIN 用 Keystore
- API Key 需要安全加固

证据：[docs/整体目标/FINAL-REQUIREMENTS.md](/C:/Users/陈仕杰/Desktop/DiaryApp/docs/整体目标/FINAL-REQUIREMENTS.md)

**影响**

- PIN 提示和认证状态容易被本地导出或调试手段读取。
- API Key 明文持久化对于多服务商配置尤其危险。
- 后续如果加入更多 AI 自动能力，这个风险会继续扩大。

**建议动作**

- PIN 从 SharedPreferences 迁移到 Android Keystore + EncryptedSharedPreferences 或本地加密封装。
- AI Key 至少迁移到 EncryptedSharedPreferences，并统一抽象为 `SecureConfigStore`。
- 安全配置不要再散落在多个类里直接 `getSharedPreferences(...)`。

**状态更新（2026-06-26，Packet-03 后）**

- 上述“PIN / AI Key 仍直接保存在普通 SharedPreferences”的判断已部分过时：
  - 当前已新增 `SecureConfigStore`
  - AI API Key 已迁入安全存储
  - PIN hash/salt/hint 已迁入安全存储，并支持旧值自动迁移
- 仍未完全解决的点：
  - AI endpoint/model 仍是普通偏好
  - 安全配置访问入口仍然分散，尚未形成统一安全配置层

---

### P0-4 首页/搜索仍以全量加载为基础，功能越多越容易拖垮主入口

**问题描述**

`HomeViewModel` 直接长期持有 `dao.getAllPreviews()` 的全量结果，并在其上构建：

- 首页日历摘要
- 首页统计
- 搜索结果
- 筛选结果
- 智能问候语相关数据

证据：[app/src/main/java/com/diary/app/ui/home/HomeViewModel.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/home/HomeViewModel.kt:161)  
证据：[app/src/main/java/com/diary/app/ui/home/HomeViewModel.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/home/HomeViewModel.kt:202)  
证据：[app/src/main/java/com/diary/app/ui/home/HomeViewModel.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/home/HomeViewModel.kt:266)

在筛选为空时，搜索还会继续对内存中的全量条目做过滤。

**影响**

- 首页已经是应用第一入口，一旦数据量上升，启动、回到前台、搜索输入都会一起变慢。
- 新增高级筛选、语义搜索、提示卡、连续记录展示后，首页会继续膨胀。
- 这也是“Android Studio 正常、真机 release 更容易暴露问题”的典型温床。

**建议动作**

- 把首页拆成“摘要查询”而不是“全量预览缓存”。
- 为首页建立专用 DAO：
  - 日历摘要查询
  - 最近条目分页查询
  - 搜索分页查询
  - 首页统计聚合查询
- 搜索结果改为数据库过滤优先，内存过滤只做 UI 层的二次精修。

---

## 3. P1 问题：高概率持续拖慢后续开发

### P1-1 核心页面体积过大，已进入“修改一处、回归多处”的危险阶段

**证据**

- [app/src/main/java/com/diary/app/ui/stats/StatsScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/stats/StatsScreen.kt:1) 约 2110 行
- [app/src/main/java/com/diary/app/ui/editor/EditorScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/editor/EditorScreen.kt:1) 约 1878 行
- [app/src/main/java/com/diary/app/ui/todo/TodoScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/todo/TodoScreen.kt:1) 约 1555 行
- [app/src/main/java/com/diary/app/ui/home/HomeScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/home/HomeScreen.kt:1) 约 1517 行
- [app/src/main/java/com/diary/app/ui/timeline/TimelineScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/timeline/TimelineScreen.kt:1) 约 1328 行
- [app/src/main/java/com/diary/app/ui/annualreport/AnnualReportScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/annualreport/AnnualReportScreen.kt:1) 约 1241 行
- [app/src/main/java/com/diary/app/ui/profile/ProfileScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/profile/ProfileScreen.kt:1) 约 1159 行
- [app/src/main/java/com/diary/app/ui/editor/EditorViewModel.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/editor/EditorViewModel.kt:1) 约 400 行

**问题本质**

这不只是“文件大”，而是状态、事件、导航、弹窗、副作用、数据适配混在一起。后果是：

- 很难做局部测试
- 很难做 UI/交互替换
- 很难判断某个开关影响了哪一段逻辑
- 新 AI 接手时上下文成本极高

**建议动作**

- 先拆最核心三块：Home、Editor、Stats。
- 拆分原则不是按技术层，而是按业务块：
  - Header / summary
  - content sections
  - dialog / sheet
  - mapper / formatter
  - side-effect coordinator

---

### P1-2 SharedPreferences 分散且命名无统一规范，偏好体系已经失控

**现状**

代码中直接出现大量 `getSharedPreferences(...)` 调用，涉及：

- `diary_prefs`
- `search_prefs`
- `editor_drafts`
- `ai_cache`
- `notification_cache`
- `sensor_health`
- `achievement_notified`
- `diary_db_prefs`
- 多个 experimental / reminder / theme 偏好文件

证据：全局检索结果见  
[app/src/main/java](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java)

**影响**

- 偏好来源不透明，排查问题需要跨很多文件。
- 很容易出现设置页改了，实际消费方没统一读取的情况。
- 未来若要迁移 DataStore，工作量会被历史分散设计放大。

**建议动作**

- 第一阶段先收口成 3 类：
  - 用户偏好 `UserPreferencesStore`
  - 安全配置 `SecureConfigStore`
  - 技术缓存 `RuntimeCacheStore`
- 第二阶段再考虑 DataStore 迁移。

---

### P1-3 搜索增强只完成了一部分，需求和实现之间仍有明显断层

**已完成部分**

- 搜索历史持久化已经做了  
  证据：[app/src/main/java/com/diary/app/ui/home/HomeViewModel.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/home/HomeViewModel.kt:140)
- 已有基础筛选：心情、天气、收藏、日期  
  证据：[app/src/main/java/com/diary/app/ui/home/HomeViewModel.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/home/HomeViewModel.kt:145)
- 已有基础智能解析入口  
  证据：[app/src/main/java/com/diary/app/ui/home/HomeViewModel.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/home/HomeViewModel.kt:370)

**未完成或仍不一致的部分**

- 标签筛选未进入当前筛选状态结构
- 字数范围筛选未落地
- 建议词目前只来自标签名，没有地点补全、热门词、历史频率整合
- 语义搜索更像“规则/提示词解析后的高级筛选”，还不是稳定的语义检索能力
- `HomeScreen` 中未检索到完整的建议浮层渲染证据，说明 UI 端交互闭环仍不完整

**影响**

- 用户会感觉“看起来有高级搜索，但不够完整”
- 后续继续补需求时容易在已有半成品上叠加更多例外逻辑

**建议动作**

- 先把搜索能力定义成三层：
  - 基础全文检索
  - 结构化筛选
  - AI 语义解析
- 每层有独立状态模型，不要继续在 `HomeViewModel` 里平铺扩展。

---

### P1-4 待办系统已经远超“最简精致待办”，产品方向和代码复杂度不匹配

**现状**

需求要求待办走“最简单版本”，但当前 Todo 系统已经包含：

- 任务/提醒/习惯多类别
- 周期任务
- 习惯打卡记录
- 标签联动
- 习惯详情页与记录弹窗

证据：[app/src/main/java/com/diary/app/ui/todo/TodoViewModel.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/todo/TodoViewModel.kt:53)  
证据：[app/src/main/java/com/diary/app/ui/todo/TodoScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/todo/TodoScreen.kt:116)

**影响**

- 功能不是越多越好，当前复杂度已经和用户期待偏离。
- 待办模块变成一个次级产品，维护成本不成比例。

**建议动作**

- 先做产品裁剪而不是再加功能。
- 明确保留层次：
  - 保留：任务、提醒、简单完成动画、分类
  - 延后：复杂习惯交互、深度统计、过多详情入口

---

## 4. P2 问题：不是马上爆炸，但会持续累积债务

### P2-1 月报/年报/周报能力分散，报告体系重复建设风险高

当前已经有：

- [app/src/main/java/com/diary/app/ui/monthlyreport/MonthlyReportScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/monthlyreport/MonthlyReportScreen.kt:1)
- [app/src/main/java/com/diary/app/ui/annualreport/AnnualReportScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/annualreport/AnnualReportScreen.kt:1)
- [app/src/main/java/com/diary/app/ui/weeklyreport/WeeklyReportScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/weeklyreport/WeeklyReportScreen.kt:49)

说明周报基础已经出现，不再是完全空白。但三套报告如果继续各写一套 UI、统计、分享、文案，就会越来越难统一。

**建议动作**

- 报告能力沉到 shared report engine：
  - 时间范围
  - 指标聚合
  - 卡片 DSL
  - 分享渲染

---

### P2-2 成就系统已经开始扩展挑战，但“成就/挑战/连续记录”还没有统一叙事层

当前 Challenge 相关能力已经存在：

- [app/src/main/java/com/diary/app/data/ChallengeManager.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/data/ChallengeManager.kt:11)
- [app/src/main/java/com/diary/app/ui/achievement/AchievementScreen.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/achievement/AchievementScreen.kt:134)

这意味着“每周挑战完全未开始”这一判断已经过时。真正的问题变成：

- 成就、挑战、连续记录、目标追踪分属不同视觉和数据体系
- 首页与成就页的激励反馈没有统一状态中心

**建议动作**

- 建立 `ProgressSystem` 概念层，统一：
  - streak
  - challenge
  - achievement
  - writing goal

---

### P2-3 测试数量比之前更好了，但覆盖面和模块复杂度仍不成比例

当前主代码约 203 个文件，测试约 33 个文件。

已有不少新增测试集中在：

- 迁移
- 搜索工具
- 导出/导入
- 快捷入口
- Boot 恢复

这是明显进步，但和当前功能体量相比仍然偏薄，尤其以下高风险区域覆盖不够：

- HomeScreen / HomeViewModel 主流程
- TodoScreen 复杂交互
- 报告页聚合逻辑
- 安全配置迁移
- 实验功能和导航开关

**建议动作**

- 别追求总体覆盖率数字，先补“高损失路径”。

---

## 5. 模块级矛盾摘要

### 编辑器

- 名义上已删除模板，语义上仍残留模板注入路径。
- AI 写作引导与富文本编辑器桥接层还不够语义清晰。
- 后续要做语音、附件、表格、代码块时，当前单文件结构会迅速失控。

### 首页

- 首页承担了太多责任：概览、搜索、筛选、统计、提示、快捷入口、日历。
- 当前实现更像“聚合控制台”，不是轻量首页。

### 统计与报告

- StatsScreen、月报、年报、周报之间边界模糊。
- 指标复用与 UI 复用不足，越做越容易复制粘贴。

### 待办

- 用户要简洁，代码却逐步走向复杂任务系统。
- 这是典型的产品目标漂移。

### 安全与设置

- 设置项很多，但安全能力并未形成系统化设计。
- 偏好、缓存、敏感配置、实验开关都混在一起。

---

## 6. 推荐整改顺序

### 第一阶段：先止血

1. 移除 destructive migration 的产品路径。
2. 收口健康/Widget/旧模板等已废弃能力。
3. 把 PIN / AI Key 迁移到更安全的存储方案。

### 第二阶段：核心结构治理

1. 拆 Home、Editor、Stats 三个最大模块。
2. 收口 SharedPreferences，建立统一配置访问层。
3. 首页搜索改为查询驱动，不再依赖全量缓存。

### 第三阶段：再继续功能扩展

1. 在稳定结构上补搜索增强、连续记录、报告分享、地图升级。
2. 统一激励系统与报告系统。

---

## 7. 给后续 AI 的接手建议

如果后续由其他 AI 继续接手，不要直接从“加新功能”开始。更合理的顺序是：

1. 先读本文件，识别哪些是“已做一半”的能力，避免重复判断。
2. 再读 [docs/ai-handoff-bundle/feature-expansion-report.md](/C:/Users/陈仕杰/Desktop/DiaryApp/docs/ai-handoff-bundle/feature-expansion-report.md)，确认哪些扩展方向值得保留。
3. 真正开工前，先做一次“需求残留清理 + 首页/编辑器/统计三大模块边界图”。
