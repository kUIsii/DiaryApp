# DiaryApp「成就收藏」模块深化 — 系统架构设计 + 任务分解

> 架构师：高见远（Gao）｜模块：Achievement Collection Deepening｜基线：Jetpack Compose + Material 3 + 手动 DI（无 Hilt）+ Room v38
> 配套图：`docs/class-diagram.mermaid`、`docs/sequence-diagram.mermaid`

---

## 1. 实现方案 + 框架选型

### 1.1 整体分层架构

```
写入层(ViewModel) ──publish──▶ AchievementEventBus(SharedFlow)
                                        │ collect(IO, 串行)
                                        ▼
                              AchievementEngine(核心)
                          ┌─────────────┼─────────────┐
                          ▼             ▼             ▼
                  DiaryStatsSnapshot  AchievementRules  AchievementRepository
                  (previews 聚合)     (51+ 规则)        (Room 读写)
                                                        │
                          ┌─────────────────────────────┤
                          ▼                             ▼
                 AchievementNotificationManager    CrossSystemManager
                 (in-app 横幅 + 庆祝去重)          (首页/Widget 联动 + Level)
```

- **事件总线 `AchievementEventBus`**：基于 `AppContainer` 手动 DI 持有的 `MutableSharedFlow<AchievementEvent>`，写入层在「保存/收藏/打卡/编辑首篇/完成挑战日」时 `publish`，引擎 `collect`（Dispatchers.IO，串行）消费。
- **增量引擎 `AchievementEngine`**：订阅事件 → 构建一次轻量 `DiaryStatsSnapshot`（仅 `DiaryDao.getAllPreviews()` 投影与少量聚合，**绝不读正文**）→ 仅对「受该事件影响」的成就执行 `evaluateKey()`；达标则 `unlockWithProof` 写 `unlockedAt + relatedEntryId`，否则 `setProgress`。
- **全量兜底 `recompute()`**：保留旧 `checkAndUnlock` 的语义为 `engine.recompute()`，仅在「首次启动 / 数据导入 / 显式刷新」时调用，作为最终一致性与迁移兜底。**不再**在每次 `AchievementViewModel.init` 调用。
- **庆祝与联动**：解锁瞬间经 `AchievementNotificationManager`（已有安静时段/去重）弹 in-app 横幅，并经 `CelebrationController` 发射 `AchievementCelebration` 给根布局的 `AchievementCelebrationOverlay` 播放克制动效；同时更新 `CrossSystemManager` 的 `recentAchievementUnlock` / `nextAchievementMilestone` / 新增 `levelInfo`。

### 1.2 关键设计决策与理由

| 决策 | 选择 | 理由 |
|---|---|---|
| 事件驱动落点 | 统一的 `AchievementEventBus` + 在 ViewModel 写入处 publish | 与现有手动 DI 一致；避免包裹 Room DAO（Room 接口无法干净代理）；调用点集中、可测 |
| 增量 vs 聚合 | 事件触发 + 轻量快照混合 | 纯增量无法表达「单月心情变化次数」「连续12个月」等跨期条件；对这类成就在事件触发时基于一次 `previews` 聚合估算，仍远优于全量重算 |
| 12 个不可解锁成就 | 全部纳入 `AchievementRules` 显式规则 | 补 `time_capsule_master`(12 个不同月份, 已从 monthBuckets 算)、`mood_rollercoaster`(单月心情落差计数)、`fearless_recorder`(极端天气集合命中)、`legendary_all_categories`(8 分类各≥1 已解锁)、5 个隐藏(时间/节日精确判定)、`flash_writer/deep_writer/twin_stars`(真实判定) |
| 数据模型增强 | `Achievement` 加 `relatedEntryId / proofType / lastEventAt` | 支持「跳转到促成它的那篇日记」与增量正确性回溯；迁移用幂等 `ALTER` |
| 等级/经验 | P1-4 轻量版，计算式无表 | EXP = Σ(稀有度权重)，Level 由阈值表得出；不落库，随时从已解锁集重算，零迁移 |
| 本地分享 | Compose `GraphicsLayer.toImageBitmap()` + `FileProvider` + `ACTION_SEND` | 零新依赖、不联网；满足隐私审计 |
| 设计系统迁移(Q6) | 新增 `LocalAchievementColors` CompositionLocal | `LocalExtendedColors` 仅有 success/warning/info/gradient 槽，**无分类/稀有度色槽**；分类色属「品牌身份色」，稳定板更优，把字面量集中到单一定义点，调用点不再硬编码 |
| 月度挑战(Q7) | 复用 `MonthlyChallenge`/`ChallengeDailyLog`，限 1 模板 | 死入口变活；达成给 1 个限定成就 `monthly_champion`（经 seed 自动播种） |

