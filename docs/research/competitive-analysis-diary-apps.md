# 日记/日志应用竞品分析与参考

> 应用重新设计灵感的综合调研。整理于 2026-06-10。

---

## 第一部分：开源日记/日志应用

### 1. StoryPad (Flutter)
- **GitHub:** https://github.com/theachoem/storypad (880 stars, GPL-3.0)
- **下载量:** 100k+
- **平台:** Android, iOS, iPad, Web

**核心功能:**
- 基于时间线的日志记录（无文件夹，条目按时间顺序排列）
- 多页条目，适合长文写作
- 富文本：粗体、列表、复选框、颜色、1300+ Google Fonts
- 照片回忆，每页支持多张图片及自定义布局
- "往昔回忆" — 同日期的历史条目（On This Day）
- 标签、星标、搜索等组织方式
- 心情/情绪追踪，45+ 种情绪，日历视图，历史视图
- 设备锁定：PIN、FaceID、指纹
- 本地优先，可选 Google Drive 同步
- 20+ 配色主题，深色/浅色模式，20+ 种语言

**Pro 功能（一次性购买）:**
- 自定义主题背景、每日写作模板、环境音效
- 经期日历、语音日记、Markdown 导出、字数统计、置顶笔记
- 自动 Google Drive 备份

**技术实现:**
- Flutter (Dart 93%), ObjectBox 存储, MVVM 架构
- 三层状态管理：Global (ProviderScope), View (ChangeNotifierProvider), Widget (Stateful)

**值得借鉴的 UI 模式:**
- 时间线优先导航（无文件夹层级）
- 往昔/On This Day 功能
- 心情追踪与日历热力图
- 多页条目支持长文写作

---

### 2. June (Jetpack Compose)
- **GitHub:** https://github.com/DenserMeerkat/June (149 stars, GPL-3.0)
- **平台:** 仅 Android
- **定位:** Pixel Journal 的开源替代品

**核心功能:**
- 多媒体条目：照片、视频、GPS 定位
- 音乐集成：粘贴 Spotify/Apple Music 链接，自动获取封面和元数据
- 富文本：粗体、斜体、下划线、高亮、标签自动补全
- 基于 Emoji 的心情追踪
- 三类标签：空间、人物、主题
- 月视图日历，显示媒体、歌曲、位置
- 日历连续打卡和写作指标
- 可配置的提醒通知
- 高级搜索：内容 + 日期 + 标签筛选（如 `@John` + `#Travel`）
- 生物识别解锁或自定义 PIN（隐私保险箱）
- 屏幕截图和最近任务菜单保护
- 离线优先，网络断开开关
- WebDAV 同步（Nextcloud, ownCloud）
- Material You 动态主题

**技术实现:**
- 100% Kotlin, MVVM + Clean Architecture
- Jetpack Compose + Material Design 3
- Room + DataStore, Koin DI, Jetpack Navigation Compose
- Coil 图片加载, Media3/ExoPlayer 视频/音频
- MapLibre 矢量地图, Retrofit + OkHttp
- JDK 17, 154 commits, 12 releases

**值得借鉴的 UI 模式:**
- 三组标签系统（空间/人物/主题）
- 音乐链接自动获取与封面展示
- 日历连续打卡可视化
- 隐私保险箱与屏幕截图保护
- 网络断开开关实现真正的离线模式

---

### 3. SwashbucklerDiary (Blazor/.NET)
- **GitHub:** https://github.com/Yu-Core/SwashbucklerDiary (1.5k stars, AGPL-3.0)
- **平台:** Windows, Android, macOS, Linux, Web

**核心功能:**
- Markdown 即时渲染，使用 Vditor 编辑器
- 数学公式、思维导图、图表、流程图、甘特图、时序图、乐谱
- 条目中支持图片、音频、视频
- 极简 UI 设计，响应式布局，深色模式
- 所有数据本地存储，无需联网
- 隐私模式保护私密条目
- 基于标签的分类
- 每条日记的天气、心情、位置元数据
- 多格式导出，文字/图片分享
- WebDAV 备份，局域网同步传输
- 支持 12 种语言

