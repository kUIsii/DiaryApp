# AI 辅助 UI 设计工具调研报告

**文档版本：** v1.0
**调研日期：** 2026-06-14

---

## 目录

1. [调研背景](#一调研背景)
2. [Pencil + MCP 方案分析](#二pencil--mcp-方案分析)
3. [主流 AI 设计工具](#三主流-ai-设计工具)
4. [Jetpack Compose 专项分析](#四jetpack-compose-专项分析)
5. [推荐工作流](#五推荐工作流)
6. [总结与建议](#六总结与建议)

---

## 一、调研背景

本次调研旨在梳理当前主流 AI 辅助 UI 设计工具的能力边界，重点评估它们对 **Jetpack Compose** 开发工作流的支持程度，为日记 App 探索从设计稿到 Compose UI 的高效路径。

**调研目标：**
- Pencil + MCP 方案的可行性
- 主流 AI 设计工具的能力和局限
- Jetpack Compose 的最佳 AI 辅助方案
- 推荐的工作流程

---

## 二、Pencil + MCP 方案分析

### 2.1 Pencil 工具介绍

**什么是 Pencil：**
Pencil 是由 Evolus 公司开发的免费开源 GUI 原型设计工具，最早发布于 2008 年。基于 Electron 构建，支持 Windows、macOS 和 Linux。

**主要功能：**
- 内置形状集合（Stencils）：预装通用形状、桌面/Web UI 组件、Android/iOS GUI 组件
- 社区模板和扩展
- 图表绘制支持（连接器）
- 多种导出格式：PNG、HTML、SVG、PDF
- 剪贴画浏览器（OpenClipart.org）
- 页面间链接（交互原型）

**与其他工具对比：**

| 维度 | Pencil | Figma | Sketch |
|------|--------|-------|--------|
| **价格** | 完全免费开源 | 免费版 + 付费版 | 付费（$99/年） |
| **平台** | 全平台桌面端 | Web + 桌面端 | 仅 macOS |
| **协作** | 无实时协作 | 强大的实时协作 | 需第三方插件 |
| **组件系统** | 基础 stencil | 高级组件、变体、Auto Layout | 符号系统 |
| **交互原型** | 简单页面跳转 | 完整交互原型 | 需插件支持 |
| **AI 集成** | 无原生支持 | 原生 AI 功能 + MCP Server | 第三方插件 |
| **维护状态** | **已基本停止维护** | 持续活跃开发 | 持续活跃开发 |

**关键问题：Pencil 最后一次更新在 2023 年初，之前更有长达 3 年的更新空白期。项目已基本停止维护。**

---

### 2.2 MCP 协议介绍

**什么是 MCP：**
MCP（Model Context Protocol）是一个开源标准协议，用于将 AI 应用程序连接到外部系统。由 Anthropic 主导推出，官方比喻为"AI 的 USB-C 接口"。

**核心架构：**
- **MCP Host（宿主）：** 如 Claude Desktop、Cursor IDE、VS Code
- **MCP Client（客户端）：** 宿主内部的 MCP 连接组件
- **MCP Server（服务器）：** 暴露特定工具或数据源的服务

**支持的 AI 应用：**
- Claude（原生支持）
- ChatGPT（已支持）
- VS Code（Copilot）
- Cursor、Windsurf

**设计工具相关 MCP Server：**
- Figma（通过 Framelink MCP，15.1k Star）
- 文件系统、Git、数据库等通用 Server

---

### 2.3 Pencil + MCP 可行性分析

**结论：Pencil + MCP 的组合在当前不具备实用价值。**

**原因：**

1. **没有 Pencil MCP Server**
   - 在 MCP 官方 Registry 中搜索不到任何与 Pencil 相关的服务器实现
   - 没有社区在开发

2. **Pencil 没有原生 API**
   - 是桌面应用，不提供 REST API 或 SDK
   - 文件格式（.epz）是私有的 XML 打包格式

3. **项目已停滞**
   - 最后一次更新在 2023 年初
   - 不会有官方的 MCP 支持

4. **功能过于基础**
   - 缺少 Auto Layout、组件变体、Design Token 等现代设计系统概念

**唯一可行路径：**
导出为图片（PNG/SVG），然后借助通用的图像识别 MCP 或直接将图片传给多模态 AI 模型。但这本质上与"截图转代码"无异。

---

## 三、主流 AI 设计工具

### 3.1 v0.dev（Vercel）

**主要功能：** 通过自然语言 prompt 生成可运行的 Web 应用。支持 Design Mode 可视化编辑、一键部署到 Vercel。

**技术栈：** 严格绑定 Next.js/React + shadcn/ui + Tailwind CSS。

**定价：**
- Free：$0/月，$5 额度，每天 7 条消息
- Team：$30/用户/月
- Business：$100/用户/月

**局限性：** 只能生成 Web 代码，不支持 Flutter、SwiftUI、Jetpack Compose。

---

### 3.2 Bolt.new（StackBlitz）

**主要功能：** AI 全栈开发平台。支持导入 Figma 设计稿和 GitHub 代码库。内置多种设计系统。

**技术栈：** 以 React 生态为主。

**定价：**
- Free：$0，300K tokens/天，1M tokens/月
- Pro：$25/月，10M tokens/月起

**局限性：** 核心能力在 Web 领域，不原生支持移动端原生开发。

---

### 3.3 Lovable

**主要功能：** 对话式 AI 应用构建器，支持截图/文档输入、GitHub 同步、一键部署。

**技术栈：** 固定 React + Next.js + Supabase。

**定价：**
- Free：每天 5 额度，月上限 30 额度
- Pro：$25/月，100 额度/月

**局限性：** 技术栈完全锁定，不支持任何原生移动开发。

---

### 3.4 Screenshot to Code（开源项目）

**主要功能：** 将截图、Mockup、Figma 设计稿甚至屏幕录制转换为可运行代码。GitHub 72,900+ Star。

**支持的技术栈：**
- HTML + Tailwind CSS
- React + Tailwind
- Vue + Tailwind
- Bootstrap
- Ionic + Tailwind

**定价：** 完全开源免费（MIT 协议），需要自备 AI API Key。

**局限性：** 只支持 Web 技术栈，不支持原生移动端。

---

### 3.5 Uizard

**主要功能：** 专注 UI 原型设计的 AI 平台。
- Autodesigner 2.0：从文字 prompt 生成多屏幕可编辑原型
- Screenshot Scanner：将截图转为可编辑 Mockup
- Wireframe Scanner：将手绘线框图转为数字设计

**特点：** 输出的是设计文件，不是直接的代码。

---

### 3.6 Figma AI 功能

**主要功能：**
- AI 自动生成设计
- 自动布局建议
- 重命名图层
- 内容生成
- 设计系统建议

**代码导出：** 通过插件（Figma to Code、Locofy、Anima）可导出 React、Vue、HTML/CSS、Flutter 代码。

---

### 3.7 Cursor + Design Mode

**主要功能：**
- 视觉选择：在浏览器中点击 UI 元素
- 绘图标注：在界面上直接绘制标注指导 AI 修改
- 语音输入
- Canvas Design Mode

**特点：** 通用代码编辑器，理论上支持任何语言和框架，包括 Jetpack Compose。

---

## 四、Jetpack Compose 专项分析

### 4.1 工具 Compose 支持对比

| 工具 | Compose 支持 | 说明 |
|------|-------------|------|
| **Gemini in Android Studio** | **原生支持** | 可从图片/Wireframe 生成 Compose UI 代码 |
| **Cursor** | **间接支持** | 通用 AI 编辑器，模型理解 Compose 语法 |
| **Claude/ChatGPT** | **支持** | 可以生成 Compose 代码，需要详细 prompt |
| **v0.dev / Bolt.new / Lovable** | **不支持** | 只生成 Web 代码 |
| **Screenshot to Code** | **不支持** | 只生成 Web 代码 |
| **Uizard / Figma AI** | **不支持** | 设计工具，不生成原生代码 |

### 4.2 Gemini in Android Studio 详解

这是目前**最成熟的 Compose AI 方案**：

**功能：**
1. **图片转 Compose UI：** 附加 Wireframe 或 Mockup 图片，Gemini 直接生成 Compose 代码
2. **Compose Preview 生成：** 自动为 UI 代码生成 Preview 函数
3. **UI 转换：** 通过 AI 修改和转换现有 Compose UI 代码
4. **Agent Mode：** 处理跨文件的复杂 UI 任务
5. **代码补全：** AI 驱动的 Compose 代码智能提示
6. **Crash 分析：** 分析运行时崩溃并提供修复建议

**优势：** Google 作为 Android/Compose 的开发者，训练数据最丰富，对 Compose 的理解最深。

### 4.3 Compose AI 生成最佳实践

1. **先设计后生成：** 用 Figma/Uizard 先完成视觉设计，再用 Gemini 或 Cursor 逐步转为 Compose
2. **组件化思维：** 先让 AI 生成基础组件（Button、Card、ListItem），再组合成页面
3. **迭代式生成：** 不要一次生成整个页面，而是分区域、分组件逐步生成
4. **Prompt 工程：** 提供明确的 Material 3 规范、具体的尺寸/颜色/间距参数
5. **及时校验：** 每生成一个组件就 Preview 验证，发现问题立即修正

---

## 五、推荐工作流

### 5.1 方案一：Figma + Gemini in Android Studio（推荐）

**适用场景：** 需要精细设计控制的场景

**工作流程：**

```
阶段 1: 设计
├── 在 Figma 中完成设计
├── 利用 Figma AI 辅助布局和组件选择
└── 导出设计稿截图

阶段 2: AI 转换
├── 在 Android Studio 中用 Gemini 附加截图生成 Compose 代码
├── 用 Gemini Agent Mode 处理跨文件的 UI 组装
└── Preview 验证

阶段 3: 精调
├── Android Studio Preview 实时预览
├── Gemini Agent Mode 处理跨文件修改
└── 手动处理复杂交互逻辑

阶段 4: 集成测试
├── 真机/模拟器验证
├── 不同屏幕尺寸适配
└── 性能优化
```

**优势：**
- Figma 有免费版可用
- Gemini 对 Compose 理解最深
- 设计数据精确，不是图像识别猜测

---

### 5.2 方案二：手绘 + Cursor（轻量）

**适用场景：** 快速迭代和个人项目

**工作流程：**

```
阶段 1: 设计
├── 手绘 Wireframe 或用简单工具画线框图
└── 拍照/截图

阶段 2: AI 转换
├── 在 Cursor 中附加图片
├── 用 Agent 生成 Compose 代码
└── 用 Design Mode 可视化调整

阶段 3: 验证
└── 真机验证
```

**优势：**
- 无需学习专业设计工具
- Cursor Design Mode 支持可视化调整
- 适合快速原型

---

### 5.3 方案三：通用 LLM + Android Studio（最灵活）

**适用场景：** 对 Compose 已有一定了解的开发者

**工作流程：**

```
阶段 1: 设计
├── 用任何设计工具（甚至纸笔）确定设计方案

阶段 2: AI 辅助
├── 在 Claude/ChatGPT 中描述 UI 需求
├── 获取 Compose 代码片段
└── 在 Android Studio 中集成和调整

阶段 3: 迭代
└── 利用 Live Preview 快速迭代
```

**优势：**
- 最灵活，不受工具限制
- Claude 等模型对 Compose 有相当理解
- 适合生成特定功能的代码片段（动画、手势等）

---

## 六、总结与建议

### 6.1 核心发现

1. **Pencil + MCP 不可行：** Pencil 项目已停止维护，不存在 MCP Server，不建议投入时间。

2. **当前 AI 设计工具主流方向是 Web：** v0、Bolt、Lovable、Screenshot to Code 全部聚焦 Web 技术栈，对原生移动端几乎没有支持。

3. **Gemini in Android Studio 是 Compose 领域最佳选择：** 作为 Google 官方工具，对 Compose 理解最深，直接集成在开发环境中。

4. **通用 LLM 是重要补充：** Claude、GPT 等模型对 Compose 有相当理解，可以生成高质量代码片段。

5. **Figma 仍然是设计环节核心：** AI 工具还无法替代专业设计工具，Figma + AI 辅助是最务实的方案。

6. **完全自动化的"设计到代码"尚不成熟：** 所有工具都需要人工审查和调整，AI 更适合做"加速器"而非"替代者"。

### 6.2 对日记 App 的建议

考虑到项目是 Jetpack Compose 原生 Android 应用，建议采用以下组合：

**推荐组合：**

| 环节 | 工具 | 说明 |
|------|------|------|
| **设计** | Figma 或手绘 | 完成 UI 设计 |
| **代码生成** | Gemini in Android Studio | 图片转 Compose 功能 |
| **迭代优化** | Cursor Design Mode | 可视化调整 |
| **细节补充** | Claude 等通用 LLM | 生成动画、手势等代码片段 |

### 6.3 关于 MCP 在设计领域的展望

MCP 作为开放标准，在设计工具领域的应用才刚刚开始。目前只有 Figma 拥有成熟的 MCP Server（Framelink）。随着协议普及，预计会有更多设计工具推出 MCP 集成。

**当前最佳 MCP 方案：**
- Figma + Framelink MCP（15.1k Star）
- 结构化设计数据传递，不是图像识别猜测
- 设计师和开发者可以在同一个工具中协作

### 6.4 替代方案对比总结

| 维度 | Figma + MCP | v0.dev | Screenshot to Code | Pencil + MCP |
|------|-------------|--------|--------------------|--------------|
| **数据精度** | 结构化设计数据 | 文字描述 | 图像识别 | 不存在 |
| **输出质量** | 高 | 高 | 中 | N/A |
| **上手难度** | 中（需学 Figma） | 低 | 低 | N/A |
| **成本** | 免费版可用 | 免费+付费 | 免费开源 | N/A |
| **框架支持** | 任意 | Next.js 为主 | 多种 | N/A |
| **维护状态** | 活跃 | 活跃 | 活跃 | 停滞 |
| **MCP 集成** | 原生支持 | 无 | 无 | 无 |

---

## 附录：工具链接

| 工具 | 链接 |
|------|------|
| Pencil | https://pencil.evolus.vn |
| Figma | https://figma.com |
| Framelink MCP | https://github.com/anthropics/framelink-figma-mcp |
| v0.dev | https://v0.app |
| Bolt.new | https://bolt.new |
| Lovable | https://lovable.dev |
| Screenshot to Code | https://github.com/abi/screenshot-to-code |
| Uizard | https://uizard.io |
| Cursor | https://cursor.sh |
| Gemini in Android Studio | https://developer.android.com/studio/preview/gemini |