### 1.3 依赖包列表

**零新增依赖**。所需能力均已具备：
- Compose `GraphicsLayer.toImageBitmap()`（分享生图）、`animate*`/`InfiniteTransition`（动效）、`SharedFlow`（事件总线）
- 系统 `Intent.ACTION_SEND` + `FileProvider`（分享）、`Vibrator`（P2-5 震动）
- Room / Kotlin Coroutine / StateFlow（既有）

---

## 2. 文件列表（相对路径）

### 新增文件

**引擎层（P0 地基，M1）**
- `app/src/main/java/com/diary/app/achievement/AchievementEvent.kt` — 事件 sealed 接口及子类
- `app/src/main/java/com/diary/app/achievement/AchievementEventBus.kt` — SharedFlow 发布/订阅（AppContainer 持有）
- `app/src/main/java/com/diary/app/achievement/AchievementEngine.kt` — 核心增量引擎 + `recompute()` 兜底
- `app/src/main/java/com/diary/app/achievement/AchievementRules.kt` — 全部成就（含 12 个补全）的评估规则
- `app/src/main/java/com/diary/app/achievement/DiaryStatsSnapshot.kt` — 轻量聚合快照（基于 previews）
- `app/src/main/java/com/diary/app/achievement/AchievementLevelCalculator.kt` — P1-4 EXP/Level 计算（无表）
- `app/src/main/java/com/diary/app/achievement/CelebrationController.kt` — 庆祝发射器（SharedFlow）

**数据层（M1/M3）**
- `app/src/main/java/com/diary/app/data/WishlistItem.kt` — 心愿单实体（P1-2）
- `app/src/main/java/com/diary/app/data/DisplayCaseEntry.kt` — 陈列柜排序/封面实体（P1-3）
- `app/src/main/java/com/diary/app/data/AchievementCollectionDao.kt` — 心愿单/陈列柜 DAO

**UI 层（M2/M3/M4）**
- `app/src/main/java/com/diary/app/ui/theme/AchievementColors.kt` — `LocalAchievementColors`（Q6 设计系统迁移）
- `app/src/main/java/com/diary/app/ui/achievement/AchievementWishlistScreen.kt` — 心愿单页（P1-2）
- `app/src/main/java/com/diary/app/ui/achievement/AchievementDisplayCaseScreen.kt` — 陈列柜页（P1-3）
- `app/src/main/java/com/diary/app/ui/achievement/AchievementMonthlyChallengeScreen.kt` — 月度挑战页（P1-6）
- `app/src/main/java/com/diary/app/ui/achievement/AchievementCelebrationOverlay.kt` — 克制动效覆盖层（P0-4）
- `app/src/main/java/com/diary/app/ui/achievement/AchievementShareCard.kt` — 分享卡 Composable（P1-5）
- `app/src/main/java/com/diary/app/ui/achievement/AchievementBitmapExporter.kt` — Compose→Bitmap 导出（P1-5）
- `app/src/main/java/com/diary/app/ui/achievement/AchievementLevelBadge.kt` — 等级/EXP 展示（P1-4）
- `app/src/main/java/com/diary/app/ui/achievement/AchievementHiddenClue.kt` — 隐藏线索文案组件（P1-7）

**测试**
- `app/src/test/java/com/diary/app/achievement/AchievementRulesTest.kt` — 12 个补全规则 + 等级公式单测

### 修改文件