**技术实现:**
- Blazor Hybrid (MAUI) + Blazor Server + Blazor WebAssembly
- Masa Blazor 组件, SqlSugar ORM, SQLite
- Serilog 日志, EmbedIO 局域网同步
- Plugin.Maui.Biometric 生物认证
- 38 releases, 1369 commits

**值得借鉴的 UI 模式:**
- 无需云端的局域网同步（点对点）
- 隐私模式切换
- 每条日记的天气/心情/位置元数据
- 日记条目中的多格式富文本内容（图表、示意图）

---

### 4. Memex (Flutter)
- **GitHub:** https://github.com/memex-lab/memex (409 stars, GPL-3.0)
- **平台:** iOS, Android
- **定位:** AI 驱动的本地优先日志

**核心功能:**
- 多代理 AI 架构：记录整理、卡片生成、洞察分析、评论、记忆摘要、媒体分析
- 自动生成结构化卡片：任务、日常、事件、文章、联系人、测量数据、照片
- 自动标签、实体提取、交叉引用链接
- P.A.R.A. 方法论（项目、领域、资源、归档）
- 洞察卡片含图表（趋势、柱状、雷达、气泡）、地图、时间线、画廊
- AI 伙伴角色，具有持久记忆、自动评论、一对一聊天
- 兼容 SillyTavern 角色导入
- 所有记录最终存储为互相关联的 Markdown 文件
- 一键完整 Markdown 导出（零供应商锁定）
- 本地文件系统 + SQLite，无云依赖
- iCloud Drive / 设备存储 / 应用存储备份
- 生物识别认证
- 支持 12+ LLM 提供商（Gemini, OpenAI, Claude, Bedrock, Kimi, Qwen, Ollama 等）
- 自定义代理系统，支持事件驱动触发器，每个代理独立 LLM 配置

**技术实现:**
- Flutter (Dart >= 3.6), Drift (SQLite), Provider + MVVM
- `dart_agent_core` 代理框架
- Build flavors: globalDev/cnDev, global/cn

**值得借鉴的 UI 模式:**
- AI 驱动的原始记录自动整理为结构化条目
- 含图表和可视化的洞察卡片
- P.A.R.A. 知识组织方法
- Markdown 作为通用导出格式

---

### 5. Loop Habit Tracker
- **GitHub:** https://github.com/iSoron/uhabits (10k stars, GPL-3.0)
- **平台:** Android (Google Play + F-Droid)

**核心功能:**
- 习惯评分公式：每次打卡增强，漏打卡减弱（不会重置）
- 灵活的日程安排：每天、每周 3 次、隔天、自定义
- 每个习惯独立提醒，支持从通知栏打卡/关闭
- 桌面小组件
- CSV 和 SQLite 导出，Tasker 自动化
- 无限习惯数量，无内购，完全离线
- 夜间模式，极简 UI，优化速度

**技术实现:**
- Kotlin 83%, Java 16%
- 两个模块：`uhabits-android`（UI）和 `uhabits-core`（逻辑）
- 44 releases, 2642 commits

**值得借鉴的 UI 模式:**
- 习惯评分公式（基于强度，而非仅连续天数）
- 从通知栏直接打卡/关闭
- 轻量、快速、无需账号

---

### 6. Habitica (游戏化)
- **GitHub:** https://github.com/HabitRPG/habitica-android (1.8k stars, GPL-3.0)
- **平台:** Android, Wear OS

**核心功能:**
- RPG 游戏化：成功升级，失败扣血
- 赚取金币购买武器和装备
- 习惯养成 + 待办管理
- Wear OS 配套应用
- 社交功能（组队、副本）

**技术实现:**
- Kotlin 90%, Java 10%
- Coroutines, Realm 数据库
- 多模块：Habitica/, common/, shared/, wearos/, build-logic/
- Detekt 静态分析, Fastlane CI/CD
- 6333 commits, 27 releases

