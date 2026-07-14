# 深化 PRD：DiaryApp「成就收藏」模块

> 一句话定位：把"成就收藏"从"只能看的徽章陈列馆"，升级为"会随你记录而生长、可主动收藏、可炫耀、可回望的生活藏品系统"。
> 目标版本：v2.78.22-experimental 之后

---

## 0. 代码现状核查结论（基于真实代码）

- `Achievement` 实体字段：`key/name/description/iconName/unlockedAt/progress(Int)/target(Int)/category(String)/tier(Int)/iconEmoji/flavorText/isHidden`。**没有 `relatedEntryId` 列**——"跳转到促成它的那篇日记"目前结构上不可实现。
- `AchievementState` 里有 `relatedEntryId: Long?`，但 `AchievementRepository.getAllItems()` 永远传 `null`。
- 8 分类 × 4 稀有度已齐备；种子数据共 **51 个成就**（WRITING 13 / HABIT 4 / TIME 7 / MOOD 6 / WEATHER 6 / COLLECTOR 2 / EXPLORER 9 / LEGENDARY 4）。
- 解锁引擎 `checkAndUnlock()` 是**一次性全量重算**（读全部日记统计算一遍），无事件驱动、无增量；仅在 `DiaryApplication.onCreate`、`AchievementViewModel.init`、`refresh()` 调用。
- 进度模型只有单调计数 `progress/target`，无法表达"连续 N 篇心情≥5""写了全部 7 个星期几"等条件型成就。
- **当前有 12 个成就永远无法解锁（约 23.5%）**：`time_capsule_master`、`mood_rollercoaster`、`fearless_recorder`、`legendary_all_categories` 这 4 个不在 `updates` 映射里；5 个隐藏成就（`time_traveler/new_year_eve/midnight_bell/full_moon/first_echo`）同样缺映射；`flash_writer/deep_writer/twin_stars` 被硬编码为 0。
- `AchievementScreen`：3 列网格 + 进度环总览卡 + 状态/分类/稀有度筛选 chip + 紧凑徽章卡，已用 `GlassCard`/`GradientBackground`/`MaterialTheme.colorScheme`。
- `AchievementBadge`（`AchievementIcons.kt`）：渐变圆 + 矢量图标 + 按稀有度差异化绘制，锁定态加遮罩与锁图标——良好视觉基底。
- 详情页：徽章 + 名 + 稀有度 + 描述 + 风味文案 + 进度条 + 解锁时间 + **纯文本分享**，无进度故事线、无日记跳转、无庆祝动效、无生图导出。
- "月度挑战"入口卡已存在（`onNavigateToMonthlyChallenge`），且 `MonthlyChallenge`/`ChallengeDailyLog` 表已建，但**没有对应界面**——死入口。
- ⚠️ 多处**硬编码颜色**违反"禁止硬编码颜色"：`AchievementScreen.TierColors`、`AchievementIcons.CategoryColors`、详情页 `tierColor()`/`oldTierColor()`、`AchievementBadge` 内 `Color.Black/White` 直接使用。需迁移到 `MaterialTheme.colorScheme` + `LocalExtendedColors`。
- `CrossSystemManager` 已暴露 `recentAchievementUnlock` / `nextAchievementMilestone` 两个 StateFlow，首页/通知联动"管道"已存在。

---

## 1. 产品目标

1. **让解锁"活"起来**：从"进页面才重算"改为"写日记/收藏/打卡即实时触发"，补齐 12 个不可解锁成就。
2. **让藏品"可被拥有"**：主动收藏/心愿单、陈列柜与封面，把"已解锁成就"从列表升级为可把玩、可排序、可设封面的个人收藏。
3. **让成长"可感知"**：克制庆祝动效、连击/里程碑、等级/经验与分享，形成"记录→反馈→再记录"正循环，不触碰隐私（分享仅本地）。

---

## 2. 用户故事

| # | 故事 |
|---|------|
| US-1 | 保存日记时**立刻**收到解锁提示，而非进成就页才看到 |
| US-2 | 把"还差一点"的成就**加心愿单并置顶**，打开就看进度 |
| US-3 | 点开已解锁成就能**跳到促成它的那篇日记** |
| US-4 | 把刚解锁成就**生成本地图片**导出/分享，绝不联网 |
| US-5 | 已解锁成就能像**藏品一样陈列**，可排序、设封面 |
| US-6 | 看到**连击/streak 与里程碑庆祝**，有节奏感但不打扰 |
| US-7 | 隐藏成就给**模糊线索**而非剧透，解锁有惊喜 |
| US-8 | 成就能**汇入年度报告**，看"这一年点亮了什么" |
| US-9 | 有**月度/季节性挑战链**，完成给限定成就 |
| US-10 | 有**等级/经验**凝练"我是怎样的记录者" |