- `app/src/main/java/com/diary/app/data/Achievement.kt` — 加 `relatedEntryId / proofType / lastEventAt`
- `app/src/main/java/com/diary/app/data/AchievementModels.kt` — `AchievementDef` 加 `clue: String`（隐藏线索，P1-7）
- `app/src/main/java/com/diary/app/data/AchievementDao.kt` — 加 `unlockWithProof(key, entryId, proofType, at)` 覆盖式更新
- `app/src/main/java/com/diary/app/data/AchievementRepository.kt` — 接入引擎；`getAllItems` 用新 `relatedEntryId` 列；新增心愿单/陈列柜/等级方法；保留 `recompute()` 兜底
- `app/src/main/java/com/diary/app/data/DiaryDatabase.kt` — version 38→39；新增 `MIGRATION_38_39`；注册 `WishlistItem`/`DisplayCaseEntry`；加入 `ALL_MIGRATIONS`
- `app/src/main/java/com/diary/app/ui/achievement/AchievementScreen.kt` — 移除 `TierColors` 硬编码 → `LocalAchievementColors`；新增心愿单/陈列柜/等级入口卡；隐藏线索筛选
- `app/src/main/java/com/diary/app/ui/achievement/AchievementIcons.kt` — 移除 `CategoryColors` 硬编码 → `LocalAchievementColors`；`AchievementBadge` 内 `Color.Black/White` 改 `colorScheme` 语义色
- `app/src/main/java/com/diary/app/ui/achievement/AchievementDetailScreen.kt` — 移除 `tierColor/categoryColor` 硬编码；加进度故事线、相关日记跳转、分享、设封面
- `app/src/main/java/com/diary/app/ui/achievement/AchievementDetailSheet.kt` — 移除 `oldTierColor` 硬编码
- `app/src/main/java/com/diary/app/ui/achievement/AchievementGalleryState.kt` — 支持隐藏线索、心愿单进行中区块
- `app/src/main/java/com/diary/app/ui/achievement/AchievementViewModel.kt` — 由 AppContainer 注入引擎；新增 `wishlist/displayCase/levelInfo/celebration` StateFlow；`refresh()` 改调 `engine.recompute()`
- `app/src/main/java/com/diary/app/di/AppContainer.kt` — 持有 `AchievementEngine`/`AchievementEventBus`/`AchievementLevelCalculator` 并 `engine.start()`
- `app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt` — `Screen` sealed 加 `Wishlist/DisplayCase/MonthlyChallenge`；注册 composable 路由
- `app/src/main/java/com/diary/app/data/CrossSystemManager.kt` — 新增 `levelInfo` StateFlow + `updateLevelInfo()`
- `app/src/main/java/com/diary/app/ui/editor/EditorViewModel.kt` — 保存/编辑后 `bus.publish(EntrySaved/EntryEdited)`
- 收藏切换调用点（如 `HomeViewModel` / `FavoritesScreen` / `DiaryDetailViewModel` 中 `toggleFavorite`）— `bus.publish(EntryFavorited)`
- `app/src/main/java/com/diary/app/ui/quickcheckin/QuickCheckinViewModel.kt`（或对应写入处）— `bus.publish(QuickCheckinCreated)`
- `app/src/main/java/com/diary/app/data/UnifiedAchievementSeedData.kt` — 隐藏成就加 `clue`；新增 `monthly_champion`（P1-6，限 1）
- `app/proguard-rules.pro` — 确认/补充 Room 实体与 DAO `-keep`（R8 注意事项，见 §6/§8）

---

## 3. 数据结构和接口（类图，详见 `docs/class-diagram.mermaid`）

核心契约（Kotlin 签名摘要）：

