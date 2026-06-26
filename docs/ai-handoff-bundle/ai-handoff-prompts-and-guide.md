# DiaryApp 交付其他 AI 的提示词与执行指引

> 更新时间：2026-06-26
> 用途：把当前仓库、问题状态、文档入口和执行边界，清楚交给下一位 AI，避免重复误判。

---

## 1. 先给其他 AI 的一句话说明

这是一个功能很多、但存在明显“半实现状态”和“结构债务”的本地 Android 日记 App。接手时不要默认很多需求是未开始，也不要默认已有 UI 就代表链路完整，必须先按文档和当前代码核对真实完成度。

---

## 2. 接手前必须先读的文件

按这个顺序读最稳：

1. [docs/ai-handoff-bundle/code-audit-major-issues.md](/C:/Users/陈仕杰/Desktop/DiaryApp/docs/ai-handoff-bundle/code-audit-major-issues.md)
2. [docs/ai-handoff-bundle/code-audit-addendum-round2.md](/C:/Users/陈仕杰/Desktop/DiaryApp/docs/ai-handoff-bundle/code-audit-addendum-round2.md)
3. [docs/ai-handoff-bundle/feature-expansion-report.md](/C:/Users/陈仕杰/Desktop/DiaryApp/docs/ai-handoff-bundle/feature-expansion-report.md)
4. [docs/ai-handoff-bundle/feature-expansion-addendum-round2.md](/C:/Users/陈仕杰/Desktop/DiaryApp/docs/ai-handoff-bundle/feature-expansion-addendum-round2.md)
5. [docs/整体目标/FINAL-REQUIREMENTS.md](/C:/Users/陈仕杰/Desktop/DiaryApp/docs/整体目标/FINAL-REQUIREMENTS.md)
6. [docs/整体目标/FEATURE-DETAILS.md](/C:/Users/陈仕杰/Desktop/DiaryApp/docs/整体目标/FEATURE-DETAILS.md)
7. [docs/feature-inventory.md](/C:/Users/陈仕杰/Desktop/DiaryApp/docs/feature-inventory.md)
8. [docs/build-notes.md](/C:/Users/陈仕杰/Desktop/DiaryApp/docs/build-notes.md)

如果要改核心入口，再读：

1. [app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt:1)
2. [app/src/main/java/com/diary/app/DiaryApplication.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/DiaryApplication.kt:1)
3. [app/src/main/java/com/diary/app/data/DiaryDatabase.kt](/C:/Users/陈仕杰/Desktop/DiaryApp/app/src/main/java/com/diary/app/data/DiaryDatabase.kt:1)

---

## 3. 接手时最容易误判的点

### 不要误判为“完全没做”

- 周报页面已经存在，但还没进正式导航主链。
- 挑战系统已经接入成就页，不是空白。
- 地图路线、聚类和地点统计已经有基础逻辑。
- AI 自动标签、风格分析、AI 传记都有一定实现。
- 搜索历史持久化已经做了。

### 不要误判为“已经做完”

- 热力图模式现在更像 UI 状态，不是真热力图层。
- 搜索建议状态已存在，但首页 UI 没有完整建议浮层闭环。
- 设置页很多开关没有和真正运行时消费方统一。
- 月报/年报分享目前更像文本分享，不是完整分享图输出。
- 模板/健康/Widget 等旧能力仍有残留，不代表产品仍应该继续做。

---

## 4. 当前最值得优先处理的主题

### 第一优先级：结构止血

1. 清理已废弃或搁置能力残留
2. 统一设置/提醒/通知配置来源
3. 去掉数据库 destructive migration 的产品路径
4. 收口敏感信息存储

### 第二优先级：把高价值半成品补成闭环

1. 首页写作提示卡
2. 连续记录首页展示
3. 搜索历史/建议/筛选完整闭环
4. 周报接入主导航与主体验路径
5. 写作目标首页/统计联动

### 第三优先级：做真正的差异化

1. AI 问答引导写作
2. AI 自动标签
3. 标签层级
4. 报告分享图
5. 地图高级模式

---

## 5. 对其他 AI 的执行约束

1. 不要先写大而空的规划，先核对当前代码事实。
2. 不要把“看见一个页面文件存在”当成能力已经接通。
3. 不要把“需求文档写了”当成代码里真的没有。
4. 修改前先确认该功能是否有两套设置来源或两套入口。
5. Gradle 验证要串行跑，不要并行。
6. 本地测试如果异常，优先检查 JDK 版本，当前仓库更适合 JDK 17。
7. 不要顺手做无关大重构，除非它直接阻碍当前主轴完成。

---

## 6. 推荐给其他 AI 的标准工作流