**值得借鉴的 UI 模式:**
- 习惯追踪的游戏化层
- Wear OS 支持快速打卡

---

## 第二部分：商业应用分析

### 7. Day One
- **官网:** https://dayoneapp.com/
- **平台:** iPhone, Android, iPad, Mac, Apple Watch, Windows, Web
- **获奖:** Apple Design Award 获奖应用

**核心功能:**
- 支持 Markdown 的富文本编辑
- 多本日记，区分生活不同方面
- 端到端加密，自动云备份
- 密码 + 生物识别安全保护
- 无限照片、视频、手写条目、语音转文字
- 智能相机：从照片中提取文字
- **On This Day** 功能重温过往日记
- 地图视图按位置浏览条目
- 实体书打印（纸质日记本导出）
- 自动捕获元数据：时间、日期、天气、步数
- 日记模板、日记连续打卡、日历视图
- IFTTT 集成（Spotify, YouTube, Strava, Fitbit, Facebook, Twitter）
- Apple Shortcuts、浏览器扩展、邮件写日记
- 跨设备无缝同步

**值得借鉴的 UI 模式:**
- On This Day / 往昔回忆
- 基于位置的地图视图浏览条目
- 自动捕获元数据（天气、步数、位置）
- 日记连续打卡与日历视图
- 多本日记区分生活领域
- 实体书导出

---

### 8. Journey
- **官网:** https://journey.cloud/
- **平台:** Android, iOS, Mac, Windows, Web, Chromebook

**核心功能:**
- 全平台云同步
- 支持媒体的富文本编辑器
- 时间线和日历视图
- 心情追踪
- 结构化条目的模板
- 密码/生物识别隐私锁
- 导出为 PDF、DOCX、JSON
- IFTTT 和 Zapier 集成
- Google Drive 同步选项

---

### 9. Diaro
- **官网:** https://diaroapp.com/
- **平台:** Android, iOS, Web

**核心功能:**
- 基于文件夹和标签的组织方式
- 多图条目，支持图片说明
- 位置和天气自动标签
- 密码保护
- Dropbox 同步
- 导出为 PDF、DOCX、TXT
- 多语言支持（45+ 种语言）
- 带筛选的搜索（日期、标签、位置、文件夹）

---

### 10. Momento
- **官网:** https://momentoapp.com/
- **平台:** 仅 iOS
- **获奖:** Apple Editors' Choice, App of the Day

**核心功能:**
- 自动社交媒体导入：Facebook, Twitter, Instagram, Swarm, Flickr, Medium, Spotify, Goodreads
- 日/月/年摘要
- 日历导航，全文搜索
- 人物、地点、标签的探索视图
- 自定义提醒，3D Touch 快捷操作
- 密码/Touch ID 锁定，iCloud 备份

**值得借鉴的 UI 模式:**
- 社交网络内容自动聚合
- 日/月/年摘要视图
- 人物/地点/标签探索视图

---

### 11. Obsidian (Markdown 知识库)
- **官网:** https://obsidian.md/
- **平台:** Windows, Mac, Linux, iOS, Android

**核心功能:**
- 移动端完整桌面体验（标签页、命令面板、插件、自定义快捷键）
- Markdown 原生编辑，实时预览
- 插件生态系统（1000+ 社区插件）
- 知识关联的图谱视图
- 本地优先，可选 Obsidian Sync
- 可自定义工具栏，触控优化手势
- 个人使用免费

**值得借鉴的 UI 模式:**
- 命令面板快速操作
- 插件架构实现可扩展性
- 条目关联的图谱视图
- Markdown 作为主要格式

---

### 12. Standard Notes (加密笔记)
- **官网:** https://standardnotes.com/
- **平台:** Windows, Mac, Linux, iOS, Android, Web