---

## 3. 需求池（P0 / P1 / P2）

### P0 — 地基与核心体验

- **P0-1 事件驱动解锁引擎**：废弃"进页面全量重算"，写操作（insertEntry/收藏切换/打卡/编辑）派发 `AchievementEvent`，`AchievementEngine` 增量评估仅受影响的成就并即时更新；保留全量 `recompute()` 作迁移/兜底。验收：保存日记 ≤1s 内相关进度/解锁同步；全量重算调用下降 ≥90%；增量评估不读全部日记原文。
- **P0-2 补全现有成就解锁逻辑**：把 12 个"永远无法解锁"的成就纳入引擎（隐藏 5 个 + `time_capsule_master`/`mood_rollercoaster`/`fearless_recorder`/`legendary_all_categories` + `flash_writer/deep_writer/twin_stars` 改真实判定）。验收：51 个中 ≥50 个有可验证解锁路径；隐藏成就满足时出现解锁记录；`legendary_all_categories` 在 8 分类全解锁后触发。
- **P0-3 数据模型增强**：`Achievement` 实体新增 `relatedEntryId: Long?`（Room 迁移 `ALTER TABLE`）+ `proofType`/`lastEventAt` 等字段支持条件型成就增量判定。让 `AchievementState.relatedEntryId` 真正写入。验收：已解锁成就 100% 能回溯"最近一次促成它的日记 id"；提供不破坏旧数据的迁移。
- **P0-4 解锁实时庆祝动效 + 即时 in-app 通知**：解锁瞬间播放**克制**庆祝动效（徽章放大/光晕/柔光粒子，复用 `AchievementBadge`），并即时经 `AchievementNotificationManager` 弹 in-app 横幅（不弹系统通知）。验收：解锁到横幅 ≤1s；同成就不重复弹（去重）；动效 ≤1.2s、可关、不打断写作；安静时段不弹。

### P1 — 深入优化与功能拓展

- **P1-1 成就详情页丰富化**：增加"进度故事线"（关键节点时间轴）、"解锁瞬间回看"、"跳转到促成它的那篇日记"。验收：已解锁详情含"查看对应日记"入口并可达；时间轴至少"开始/最近进展/达成"三节点。
- **P1-2 用户主动"收藏/心愿单"**：任意成就可加入心愿单，置顶/排序、标记进行中；总览快捷入口，首页展示 1–3 个进行中心愿。验收：增删改 <200ms；置顶项在总览优先；本地持久化。
- **P1-3 成就陈列柜 / 成就墙**：已解锁成就以"藏品"陈列，自定义排序、设封面（选 1 枚作封面）、网格/瀑布流切换。验收：仅含已解锁项；封面即时生效并同步总览头；排序本地持久化；零已解锁时引导空态。
- **P1-4 等级/经验/声望 或 成就路线**：基于已解锁成就（按稀有度加权）算 EXP→Level，给"记录者类型"称号；1–2 条成就路线。验收：公式可配置可单测；等级变化触发轻度庆祝；与 `CrossSystemManager`/首页联动。（是否引入见 §5 Q1，推荐轻量版）
- **P1-5 成就分享本地化（生图导出，不联网）**：成就卡渲染为本地图片存沙盒/相册，经系统分享面板分享；全程不联网不上传。验收：产物为本地 PNG（≥1080px 宽）；隐私审计无网络请求；失败本地兜底（复制文本）。
- **P1-6 限时/季节性/挑战链成就**：复用 `MonthlyChallenge`/`ChallengeDailyLog` 数据模型，落地"月度挑战"界面（补全死入口），完成给限定成就；季节性/节日限时成就（如"跨年守夜人"接真实判定）。验收：月度挑战可进入、记录完成天数、达成给成就；限时到点自动过期/锁线索。
- **P1-7 隐藏成就线索提示**：隐藏成就未解锁显示模糊线索而非 `???`；解锁揭示全貌+庆祝。验收：5 个隐藏成就各有独立线索文案；解锁前不泄露名称/条件；线索在"隐藏线索"筛选下可见。

### P2 — 进阶与联动

