# Pencil（pencil.dev）深度调研报告

**文档版本：** v1.0
**调研日期：** 2026-06-14

---

## 目录

1. [工具概述](#一工具概述)
2. [安装和配置](#二安装和配置)
3. [核心功能](#三核心功能)
4. [实际案例](#四实际案例)
5. [对 Compose 的支持](#五对-compose-的支持)
6. [应用到日记 App 的方案](#六应用到日记-app-的方案)
7. [局限性和风险](#七局限性和风险)
8. [总结与建议](#八总结与建议)

---

## 一、工具概述

### 1.1 基本信息

| 项目 | 信息 |
|------|------|
| **官网** | https://pencil.dev |
| **文档** | https://docs.pencil.dev |
| **开发公司** | High Agency, Inc. |
| **GitHub** | github.com/highagency |
| **首次发布** | 2025年12月底 |
| **当前版本** | v1.1.63（2026年6月9日） |
| **更新频率** | 每 3-7 天一个新版本 |
| **价格** | **目前完全免费** |

### 1.2 核心定位

Pencil 是一个**基于 MCP 的设计画布工具**，核心理念是 **"Design on canvas. Land in code."**（在画布上设计，落地为代码）。

**它不是传统设计工具**（如 Figma、Sketch），而是一个 **AI 驱动的设计-开发一体化工作流平台**。

### 1.3 产品形态

Pencil 提供三种产品形态：

1. **IDE 扩展（推荐）** — 集成到 VS Code、Cursor、Windsurf
2. **桌面应用** — 独立运行的原生应用
3. **CLI 工具** — 命令行界面，适合自动化

### 1.4 支持的 AI 工具

| AI 工具 | 支持状态 |
|---------|----------|
| Claude Code (CLI) | 完全支持 |
| Claude Desktop | 支持 |
| Cursor | 完全支持 |
| Windsurf (Codeium) | 支持 |
| Codex CLI (OpenAI) | 支持 |
| Gemini CLI | 可通过 MCP 集成 |

---

## 二、安装和配置

### 2.1 VS Code / Cursor 插件安装

```
1. 打开 VS Code 或 Cursor
2. 按 Ctrl+Shift+X 打开扩展面板
3. 搜索 "Pencil"（开发者：High Agency）
4. 点击 "Install" 安装
5. 安装后左侧活动栏会出现铅笔图标
6. 创建 .pen 文件验证安装成功
```

**重要提示：** 不要从官网下载独立桌面应用与 Cursor 配合使用，否则会导致 MCP 连接失败。应始终通过 IDE 扩展面板安装。

### 2.2 CLI 工具安装

```bash
npm install -g @pencil.dev/cli
```

需要 Node.js 18 或更高版本。

### 2.3 系统要求

- **操作系统**：macOS、Windows、Linux 均支持
- **IDE**：VS Code、Cursor、Windsurf 或 Antigravity IDE
- **CLI**：Node.js 18+
- **AI 功能**：Claude Code CLI 已安装并认证
- **账号**：Pencil 质号（使用时需登录）

### 2.4 Claude Code 认证

```bash
# 安装 Claude Code CLI
npm install -g @anthropic-ai/claude-code-cli

# 认证
claude
# 通过浏览器完成认证
```

---

## 三、核心功能

### 3.1 无限画布

Pencil 提供**无限画布**，操作逻辑与专业设计工具一致：

| 操作 | 快捷键 |
|------|--------|
| 平移画布 | Spacebar + 拖拽 |
| 缩放 | Cmd/Ctrl + 滚轮 |
| 缩放至 100% | 0 |
| 缩放以适应所有元素 | 1 |
| 缩放至选区 | 2 |
| 全选 | Cmd/Ctrl + A |

### 3.2 组件系统

**创建组件：**
- 选中任意元素
- 按 `Cmd/Ctrl + Option/Alt + K`
- 组件源以**洋红色边框**标识，实例以**紫色边框**标识

**Slots 系统：**
- Slots 是组件内的可放置区域
- 允许在组件中定义灵活的内容区域
- 支持配置建议组件

**设计库（Design Libraries）：**
- 可将 `.pen` 文件转换为设计库（`.lib.pen`）
- 库中的组件修改会自动同步到所有引用该库的文件

### 3.3 实时双向同步

**Design → Code：**
1. 在 Pencil 画布上完成设计
2. 将 `.pen` 文件保存在项目工作区
3. 按 `Cmd/Ctrl + K` 打开 AI 聊天
4. 用自然语言请求代码生成

**Code → Design：**
1. AI 代理可以访问项目中的源代码文件
2. 将现有组件在画布上可视化重建
3. 导入的元素包括组件结构、布局定位和样式细节

### 3.4 Figma 兼容

**导入方式：**
- **完整文件导入**：通过 File > "Import Image/SVG/Figma..."
- **单层复制粘贴**：在 Figma 中选择元素，直接粘贴到 Pencil 画布

**限制：** 不支持从 Figma 复制粘贴图片元素，需单独导入。

### 3.5 MCP 集成

**MCP 服务器特性：**
- 本地运行，无云端依赖
- 设计文件保留在本地机器上
- 使用 Pencil 时自动启动，无需额外配置

**核心 MCP 工具：**

| 工具名称 | 功能 |
|----------|------|
| `batch_design` | 创建、修改、移动、删除设计元素，生成图片 |
| `batch_get` | 读取组件、按模式搜索、检查层级结构 |
| `get_screenshot` | 渲染预览、对比前后效果 |
| `snapshot_layout` | 分析布局、检测定位问题 |
| `get_variables` / `set_variables` | 读写设计令牌，与 CSS 同步 |

### 3.6 版本控制

`.pen` 文件完全兼容 Git：

- 基于 JSON 的结构化数据格式
- 可读性强，支持 Git diff 查看
- 像提交代码一样提交 `.pen` 文件
- 支持分支和合并设计

---

## 四、实际案例

### 4.1 Web 应用案例

**案例一：QQ 音乐应用重新设计**
- 使用 Pencil 重新设计了 QQ 音乐应用
- AI 生成了多个屏幕（首页、播放器、搜索页等）
- 自动生成了一个可运行的 Next.js 项目
- 从设计到可运行代码的时间从数小时缩短到几分钟

**案例二：企业级仪表板**
- 包括数据可视化、用户管理、报表展示等功能
- 支持深色/浅色主题切换
- 组件复用性高，符合设计系统标准

### 4.2 移动端案例

**案例三：微信小程序**
- 支持微信小程序的设计与开发
- 符合微信小程序的设计规范
- 生成的代码符合微信小程序的开发规范

### 4.3 组件库案例

**案例四：工业级 UI 组件库**
- GitHub 81 星
- 包括设计令牌、组件规范、工作流程
- 组件 JSON 规范清晰，易于实现

### 4.4 效率提升

| 阶段 | 传统流程 | Pencil 流程 | 提升 |
|------|---------|------------|------|
| 设计阶段 | 数小时 | 几分钟 | 90%+ |
| 开发阶段 | 数天 | 数小时 | 80%+ |
| 沟通成本 | 高 | 低 | 70%+ |
| **整体** | 数天-数周 | 数小时-数天 | **70-80%** |

### 4.5 用户评价

**正面评价：**
- "Pencil 彻底改变了我的工作流程，从设计到代码的时间缩短了 80%"
- "作为前端开发者，我不再需要等待设计师的设计稿"
- ".pen 文件的 Git 支持太棒了，团队协作变得非常方便"

**负面评价：**
- "AI 生成的设计有时候不够精细，需要大量手动调整"
- "MCP 配置对新手来说有点复杂"
- "插件稳定性有待提高，偶尔会出现连接问题"

---

## 五、对 Compose 的支持

### 5.1 当前状态

**Pencil 不直接支持 Jetpack Compose 代码生成。**

官方文档中未提及对以下原生移动端框架的直接支持：
- Flutter
- SwiftUI
- Jetpack Compose

Pencil 主要面向 Web 开发，生成的是 Web 技术栈代码（React、Vue、HTML/CSS 等）。

### 5.2 间接支持的可能性

由于代码生成是通过 AI 代理完成的，理论上可以尝试在提示词中指定 Compose 框架：

```
"根据这个设计稿，生成 Jetpack Compose 代码，使用 Material 3 规范"
```

但官方未提供相关文档或示例，效果可能不稳定。

### 5.3 替代方案

**方案一：Pencil 设计 → AI 转换 Compose**
```
1. 在 Pencil 中完成 UI 设计
2. 导出设计截图
3. 用 Gemini in Android Studio 附加截图生成 Compose 代码
4. 或用 Claude Code 描述设计稿生成 Compose 代码
```

**方案二：Pencil 设计 → Figma → AI 转换 Compose**
```
1. 在 Pencil 中完成 UI 设计
2. 导入到 Figma（利用 Figma 兼容性）
3. 用 Framelink MCP 让 AI 读取 Figma 设计数据
4. AI 生成 Compose 代码
```

**方案三：跳过 Pencil，直接用 AI**
```
1. 用自然语言描述 UI 需求
2. 用 Gemini in Android Studio 或 Claude Code 直接生成 Compose 代码
3. 在 Android Studio Preview 中迭代优化
```

---

## 六、应用到日记 App 的方案

### 6.1 推荐方案：Pencil + AI 混合工作流

考虑到你的日记 App 是 Jetpack Compose 项目，推荐以下工作流：

```
阶段 1: UI 设计（Pencil）
├── 在 Cursor 中安装 Pencil 插件
├── 用自然语言描述 UI 需求
│   "设计一个日记卡片，包含：
│    - 日期标题（18sp，深色）
│    - 日记摘要（14sp，最多3行）
│    - 情绪标签（绿色背景）
│    - 天气图标"
├── AI 在 Pencil 画布中生成设计稿
├── 像 Figma 一样拖拽调整细节
└── 导出设计截图

阶段 2: Compose 代码生成（AI）
├── 方案 A：用 Gemini in Android Studio
│   └── 附加设计截图 → 生成 Compose 代码
├── 方案 B：用 Claude Code
│   └── 描述设计稿 → 生成 Compose 代码
└── 方案 C：用 Cursor
    └── 附加设计截图 → 生成 Compose 代码

阶段 3: 迭代优化
├── Android Studio Preview 实时预览
├── 真机测试
├── 调整细节
└── 重复阶段 1-2 直到满意
```

### 6.2 具体应用场景

**场景一：日记卡片组件**

```
Pencil 设计：
"设计一个日记卡片组件：
- 圆角 16dp
- 白色背景，柔和阴影
- 顶部：日期（18sp）+ 天气图标
- 中部：日记摘要（14sp，最多3行）
- 底部：情绪标签（绿色背景，白色文字）
- 整体风格：温暖、舒适、简洁"

AI 转换 Compose：
"根据这个设计截图，生成 Jetpack Compose 代码：
- 使用 Material 3
- 使用 Card 组件
- 使用 Row/Column 布局
- 颜色使用 MaterialTheme.colorScheme"
```

**场景二：消息中心页面**

```
Pencil 设计：
"设计一个消息中心页面：
- 顶部：标题栏 + 返回按钮
- 分类标签栏：全部/月报/年报/胶囊/里程碑/回顾
- 消息列表：图标 + 标题 + 摘要 + 时间
- 空状态：插图 + 提示文字"

AI 转换 Compose：
"根据这个设计截图，生成 Compose 代码：
- 使用 Scaffold + TopAppBar
- 使用 LazyColumn 实现列表
- 使用 TabRow 实现分类标签
- 使用 Material 3 组件"
```

**场景三：月度报告页面**

```
Pencil 设计：
"设计一个月度报告页面：
- HorizontalPager 翻页效果
- 第1页：封面（月份 + 统计摘要）
- 第2页：情绪趋势图（折线图）
- 第3页：写作习惯（柱状图）
- 第4页：精选日记
- 底部：页面指示器"

AI 转换 Compose：
"根据这个设计截图，生成 Compose 代码：
- 使用 HorizontalPager
- 使用 Canvas 绘制图表
- 使用 Material 3 卡片
- 添加页面指示器"
```

### 6.3 设计系统建立

在 Pencil 中建立日记 App 的设计系统：

**颜色 Token：**
```
Primary: #5B8C5A（温暖的绿色）
Background: #FAFAF8（米白色）
Surface: #FFFFFF（白色）
Text Primary: #080800（近黑）
Text Secondary: #707068（灰色）
```

**字体 Token：**
```
Title: 18sp, SemiBold
Body: 14sp, Regular
Caption: 12sp, Regular
```

**间距 Token：**
```
xs: 4dp
s: 8dp
m: 16dp
l: 24dp
xl: 32dp
```

**组件库：**
```
DiaryCard - 日记卡片
MessageCard - 消息卡片
Tag - 情绪标签
SearchBar - 搜索栏
NavigationBar - 导航栏
```

---

## 七、局限性和风险

### 7.1 技术局限

| 局限 | 说明 | 影响 |
|------|------|------|
| **不支持 Compose** | 主要面向 Web，不直接生成 Compose 代码 | 需要额外的 AI 转换步骤 |
| **产品年轻** | 仅半年历史，功能可能不够成熟 | 可能遇到 bug 或不稳定 |
| **无实时协作** | 不支持多人实时协作 | 需通过 Git 工作流替代 |
| **Figma 图片限制** | 不支持从 Figma 复制粘贴图片 | 需单独导入图片 |

### 7.2 学习成本

| 阶段 | 时间 | 内容 |
|------|------|------|
| 初学者 | 2-4 小时 | 基本使用 |
| 中级用户 | 1-2 天 | 熟练使用 |
| 高级用户 | 1-2 周 | 深度定制和优化 |

### 7.3 风险评估

**依赖风险：**
- 产品年轻，长期可持续性未知
- 社区规模较小，支持有限
- 免费政策可能变化

**缓解措施：**
- 设计资产（.pen 文件）基于开放格式，可导出
- 可随时切换到其他工具（Figma、手绘等）
- 保持设计系统文档化，便于迁移

---

## 八、总结与建议

### 8.1 核心发现

1. **Pencil 是一个创新的设计工具**，将设计和开发集成到同一个环境
2. **目前完全免费**，降低了使用门槛
3. **不直接支持 Compose**，但可以通过 AI 间接实现
4. **产品仍在早期**，功能和稳定性有待提升
5. **适合快速原型**，不适合高度定制化的设计项目

### 8.2 对你的建议

**短期（现在）：**
1. 在 Cursor 中安装 Pencil 插件
2. 试验设计一个简单的日记卡片
3. 评估设计质量和效率提升
4. 决定是否继续使用

**中期（如果效果好）：**
1. 建立日记 App 的设计系统（颜色、字体、间距、组件）
2. 用 Pencil 设计主要页面
3. 结合 Gemini in Android Studio 生成 Compose 代码
4. 迭代优化

**长期（持续）：**
1. 积累设计资产和组件库
2. 优化工作流程
3. 关注 Pencil 的更新和新功能

### 8.3 最终建议

**推荐尝试 Pencil**，原因：
1. **免费**：无成本风险
2. **低门槛**：不需要学 Figma
3. **高效率**：显著提升设计到代码的速度
4. **创新性**：代表了 AI 时代设计工具的发展方向

**但要有备选方案**：
- 如果 Pencil 效果不好，可以切换到 Figma + Gemini
- 或者直接用 AI 生成 Compose 代码（跳过设计工具）

---

## 附录：工具链接

| 工具 | 链接 |
|------|------|
| Pencil 官网 | https://pencil.dev |
| Pencil 文档 | https://docs.pencil.dev |
| Pencil GitHub | https://github.com/highagency |
| VS Code 扩展 | 搜索 "Pencil"（开发者：High Agency） |
| CLI 工具 | `npm install -g @pencil.dev/cli` |
| 示例脚本 | https://github.com/highagency/pencil-scripts |
| OpenPencil（开源替代） | https://github.com/OpenPencil |