**核心功能:**
- 默认端到端加密
- 多种编辑器类型：纯文本、Markdown、富文本、代码、电子表格
- 零知识架构
- 标签和智能视图
- 2FA 支持
- 本地优先，加密同步
- 开源（服务端 + 客户端）
- 无数据挖掘，无广告

**值得借鉴的 UI 模式:**
- 编辑器类型切换（纯文本/Markdown/富文本）
- 零知识加密
- 基于标签的智能视图

---

## 第三部分：富文本编辑器库（Android 原生）

### 13. compose-rich-editor（Compose 首选方案）
- **GitHub:** https://github.com/MohamedRejeb/compose-rich-editor (1.8k stars, Apache-2.0)
- **平台:** Android, iOS, Desktop, Web (Compose Multiplatform)

**功能特性:**
- 所见即所得编辑，实时格式化
- Span 样式：粗体、斜体、下划线、删除线、颜色、字号
- 段落样式：文本对齐（左/中/右/两端）
- 链接，支持自定义 URI 处理
- 行内代码，可配置颜色
- 有序和无序列表
- 内置撤销/重做（富文本感知，覆盖 BasicTextField 默认行为）
- **提及/话题标签/斜杠命令**（实验性）：通用触发器系统、原子令牌、HTML 和 Markdown 双向转换
- HTML 导入/导出：`setHtml()`, `toHtml()`
- Markdown 导入/导出：`setMarkdown()`, `toMarkdown()`
- Coil 3 集成图片加载
- 配置项：链接颜色、代码块颜色等

**用法:**
```kotlin
val state = rememberRichTextState()
RichTextEditor(state = state)
// Export
val html = state.toHtml()
val markdown = state.toMarkdown()
```

**状态:** RC 阶段 (1.0.0-rc14), 1018 commits, 151 forks

---

### 14. Cascade Editor（块级编辑器，Notion 风格）
- **GitHub:** https://github.com/linreal/cascade-editor (128 stars, MIT)
- **平台:** Android, iOS, Desktop (Compose Multiplatform)

**功能特性:**
- 块级文档模型（Notion/Craft 风格）
- 块类型：段落、标题(H1-H6)、待办、无序列表、有序列表、引用、代码、分割线
- 块内富文本 Span：粗体、斜体、下划线、删除线、行内代码、高亮、链接
- 斜杠命令，支持模糊搜索、键盘导航、子菜单
- 通过 `SlashCommandRegistry` 自定义斜杠命令
- 拖拽块重新排序
- 缩进（0-5 级），带动画边距
- 撤销/重做，混合历史记录（紧凑条目 + 全文档检查点）
- JSON 和 HTML 序列化，带版本化 schema
- 通过 `CustomBlockType` 接口自定义块类型
- 只读模式，支持原生文本选择
- 主题：20+ 颜色槽位，排版自定义
- 每个块的崩溃隔离
- 1600+ 测试用例

**架构（6 层）:**
```
UI Layer → Text State Layer → State Layer → Action Layer → Registry Layer → Core Layer
```

**用法:**
```kotlin
val stateHolder = rememberEditorState(
    initialBlocks = listOf(
        Block.heading(level = 1, text = "Hello"),
        Block.paragraph(text = "Start editing..."),
    )
)
CascadeEditor(stateHolder = stateHolder)
```

**状态:** 活跃开发中，已发布至 Maven，Android Weekly 推荐

---

### 15. Hyphen（所见即所得 Markdown 编辑器）
- **GitHub:** https://github.com/DenserMeerkat/hyphen (54 stars)
- **平台:** Android, Desktop, Web (Compose Multiplatform)

**功能特性:**
- Markdown 为数据源（零转换损耗）
- 即时行内格式化：输入 `**text**` 实时转换
- 支持：粗体、斜体、下划线、删除线、高亮、链接、行内代码
- 标题 (H1-H6)、无序/有序列表、引用、复选框
- 智能剪贴板：复制为干净的 Markdown，可粘贴到 Slack/GitHub/Obsidian
- 触发器式自动补全（`@`, `#` 提及）
- 完整键盘快捷键（Ctrl+B/I/U, Ctrl+K 插入链接等）
- 粒度化撤销/重做，按词边界
- 两个编辑器 Composable：`HyphenBasicTextEditor`（基础）和 `HyphenTextField`（Material 3）
- `markdownFlow` 响应式观察，带防抖
- 提及点击处理、悬浮卡片、上下文菜单

