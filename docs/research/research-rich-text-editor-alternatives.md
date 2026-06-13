# Android 富文本编辑器方案调研报告

> 调研日期：2026-06-10
> 背景：当前 DiaryApp 使用 Quill.js + WebView 方案，在滚动、光标管理、键盘交互等方面存在诸多问题。

---

## 一、当前方案问题分析 (Quill.js + WebView)

从 Quill.js GitHub Issues 和项目实际代码来看，当前方案存在以下核心问题：

### 1.1 Android IME / 输入法问题（最严重）
- **中文输入法冲突**：Quill 的 composition 事件处理与 Android Gboard、搜狗等输入法存在严重冲突（Issue #3969, #2305）
- **text-change 事件延迟**：Gboard 输入时，text-change 事件要到按下空格/换行/标点才触发（Issue #3435，2021 年至今未修复）
- **Samsung 键盘兼容性**：Samsung 预测文本键盘会完全破坏 Quill 的输入处理（Issue #2028）
- **选中后输入异常**：选中文字后输入不触发 IME composition（Issue #4748）；选中单词后输入会多删字符（Issue #4747）

### 1.2 Enter 键 / 换行问题
- **键盘收起**：按 Enter 键会导致键盘收起（Issue #3153, #3154，Chrome 85.0+）
- **自动滚动**：换行时 WebView 会自动滚动到光标位置，但行为不可预测
- **格式化状态下 Enter 报错**：在格式化文本中按 Enter 会抛出错误（Issue #1622，2017 年至今未修复）

### 1.3 滚动问题
- **粘贴跳动**：粘贴内容时页面会跳动（Issue #1374，v2 已修复部分）
- **格式化需要滚动才生效**：格式化按钮有时需要滚动才能应用到全部选中文本（Issue #1102）
- **光标跳转**：删除嵌入内容后光标跳到行尾（Issue #2234）

### 1.4 根本原因
Quill.js 的架构设计假设了"浏览器会正确处理 contenteditable"，但 Android WebView 的 contenteditable 实现与桌面浏览器差异巨大：
- Android WebView 的 `contenteditable` 行为由 Chromium 引擎决定，但有大量 WebView 特有的 bug
- IME composition 事件在 Android 上的触发顺序和时机与桌面浏览器完全不同
- Android WebView 的 `scrollIntoView` 行为与桌面浏览器不一致

---

## 二、Web 端富文本编辑器方案对比

### 2.1 Quill.js（当前方案）

| 维度 | 评估 |
|------|------|
| **架构** | 基于 contenteditable + Parchment（自定义 DOM 模型） |
| **移动端键盘** | 差。IME/composition 处理有大量已知 bug，中文输入法问题严重 |
| **滚动** | 中等。v2 修复了部分问题，但 Android 上仍有跳动 |
| **光标管理** | 差。Android 上光标位置不准确，选区管理有 bug |
| **社区维护** | 中等。GitHub 76k+ stars，但 Android 相关 issue 长期不修复（#1622 从 2017 年至今） |
| **集成复杂度** | 低。API 简单，上手快 |
| **适合移动 WebView** | 不适合。已知 Android 问题太多且长期不修复 |

**结论**：Quill.js 是最不适合 Android WebView 的选择之一。其 IME 处理在 Android 上有根本性缺陷。

### 2.2 Lexical（Meta/Facebook）

| 维度 | 评估 |
|------|------|
| **架构** | 自有状态模型 + DOM reconciler（类似 React 的双缓冲机制），不直接暴露 contenteditable |
| **移动端键盘** | 中等。有 `android-bug` 标签追踪问题。已修复浏览器检测误判（PR #8267），但 bold/italic 在 Android WebView 12 上仍有 bug（Issue #7886） |
| **滚动** | 未有明确文档。状态模型驱动 DOM 更新，理论上可精确控制 |
| **光标管理** | 中等。有 Selection 对象管理，但 Android 上 backspace 曾删除两个字符（Issue #4941，已修复） |
| **社区维护** | 活跃。Meta 维护，GitHub 33k+ stars，持续更新 |
| **集成复杂度** | 中等。模块化设计，需要手动组合插件 |
| **适合移动 WebView** | 有潜力但不成熟。Android WebView 兼容性仍在改进中 |

**优势**：
- 轻量级，模块化
- Meta 内部在 Instagram/Facebook 使用
- 状态模型与 DOM 分离，理论上更容易处理 Android 特有问题
- 有 Notion 风格的 block editor 示例

**劣势**：
- 相对较新（2022 年开源），文档不如 ProseMirror 完善
- Android WebView 上仍有已知 bug（#7886 格式化问题未修复）
- 没有官方的移动端优化指南