```kotlin
// AchievementEvent.kt
sealed interface AchievementEvent
data class EntrySaved(val entryId: Long, val moodLevel: Int?, val weather: String?,
    val createdAt: Long, val writingDurationSeconds: Long?, val isFirstEntry: Boolean) : AchievementEvent
data class EntryEdited(val entryId: Long, val isFirstEntry: Boolean) : AchievementEvent
data class EntryFavorited(val entryId: Long) : AchievementEvent
data class QuickCheckinCreated(val checkinId: Long, val createdAt: Long) : AchievementEvent
data class HabitRecorded(val todoId: Long, val recordDate: Long) : AchievementEvent
data class ChallengeDayCompleted(val challengeId: Long, val date: Long, val diaryId: Long?) : AchievementEvent
object RecomputeRequested : AchievementEvent

// AchievementEventBus.kt
class AchievementEventBus {
    private val _events = MutableSharedFlow<AchievementEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<AchievementEvent> = _events.asSharedFlow()
    fun publish(event: AchievementEvent) { _events.tryEmit(event) }
}

// AchievementEngine.kt
class AchievementEngine(
    private val repository: AchievementRepository,
    private val diaryDao: DiaryDao,
    private val bus: AchievementEventBus,
    private val notifier: AchievementNotificationManager,
    private val cross: CrossSystemManager,
    private val levelCalculator: AchievementLevelCalculator,
    private val appContext: Context
) {
    fun start()  // bus.events.onEach { evaluate(it) }.launchIn(ioScope)
    fun evaluate(event: AchievementEvent)
    fun recompute()  // 兜底：基于全量 snapshot 跑全部规则
    private fun evaluateKey(key: String, snap: DiaryStatsSnapshot, event: AchievementEvent): RuleResult
    private fun onUnlock(ach: Achievement, entryId: Long?, proofType: String)
}

// AchievementRepository 扩展方法
suspend fun unlockWithProof(key: String, entryId: Long?, proofType: String)
suspend fun setProgress(key: String, value: Int)
fun getWishlist(): Flow<List<WishlistItem>>
suspend fun addToWishlist(key: String, isPinned: Boolean, isActive: Boolean)
fun getDisplayCase(): Flow<List<DisplayCaseEntry>>
suspend fun setDisplayCaseOrder(entries: List<DisplayCaseEntry>)
suspend fun setCover(key: String)

// 新实体
@Entity(tableName = "wishlist_items")
data class WishlistItem(@PrimaryKey val achievementKey: String, val isPinned: Boolean,
    val isActive: Boolean, val sortOrder: Int, val addedAt: Long, val note: String = "")

@Entity(tableName = "display_case_entries")
data class DisplayCaseEntry(@PrimaryKey val achievementKey: String, val sortOrder: Int,
    val isCover: Boolean, val addedAt: Long)

// Achievement 新字段
data class Achievement(..., val relatedEntryId: Long? = null, val proofType: String = "", val lastEventAt: Long? = null)

// 等级（无表，计算式）
data class LevelInfo(val level: Int, val exp: Int, val expForNext: Int, val title: String)
object AchievementLevelCalculator { fun compute(unlocked: List<Achievement>): LevelInfo }

// ViewModel 新增 StateFlow
val wishlist: StateFlow<List<WishlistItem>>
val displayCase: StateFlow<List<DisplayCaseEntry>>
val levelInfo: StateFlow<LevelInfo>
val celebration: SharedFlow<AchievementCelebration>

// CrossSystemManager 新增
private val _levelInfo = MutableStateFlow<LevelInfo?>(null)
val levelInfo: StateFlow<LevelInfo?> = _levelInfo.asStateFlow()
fun updateLevelInfo(info: LevelInfo?) { _levelInfo.value = info }
```

关系：`AchievementEngine --> AchievementEventBus`(订阅)、`--> AchievementRepository`(读写)、`--> AchievementNotificationManager`(解锁)、`--> CrossSystemManager`(联动)。`AchievementViewModel --> AchievementRepository/CrossSystemManager`(观察)。`AchievementLevelCalculator ..> Achievement`(读)。完整类图见 `docs/class-diagram.mermaid`。

---

## 4. 程序调用流程（时序图，详见 `docs/sequence-diagram.mermaid`）

**主线：保存日记 → 实时解锁 → 庆祝 → 首页/Widget 刷新**

1. `EditorViewModel` 调 `DiaryDao.insertEntry` 拿到 `entryId`，随后 `bus.publish(EntrySaved(...))`。
2. `AchievementEngine` 在 IO 串行 `collect` 到事件 → `repository.buildSnapshot()`（仅 `getAllPreviews()` 投影 + 聚合，不读正文）→ 产出 `DiaryStatsSnapshot`。
3. 仅对「受该事件影响」的成就 `evaluateKey()`：达标则 `unlockWithProof(key, entryId, "entry")`（写入 `unlockedAt + relatedEntryId + proofType + lastEventAt`）；否则 `setProgress(key, value)`。
4. 解锁时 `notifier.notifyAchievementUnlocked(achievement)`（已有安静时段/去重）→ 弹 in-app 横幅；同时 `CelebrationController.emit` 给根布局 `AchievementCelebrationOverlay` 播克制徽章动效。
5. 引擎更新 `CrossSystemManager.updateRecentAchievementUnlock(name)` 及 `updateLevelInfo(...)`。
6. `HomeScreen` 收集 `CrossSystemManager` 的 StateFlow，刷新首页/Widget「刚解锁 / 最接近」。