1. 阅读本文件和四份核心文档。
2. 只选一个主轴，不要并行推进多个大模块。
3. 先确认：
   - 该主轴是否已有半实现
   - 导航是否接通
   - 设置是否真实生效
   - 是否已有测试基础
4. 再写小范围实现计划。
5. 先补测试，再做实现。
6. 完成后更新文档，不要只改代码不改状态说明。

---

## 7. 可直接复制给其他 AI 的提示词

### 提示词 A：继续全局审计

```text
你现在接手的是 DiaryApp，一个本地 Android 日记应用。先不要直接开发功能，也不要假设需求为空白或已完成。

请按下面顺序工作：
1. 先阅读：
   - docs/ai-handoff-bundle/code-audit-major-issues.md
   - docs/ai-handoff-bundle/code-audit-addendum-round2.md
   - docs/ai-handoff-bundle/feature-expansion-report.md
   - docs/ai-handoff-bundle/feature-expansion-addendum-round2.md
   - docs/ai-handoff-bundle/ai-handoff-prompts-and-guide.md
   - docs/整体目标/FINAL-REQUIREMENTS.md
2. 基于当前仓库代码继续做更细的全局审计，重点找：
   - 半实现能力
   - 已废弃功能残留
   - 设置来源分裂
   - 导航未接通页面
   - UI 有但真实运行链路不完整的功能
3. 只输出高价值问题，不要写小毛病。
4. 每个问题必须给：
   - 严重级别
   - 影响范围
   - 关键文件
   - 当前真实状态
   - 建议处理顺序
5. 如果发现现有文档有过时判断，请直接修正文档。
```

### 提示词 B：继续做功能拓展规划

```text
你现在接手 DiaryApp 的后续功能拓展工作。不要从零规划，而是基于现有代码和已有文档继续深入。

必须先阅读：
- docs/ai-handoff-bundle/feature-expansion-report.md
- docs/ai-handoff-bundle/feature-expansion-addendum-round2.md
- docs/ai-handoff-bundle/code-audit-major-issues.md
- docs/ai-handoff-bundle/code-audit-addendum-round2.md
- docs/ai-handoff-bundle/ai-handoff-prompts-and-guide.md
- docs/整体目标/FINAL-REQUIREMENTS.md

你的任务：
1. 识别当前哪些功能已经有基础实现但还没有形成闭环。
2. 在不违背 FINAL-REQUIREMENTS 的前提下，继续补充高价值功能拓展路线图。
3. 对每个拓展方向给出：
   - 当前状态
   - 已有基础
   - 缺口
   - 用户价值
   - 推荐交互
   - 数据结构/DAO 影响
   - 测试建议
   - 建议优先级
4. 优先考虑：首页、搜索、编辑器、统计报告、连续记录、标签系统、AI 辅助。
5. 避免再提出用户已经明确删除的方向，如旧模板、健康系统扩展、称号系统回归等。
```

### 提示词 C：直接开始实施某个主轴

```text
你现在不是来重新理解全项目，而是来落地一个明确主轴。先阅读：
- docs/ai-handoff-bundle/code-audit-major-issues.md
- docs/ai-handoff-bundle/code-audit-addendum-round2.md
- docs/ai-handoff-bundle/feature-expansion-report.md
- docs/ai-handoff-bundle/feature-expansion-addendum-round2.md
- docs/ai-handoff-bundle/ai-handoff-prompts-and-guide.md

然后只针对这个主轴工作：<在这里替换成具体主题，例如“搜索增强闭环”或“设置与提醒系统统一”>

要求：
1. 先确认当前代码是否已有半实现。
2. 列出将修改的文件和原因。
3. 先补或更新测试。
4. 再做最小但完整的实现闭环。
5. 完成后更新相关 md 文档，明确哪些状态已变化。
6. 不要顺手做无关重构，除非它直接阻碍该主轴完成。
```

---

## 8. 推荐的具体拆分主题

如果要把工作拆给多个 AI，推荐这样分：

1. `AI-1：设置与提醒系统统一`
2. `AI-2：首页搜索闭环`
3. `AI-3：报告系统统一与周报接线`
4. `AI-4：编辑器 AI 引导与附件路线`
5. `AI-5：标签层级与标签治理`

这些主题边界更清晰，互相冲突也更少。

---

## 9. 完成一个主轴后要回写什么

后续 AI 每完成一个主题，至少要回写以下内容：

1. 改了哪些能力状态
2. 哪些文档已过时并已修正
3. 哪些需求仍未闭环
4. 下一位 AI 最应该接什么

这样项目才不会一直在同一片区域里重复理解。