**用法:**
```kotlin
val state = rememberHyphenTextState()
HyphenTextField(state = state)
val markdown = state.toMarkdown()
```

**状态:** Alpha (0.5.0-alpha06), 58 commits

---

### 16. AztecEditor-Android (WordPress)
- **GitHub:** https://github.com/wordpress-mobile/AztecEditor-Android (729 stars, MPL-2.0)
- **平台:** 仅 Android

**功能特性:**
- 基于原生 EditText 的所见即所得 HTML 编辑器
- `AztecText`（可视化）+ `SourceViewEditText`（HTML 源码）+ `AztecToolbar`
- 富文本格式化：粗体、列表（有序/无序/任务）、代码/pre 块
- 通过 `aztec.addPlugin()` 的插件系统
- Placeholder API，在 Span 上覆盖自定义视图（视频、嵌入）
- 通过可插拔 getter 加载图片（Glide/Picasso 模块）
- 视频缩略图加载
- 撤销/重做历史
- 带复选框的任务列表

**技术实现:**
- Kotlin 97%，扩展 Android EditText
- 使用 Spannable/Spanned API
- 4066 commits, 92 releases
- 灵感来自 Knife 库

---

## 第四部分：日历与热力图组件

### 17. Kalendar（Compose 日历）
- **GitHub:** https://github.com/hi-manshu/kalendar (919 stars, Apache-2.0)

**功能特性:**
- 四种日历类型：Oceanic（月视图）、Firey（周视图）、Solaris（可滑动月视图）、Aerial（可滑动周视图）
- 可自定义颜色、样式、主题
- 日历上的事件管理
- KMP 支持（Android + iOS）
- 无内置热力图功能

---

### 18. ComposeCalendar
- **GitHub:** https://github.com/boguszpawlowski/ComposeCalendar (379 stars)

**功能特性:**
- Jetpack Compose 日历组件
- 可自定义日期内容、头部、月份导航

---

### 19. Expandable-Compose-Calendar
- **GitHub:** https://github.com/mateusz800/Expandable-Compose-Calendar (116 stars)

**功能特性:**
- 可展开的日历组件
- 折叠/展开状态间的平滑动画

---

## 第五部分：加密与备份方案

### 20. Room + SQLCipher 模式
- **参考:** https://github.com/Lenz-K/android-encrypted-room-database-example (18 stars)

**模式:**
- 使用 `androidx.room` 配合 `net.zetetic:android-database-sqlcipher`
- 用加密的 SQLCipher 数据库替换默认 SQLite
- 支持基于密码的加密
- 密钥可从生物识别认证派生

**使用此模式的项目:**
- Hold: 安全加密笔记应用 (Compose + Room + SQLCipher + Hilt)
- WeightFlow: 隐私优先的体重追踪器 (Compose + Room + SQLCipher)
- vaultbell-android: 个人保险库与提醒应用 (Room + SQLCipher)

---

### 21. Jetpack 生物识别认证
- **参考:** https://github.com/BharathVishal/Biometric-Authentication-Android (66 stars)

**模式:**
- 使用 `androidx.biometric` 库
- 支持生物识别 + PIN/密码备用方案
- 与 `CryptoObject` 集成进行密钥操作
- 兼容 Kotlin + Jetpack Compose

---

### 22. WebDAV 同步
- **使用 WebDAV 的应用:** SwashbucklerDiary, June
- **库:** iCalDAV/iCalDAV (11 stars) — Kotlin CalDAV 客户端，支持离线同步
- **模式:** 使用 WebDAV 实现自托管同步（Nextcloud, ownCloud），无专有云依赖