### 2.3 ProseMirror

| 维度 | 评估 |
|------|------|
| **架构** | 文档模型 + 视图层分离，模块化插件系统 |
| **移动端键盘** | 中等偏上。故意让浏览器处理原生输入（"Even typing is usually left to the browser"），避免破坏自动纠错、大小写等功能 |
| **滚动** | 中等。DOM 更新时尽量减少不必要的重绘 |
| **光标管理** | 好。DOM selection 更新时会检查是否真的需要同步，避免干扰浏览器原生选择行为 |
| **社区维护** | 成熟稳定。由 Marijn Haverbeke（CodeMirror 作者）维护，NYT/Tiptap/Asana 等赞助 |
| **集成复杂度** | 高。底层 API，需要大量自定义代码 |
| **适合移动 WebView** | 中等偏上。对浏览器原生行为的尊重是正确的策略 |

**优势**：
- 架构最成熟，文档最完善
- "让浏览器处理原生输入"的策略对 Android WebView 最友好
- 丰富的文档 schema 系统
- 内置协作编辑支持
- 商业公司验证（NYT、Guardian、Tiptap 等）

**劣势**：
- API 偏底层，学习曲线陡峭
- 没有开箱即用的 UI 组件
- 不直接提供富文本功能，需要大量自定义

### 2.4 TipTap（基于 ProseMirror）

| 维度 | 评估 |
|------|------|
| **架构** | ProseMirror 的高级封装，headless + 扩展系统 |
| **移动端键盘** | 中等。继承 ProseMirror 的策略，但增加了一层抽象可能引入新问题 |
| **滚动** | 中等。同 ProseMirror |
| **光标管理** | 好。同 ProseMirror |
| **社区维护** | 活跃。33k+ GitHub stars，Y Combinator S23，12.8M npm 月下载 |
| **集成复杂度** | 中等。比 ProseMirror 简单很多，有丰富的扩展 |
| **适合移动 WebView** | 中等偏上。ProseMirror 底层 + 更好的开发体验 |

**优势**：
- ProseMirror 的所有优势 + 更好的 DX
- 100+ 官方扩展
- headless 设计，UI 完全自定义
- 商业支持（Notion、GitLab、LinkedIn 等客户）

**劣势**：
- 商业功能（协作、评论、导出）需要付费
- 多一层抽象，调试可能更困难
- 移动端没有专门的优化文档

### 2.5 Slate.js

| 维度 | 评估 |
|------|------|
| **架构** | React 专用，插件化，schema-less |
| **移动端键盘** | 差。已知 Android 问题：Slate + Android + 选区 = 噩梦（社区反馈） |
| **滚动** | 中等 |
| **光标管理** | 差。Android 上光标管理是 Slate 最大的痛点之一 |
| **社区维护** | 中等。仍在 beta 阶段，API 不稳定 |
| **集成复杂度** | 中等。React 生态内集成方便 |
| **适合移动 WebView** | 不适合。Android 支持是已知弱点 |

**结论**：Slate.js 是 React 生态内最灵活的方案，但 Android 支持是其最大短板。不推荐用于 Android WebView。

### 2.6 Editor.js（CodeX）

| 维度 | 评估 |
|------|------|
| **架构** | Block-based，每个 block 独立 contenteditable，输出 JSON |
| **移动端键盘** | 中等。Block 间隔离意味着每个 block 的 contenteditable 更简单 |
| **滚动** | 中等。Block 间独立，滚动冲突较少 |
| **光标管理** | 中等。Block 内部管理简单，但跨 block 移动需要自定义处理 |
| **社区维护** | 活跃。开源，Product Hunt #1 |
| **集成复杂度** | 中等。插件系统 |
| **适合移动 WebView** | 有潜力。Block 隔离架构理论上对移动更友好 |

**优势**：
- JSON 输出，内容与渲染分离
- Block 隔离减少了单个 contenteditable 的复杂度
- 可以用同一份数据渲染原生 Android UI

**劣势**：
- 不是传统 WYSIWYG，用户需要适应 block 编辑模式
- Block 间的光标移动和选择需要大量自定义
- 生态不如 ProseMirror/TipTap 丰富

### 2.7 BlockNote（基于 ProseMirror）

| 维度 | 评估 |
|------|------|
| **架构** | ProseMirror + React，block-based，类似 Notion |
| **移动端键盘** | 中等。同 ProseMirror 底层 |
| **滚动** | 中等 |
| **光标管理** | 好。同 ProseMirror |
| **社区维护** | 活跃。100k+ 周 npm 安装，法国/德国/荷兰政府使用 |
| **集成复杂度** | 低。开箱即用的 Notion 风格编辑器 |
| **适合移动 WebView** | 中等。React 依赖是限制 |