**副线：心愿单 / 陈列柜交互**

- 图鉴页「加入心愿单」→ `VM.addToWishlist(key)` → `repo.addToWishlist` → `AchievementCollectionDao.upsert(WishlistItem)` → Room Flow 发射 → UI 更新角标/总览。
- 陈列柜页拖拽排序/设封面 → `VM.setDisplayCaseOrder / setCover` → `upsert(display_case_entries)` → Flow 更新网格并同步总览封面。

完整时序见 `docs/sequence-diagram.mermaid`（含两段图）。

---

## 5. 任务列表（有序、含依赖、按里程碑）

> 说明：以下按 **M1→M4 四个里程碑**组织，顶层为 5 个里程碑任务（T01–T05），每个含子文件与依赖。这与 Bob 模板的「≤5 任务」上限一致，同时落实主理人要求的里程碑拆分。

### T01 — M1 地基：事件引擎 + 迁移 + 补全解锁（P0-1 / P0-2 / P0-3）
- **依赖**：无（首批）
- **优先级**：P0
- **涉及文件**：`AchievementEvent.kt`、`AchievementEventBus.kt`、`AchievementEngine.kt`、`AchievementRules.kt`、`DiaryStatsSnapshot.kt`、`Achievement.kt`(加字段)、`AchievementDao.kt`(`unlockWithProof`)、`AchievementRepository.kt`(接入引擎+`relatedEntryId`+`recompute`兜底)、`DiaryDatabase.kt`(v38→39 + `MIGRATION_38_39` + 注册实体)、`AppContainer.kt`(持有并 `start()`)、`EditorViewModel.kt`(publish `EntrySaved/EntryEdited`)、收藏/打卡/习惯/挑战写入点(publish 对应事件)、`UnifiedAchievementSeedData.kt`(12 个规则所需数据已在 `AchievementRules` 内聚)、`proguard-rules.pro`(确认 keep)
- **验收**：保存日记 ≤1s 内相关进度/解锁同步；全量重算调用下降 ≥90%；51 个中 ≥50 个有可验证路径；`relatedEntryId` 100% 可回溯。

### T02 — M1 庆祝动效 + in-app 横幅（P0-4）
- **依赖**：T01
- **优先级**：P0
- **涉及文件**：`AchievementNotificationManager.kt`(复用，去重已具备)、`AchievementCelebrationOverlay.kt`、`CelebrationController.kt`、`AchievementViewModel.kt`(新增 `celebration` SharedFlow)、根布局(如 `DiaryApp`/`HomeScreen`)挂载 Overlay
- **验收**：解锁到横幅 ≤1s；同成就不重复弹；动效 ≤1.2s 可关；安静时段不弹。

### T03 — M2 详情丰富化 + 心愿单 + 陈列柜 + 设计系统迁移（P1-1 / P1-2 / P1-3 / Q6）
- **依赖**：T01（心愿单/陈列柜数据层由 T01 的 `AchievementCollectionDao` 与实体提供；本任务聚焦 UI 与迁移）
- **优先级**：P1
- **涉及文件**：`AchievementColors.kt`(新建 `LocalAchievementColors`)、`AchievementScreen.kt`(去 `TierColors`、加入口卡/隐藏线索筛选)、`AchievementIcons.kt`(去 `CategoryColors`、`Badge` 内 `Color.Black/White`)、`AchievementDetailScreen.kt`(去 `tierColor/categoryColor`、加进度故事线+相关日记跳转+分享/设封面按钮)、`AchievementDetailSheet.kt`(去 `oldTierColor`)、`AchievementWishlistScreen.kt`、`AchievementDisplayCaseScreen.kt`、`AchievementGalleryState.kt`(隐藏线索/心愿单区块)、`AchievementHiddenClue.kt`、`AchievementViewModel.kt`(`wishlist/displayCase` StateFlow)、`DiaryNavHost.kt`(注册路由)、`AchievementRepository.kt`(心愿单/陈列柜方法)
- **验收**：详情含时间轴(开始/最近/达成)；「查看对应日记」可达；心愿单增删改 <200ms、置顶优先、持久化；陈列柜仅已解锁、设封面即时生效、空态引导。

