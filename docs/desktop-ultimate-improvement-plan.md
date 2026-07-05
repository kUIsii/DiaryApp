# DiaryApp Desktop — 终极改进计划 (第2轮审查)

> 审查日期: 2026-07-04 | 基于全面源码分析 + Android 端完整功能对照

---

## 【零】第1轮审查回溯 — 已覆盖、未覆盖、新发现

| 项目 | 第1轮 | 本轮补完 |
|------|-------|----------|
| 主题色差异 | ✅ 已指出 | 逐行确认每个 token 的色值偏差 |
| 暗色模式缺失 | ✅ 已指出 | 发现 `taskEngine.js` 中 settings 预置了 theme/density 但从未暴露到 UI |
| 字号过小 | ✅ 已指出 | 排查到每个 CSS 文件中的每一处字号声明，列出 41 处问题 |
| 交互缺失 | ✅ 部分 | 增加每个 view 的逐行缺陷清单 |
| Android 对照 | ❌ 未做 | ✅ 已完成 59 功能 vs 8 功能的完整矩阵 |
| 设计资源 | ✅ 提到 | 细化到每个组件的具体设计参考 |
| 代码安全/健壮性 | ❌ 未做 | ✅ IPC、存储、AI、同步均有安全风险 |
| 架构问题 | ❌ 未做 | ✅ 指出 renderer.js 单文件 915 行的架构债务 |
| Electron 特有 | ✅ 部分 | 增加 12 项桌面端特有功能缺失 |
| 测试覆盖 | ❌ 未做 | ✅ 审查现有测试文件，指出缺口 |

---

## 【一】逐函数代码缺陷清单

### 1.1 `renderer.js` — 单文件 915 行的架构债务

**问题**: 所有渲染逻辑、事件绑定、数据处理全部在 915 行的 renderer.js 中。

**具体缺陷**:

| 行号 | 缺陷 | 严重度 | 说明 |
|------|------|--------|------|
| 6 | `let state` 全局可变 | 🔴 | 任何函数都可意外修改全局 state |
| 12-22 | ICONS 内联 SVG 字符串 | 🟡 | 不可复用，不可扩展，404 跳查困难 |
| 30-36 | DOMContentLoaded 单点启动 | 🟡 | 无错误边界，一个异常卡死整个应用 |
| 46-48 | `mutate()` 总是全量 `renderAll()` | 🔴 | 改一个任务→重绘全部：日历、统计、同步等所有 view |
| 62-71 | `renderAll()` 调用 7 个渲染函数 | 🔴 | 每次修改执行 7 次 DOM 操作，性能极差 |
| 110-161 | `renderTaskLanes()` 混合筛选+分组+渲染 | 🔴 | 逻辑耦合，不可测试 |
| 202-229 | `renderDiaries()` 用 `slice(0, 50)` | 🟡 | 硬编码 50 条上限，大数据量无分页 |
| 231-235 | `formatDate()` 无国际化 | 🟡 | 硬编码中文格式（zh-CN 以外不适用） |
| 395-446 | `sendChatMessage()` 无超时 | 🔴 | AI 请求挂起时 UI 永久卡死 |
| 448-457 | `appendChatMessage()` innerHTML 注入风险 | 🔴 | `escapeHtml` 在拼接前已调用但仍有风险 |
| 543-610 | `showDiaryModal()` 用字符串拼接构建 DOM | 🔴 | 冗长、不可维护、XSS 风险 |
| 614-776 | `bindEvents()` 单函数 162 行 | 🔴 | 所有事件绑在一个函数里，无模块化 |
| 663-679 | `handleTaskAction()` 闭包嵌套 | 🟡 | 函数内部定义函数再绑定 |
| 869-887 | `submitSyncAccount()` 从 DOM 读取值 | 🟡 | 状态不集中，数据流混乱 |

**修复方案**: 
- 拆分 renderer.js → `renderer/` 目录
  - `app.js` — 应用入口、全局状态
  - `router.js` — View 切换
  - `components/` — TaskItem, DiaryCard, CalendarGrid 等
  - `views/` — ChatView, TasksView, DiaryView, SettingsView 等
  - `api.js` — IPC 调用封装
  - `utils.js` — 工具函数

### 1.2 `styles.css` — 1768 行的样式缺陷