---

## 第六部分：图片与媒体处理

### 23. Coil（图片加载）
- **GitHub:** https://github.com/coil-kt/coil (11.8k stars, Apache-2.0)
- **最新版本:** 3.4.0

**功能特性:**
- 协程优先的图片加载
- 内存 + 磁盘缓存，降采样
- Compose `AsyncImage` Composable
- 模块：coil-core, coil-compose, coil-gif, coil-svg, coil-video
- OkHttp, Ktor 2, Ktor 3 网络选项
- 轻量：仅依赖 Kotlin, Coroutines, Okio

**用法:**
```kotlin
AsyncImage(
    model = "https://example.com/image.jpg",
    contentDescription = null,
)
```

---

### 24. Media3 / ExoPlayer（视频/音频）
- June 使用其进行视频/音频播放
- Google 官方媒体播放库
- 支持本地和流媒体

---

### 25. MapLibre（地图）
- June 使用其进行矢量地图渲染
- 支持多种瓦片提供商（Carto, MapTiler, Mapbox, Stadia, OSM）
- 免费开源

---

## 第七部分：小组件实现

### 26. Jetpack Glance（Compose 小组件）
- **参考仓库:** https://github.com/ArisGuimera/AndroidWidgetsTutorial (40 stars)

**模式:**
- 使用 `GlanceAppWidget` 类配合 `GlanceTheme`
- `GlanceAppWidgetReceiver` 管理小组件生命周期
- 通过 `WorkManager` 定期更新
- 类似 Compose 的小组件 UI API

**尚未发现日记专用的 Glance 小组件** — 这是一个差异化机会。

**来自其他应用的小组件创意:**
- Day One：带条目预览的日历视图
- Loop Habit Tracker：习惯打卡网格
- StoryPad：顶部置顶笔记

---

## 第八部分：通知与提醒系统

### 27. 通知模式

**Loop Habit Tracker 模式:**
- 每个习惯在选定时间独立提醒
- 从通知栏直接打卡或关闭
- 无需打开应用

**June 模式:**
- 可配置的提醒通知
- 灵活的调度安排

**通用 Android 模式:**
- 使用 `AlarmManager` 或 `WorkManager` 进行调度
- `NotificationCompat.Builder` 构建通知
- `NotificationChannel` 用于 Android 8+ 分类
- `PendingIntent` 处理操作（从通知栏打卡/关闭）

---

## 第九部分：关键功能模式总结

### On This Day / 往昔回忆
| 应用 | 实现方式 |
|-----|---------|
| Day One | "On This Day" 功能，展示往年同日期的日记 |
| StoryPad | "往昔回忆" — 同日期的历史条目 |
| Momento | 日/月/年摘要，自动聚合 |

### 富文本编辑方案
| 方案 | 优点 | 缺点 | 示例 |
|------|------|------|------|
| 原生 Compose (compose-rich-editor) | 完全控制，无 WebView，现代 API | 仅限 Compose，仍在成熟中 | compose-rich-editor, Cascade, Hyphen |
| 原生 EditText (Aztec) | 成熟，久经考验，WordPress 支持 | 基于 View，非 Compose 原生 | AztecEditor |
| Markdown 优先 (Hyphen) | 导出干净，兼容 Obsidian | 所见即所得感较弱 | Hyphen |
| 块级编辑 (Cascade) | 类 Notion 体验，结构化 | 较新，采用率较低 | Cascade Editor |
| WebView (Vditor) | 功能完整，渲染丰富 | 性能和无障碍问题 | SwashbucklerDiary |