- **P2-1 连击/里程碑庆祝**：streak/连续类成就连击庆祝与里程碑（7/30/100 天）专属动效与横幅；"本周目标进度"轻量环。
- **P2-2 与年度报告联动**：成就解锁记录（时间/稀有度）汇入年度报告"年度点亮地图"。
- **P2-3 与首页/Widget 联动**：复用 `CrossSystemManager` 的 `recentAchievementUnlock`/`nextAchievementMilestone`，首页/Widget 展示"刚解锁/最接近"。
- **P2-4 成就图鉴导出**：个人成就图鉴导出本地 PDF/长图（仅本地）。
- **P2-5 克制反馈（音效/震动）**：解锁可关闭轻微震动/音效，遵循系统勿扰与安静时段，不依赖外部 SDK。

---

## 4. UI 设计稿（结构描述）

### 4.1 信息架构

```
Home/Widget → 成就总览 → 成就图鉴 / 成就陈列柜 / 我的心愿单
图鉴/陈列柜/心愿单 → 成就详情 → 促成它的那篇日记
```

### 4.2 主要页面

- **① 成就总览**：进度环（总解锁占比）+ hero 文案；若引入等级则加 EXP/等级细条+称号；快捷区（月度挑战｜陈列柜｜心愿单）；区块：最近解锁、最接近完成、心愿单进行中。
- **② 成就图鉴**：3 列网格 + 筛选（状态/分类/稀有度）走语义色；徽章卡已解锁显真名+徽章，未解锁显进度%，心愿单项加"★追踪"角标，隐藏项显线索。
- **③ 成就详情**：大号 `AchievementArtwork` + 稀有度/分类 chip + 名；进度故事线（三节点）；解锁瞬间回看 + "查看对应日记"；行动区：心愿单、分享（本地生图）、设封面。
- **④ 成就陈列柜**：仅已解锁，网格/瀑布流，长按拖拽排序，设封面同步总览头，空态引导。
- **⑤ 我的心愿单**：用户主动加入，按进度/置顶排序，显进度条与"距达成还差 X"。

### 4.3 解锁 → 庆祝 → 联动 流程

```
写日记/收藏/打卡/编辑 → 派发 AchievementEvent
  → AchievementEngine 增量评估 → 达条件?
    是 → 写 unlockedAt + relatedEntryId → 克制庆祝动效 + in-app 横幅 → CrossSystemManager 更新首页/Widget
    否 → 更新 progress + lastEventAt
```

---

## 5. 待确认问题（需用户/架构师拍板）

1. **Q1 是否引入等级/经验/声望（P1-4）？** 推荐：轻量版（EXP 由已解锁成就按稀有度加权，1–2 条路线），不做复杂声望经济。备选：仅"记录者类型"称号，不做数值等级。
2. **Q2 分享是否仅本地？** 推荐：仅本地生图 + 系统分享面板，绝不联网上传。需确认是否复用 `Share` 意图 + 新增 `Compose→Bitmap` 渲染。
3. **Q3 庆祝动效强度？** 推荐：克制版（徽章光晕+轻微缩放+柔光粒子，≤1.2s，可关，安静时段不弹）。需确认是否允许轻微震动/音效（P2-5）。
4. **Q4 事件驱动落点？** 推荐 hook：`insertEntry`、收藏切换、打卡(`QuickCheckin`/`HabitRecord`)、编辑首篇。需确认是否新增统一 `AchievementEventBus`（基于 `AppContainer` 手动 DI）。
5. **Q5 隐藏成就线索尺度？** 推荐：模糊线索而非完全 `???`，绝不泄露名称/条件。
6. **Q6 设计系统迁移范围？** 必须移除 `TierColors`/`CategoryColors`/`oldTierColor` 等硬编码，改 `LocalExtendedColors` 语义色。需确认 `LocalExtendedColors` 是否已定义分类/稀有度色槽。
7. **Q7 月度挑战是否本期一并落地（P1-6）？** 推荐：本期补全界面复用已有 `MonthlyChallenge` 模型（死入口变活），限定 1 条月度挑战模板，避免范围蔓延。

---

## 附录：灵感参考（非必须）

- Duolingo 连击(Streak)：每日连续 + 里程碑庆祝 + 轻提醒 → P0-4/P2-1
- Apple 健身圆环：多环并行、接近闭合张力 → 总览进度环与"最接近完成"
- 游戏图鉴/收藏册：分类陈列、排序、封面、隐藏线索 → P1-3/P1-7
- Steam 成就/年度回顾：解锁瞬间回看、年度汇总 → P1-1/P2-2
