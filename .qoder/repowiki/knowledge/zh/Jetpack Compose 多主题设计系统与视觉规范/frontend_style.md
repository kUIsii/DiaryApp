## 1. 核心架构与技术栈
该应用采用 **Jetpack Compose** 作为唯一的 UI 框架，基于 **Material3** 构建了一套高度定制化的多主题设计系统。系统不依赖外部 CSS 或传统 XML 布局（除 Widget 外），而是通过 Kotlin 代码定义设计令牌（Design Tokens）、颜色方案和排版样式。

### 关键特性：
- **多主题家族支持**：内置 7 种视觉主题家族（雾蓝、苔藓绿、海潮蓝、陶粉玫瑰、沙金琥珀、陶土纸页、墨蓝黑），每种家族均包含浅色和深色两种模式，共 14 种预设主题。
- **动态字体缩放**：通过 `LocalFontScale` 和 `CompositionLocalProvider` 实现全局字体大小的动态调整，支持从“极小”到“特大”的 7 级用户偏好设置。
- **语义化颜色扩展**：除了 Material3 标准色盘，还通过 `LocalExtendedColors` 提供了成功、警告、信息及渐变起止色的语义化访问接口。

## 2. 关键文件与目录结构
前端样式逻辑主要集中在 `app/src/main/java/com/diary/app/ui/` 目录下：

- **主题核心**：
  - `ui/theme/Theme.kt`：主题入口 `DiaryAppTheme`，负责根据 `ThemeMode` 切换 `ColorScheme` 和 `Typography`，并处理状态栏适配。
  - `ui/theme/Color.kt`：定义了所有主题家族的调色板（如 `FogBlueLightBg1`, `MossGreenDarkAccent` 等）及语义色（成功、错误、心情、天气色）。
  - `ui/theme/ThemeMode.kt`：定义 `ThemeMode` 枚举及 `ThemeFamily`，管理主题的元数据和切换逻辑。
  - `ui/theme/DesignTokens.kt`：统一定义间距（4dp 网格）、圆角、字号、图标尺寸及动画时长常量。
  - `ui/theme/Type.kt`：定义基础排版样式，标题采用衬线体（Serif），正文采用默认无衬线体，营造日记的书写感。

- **通用视觉组件**：
  - `ui/components/GlassCard.kt`：核心卡片组件，支持根据当前主题自动匹配背景色与边框色，内置按压缩放动画（0.97x）和可选阴影。
  - `ui/components/GradientBackground.kt`：全局背景组件，提供垂直三色渐变背景，并叠加了性能优化的点阵纹理（`DotGridOverlay`）以模拟笔记本纸质质感。

## 3. 视觉设计规范与约定

### 3.1 色彩系统
- **主题一致性**：每个主题家族都定义了完整的背景梯度（Start/Mid/End）、表面色、文本色（Primary/Secondary/Tertiary）及强调色。
- **深色模式适配**：深色主题并非简单的反色，而是采用了低饱和度的深色调（如 `FogBlueDarkBg1: #0B0D12`），确保在暗光环境下的舒适度。
- **语义色固定**：成功（绿）、警告（橙）、错误（红）等语义色在所有主题中保持相对固定，以确保功能提示的一致性。

### 3.2 排版策略
- **字体选择**：标题（Headline/Title）统一使用 `FontFamily.Serif`（衬线体），增强文学感和正式感；标签和辅助信息使用默认无衬线体。
- **行高与字距**：正文行高设定为 `28.sp` (Large) 和 `23.sp` (Medium)，字距微调至 `0.4.sp`，提升长文阅读体验。

### 3.3 布局与间距
- **4dp 网格系统**：所有间距遵循 4dp 倍数原则（4, 8, 12, 16, 20, 24 dp）。
- **圆角规范**：
  - 小元素（标签）：8.dp
  - 中等元素（按钮、输入框）：12.dp
  - 大卡片/对话框：16.dp
  - 底部弹窗/全屏卡片：20.dp

### 3.4 交互与动效
- **微交互**：`GlassCard` 等可点击组件内置了 100ms 的按压缩放动画，提供触觉反馈之外的视觉确认。
- **动画时长**：定义了快速（150ms）、正常（250ms）、慢速（400ms）三档标准动画时长。

## 4. 开发者指南

1. **新增主题**：若需添加新主题，需在 `Color.kt` 中定义全套色值，在 `ThemeMode.kt` 中增加枚举项，并在 `Theme.kt` 的 `when` 分支中配置 `ColorScheme` 和 `ExtendedColors`。
2. **使用颜色**：优先通过 `MaterialTheme.colorScheme` 获取标准色，或通过 `LocalExtendedColors.current` 获取渐变色/语义色。避免硬编码 `Color(...)`。
3. **背景使用**：页面根节点应使用 `GradientBackground` 组件，以确保全局背景渐变和点阵纹理的统一应用。
4. **卡片封装**：内容区块应优先使用 `GlassCard`，它已处理了主题适配、边框、阴影及点击态动画。
5. **字体适配**：在自定义 Text 样式时，应考虑 `LocalFontScale` 的影响，或直接使用 `MaterialTheme.typography` 中的预设样式。