### T04 — M3 等级/经验 + 本地分享 + 月度挑战 + 隐藏线索（P1-4 / P1-5 / P1-6 / P1-7 / Q1-Q5,Q7）
- **依赖**：T01、T03
- **优先级**：P1
- **涉及文件**：`AchievementLevelCalculator.kt`、`AchievementLevelBadge.kt`、`AchievementShareCard.kt`、`AchievementBitmapExporter.kt`、`AchievementMonthlyChallengeScreen.kt`、`UnifiedAchievementSeedData.kt`(隐藏 `clue` + `monthly_champion`)、`CrossSystemManager.kt`(`levelInfo`)、`AchievementViewModel.kt`(`levelInfo` StateFlow + `shareAchievement`)、`AchievementDetailScreen.kt`(分享/线索渲染)、`DiaryNavHost.kt`(月度挑战路由)
- **验收**：等级公式可配置可单测；本地 PNG(≥1080px) 无网络；月度挑战可进入、达成给限定成就；隐藏成就各独立线索、解锁前不剧透名称/条件。

### T05 — M4 P2 联动（P2-1 ~ P2-5）
- **依赖**：T02、T04
- **优先级**：P2
- **涉及文件**：`AchievementEngine.kt`(里程碑/连击事件发射)、`CrossSystemManager.kt`(复用 `recentAchievementUnlock`/`nextAchievementMilestone`)、首页/Widget 联动、`AchievementBitmapExporter.kt`(P2-4 图鉴长图，可并入)、震动/音效工具(P2-5，遵循勿扰)、年度报告接入(P2-2)
- **验收**：连击/里程碑专属动效与横幅；成就点亮汇入年度报告；首页/Widget「刚解锁/最接近」联动生效；图鉴本地导出；克制震动/音效可关且遵循安静时段。

---

## 6. 依赖包列表（确认）

**本模块无需新增任何第三方依赖。** 全部能力由现有栈提供：
- Jetpack Compose（`GraphicsLayer.toImageBitmap`、`animate*`、`InfiniteTransition`、`SharedFlow`）
- AndroidX（`Intent.ACTION_SEND`、`FileProvider`、`Vibrator`）
- Room / Kotlin Coroutine / StateFlow（既有）

若后续要做 P2-4「PDF 导出」，仍可用纯 Canvas 长图拼接（Bitmap 拼接），无需引入 PDF 库；如确需 PDF 再评估 `iText`/`PdfDocument`（Android 自带 `PdfDocument` 即可，仍零依赖）。

---

## 7. 共享知识（跨文件约定）

1. **命名**：事件类以 `XxxCreated/Changed` 命名；事件 `key` 字段与 `AchievementDef.key` 完全一致；UI 组件以 `Achievement` 前缀（与既有 `AchievementScreen`/`AchievementBadge` 一致）。
2. **语义色用法（Q6 铁律）**：
   - 移除所有 `Color(0xFF…)`、`Color.Black`、`Color.White` 字面量调用点。
   - 分类色 / 稀有度色统一走 `LocalAchievementColors.current.categoryColors[cat]` / `.tierColors[tier]`。
   - 中性/强调/成功态走 `MaterialTheme.colorScheme`（primary/onSurface/onSurfaceVariant/surfaceVariant）与 `LocalExtendedColors`（success/warning/info）。
   - 卡片一律复用 `GlassCard`，适配 7 套主题 × 明暗。