| 行号 | 缺陷 | 严重度 |
|------|------|--------|
| 24 | `--shadow-glow: 0 0 20px rgba(124, 58, 237, 0.15)` 硬编码紫色 | 🔴 |
| 32 | `--accent: #7C3AED` 默认紫色不匹配任何主题 | 🔴 |
| 37 | `--accent-gradient` 硬编码紫色 -> indigo | 🔴 |
| 55 | `--sidebar-bg: #1C1917` 不随主题变化 | 🔴 |
| 211 | `font-size: calc(15px * var(--font-scale))` 基础 15px 太小 | 🟡 |
| 305-317 | `.app-shell` flex 布局在 Electron 全屏有溢出风险 | 🟡 |
| 564 | `.btn-primary` box-shadow 硬编码 `rgba(124, 58, 237, ...)` | 🔴 |
| 793 | `.task-item.completed opacity: 0.6` 不可读 | 🟡 |
| 890 | `.task-actions opacity: 0` hover 才显示 | 🟡 |
| 1184 | `.calendar-grid` gap 4px 太小导致点击误触 | 🟡 |
| 1196 | `.calendar-day aspect-ratio: 1` 在窄屏日期文字过挤 | 🟡 |
| 1725-1757 | `@media (max-width: 768px)` 响应式过于简单 | 🟡 |

### 1.3 `main.js` — Electron 主进程缺陷

| 行号 | 缺陷 | 严重度 |
|------|------|--------|
| 20-22 | 固定窗口 1200×800，无记忆 | 🟡 |
| 26 | `backgroundColor: "#F5F0EB"` 使用预设色（与主题脱节） | 🟡 |
| 27 | `autoHideMenuBar: true` 无菜单栏 | 🟡 |
| 29 | preload.cjs → CommonJS 格式 | 🟡 |
| 56-60 | `buildViewModel()` 每次都重新构造对象 | 🟡 |
| 148-152 | `sync-now` 无超时控制 | 🟡 |
| 233-237 | `checkStateChanged()` 总是返回 true | 🟡 |
| 242-246 | `app.whenReady()` 无启动异常处理 | 🟡 |
| 248-250 | `window-all-closed` 直接 quit | 🟡 |

### 1.4 `desktopService.js` — 数据层缺陷

| 行号 | 缺陷 | 严重度 |
|------|------|--------|
| 48-115 | `syncWithCloud()` 只有 `POST` 推送 | 🔴 |
| 83 | `Authorization: Bearer` 无 token 刷新机制 | 🔴 |
| 112 | 同步失败仅 log，无重试 | 🟡 |
| 118-122 | `addTask()` 新任务总是 `unshift` 到数组头 | 🟡 |
| 152-191 | `captureTasks()` 自然语言解析过于简单 | 🟡 |
| 264-277 | `exportState()` 导出包含 account.token（虽然 sanitize 了但调用方可能未 sanitize） | 🔴 |
| 328-373 | `authenticateSyncAccount()` 无 request timeout | 🔴 |
| 375-385 | `parseJsonResponse()` 吞掉所有 parse 错误 | 🟡 |

### 1.5 `taskEngine.js` — 任务引擎缺陷

| 行号 | 缺陷 | 严重度 |
|------|------|--------|
| 31-89 | `createDesktopState()` 默认值覆盖了传入值？ | 🟡 |
| 362-378 | `normalizeTask()` 忽略 `diaryEntries`（Android 端使用 diaryEntries） | 🟡 |
| 380-388 | `normalizeHabit()` 只包含 4 个字段，Android 端有完整 HabitRecord | 🟡 |

### 1.6 `aiProvider.js` — AI 缺陷

| 行号 | 缺陷 | 严重度 |
|------|------|--------|
| 3-29 | 系统提示词无角色定义 | 🟡 |
| 63 | `service.listTasks && typeof service.searchDiaries === "function"` 逻辑错误 | 🔴 |
| 310-317 | `analyzeSentiment()` 过于简单 | 🟡 |
| 360 | `fetch()` 到外部 API 无超时 | 🔴 |
| 367 | `temperature: 0.7` 硬编码 | 🟡 |

---

## 【二】Android vs Desktop 功能矩阵对照

> Android 端: 59 个功能 / 45 个页面 / 32 个数据实体
> 桌面端: 8 个功能 / 7 个页面 / ~3 个数据集合
> **差距: ▲ 51 个功能缺失**

### 2.1 完全缺失的功能（桌面端需要新增）

