# DiaryApp 代码布局

> 适用于 AI 快速上手项目。259 个 Kotlin 源文件，~62,000 行代码。
> 构建命令：`./gradlew assembleExperimentalRelease`

---

## 📁 顶层包 (`com/diary/app/`)

```
com/diary/app/
├── MainActivity.kt           # 主入口：setContent + 边到边 + 主题 + 导航 + 锁屏门控
├── DiaryApplication.kt       # Application 子类：初始化 DB/通知渠道/AMap/WorkManager/主题
├── ai/                       # AI 集成层（多 Provider）
├── biometric/                # 生物识别 / PIN 锁
├── data/                     # 数据层（Room DB + DAO + Repository + Manager）
├── di/                       # 手动依赖注入（AppContainer）
├── health/                   # Health Connect 集成
├── reminder/                 # 通知与提醒调度
├── ui/                       # 所有 UI 屏幕 + ViewModel + 通用组件
├── update/                   # 内建更新检查与 APK 安装
├── util/                     # 工具类
├── voice/                    # 语音录制
├── weather/                  # 天气获取与缓存
└── widget/                   # 桌面小组件（Todo + 倒数日）
```

---

## 🗄️ 数据层 (`data/`) — 55 文件

### 核心三件套

| 文件 | 说明 |
|------|------|
| `DiaryDatabase.kt` | Room @Database (v35)，34 步迁移（1161 行），暴露 DiaryDao/AchievementDao/TitleDao |
| `DiaryDao.kt` | 主 DAO（~934 行），操作 30+ 表的 CRUD |
| `AchievementDao.kt` | 成就专用 DAO |
| `TitleDao.kt` | 称号系统 DAO |

### 实体（37 个 @Entity，分散在 31 个文件）

| 文件（表名） | 说明 |
|---|---|
| `DiaryEntry.kt` (`diary_entries`) | 核心日记 — title/content/mood/weather/location/favorite/writingTime |
| `Tag.kt` (`tags`) | 标签 — name/color/isPreset |
| `DiaryTag.kt` (`diary_tags`) | 日记↔标签多对多关联 |
| `DiaryImage.kt` (`diary_images`) | 图片元数据 |
| `DiaryEmbedding.kt` (`diary_embeddings`) | 向量嵌入（语义搜索） |
| `DiarySummary.kt` (`diary_summaries`) | AI 摘要缓存 |
| `TodoItem.kt` (`todo_items`) | 待办 — title/isDone/priority/dueDate/repeatRule |
| `HabitRecord.kt` (`habit_records`) | 习惯打卡记录 |
| `TrashEntry.kt` (`trash_entries`) | 软删除日记 |
| `CountDownItem.kt` (`countdown_items`) | 倒数日 |
| `TimeCapsule.kt` (`time_capsules`) | 时间胶囊 |
| `Achievement.kt` / `AchievementModels.kt` | 成就 |
| `Goal.kt` (`goals`) | 目标 |
| `FocusSession.kt` (`focus_sessions`) | 专注会话 |
| `VoiceMemo.kt` (`voice_memos`) | 语音备忘录 |
| `EmotionRadar.kt` (`emotion_radar`) | 情绪雷达 |
| `MemoryAnchor.kt` (`memory_anchors` + `anchor_relations`) | 记忆锚点 |
| `Decision.kt` (`decisions`) | 决策 |
| `TrackedPerson.kt` (`tracked_persons` + `person_mentions`) | 人物追踪 |
| `ExtractedValue.kt` (`extracted_values`) | 价值观提取 |
| `WritingFingerprint.kt` (`writing_fingerprints`) | 写作指纹 |
| `MonthlyChallenge.kt` (`monthly_challenges` + `challenge_daily_logs`) | 月度挑战 |
| `StreakShield.kt` (`streak_shields`) | 连续天数保护 |
| `TitleModels.kt`（3 实体） | 称号系统 |
| `EasterEgg.kt` / `SmallWin.kt` / `QuickCheckin.kt` / … | 其他 |

### Repository / Manager

| 文件 | 说明 |
|---|---|
| `BackupManager.kt` | 备份恢复（1047 行），JSON 导出导入所有表 |
| `DiaryExporter.kt` / `DiaryImporter.kt` | 单篇导出/导入 |
| `DiaryMediaManager.kt` | 媒体附件管理 |
| `CrossSystemManager.kt` | 跨系统共享状态 |
| `AchievementRepository.kt` | 成就解锁逻辑 |
| `TitleManager.kt` / `TitleChecker.kt` | 称号管理器 |
| `TemplateManager.kt` | 日记模板 |
| `WritingCoach.kt` | 写作教练 |

---

## 🖥️ UI 层 (`ui/`) — 67 个子包，~120 文件

### 导航与入口