3. **事件派发约定**：写入在 IO 线程 `publish`；引擎 `collect` 在 IO 串行（避免并发写 DB）；事件对象不可变、轻量（仅 id/标量，不携带正文）。
4. **迁移编写约定**：
   - 新列一律 `ALTER TABLE … ADD COLUMN` 且默认值非空；用 `DiaryDatabase.addColumnIfMissing(db, …)` 幂等写法，避免中断恢复报 `duplicate column name`。
   - 升 version 必须：`DiaryDatabase` 改 `version = 39`、新增 `MIGRATION_38_39`、加入 `ALL_MIGRATIONS` 数组、实体加进 `@Database(entities=…)`。
   - **R8/ProGuard**：升 version 后必须 **clean rebuild（清 KSP 缓存）**，否则 `DiaryDatabase_Impl does not exist`。确认 `proguard-rules.pro` 含 Room 保留规则（实体/DAO/数据库实现类），新增 `WishlistItem`/`DisplayCaseEntry`/`AchievementCollectionDao` 同样被覆盖；建议显式 `-keep class com.diary.app.data.** { *; }` 与 `@Keep`/`-keep class * extends androidx.room.RoomDatabase`。
5. **relatedEntryId 回溯约定**：仅当 `proofType ∈ {"entry","favorite","time"}` 且 `relatedEntryId != null` 时，详情页才显示「查看对应日记」按钮并跳 `Screen.Detail(relatedEntryId)`；纯聚合成就（如 `legendary_all_categories`）按钮隐藏。
6. **等级约定（P1-4）**：EXP 权重 COMMON=10 / RARE=25 / EPIC=60 / LEGENDARY=150；Level 阈值表集中放 `AchievementLevelCalculator`，公式单测覆盖；不落库，随时重算。
7. **庆祝去重**：沿用 `AchievementNotificationManager` 的 `achievement_notified` SharedPreferences 去重 + 引擎解锁前 `if (unlockedAt != null) return` 双保险。

---

## 8. 待明确事项（逐条回答 PRD §5 的 7 个问题 + 额外风险）

### Q1 是否引入等级/经验（P1-4）
- **推荐**：轻量版。EXP = Σ 已解锁成就按稀有度加权（COMMON=10/RARE=25/EPIC=60/LEGENDARY=150）；Level 由累计 EXP 阈值表得出（如每级所需 EXP 递增）；给「记录者类型」称号（1 条主路线，称号由最高稀有度/分类主导）。不做复杂声望经济。
- **需用户拍板**：① 是否接受 1 条主路线（推荐）而非 2 条；② 是否展示具体 EXP 数字还是仅等级+进度条；③ Level 曲线由我给默认阈值表是否 OK。

### Q2 分享是否仅本地（P1-5）
- **推荐**：仅本地。Compose 渲染 `AchievementShareCard` → `GraphicsLayer.toImageBitmap()` → 存 `cacheDir`/经 `MediaStore` 入相册 → `FileProvider` + `ACTION_SEND` 系统分享面板。**全程不联网、不上传。**
- **需用户拍板**：① 是否允许「保存到相册」（MediaStore，仍本地）；② 单卡(推荐)还是长图图鉴(P2-4)；③ 分享卡是否含用户昵称/日期（默认含，纯本地）。

### Q3 庆祝动效强度（P0-4）
- **推荐**：克制版。徽章光晕 + 轻微缩放 + 柔光粒子，≤1.2s，可关，安静时段（`NotificationPreferencesManager.isInQuietHours`）不弹；写作页内抑制以免打断。
- **需用户拍板**：① 是否允许轻微震动（归属 P2-5，建议同期一并「可关」）；② 动效是否在「正在编辑」时完全静默（推荐静默）。

### Q4 事件驱动落点
- **推荐**：新增统一 `AchievementEventBus`（基于 `AppContainer` 手动 DI 的 `SharedFlow`）。在以下写入处 `publish`：`EditorViewModel` 保存/首编、收藏切换（`toggleFavorite` 调用点）、`QuickCheckin` 创建、`HabitRecord` 写入、`ChallengeDailyLog` 完成。引擎订阅并增量评估。
- **需用户拍板**：① 是否 hook `updateEntry`（编辑任意日记）全量，还是仅「首篇编辑」(`first_echo`)（推荐仅首编，降低噪声）；② 删除/撤回日记时是否回滚进度（**本期不做回滚**，仅正向增量，靠 `recompute()` 兜底最终一致）。

### Q5 隐藏成就线索尺度（P1-7）
- **推荐**：模糊线索，绝不泄露名称/条件。5 个隐藏成就各配独立 `clue` 文案（在 `AchievementDef.clue`，`UnifiedAchievementSeedData` 补）；「隐藏线索」筛选下可见；解锁后自动切全貌。
- **需用户拍板**：① 线索是否随「进度接近」增强提示（推荐保持统一模糊，避免变相剧透）；② 是否允许在达成条件的「前一步」显示更具体提示（推荐否）。