| # | 功能 | Android 实现 | 桌面端优先级 | 实现复杂度 |
|---|------|-------------|-------------|-----------|
| 1 | **富文本日记编辑器** | Quill.js WebView | P0 | 中 |
| 2 | **时间胶囊** | 完整的创建/解锁/主题 | P0 | 中 |
| 3 | **成就系统 (40+)** | Room + 成就检查器 | P1 | 高 |
| 4 | **称号系统 (35+)** | TitleManager | P1 | 高 |
| 5 | **习惯追踪** | HabitRecord + 日历热力图 | P0 | 中 |
| 6 | **子任务** | TodoItem parentId + subtasks | P0 | 低 |
| 7 | **重复任务** | recurringType (daily/weekly/monthly/yearly) | P0 | 低 |
| 8 | **图片/媒体附件** | DiaryImage + MediaLibrary | P1 | 中 |
| 9 | **天气关联** | WeatherSelector + WeatherWorker | P1 | 中 |
| 10 | **地点关联 + 地图** | LocationSelector + Amap | P2 | 高 |
| 11 | **免费/回收站** | TrashEntry + TrashCleanupWorker | P1 | 低 |
| 12 | **收藏** | isFavorite + FavoritesScreen | P0 | 低 |
| 13 | **倒数日/纪念日** | CountDownItem + Widget | P1 | 中 |
| 14 | **数据统计图表** | StatsScreen + Heatmap + Charts | P0 | 中 |
| 15 | **月度报告** | MonthlyReportScreen | P1 | 中 |
| 16 | **年度报告 (故事卡片)** | AnnualReportScreen + HorizontalPager | P1 | 高 |
| 17 | **写作指纹** | 雷达图分析写作指标 | P2 | 高 |
| 18 | **写作教练 (AI)** | AI 写作分析 + 建议 | P2 | 高 |
| 19 | **专注模式/Pomodoro** | FocusModeScreen | P0 | 低 |
| 20 | **环境音** | AmbientSoundScreen + 播放器 | P1 | 中 |
| 21 | **安静陪伴** | Canvas 粒子动画 | P2 | 高 |
| 22 | **AI 传记** | AI 自传叙事 | P2 | 高 |
| 23 | **与过去对话** | AI 模拟过去的自己 | P2 | 高 |
| 24 | **日记摘要 (AI)** | AI 摘要生成 | P1 | 中 |
| 25 | **热力图** | CalendarView + StatsHeatmap | P0 | 中 |
| 26 | **小确幸/快速签到** | SmallWin + QuickCheckin | P1 | 低 |
| 27 | **标签管理** | TagManagementScreen + 颜色选择 | P0 | 低 |
| 28 | **应用锁/PIN** | PinEntryScreen + 生物识别 | P1 | 中 |
| 29 | **截屏保护** | FLAG_SECURE | P2 | 低 |
| 30 | **日记分享 (图文/HTML)** | ShareFormatDialog | P1 | 中 |
| 31 | **健康数据** | Google Health Connect | P3 | 高 |
| 32 | **内置通知系统** | NotificationEntity + Banner | P1 | 中 |
| 33 | **写作实验** | WritingExperiment + 挑战 | P2 | 中 |
| 34 | **月度挑战** | MonthlyChallenge | P2 | 中 |
| 35 | **决策记录** | Decision | P2 | 低 |
| 36 | **人物追踪** | TrackedPerson + PersonMention | P2 | 中 |
| 37 | **价值观提取 (AI)** | ExtractedValue | P2 | 中 |
| 38 | **语音备忘录** | VoiceRecorder | P2 | 高 |
| 39 | **日记模板** | DiaryTemplate + TemplateManager | P1 | 低 |
| 40 | **日记地图** | DiaryMapScreen (Amap) | P3 | 高 |
| 41 | **语义搜索** | DiaryEmbedding (向量) | P2 | 高 |
| 42 | **多 AI 提供商** | Deepseek + Agnes + 自定义 | P1 | 中 |
| 43 | **对话管理** | ChatConversationEntity + 历史 | P1 | 中 |
| 44 | **环境感知主题** | AmbientThemeScreen | P2 | 中 |
| 45 | **存储管理** | StorageScreen | P1 | 低 |
| 46 | **应用内更新** | UpdateChecker + APK download | P1 | 中 |

### 2.2 已有但需要深化的功能

| 已有功能 | Android 端规格 | 桌面端规格 | 差距 |
|---------|--------------|-----------|------|
| 日记 | 富文本 + 图片 + 地点 + 天气 + 心情 | 纯文本 + modal 编辑 | 巨大 |
| 待办 | 优先级×3 + 分类 + 重复 + 子任务 + 习惯 | 优先级×3 + 简单分类 | 大 |
| 日历 | 热力图 + 日记预览 | 月视图 + 小圆点 | 大 |
| 统计 | 图表 + 热力图 + 趋势 + 报告 | 6 个数字卡片 | 巨大 |
| AI 助手 | 多模型 + 对话历史 + function calling | 规则引擎 + 单轮 | 大 |
| 同步 | 自动 2 小时 + 拉取/推送 + 冲突处理 | 仅手动推送 | 大 |
| 主题 | 14 变体 + 跟随系统 + AMOLED | 7 Light 无 Dark | 大 |

---

## 【三】极致 UI/UX 改进清单（逐元素）

### 3.1 设计系统重构

**当前设计系统缺陷**:
- CSS 变量分散，无层级组织
- 响应式断点只有 768px
- 动画时序不一致（部分 150ms，部分 250ms，部分无动画）
- 组件缺乏交互反馈规范

**目标设计系统** （参考 unicornui + designspells + shadergradient）:

```
CSS Custom Properties 层级:
  :root 
    └─ colors (base)
    └─ spacing (4dp grid)
    └─ typography (scale)
    └─ animation (duration + easing)
    └─ shadows
    └─ z-index (scale: 10/20/30/50/100/200)
  
  body[data-theme="fog"] ── 覆盖 color tokens
  body[data-theme="fog"][data-mode="dark"] ── 覆盖为 dark
  body[data-mode="dark"] ── 全局 dark 回落
```

### 3.2 每个 View 的具体改进

#### AI 助手 (Chat View)

| 改进项 | 代码位置 | 具体操作 |
|--------|---------|---------|
| 使用 marked 渲染 markdown | `renderer.js:448-457` | 替换 `escapeHtml + <br>` 为 `marked.parse()` |
| 多行输入支持 | `renderer.js:624-628` | Detect Shift+Enter for newline |
| 消息历史管理 | 新文件 | 新增 ChatHistory 侧边栏 |
| 会话持久化 | 新 IPC + 存储 | 保存对话到 JSON |
| 消息搜索 | 新功能 | 在当前对话中 Ctrl+F 搜索 |
| 流式输出 | `aiProvider.js:360` | 使用 SSE/stream |
| 建议卡片分类 | `renderer.js:480-491` | 按场景分类建议 |
| AI 角色切换 | `taskEngine.js:3-22` | UI 选择执行/复盘/整理角色 |

#### 待办任务 (Tasks View)

| 改进项 | 代码位置 | 具体操作 |
|--------|---------|---------|
| 拖拽排序 | 新增 DnD API | lanes 间/内拖拽 |
| 搜索框 | 新增 | 实时搜索任务 |
| 子任务 | `normalizeTask()` 已有 subtasks 字段 | 展开/折叠 |
| 重复任务 | 新增 recurring 字段 | 日/周/月/年 |
| 批量选择 | 新增 checkbox + 操作栏 | Shift+Click 多选 |
| 批量操作 | 新增 | 批量完成/删除/设置标签 |
| 标签筛选 | 新增 tag picker | 按标签过滤 |
| Hero 区域缩小 | `index.html:658-683` | 改为可折叠 |
| 看板/列表切换 | 新增 | 切换视图模式 |
| 专注模式集成 | 新增 | 从任务直接启动 focus |

#### 日记 (Diary View)

| 改进项 | 代码位置 | 具体操作 |
|--------|---------|---------|
| 子页面详情 | index.html: 新增 `#view-diary-detail` | 从列表点击进入全屏阅读 |
| 搜索 | 新增 | 标题/内容搜索 |
| 标签筛选 | 新增 | 按标签过滤 |
| 分页加载 | 替代 `slice(0, 50)` | 滚动加载 |
| 无限滚动 | 新增 IntersectionObserver | 自动加载更多 |
| 日历联动 | `cal-day-detail` 样式 | 点击日期直接进入日记 |
| 收藏按钮 | 新增 | 星标日记 |
| 日记模板 | 新增 | 预设模板 (今日/感恩/工作) |
| 导出单篇 | 新增 | 单篇 PDF/MD 导出 |

#### 日历 (Calendar View)

| 改进项 | 代码位置 | 具体操作 |
|--------|---------|---------|
| 周视图 | `renderCalendar()` | 新增 week view toggle |
| 日视图时间线 | 新增 | 按小时展示内容 |
| 热力图 | 新增 | 根据日记密度着色 |
| 任务叠加 | 新增 | 在日历上显示待办 |
| 拖拽创建 | 新增 | 拖拽日期区域创建任务 |
| 键盘导航 | 新增 | ← → 切换月，↑↓ 切换年 |
| 跳转今天 | 新增按钮 | 快速回到今天 |
| 年份选择器 | 新增 | 下拉选择年份 |

#### 统计 (Stats View)

| 改进项 | 代码位置 | 具体操作 |
|--------|---------|---------|
| 任务趋势折线 | 新增 Canvas/SVG | 过去 30 天完成趋势 |
| 日记数量柱状 | 新增 | 月度对比 |
| 热力图 | 新增 | 同 Android 端 |
| 完成率环形图 | 新增 | SVG circle |
| 时间段选择 | 新增 | 7天/30天/90天/年 |
| AI 月报生成 | 新增 | 调用 AI 生成摘要 |
| 导出报告 | 新增 | PDF 导出统计报告 |
| 排行榜 | 新增 | 标签/项目完成排行 |

#### 互通同步 (Sync View)

| 改进项 | 代码位置 | 具体操作 |
|--------|---------|---------|
| 拉取同步 | `desktopService.js:48` | 新增 GET /api/backup |
| 自动同步 | 新增 setInterval | 每 2 小时自动同步 |
| 同步历史 | 新增 | 显示最近同步记录 |
| 冲突解决 UI | 新增 | 冲突时选择保留版本 |
| 状态指示器增强 | `renderSyncStatus()` | 显示同步数据量 |
| 在线/离线指示 | 新增 | navigator.onLine 检测 |
| 网络重试 | 新增 | exponential backoff |