| 文件 | 说明 |
|---|---|
| `navigation/DiaryNavHost.kt` | 45+ 路由的 NavHost，5 底部 Tab |
| `navigation/MainScreenSwipeController.kt` | 主页面滑动控制 |
| `theme/Theme.kt` + `Color.kt` + `Type.kt` | Material 3 主题定义 |
| `theme/ThemeMode.kt` + `ThemePreferences.kt` | 亮/暗/系统/自动夜间模式 |

### 各功能模块（按字母序）

| 子包 | 文件 | 核心职责 |
|---|---|---|
| **achievement/** | Screen + ViewModel + Detail + Icons | 成就画廊、详情、解锁动画 |
| **adaptiveinterface/** | Screen + ViewModel | 自适应 UI 设置（字体/间距/简化模式） |
| **ambientsound/** | Screen + Player + MiniBar + Service | 环境音播放器（12 种音效） |
| **ambienttheme/** | Screen + ViewModel | 环境主题设置 |
| **annualreport/** | Screen + ViewModel | Spotify Wrapped 风格年度报告（13 张卡片） |
| **assistant/** | AiAssistantScreen | AI 多轮对话助手 |
| **backup/** | BackupScreen | 本地备份/恢复 UI |
| **biography/** | Screen + ViewModel | AI 传记/生命故事 |
| **capsule/** | Screen + ViewModel + Create + Read | 时间胶囊 |
| **components/** | GlassCard / TagChip / EmptyState / … | 通用可复用 Compose 组件 |
| **countdown/** | Screen + ViewModel + Dialog | 倒数日 |
| **covertheme/** | Screen + ViewModel | 封面主题 |
| **decisions/** | Screen + ViewModel | 决策分析与回顾 |
| **detail/** | Screen + ViewModel + JsBridge + ImageViewer | 日记详情（WebView 渲染 Quill Delta） |
| **diarysummary/** | Screen + ViewModel | AI 摘要 |
| **eastereggs/** | Screen + ViewModel | 彩蛋画廊 |
| **editor/** | Screen + ViewModel + Toolbar + AiPanel + JsBridge | 富文本编辑器（Quill.js WebView） |
| **emotionarc/** | Screen + ViewModel | 情绪变化弧线图 |
| **emotionforecast/** | Screen + ViewModel | 情绪预报 |
| **emotionradar/** | Screen + ViewModel | 情绪雷达多维可视化 |
| **entrygraph/** | Screen + ViewModel | 条目关联图谱（力导向 Canvas） |
| **experimental/** | Screen + Preferences + Logic | 实验性功能开关 |
| **favorites/** | Screen + ViewModel | 收藏列表 |
| **focus/** | Screen + ViewModel | 专注模式 / 番茄钟 |
| **gentlenotification/** | Screen + ViewModel | 温和通知设置 |
| **gesturequickaction/** | Screen + ViewModel | 手势快捷操作映射 |
| **goals/** | Screen + ViewModel | 层级目标追踪 |
| **health/** | Screen + ViewModel | 健康数据看板（步数/睡眠/心率） |
| **home/** | Screen + ViewModel + CalendarView + Shortcuts | 首页/信息流/日历/快捷入口 |
| **immersive/** | Screen + ViewModel | 沉浸阅读模式（翻页+暖光） |
| **locationmemories/** | Screen + ViewModel | 位置记忆/地理围栏回忆 |
| **lock/** | PinEntryScreen | PIN 码设置/输入 |
| **lockscreenquickwrite/** | Screen + ViewModel | 锁屏快速记录 |
| **map/** | Screen + ViewModel | 心情地图（高德地图 + 心情着色） |
| **media/** | Screen + ViewModel | 媒体库网格 |
| **memoryanchors/** | Screen + ViewModel | 记忆锚点管理 |
| **memoryart/** | Screen + ViewModel | AI 记忆艺术生成 |
| **monthlychallenge/** | Screen + ViewModel | 月度写作挑战 |
| **monthlyreport/** | Screen + ViewModel | 月度报告 |
| **notification/** | Screen + Banner + State | 通知收件箱 |
| **outline/** | Screen + ViewModel | 大纲视图 |
| **personalyearbook/** | Screen + ViewModel + PdfExporter | 个人年鉴 + PDF 导出 |
| **profile/** | Screen + TagManagement + ScreenshotProtection | 个人资料/标签管理/隐私设置 |
| **quarterlyreview/** | Screen + ViewModel | 季度回顾 |
| **quickcheckin/** | Screen + ViewModel | 快速签到（心情+活动） |
| **quietcompanion/** | QuietCompanionScreen | 安静陪伴 / 白噪音 |
| **semanticsearch/** | Screen + ViewModel | 语义搜索 + 历史持久化 |
| **settings/** | SettingsScreen | 设置主页面 |
| **smallwins/** | Screen + ViewModel | 小确幸记录 |
| **stats/** | Screen + ViewModel + WordCloudView | 数据统计 + 词云 + 热力图 |
| **storage/** | Screen + ViewModel | 存储空间管理 |
| **streakshield/** | Screen + ViewModel | 连续天数保护罩 |
| **textmicroscope/** | Screen + ViewModel | 文本显微镜分析（词频/句长等） |
| **timeline/** | Screen + ViewModel + MultiSelect | 时间线列表/搜索/筛选 |
| **todo/** | Screen + ViewModel + Dialogs | 待办 + 习惯打卡 |
| **tools/** | ToolsScreen + AiManagementScreen | 工具箱入口 / AI 设置 |
| **trash/** | Screen + ViewModel | 回收站（30 天自动清理） |
| **travellog/** | Screen + ViewModel | 旅行日志 |
| **values/** | Screen + ViewModel | 价值观提取 |
| **voicerecording/** | Screen + ViewModel | 语音录制与转写 |
| **writingcoach/** | Screen + ViewModel | 写作教练 |
| **writingfingerprint/** | Screen + ViewModel | 写作指纹分析 |
| **writinghint/** | Screen + ViewModel | 写作提示 |
| **writinglab/** | Screen + ViewModel | 写作实验室 |

UI 层根文件：
- `SearchHistoryStore.kt` — 搜索历史持久化（SharedPreferences，Gson，最多 5 条）

### UI 层关键模式

1. **每个功能 = Screen + ViewModel**（MVVM），ViewModel 继承 `AndroidViewModel`
2. **数据流**：ViewModel 通过 `StateFlow` 暴露状态，Screen 用 `collectAsStateWithLifecycle` 订阅
3. **导航**：Navigation Compose，`Screen` sealed class 定义路由，`DiaryNavHost.kt` 集中注册
4. **手动 DI**：`DiaryApplication` 持有 `AppContainer`，ViewModel 通过 `Application` 获取 `Dao`

---

## 🤖 AI 层 (`ai/`) — 14 文件

| 文件 | 说明 |
|---|---|
| `AiServiceManager.kt` | 统一 AI 服务管理器（多 Provider 路由） |
| `AiServiceProvider.kt` | Provider 接口 |
| `BaseHttpProvider.kt` / `DeepseekProvider.kt` / `ModelScopeProvider.kt` / `AgnesProvider.kt` | 3 个具体 Provider 实现 |
| `AiConfigStore.kt` | AI 配置持久化（端点/模型/密钥） |
| `AiUsageTracker.kt` | 使用量配额追踪 |
| `DiarySummarizer.kt` | AI 摘要生成 |
| `InsightGenerator.kt` | AI 洞察生成 |
| `MilestoneChecker.kt` | AI 里程碑检测 |
| `RateLimiter.kt` | API 调用频率限制 |

---

## 🔐 其他关键包

| 包 | 说明 |
|---|---|
| **biometric/** | `BiometricHelper.kt` — AndroidX Biometric 指纹/面部识别 |
| **di/** | `AppContainer.kt` — 提供 `DiaryDatabase` + 各 `Dao` |
| **health/** | `HealthDataManager.kt` + `SensorHealthManager.kt` — Health Connect |
| **reminder/** | `ReminderManager.kt` + `TodoReminderManager.kt` + `BootReceiver.kt` |
| **update/** | `UpdateChecker.kt` (GitHub Release 检查) + `ApkInstaller.kt` + `ChangelogScreen.kt` |
| **weather/** | `WeatherManager.kt` (高德天气 API) + `WeatherWorker.kt` (WorkManager 周期刷新) |
| **widget/** | `TodoWidgetProvider.kt` + `CountDownWidgetProvider.kt` — 桌面小组件 |

---

## ⚙️ 构建配置

- **构建脚本**：`build.gradle.kts` (Kotlin DSL)
- **版本号规则**：`versionCode = major * 10000 + minor * 100 + patch` → versionName="major.minor.patch-experimental"
- **flavor**：`experimental` 维度，`debug`/`release` buildType
- **minSdk**=26，**targetSdk**=34，**compileSdk**=35
- **混淆**：R8 + `proguard-rules.pro`

---

## 🧩 设计决策速查

| 问题 | 方案 |
|---|---|
| DI 方案 | 手动 DI（`AppContainer`），无 Hilt/Dagger |
| 富文本编辑 | WebView + Quill.js（编辑器 + 渲染） |
| AI Provider 切换 | 3 个 Provider 实现，`AiServiceManager` 路由 |
| 地图 | 高德地图 3D SDK（非 Google Maps） |
| 图片加载 | Coil 2.5.0 |
| 备份 | JSON 导出到 `Documents/DiaryApp/` |
| 搜索 | TF-IDF + 中文二元分词（Room 内实现） |
| 异步 | Kotlin Coroutine + StateFlow |
| 后台任务 | WorkManager（备份/回收站/天气） |