**优势**：
- 开箱即用的 Notion 风格体验
- 基于 ProseMirror 的成熟底层
- 内置 AI 集成

**劣势**：
- React 依赖，无法直接在 Android WebView 中使用（需要预编译）
- 定制性不如 TipTap/ProseMirror

---

## 三、原生 Android 富文本编辑器方案

### 3.1 Aztec Editor（WordPress）

| 维度 | 评估 |
|------|------|
| **架构** | 原生 EditText + Spannable 框架 |
| **键盘处理** | 好。原生 EditText，键盘行为完全由 Android 系统管理 |
| **滚动** | 好。原生 ScrollView/RecyclerView，无 WebView 兼容问题 |
| **光标管理** | 好。原生 EditText 光标管理，系统级支持 |
| **社区维护** | 中等。WordPress Mobile 团队维护，729 stars，92 releases |
| **集成复杂度** | 中等。Kotlin 库，需要理解 Spannable API |
| **功能完整性** | 高。支持粗体、斜体、列表、代码块、图片、视频、HTML 源码编辑 |

**优势**：
- 完全原生，无 WebView 兼容问题
- 键盘/滚动/光标由 Android 系统管理，最可靠
- HTML 输入输出，与现有数据格式兼容
- WordPress 生产环境验证
- 支持图片、视频等富媒体

**劣势**：
- 需要重写编辑器 UI（当前是 Compose + WebView）
- Spannable API 学习曲线
- 高级格式（如复杂的嵌套列表）可能需要额外开发
- 仅限 Android，无法跨平台复用

### 3.2 Jetpack Compose TextField

| 维度 | 评估 |
|------|------|
| **架构** | Compose 原生 BasicTextField + AnnotatedString |
| **键盘处理** | 好。Compose 原生支持 IME |
| **滚动** | 好。Compose 原生滚动 |
| **光标管理** | 好。系统级支持 |
| **社区维护** | 官方维护，最活跃 |
| **集成复杂度** | 低。与现有 Compose 架构完美融合 |
| **功能完整性** | 低。Compose BasicTextField 不支持富文本编辑（仅支持显示） |

**劣势**：
- Compose 目前没有成熟的富文本编辑组件
- AnnotatedString 只支持显示，不支持编辑
- 需要从零构建富文本编辑逻辑

### 3.3 自建 EditText + Spannable 方案

| 维度 | 评估 |
|------|------|
| **架构** | 自定义 EditText 子类 + Android Spannable 框架 |
| **键盘处理** | 好。原生 EditText |
| **滚动** | 好。原生 |
| **光标管理** | 好。原生 |
| **社区维护** | 无。完全自建 |
| **集成复杂度** | 高。需要处理所有富文本逻辑 |
| **功能完整性** | 取决于实现 |

**优势**：
- 完全控制，可以精确匹配 App 设计语言
- 无第三方依赖
- 原生性能

**劣势**：
- 开发成本极高
- 需要处理所有 edge case（IME、选区、格式化、撤销/重做等）
- 维护成本高

---

## 四、各 App 富文本编辑方案分析

### 4.1 Obsidian
- **技术栈**：Electron + CodeMirror（桌面），Capacitor + CodeMirror（移动）
- **移动实现**：使用 Capacitor 将 Web 技术包装为原生 App，编辑器是 CodeMirror
- **启示**：即使是"原生"App，底层也大量使用 Web 技术。CodeMirror 6 的 contenteditable 处理比 Quill 成熟得多

### 4.2 Bear
- **技术栈**：原生 iOS（Swift/UIKit），无 Android 版本
- **启示**：高质量的原生编辑体验需要大量平台特定投入

### 4.3 Day One
- **技术栈**：原生 iOS + 原生 Android
- **启示**：日记 App 领域的标杆选择原生方案

### 4.4 Notion
- **技术栈**：React + 自建编辑器（基于 ProseMirror/类似架构）
- **移动实现**：React Native + 原生桥接
- **启示**：Notion 投入了大量工程资源自建编辑器，这不适合小型团队

---

## 五、"换行自动滚动"问题深入分析

### 5.1 问题本质

在 Android WebView 中使用 contenteditable 时，按下 Enter 键会触发以下链条：

1. 浏览器原生处理 Enter → 插入新行
2. contenteditable 的 DOM 结构变化
3. Chromium 的 `scrollIntoViewIfNeeded` 逻辑触发
4. WebView 的滚动位置被浏览器强制修改
5. Android 的 `windowSoftInputMode` 可能进一步干预滚动