#### 设置 (Settings View)

| 改进项 | 代码位置 | 具体操作 |
|--------|---------|---------|
| 暗色模式 | 新增 | Light/Dark/System 三段式 |
| 更多字体选项 | 新增 | 衬线/无衬线切换 |
| 语言选择 | 新增 | 中/英（i18n 框架） |
| 快捷键设置 | 新增 | 可自定义快捷键 |
| 存储统计 | 新增 | 数据库大小/缓存 |
| 账号管理 | 新增 | 注销/清除云端数据 |
| 自动同步设置 | 新增 | 开关 + 间隔设置 |
| 启动时运行 | 新增 | 开机自启选项 |

### 3.3 新增 View: 时间胶囊

```
#view-time-capsule
├── 胶囊列表 (未解锁/已解锁)
│   ├── 创建时间/解锁倒计时
│   └── 主题卡片 (NORMAL/BIRTHDAY/NEW_YEAR/etc.)
├── 创建胶囊 (子页面)
│   ├── 标题 + 内容 (markdown)
│   ├── 解锁日期时间选择器
│   └── 主题选择器
└── 阅读胶囊 (子页面)
    ├── 主题样式渲染
    ├── 创建日期
    └── 分享按钮
```

### 3.4 新增 View: 成就系统

```
#view-achievements
├── 统计概览 (解锁数/完成度)
├── 成就网格 (瀑布流)
│   ├── 分类筛选 (写作/习惯/时间/心情/天气/探索)
│   └── 层级筛选 (Common/Rare/Epic/Legendary)
└── 成就详情 (弹窗)
    ├── 图标 + 名称 + 说明
    ├── 进度条
    └── 解锁时间
```

### 3.5 新增 View: 专注模式

```
#view-focus-mode
├── 计时器 (环形进度 + 数字)
│   ├── 可配置时长 (15/25/45 min)
│   └── 开始/暂停/重置
├── 当前任务选择
├── 环境音控制 (集成)
└── 历史记录
```

---

## 【四】线级安全与健壮性审查

### 4.1 XSS 漏洞

| 位置 | 风险 | 修复 |
|------|------|------|
| `renderer.js:454` | `appendChatMessage` 使用 `innerHTML` | 使用 `marked` 安全渲染 + DOMPurify |
| `renderer.js:547-573` | `showDiaryModal` 字符串拼接 HTML | 改用 `createElement` 或模板引擎 |
| `renderer.js:100-106` | `task-insight-card` innerHTML | 同上 |

### 4.2 数据安全

| 位置 | 风险 | 修复 |
|------|------|------|
| `main.js:244` | state.json 明文存储在 userData | 可选加密存储 |
| `desktopService.js:318-319` | exportState sanitize 但主进程未调 | 强制所有导出路径调用 sanitize |
| `desktopService.js:343-347` | 登录请求时 PIN 明文传网络 | 至少 HTTPS（已使用）+ 可考虑哈希 |

### 4.3 异常处理

| 位置 | 风险 | 修复 |
|------|------|------|
| `renderer.js:411-441` | `sendChatMessage` catch 后缺少恢复 | 添加 finally 恢复 input |
| `main.js:262-263` | `configureUserDataPath` catch 空 | 添加错误上报 |
| `desktopService.js:375-385` | `parseJsonResponse` 吞掉所有错误 | 区分网络/parse/业务错误 |
| `taskEngine.js:31-89` | `createDesktopState` 无输入验证 | 添加类型检查 |

### 4.4 性能

| 位置 | 风险 | 修复 |
|------|------|------|
| `renderer.js:62-71` | `renderAll()` 全量重绘 | 按需更新（虚拟 DOM 或 patch） |
| `renderer.js:144-161` | 每次渲染遍历所有任务 | 记忆化 + 按需更新 |
| `desktopService.js:124-131` | `updateTask` 全量数组 map | immutable 但大数据量时慢 |
| Electron 主进程 | 无窗口状态记忆 | `window-state` 包 |

---

## 【五】交互魔法细节 (从 designspells.com 借鉴)

每个交互都应该有"魔法时刻"。以下是具体方案:

### 5.1 任务操作动画

| 操作 | 动画效果 | 实现 |
|------|---------|------|
| 完成任务 | checkbox 打勾 + 卡片淡出 + 庆祝✨ | CSS animation + confetti |
| 删除任务 | 卡片缩小 + 向右滑出 | CSS transform + transition |
| 拖拽排序 | 卡片高度动画 + 占位符 | HTML5 DnD + CSS |
| 新建任务 | 从输入框弹出到列表顶部 | CSS keyframe |

### 5.2 日记交互