### Q6 设计系统迁移范围（铁律）
- **现状确认**：`LocalExtendedColors`（`ui/theme/Theme.kt`）**仅有** `success/warning/info/gradientStart/gradientEnd` 槽，**无分类/稀有度语义色槽**。调用点确实存在硬编码：`AchievementScreen.TierColors`、`AchievementIcons.CategoryColors`、详情 `tierColor()/oldTierColor()`、`AchievementBadge` 内 `Color.Black/White`。
- **推荐方案**：新增 `LocalAchievementColors`（`ui/theme/AchievementColors.kt`），持 `categoryColors: Map<AchievementCategory, Color>` 与 `tierColors: Map<AchievementTier, Color>` 两张映射，在 App 根（包在 `DiaryAppTheme` 外层或 `MainActivity` 组合）统一 `CompositionLocalProvider` 提供。分类/稀有度属「品牌身份色」，用**稳定色板**（跨明暗一致，明暗仅调透明度/亮度感知）即可，字面量集中到这一处定义，调用点全部改用它 → 满足「禁止硬编码颜色」。同时 `AchievementBadge` 的 `Color.Black/White` 改为 `MaterialTheme.colorScheme.surface`/`onSurface` 叠加。
- **需用户拍板**：采用「稳定品牌色板（推荐，工作量小）」还是「改为 7 套主题各自派生分类/稀有度色（工作量大，14 个 preset × 12 槽）」。我强烈建议前者。

### Q7 月度挑战是否本期落地（P1-6）
- **推荐**：本期补全界面复用已有 `MonthlyChallenge`/`ChallengeDailyLog`（死入口变活），限 **1 条模板**（如「本月写满 20 天」），达成给 **1 个限定成就** `monthly_champion`（EXPLORER/EPIC，经 `UnifiedAchievementSeedData` 自动播种、`AchievementRules` 评估 `completedDays >= targetDays`）。避免范围蔓延。
- **需用户拍板**：① 是否新增 `monthly_champion` 限定成就（推荐），还是仅把 `monthly_challenges.status` 标 `completed` + 庆祝（不新增成就）；② 模板是否硬编码 1 条（推荐）还是可配置多条。

### 额外识别风险

1. **迁移对旧数据影响**：仅 `ALTER` 加 3 列（`relatedEntryAt` 等默认 NULL），旧 achievement 行 `relatedEntryId=null`，不破坏既有解锁；建议迁移用 `addColumnIfMissing` 幂等。需在 `MIGRATION_38_39` 后跑一次 `repository.initialize()` 确保新种子（如 `monthly_champion`）播种。
2. **增量评估正确性边界**：纯增量难以表达「单月内心情变化次数」「连续 12 个月每月有日记」等跨期聚合。本方案采用「事件触发 + 一次轻量快照聚合」混合——受影响的聚合类成就在事件到来时基于 `DiaryStatsSnapshot`（一次 `previews` 查询）估算，仍远优于旧全量重算；`recompute()` 作为兜底保证最终一致。须单测覆盖 12 个补全规则。
3. **R8 / ProGuard 风险**：升 version 必须 clean rebuild（清 KSP 缓存），确认 proguard 保留 Room 实体/DAO/数据库实现类，否则 `DiaryDatabase_Impl does not exist`。
4. **事件风暴**：批量导入/恢复数据库会突发大量 `EntrySaved`。引擎需串行 `collect` + 可适当合并（同一 coroutine 串行本就天然节流），并让 `recompute()` 兜底覆盖，避免逐条评估放大 DB 压力。
5. **relatedEntryId 回溯限制**：仅「入口事件带 entryId」的成就能跳日记；纯聚合成就无单篇 → 按钮隐藏（已约定）。
6. **隐藏成就解锁后线索切换**：由 `isUnlocked` 驱动，解锁即显示全貌、隐藏线索，无额外状态。

---

> 以上为完整架构设计，可直接交付设计 skill 出原型；原型确认后由主理人决定是否派工程师按 T01–T05 实现。