问题出在第 3-4 步：Chromium 的滚动计算在 WebView 环境中与桌面浏览器不同，特别是：
- WebView 的可视区域受键盘影响（`adjustResize` vs `adjustPan`）
- `scrollIntoView` 会同时作用于 body 和内部容器
- Android WebView 的 `visualViewport` 事件处理有时序问题

### 5.2 已知解决方案

#### 方案 A：JavaScript 层面拦截
```javascript
// 在 Enter 键按下时阻止默认行为，手动插入换行并控制滚动
editor.addEventListener('keydown', function(e) {
    if (e.key === 'Enter') {
        e.preventDefault();
        // 手动插入 <br> 或新段落
        document.execCommand('insertLineBreak');
        // 手动控制滚动到光标位置
        requestAnimationFrame(() => {
            const sel = window.getSelection();
            if (sel.rangeCount > 0) {
                const range = sel.getRangeAt(0);
                const rect = range.getBoundingClientRect();
                // 平滑滚动到光标位置
                window.scrollTo({
                    top: window.scrollY + rect.bottom - window.innerHeight + padding,
                    behavior: 'smooth'
                });
            }
        });
    }
});
```

**问题**：`document.execCommand` 已被标记为废弃，且在 Android WebView 上行为不一致。

#### 方案 B：CSS `overflow-anchor`
```css
/* 防止浏览器自动滚动到新内容 */
.ql-editor {
    overflow-anchor: none;
}
```

**效果**：部分情况下有效，但不能完全解决问题。

#### 方案 C：Android 端 `windowSoftInputMode` 配置
```xml
<activity android:windowSoftInputMode="adjustResize" />
```

配合 JavaScript 监听 `visualViewport` 事件：
```javascript
window.visualViewport.addEventListener('resize', () => {
    // 键盘出现/消失时，手动调整编辑区高度
    document.body.style.height = window.visualViewport.height + 'px';
});
```

**效果**：这是当前项目采用的方案（从 editor.html 的 `setEditorBottomGap` 和 `visualViewport` 监听可以看出）。能缓解但不能根治。

#### 方案 D：使用 `contenteditable=false` + 自建输入处理
完全绕过 contenteditable 的问题，用自定义方式处理输入。这是 CodeMirror 6 的策略——只在最小范围内使用 contenteditable，其余全部自定义管理。

### 5.3 根本解决方案

**没有完美的 WebView 方案**。所有 WebView 内的修复都是"打补丁"，因为：
- Android WebView 的 contenteditable 实现由 Chromium 控制，你无法修改 Chromium 的行为
- 不同 Android 版本、不同 WebView 版本的行为可能不同
- 输入法（Gboard、Samsung、搜狗等）的 composition 事件实现各不相同

真正的解决方案只有两条路：
1. **使用更底层的编辑器**（如 CodeMirror 6、ProseMirror），它们对 contenteditable 的使用更克制，更多依赖自定义逻辑
2. **放弃 WebView，使用原生方案**

---

## 六、综合推荐

### 6.1 方案评估矩阵

| 方案 | 键盘 | 滚动 | 光标 | 中文输入 | 迁移成本 | 推荐度 |
|------|------|------|------|----------|----------|--------|
| 继续 Quill.js | 差 | 差 | 差 | 差 | 无 | 不推荐 |
| 换 TipTap | 中上 | 中 | 好 | 中 | 高 | 可考虑 |
| 换 Lexical | 中 | 中 | 中 | 中 | 高 | 可考虑 |
| 换 ProseMirror | 中上 | 中 | 好 | 中 | 极高 | 不推荐（太底层） |
| 换 Editor.js | 中 | 中 | 中 | 中 | 高 | 可考虑 |
| 原生 Aztec | 好 | 好 | 好 | 好 | 极高 | 最佳但成本高 |
| 原生 Compose | 好 | 好 | 好 | 好 | 极高 | 无成熟方案 |

### 6.2 推荐路径

#### 短期（继续 WebView 路线）：迁移到 TipTap 或 Lexical

**TipTap 优先考虑**，原因：
1. 基于 ProseMirror，继承了其"尊重浏览器原生输入"的正确策略
2. 比 ProseMirror 易用得多，有丰富的扩展
3. headless 设计，可以完全控制 UI（与当前 Compose 架构兼容）
4. 商业级验证（Notion、GitLab 等都在用）
5. 可以保留 HTML/Delta 输出格式