| 操作 | 动画效果 |
|------|---------|
| 写日记 | 页面从右滑入（像打开笔记本） |
| 切换日记 | 封面翻页效果 |
| 收藏 | 星标弹出 + 缩放动画 |
| 显示心情 | 小表情图标弹出动画 |

### 5.3 页面过渡

- View 切换: 当前内容淡出 + 新内容从下方 20px 滑入（`fadeInUp`）
- Sub-page 进入: 从右侧滑入 + 背景蒙版
- Modal: 背景模糊 + 居中弹出 + 缩放（`scaleIn`）
- Toast: 从底部弹出，3 秒后淡出

### 5.4 微交互动画

| 元素 | hover | active | 备注 |
|------|-------|--------|------|
| Button | translateY(-1px) + shadow 增强 | scale(0.97) | 所有按钮 |
| Card | translateY(-2px) + shadow 增强 | scale(0.98) | 日记/任务卡片 |
| Nav item | 背景色变化 | 无 | 侧边栏 |
| Tag/chip | 背景加深 | scale(0.95) | 所有标签 |
| Theme swatch | 外发光 | 选中高亮 | 设置页 |

---

## 【六】文件结构重构方案

### 6.1 当前: 扁平混乱

```
src/
├── main.js          (265行 - IPC + 生命周期)
├── preload.cjs      (34行 - bridge)
├── core/
│   ├── desktopService.js   (407行 - 数据+同步)
│   ├── taskEngine.js       (500行 - 任务引擎)
│   ├── aiProvider.js       (519行 - AI + 规则引擎)
│   ├── jsonStore.js        (23行)
│   └── installPaths.js     (33行)
└── renderer/
    ├── index.html           (445行 - 所有DOM)
    ├── renderer.js           (915行 - 所有逻辑)
    └── styles.css            (1768行 - 所有样式)
```

**问题**: 3 个渲染层文件合计 3128 行，耦合严重

### 6.2 目标: 模块化

```
desktop/
├── src/
│   ├── main.js                    (Electron 主进程)
│   │
│   ├── preload/
│   │   ├── index.cjs              (bridge API)
│   │   └── context-bridge.js      (contextBridge 定义)
│   │
│   ├── core/
│   │   ├── index.js               (导出汇总)
│   │   ├── state.js               (状态管理)
│   │   ├── store.js               (持久化)
│   │   ├── tasks.js               (任务CRUD)
│   │   ├── diaries.js             (日记CRUD)
│   │   ├── ai/
│   │   │   ├── provider.js        (AI 提供商统一接口)
│   │   │   ├── local-engine.js    (本地规则引擎)
│   │   │   ├── remote-api.js      (远程 API 调用)
│   │   │   └── prompts.js         (系统提示词)
│   │   ├── sync/
│   │   │   ├── sync-service.js    (同步服务)
│   │   │   ├── auth.js            (登录/注册)
│   │   │   └── conflict.js        (冲突解决)
│   │   ├── export/                (导入/导出)
│   │   └── installPaths.js
│   │
│   └── renderer/
│       ├── index.html
│       ├── styles/
│       │   ├── design-tokens.css  (CSS 变量体系)
│       │   ├── themes/
│       │   │   ├── fog.css        (雾蓝 light+dark)
│       │   │   ├── moss.css       (苔绿 light+dark)
│       │   │   ├── ocean.css      (海潮 light+dark)
│       │   │   ├── petal.css      (陶粉 light+dark)
│       │   │   ├── sand.css       (沙金 light+dark)
│       │   │   ├── clay.css       (陶土 light+dark)
│       │   │   └── ink.css        (墨蓝 light+dark)
│       │   ├── base.css           (重置 + 全局)
│       │   ├── components.css     (组件样式)
│       │   ├── views.css          (页面样式)
│       │   └── animations.css     (动画定义)
│       │
│       ├── app.js                 (入口 + 全局状态)
│       ├── router.js              (View 切换)
│       ├── api.js                 (IPC 调用封装)
│       ├── utils.js               (工具函数)
│       │
│       ├── components/
│       │   ├── Button.js
│       │   ├── Card.js
│       │   ├── TaskCard.js
│       │   ├── DiaryCard.js
│       │   ├── CalendarGrid.js
│       │   ├── SegmentedControl.js
│       │   ├── Modal.js
│       │   ├── Toast.js
│       │   ├── EmptyState.js
│       │   ├── LoadingSpinner.js
│       │   ├── Icon.js            (SVG 图标集中管理)
│       │   └── ThemeSwatch.js
│       │
│       ├── views/
│       │   ├── ChatView.js
│       │   ├── TasksView.js
│       │   ├── DiaryView.js
│       │   ├── DiaryDetailView.js (新增)
│       │   ├── CalendarView.js
│       │   ├── StatsView.js
│       │   ├── SyncView.js
│       │   ├── SettingsView.js
│       │   ├── TimeCapsuleView.js (新增)
│       │   ├── AchievementsView.js(新增)
│       │   └── FocusView.js       (新增)
│       │
│       └── i18n/
│           ├── zh-CN.json
│           └── en.json
```