### 备份与同步策略
| 策略 | 应用 | 优点 | 缺点 |
|------|------|------|------|
| 纯本地 | Loop, June（默认） | 完全隐私，无需账号 | 无法跨设备 |
| 专有云 | Day One, Journey | 无缝同步 | 供应商锁定，订阅制 |
| Google Drive | StoryPad | 用户控制数据 | 需要 Google 账号 |
| WebDAV | June, SwashbucklerDiary | 自托管，开放标准 | 配置复杂 |
| iCloud | Momento, StoryPad (iOS) | 原生 Apple 集成 | 仅限 iOS |
| 局域网同步 | SwashbucklerDiary | 无需互联网 | 需在同一网络 |
| 端到端加密云 | Standard Notes, Day One | 隐私 + 同步 | 密钥管理复杂 |

### 加密方案
| 方案 | 实现方式 |
|------|---------|
| SQLCipher + Room | 加密本地数据库，基于密码 |
| Jetpack Biometric | 应用访问的生物识别认证 |
| 端到端加密 | Standard Notes：零知识架构，所有数据加密 |
| 应用级锁定 | PIN、图案、指纹、FaceID |

---

## 第十部分：技术栈推荐

### 面向 Jetpack Compose Android 日记应用

| 层级 | 推荐方案 | 备选方案 |
|------|---------|---------|
| UI | Jetpack Compose + Material 3 | — |
| 富文本 | compose-rich-editor (1.8k stars) | Cascade Editor（块级）或 Hyphen（Markdown） |
| 日历 | Kalendar (919 stars) | 自定义 Compose 日历 |
| 数据库 | Room + SQLCipher（加密） | Room（未加密） |
| DI | Koin 或 Hilt | — |
| 图片 | Coil 3 (11.8k stars) | — |
| 视频/音频 | Media3/ExoPlayer | — |
| 地图 | MapLibre | Google Maps Compose |
| 导航 | Jetpack Navigation Compose | — |
| 偏好存储 | DataStore | — |
| 备份 | WebDAV + 本地导出 | Google Drive API |
| 加密 | SQLCipher + Jetpack Biometric | — |
| 小组件 | Jetpack Glance | — |
| 通知 | WorkManager + NotificationCompat | — |
| 协程 | Kotlin Coroutines + Flow | — |

---

## 第十一部分：值得考虑的独特差异化方向

基于本次分析，以下功能可让日记应用脱颖而出：

1. **块级编辑器**（Cascade/Notion 风格）— 目前没有 Android 日记应用做好这一点
2. **带 AI 洞察的 On This Day** — 不仅展示过往日记，还能发现规律
3. **音乐集成**（类似 June）— 附加当时在听的音乐
4. **隐私保险箱**，支持屏幕截图保护
5. **网络断开开关**，实现真正的离线模式
6. **局域网同步**，无需任何云依赖
7. **日历热力图**展示写作连续打卡（目前没有好的 Compose 实现）
8. **自动捕获元数据**（天气、步数、位置），类似 Day One
9. **多本日记支持**，区分生活不同领域
10. **Markdown + 富文本双模式** — 在所见即所得和原始 Markdown 间切换
11. **标签分类**（空间/人物/主题），类似 June
12. **Wear OS 配套应用**，快速记录心情/打卡
13. **实体书导出**，类似 Day One
14. **语音日记与转录**
15. **手势导航**（滑动切换条目，捏合缩放时间线）

---

## 附录：GitHub Star 排名

| 排名 | 项目 | Stars | 类型 |
|------|------|-------|------|
| 1 | Loop Habit Tracker | 10,000 | 开源 |
| 2 | Coil | 11,800 | 库 |
| 3 | compose-rich-editor | 1,800 | 库 |
| 4 | Habitica Android | 1,800 | 开源 |
| 5 | SwashbucklerDiary | 1,500 | 开源 |
| 6 | Kalendar | 919 | 库 |
| 7 | StoryPad | 880 | 开源 |
| 8 | AztecEditor | 729 | 库 |
| 9 | Memex | 409 | 开源 |
| 10 | ComposeCalendar | 379 | 库 |
| 11 | Cascade Editor | 128 | 库 |
| 12 | June | 149 | 开源 |
| 13 | Expandable-Compose-Calendar | 116 | 库 |
| 14 | Hyphen | 54 | 库 |