迁移复杂度：
- 需要重写 editor.html（TipTap 替换 Quill）
- 需要调整 JS Bridge 接口（TipTap 的 API 与 Quill 不同）
- Kotlin 端基本不需要大改（WebView + JS Bridge 架构不变）

**Lexical 作为备选**，原因：
1. 更轻量
2. Meta 维护，长期有保障
3. 但 Android WebView 兼容性问题仍在修复中（#7886 未关闭）

#### 长期（如果 Web 方案始终不满意）：迁移到原生 Aztec

Aztec 是目前唯一成熟的原生 Android 富文本编辑器：
- 完全基于 EditText + Spannable，键盘/滚动/光标由系统管理
- WordPress 生产环境验证
- 支持 HTML 输入输出，与现有数据兼容
- 但迁移成本极高，需要重写整个编辑器 UI

#### 不推荐的方案

- **Slate.js**：Android 支持是最差的
- **继续 Quill.js**：已知问题太多且长期不修复
- **自建 Compose 编辑器**：开发成本不现实
- **ProseMirror 直接使用**：太底层，开发效率低

---

## 七、如果选择 TipTap 迁移的关键点

### 7.1 架构变化

```
当前：Compose UI → WebView → Quill.js → contenteditable
目标：Compose UI → WebView → TipTap (ProseMirror) → contenteditable
```

WebView + JS Bridge 架构不变，只替换编辑器引擎。

### 7.2 TipTap 在 Android WebView 上的预期改进

1. **IME 处理**：ProseMirror 的策略是"让浏览器处理原生输入"，这在 Android WebView 上更可靠
2. **Composition 事件**：ProseMirror 有专门的 composition 处理逻辑，不会像 Quill 那样频繁与输入法冲突
3. **DOM 更新**：ProseMirror 的 DOM diff 算法更精确，减少了不必要的重绘
4. **选区管理**：ProseMirror 的 Selection 系统比 Quill 更健壮

### 7.3 需要验证的风险

1. TipTap 的 bundle size 比 Quill 大（~100KB vs ~40KB gzipped）
2. 需要在真机上验证中文输入法的兼容性
3. 需要验证 Enter 键的滚动行为是否真的改善
4. 需要验证与现有 Compose UI 的交互（格式化工具栏、图片插入等）

### 7.4 建议的验证步骤

1. 在独立 HTML 页面中测试 TipTap + Android WebView 的基础输入体验
2. 重点测试：中文输入法、Enter 换行滚动、光标位置、图片插入
3. 如果基础体验OK，再进行完整迁移

---

## 八、补充：WebView 键盘/滚动最佳实践

无论选择哪个编辑器引擎，WebView 的键盘/滚动配置都应该遵循以下最佳实践：

### 8.1 AndroidManifest.xml
```xml
<activity
    android:windowSoftInputMode="adjustResize"
    android:configChanges="orientation|screenSize|keyboard|keyboardHidden" />
```

### 8.2 WebView 配置
```kotlin
webView.settings.apply {
    javaScriptEnabled = true
    domStorageEnabled = true
    // 禁用缩放，防止键盘弹出时页面缩放
    setSupportZoom(false)
    builtInZoomControls = false
    displayZoomControls = false
}
```

### 8.3 JavaScript 端
```javascript
// 使用 visualViewport API 监听键盘高度变化
window.visualViewport.addEventListener('resize', () => {
    const keyboardHeight = window.screen.height - window.visualViewport.height;
    // 动态调整编辑区底部 padding
    document.body.style.paddingBottom = keyboardHeight + 'px';
});
```

### 8.4 Kotlin 端
```kotlin
// 使用 WindowInsetsCompat 监听键盘可见性
ViewCompat.setOnApplyWindowInsetsListener(webView) { view, insets ->
    val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
    val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
    // 通知 WebView 键盘状态
    view.evaluateJavascript("setKeyboardHeight($imeHeight)", null)
    insets
}
```

---

## 九、结论

**Quill.js 在 Android WebView 上的根本问题在于其对 contenteditable 的过度依赖和不完善的 IME 处理**。这些问题中很多是 2017-2020 年报告的，至今未修复，说明 Quill 团队不打算优先解决 Android 问题。

如果要继续使用 WebView 方案，**TipTap（基于 ProseMirror）是最佳选择**，因为 ProseMirror 的"尊重浏览器原生输入"策略在 Android 上更可靠。

如果追求最佳体验，**原生 Aztec 是终极方案**，但迁移成本很高。

建议先做一个 TipTap 的 POC（Proof of Concept），在真机上验证中文输入和换行滚动的体验，再决定是否全面迁移。