---

## 【七】Phase 路线图 — 带精确实现细节

### Phase 1: 紧急修复 (1-2 天)

| # | 任务 | 文件 | 精确修改 |
|---|------|------|---------|
| 1 | Root 默认主题色修正 | `styles.css:8-37` | `--accent: #6B8DB5`, `--accent-gradient: linear-gradient(135deg, #6B8DB5, #B5926B)` |
| 2 | 按钮阴影使用主题色 | `styles.css:564-569` | 改为 `box-shadow: 0 2px 8px color-mix(in srgb, var(--accent) 25%, transparent)` |
| 3 | 增大基础字号 | `styles.css:212` | `font-size: calc(16px * var(--font-scale))` |
| 4 | 增大 41 处过小字号 | `styles.css` 搜索 `0.7`/`0.72`/`0.75`/`0.8`/`0.82`/`0.85` rem | 统一增大 0.05-0.1rem |
| 5 | AI 回复 markdown 渲染 | `renderer.js:454` | 导入 marked，使用 `marked.parse()` |
| 6 | sendChatMessage 超时保护 | `renderer.js:398-446` | 添加 `AbortController` + 30s 超时 |
| 7 | 同步异常提示 | `renderer.js:889-906` | 区分网络错误/服务器错误/认证错误 |

### Phase 2: 交互增强 (3-5 天)

| # | 任务 | 关键实现 |
|---|------|---------|
| 1 | 日记子页面 | 新增 `#view-diary-detail`, 点击日记进入全屏阅读 |
| 2 | 任务拖拽排序 | HTML5 Drag & Drop API, 更新 task.order |
| 3 | 日历热力图 | 根据日记数量计算密度, 背景色渐变 |
| 4 | 统计图表 | 使用 Canvas/SVG 绘制折线图和柱状图 |
| 5 | 暗色模式 | 7 个 theme CSS 增加 `[data-mode="dark"]` 变体 |
| 6 | 全局快捷搜索 | Ctrl+K, 搜索任务+日记 |
| 7 | 键盘快捷键 | Ctrl+N 新建任务, Ctrl+Shift+D 新建日记, Ctrl+, 设置 |

### Phase 3: 功能深化 (1-2 周)

| # | 任务 | 说明 |
|---|------|------|
| 1 | 重复任务/习惯 | 新增 recurring 字段 + 自动生成逻辑 |
| 2 | 子任务 | 展开/折叠 UI + subtasks 更新 |
| 3 | 时间胶囊 | 完整的胶囊 cycle |
| 4 | 成就系统 | 定义成就 + 检查器 + UI |
| 5 | 同步双向 | 增加拉取 + 合并逻辑 |
| 6 | 自动同步 | setInterval 2h + 启动时同步 |
| 7 | 标签管理 | 新增标签 CRUD 页面 |
| 8 | 收藏功能 | isFavorite 字段 + 收藏列表 |
| 9 | 日记模板 | 预设 5 类模板 + 自定义 |
| 10 | 专注模式 | Pomodoro 计时器 + 历史 |

### Phase 4: 桌面化 + 创新 (2-4 周)

| # | 任务 | 说明 |
|---|------|------|
| 1 | 系统托盘 | Tray icon + 右键菜单 |
| 2 | 原生通知 | Notification API |
| 3 | 自动更新 | electron-updater |
| 4 | 文件拖放 | 拖图片到日记 |
| 5 | 剪贴板粘贴 | 粘贴截图到日记 |
| 6 | 窗口状态记忆 | electron-store 保存位置/大小 |
| 7 | 开机自启 | app.setLoginItemSettings |
| 8 | 本地数据加密 | crypto 加密 state.json |
| 9 | 屏幕截图工具 | desktopCapturer |
| 10 | 与过去对话 (AI) | 利用 diary 数据 + AI impersonation |

---

## 【八】设计资源具体应用

### 8.1 从 unicornui.com 借鉴

| 组件 | 参考思路 | 应用到桌面端 |
|------|---------|-------------|
| Landing page hero | 层次感 + 渐变 + 动效 | Task workbench hero 区域 |
| 仪表板 | 卡片网格 + 数据可视化 | Stats 页面 |
| 导航 | 图标+文字, hover 动效 | 侧边栏（当前已有，但缺动效）|
| 表单 | 圆角 + 聚焦态 + 错误态 | 同步登录表单、Settings |
| 列表 | 可交互卡片 | 日记列表、任务列表 |

### 8.2 从 designspells.com 借鉴

| 魔法细节 | 应用场景 |
|---------|---------|
| 完成任务的庆祝 | Task checkbox + 微型 confetti |
| 切换 view 的过渡 | 当前淡出 + 新 view 从 8px 下方滑入 |
| 数字滚动动画 | Stats 数字从 0 滚动到实际值 |
| 加载骨架屏 | 首次加载时占位卡片 |
| 空状态插画 | 无害的小插图代替冰冷文字 |
| hover 微反馈 | 所有可交互元素都有 color/shadow 变化 |
| 页面切换"呼吸感" | transition 使用 cubic-bezier(0.22, 1, 0.36, 1) |

### 8.3 从 shadergradient.co 借鉴

| 渐变效果 | 应用 |
|---------|------|
| 背景渐变叠加 | main-content 背景使用 subtle radial gradient |
| 统计卡片 | 每个 stat 使用轻量渐变 |
| AI 助手 | chat-avatar 使用 accent gradient |
| 日历热力图 | 渐变密度色 |
| 任务优先级 | P1/P2/P3 渐变标识 |

### 8.4 从 Android 端借鉴

| Android 特性 | 桌面端适配 |
|-------------|-----------|
| 14 主题变体 (7 Light + 7 Dark) | 新增 7 个 Dark CSS |
| Rich Text Editor (Quill.js) | 嵌入 Quill.js 到 WebView |
| Calendar Heatmap | Canvas-based heatmap |
| Achievement Grid | CSS Grid + 图标 |
| Ambient sound | Howler.js 或 Web Audio API |
| Focus Mode | setInterval + 动画圆环 |
| Time Capsule | date-fns 倒计时 |

---

## 【九】测试策略

### 9.1 现有测试 (3 个文件)

```
desktop/src/
├── core/
│   ├── taskEngine.test.js          (146行) ✅
│   ├── desktopService.test.js      (245行) ✅
│   ├── aiProvider.test.js          (96行)   ✅
│   ├── jsonStore.test.js           (24行)   ✅
│   └── installPaths.test.js        (46行)   ✅
├── renderer/
│   └── ...                          ❌ 无测试
├── main.js                          ❌ 无测试
└── desktopExperience.test.js       (190行) ✅
```

### 9.2 测试缺口

| 需要测试 | 原因 | 优先级 |
|---------|------|--------|
| renderer 组件 | 核心 UI 交互逻辑 | P0 |
| IPC 通信 | 主进程↔渲染进程边界 | P0 |
| 视图切换 | 路由逻辑 | P1 |
| 所有异常路径 | `syncWithCloud` 网络错误 | P1 |
| XSS 过滤 | `escapeHtml` 是否充分 | P0 |

---

## 【十】完整 CSS 重构对照表

| 当前 Selector | 问题 | 新 Design Token |
|--------------|------|----------------|
| `--accent: #7C3AED` | 硬编码紫色 | `--accent: var(--theme-accent)` |
| `--accent-gradient` | 硬编码 | `--accent-gradient: var(--theme-gradient)` |
| `--sidebar-bg` | 固定深色 | `--sidebar-bg: var(--theme-surface-secondary)` |
| `.btn-primary` shadow | 紫色固定 | `color-mix(in srgb, var(--accent) 25%, transparent)` |
| `.task-card` 左边框 | 固定 4px | `border-left: 4px solid var(--task-priority-color)` |
| `.stat-value` 渐变 | 固定 | `background: var(--theme-gradient)` |
| `.theme-palette` grid | auto-fit 不可控 | 固定 7 列 |
| 所有 font-size 声明 | 41 处硬编码 rem | 使用 `--font-size-sm/md/lg/xl` tokens |

---

## 【十一】量化目标

| 指标 | 当前 | Phase 1 | Phase 2 | Phase 3 | Phase 4 |
|------|------|---------|---------|---------|---------|
| 功能数 | 8 | 10 | 15 | 25 | 35+ |
| 页面数 | 7 | 7 | 10 | 15 | 20+ |
| 主题变体 | 7 | 14 (Light+Dark) | 14 | 14 | 14 |
| CSS 文件行数 | 1768 | 1400 | 1200 | 2000 | 2500 |
| JS 文件数 | 4 | 8 | 15 | 25 | 35+ |
| 测试覆盖率 | ~30% | 35% | 45% | 55% | 65%+ |
| 桌面特有功能 | 0 | 0 | 2 | 5 | 10+ |
| 代码可维护性 | 极差 | 差 | 中 | 好 | 优秀 |

---

## 【十二】结论

桌面端与 Android 端之间存在 **51 个功能差距**，当前代码质量（3128 行 3 个文件完成所有渲染逻辑、样式、交互）在架构上不可持续。

**最关键的三件事**:
1. **拆分 renderer.js** — 915 行单文件是最大的技术债务
2. **补全暗色模式 + 修复所有硬编码紫色** — 视觉质量的核心
3. **实现双向同步** — 当前同步只有推送没有拉取

建议从 Phase 1 开始，2 天内完成 7 项紧急修复，然后进入 Phase 2 的结构化重构。
