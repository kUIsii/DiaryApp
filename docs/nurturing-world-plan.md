# 养成世界 — 完整设计与实现计划

## 概述

将日记从"记录工具"变成"养成世界"。用户每天写日记，不仅是在记录，更是在塑造一个世界。

三个核心功能：
1. **趣味称号** — 让用户发现自己是有趣的人
2. **情绪宠物** — 一个安静的小伙伴
3. **心情小岛** — 一个随心情变化的世界

## 实现顺序

1. 趣味称号（最独立，不影响现有功能）
2. 情绪宠物 MVP（需要称号数据基础）
3. 心情小岛 MVP（需要前两者数据基础）

---

# 一、趣味称号系统

## 核心概念

称号不是成就。成就回答"我做了多少"，称号回答"我是什么样的人"。

用户打开称号页面，看到的不是进度条，而是关于自己的故事。

## 称号分类（35+个）

### 时间旅人（7个）
- 凌晨诗人：凌晨0-3点写过3篇以上
- 黎明记录者：凌晨3-5点写过日记
- 晨光之笔：5-7点写过5篇以上
- 午后漫想家：80%以上写于12-15点
- 夜猫子：70%以上写于22-2点
- 星期杀手：每个星期几都写过
- 时光胶囊：连续12个月每月都写了

### 情绪画师（6个）
- 乐观主义者：连续10篇心情>=5
- 深度思考者：连续10篇心情<=2
- 情绪调色板：使用过全部6种心情
- 心情过山车：单月内心情变化超过4次
- 平静之海：连续20篇心情稳定在3-4
- 情绪日记家：心情与天气有明显相关性

### 风雨行者（6个）
- 雨天收藏家：雨天写了20篇以上
- 雪日笔记：下雪天写过日记
- 风暴作家：大风天写了5篇以上
- 晴天记录者：晴天写了50篇以上
- 万事皆记：所有天气类型下都写过
- 无惧风雨：极端天气下都写了

### 文字匠人（6个）
- 千字长文：单篇超过1000字
- 微言大义：少于50字但被收藏
- 图文大师：单篇包含3张以上图片
- 标签达人：使用10个以上标签
- 收藏鉴赏家：收藏20篇以上
- 万字耕耘者：累计超过5万字

### 习惯先锋（5个）
- 日更达人：连续30天
- 百日坚持：连续100天
- 回归者：断写30天后重新开始
- 闪电记录：1分钟内完成一篇
- 双子星：同一天写了2篇以上

### 隐藏彩蛋（5个）
- 时间旅行者：1月1日写日记
- 跨年守夜人：12月31日23:00后写
- 午夜钟声：0:00-0:10之间写
- 满月记录：农历十五前后写
- 首篇回响：为第一篇日记添加图片或标签

## 数据模型

### TitleDefinition（称号定义表）
```
key: String (主键)
name: String
description: String
category: String (time/mood/weather/writing/habit/hidden)
iconName: String
tier: Int (1普通/2稀有/3传说)
isHidden: Boolean
flavorText: String
```

### UserTitle（用户获得的称号表）
```
id: Long (自增主键)
titleKey: String (关联TitleDefinition)
unlockedAt: Long
relatedEntryId: Long? (可选，触发解锁的日记ID)
```

### TitleProfile（称号展示配置表）
```
id: Long = 1 (单例)
activeTitleKey: String? (当前展示的称号)
showTitleOnHome: Boolean
showTitleOnEntry: Boolean
```

## 检查机制

### 三个触发时机

1. **保存日记后**：异步检查与本篇相关的称号
2. **打开称号页时**：全量扫描所有称号
3. **应用启动时**：检查需要时间推移的称号

### TitleChecker 接口
```kotlin
interface TitleChecker {
    val key: String
    suspend fun check(diaryDao, titleDao): Boolean
}
```

### 具体检查器
- NightPoetChecker：检查凌晨写作
- DawnRecorderChecker：检查黎明写作
- RainCollectorChecker：检查雨天写作
- WordCountChecker：检查字数
- StreakChecker：检查连续天数
- ...等等

## UI展示

### 位置一：个人资料页 — 称号墙
FlowRow展示已解锁称号的小标签，点击进入完整称号册。

### 位置二：称号详情页
分类Tab + 两列网格，已解锁彩色，未解锁灰色。

### 位置三：首页（可选）
用户可选择展示当前称号。

### 位置四：解锁通知
保存日记后如果解锁新称号，顶部滑入通知横幅，3秒消失。

### 解锁动画分层
- 普通称号（1-2秒）：简单淡入
- 稀有称号（3-4秒）：有叙事性的专属动画
- 传说称号（5-6秒）：完整场景变化+音效

## 文件清单

### 新建文件
```
app/src/main/java/com/diary/app/data/TitleModels.kt
app/src/main/java/com/diary/app/data/TitleDao.kt
app/src/main/java/com/diary/app/data/TitleChecker.kt
app/src/main/java/com/diary/app/data/TitleManager.kt
app/src/main/java/com/diary/app/data/TitleSeedData.kt
app/src/main/java/com/diary/app/ui/title/TitleScreen.kt
app/src/main/java/com/diary/app/ui/title/TitleViewModel.kt
app/src/main/java/com/diary/app/ui/title/TitleUnlockAnimation.kt
```

### 修改文件
```
app/src/main/java/com/diary/app/data/DiaryDatabase.kt (注册Entity, Migration 23->24)
app/src/main/java/com/diary/app/ui/profile/ProfileScreen.kt (添加称号墙)
app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt (添加路由)
app/src/main/java/com/diary/app/ui/editor/EditorViewModel.kt (保存后触发检查)
```

---

# 二、情绪宠物系统

## 核心概念

不是聊天机器人，不是AI助手，是一个安静的小伙伴。

它的存在感是恰到好处的。太少了感觉不到，太多了觉得打扰。

## 宠物状态（8种）

1. **平静** — 默认状态，安静坐着，轻微呼吸
2. **开心** — 轻轻蹦跳，彩色光点
3. **困倦** — 打哈欠，眼睛眯着
4. **担心** — 担忧表情，轻轻颤抖
5. **难过** — 低头，灰色调，小雨滴
6. **兴奋** — 快速跳跃，星星特效
7. **好奇** — 歪头，问号特效
8. **疲惫** — 趴着，叹气

## 状态转换规则

- 时间维度：深夜→困倦，早晨→平静
- 内容情绪：积极→开心，消极→难过/担心
- 互动频率：长期未记录→担心/想念
- 累积效应：连续积极→开心，连续压力→疲惫

## 性格形成（基于大五人格简化版）

五个维度，从日记内容提取：
- 外向性："聚会"、"朋友"、"社交"
- 开放性："学习"、"尝试"、"新"
- 尽责性："计划"、"完成"、"目标"
- 宜人性："帮助"、"感谢"、"包容"
- 情绪稳定性：焦虑词汇 vs 平静词汇

每个关键词匹配微调2%，性格随时间慢慢形成。

## 反馈触发条件

- 首次写日记：欢迎
- 连续记录3天：鼓励
- 积极内容：庆祝
- 消极内容：安慰
- 深夜记录：关心
- 长期未记录：想念
- 压力内容：支持
- 目标达成：祝贺
- 新话题：好奇

## 反馈文案风格

根据宠物性格自动选择：
- 高外向性 → 活泼型
- 高宜人性 → 温暖型
- 高开放性 → 好奇型
- 高情绪稳定性 → 平静型
- 高尽责性 → 鼓励型

## 动画系统

使用 Lottie 格式：
- 待机动画：5种状态循环
- 动作动画：跳跃、挥手、睡觉
- 反应动画：开心、惊讶、安慰

## 与小岛的关系

- 小岛环境影响宠物状态
- 宠物状态影响小岛氛围
- 宠物是小岛的"代言人"

## 文件清单

### 新建文件
```
app/src/main/java/com/diary/app/data/PetModels.kt
app/src/main/java/com/diary/app/data/PetDao.kt
app/src/main/java/com/diary/app/data/PetStateMachine.kt
app/src/main/java/com/diary/app/data/PetPersonality.kt
app/src/main/java/com/diary/app/data/FeedbackGenerator.kt
app/src/main/java/com/diary/app/data/SentimentAnalyzer.kt
app/src/main/java/com/diary/app/ui/pet/PetScreen.kt
app/src/main/java/com/diary/app/ui/pet/PetViewModel.kt
app/src/main/java/com/diary/app/ui/pet/PetComposable.kt
```

### 修改文件
```
app/src/main/java/com/diary/app/data/DiaryDatabase.kt (注册Entity)
app/src/main/java/com/diary/app/ui/home/HomeScreen.kt (添加宠物角落)
app/src/main/java/com/diary/app/ui/editor/EditorViewModel.kt (保存后更新宠物)
```

---

# 三、心情小岛系统

## 核心概念

你的每一天都在塑造一个世界。写下的每一个字、记录的每一种心情，都会让小岛发生变化。

## 四个环境维度

1. **茂盛度（Lushness）** — 植被覆盖，0.0~1.0
2. **明亮度（Brightness）** — 光照氛围，0.0~1.0
3. **宁静度（Tranquility）** — 水面/风，0.0~1.0
4. **温暖度（Warmth）** — 色调温度，0.0~1.0

## 心情→环境映射

- 沮丧：四维度下降，阴云、枯草、雨滴
- 低落：轻微下降，多云、薄雾
- 平静：宁静度增加，晴间多云、草地
- 开心：四维度上升，晴天、花朵
- 愉快：明显上升，花海、彩虹
- 兴奋：大幅上升，璀璨、星光

## 等级解锁内容

- Lv3: 小鸟
- Lv5: 小木屋
- Lv8: 蝴蝶（需积极心情）
- Lv12: 松鼠
- Lv15: 灯塔（需连续7天）
- Lv18: 猫头鹰（需夜间写作）
- Lv20: 桥梁
- Lv25: 喷泉（需5种心情）
- Lv30: 守护精灵（需全部心情）
- Lv40: 守护者雕像（需连续30天）
- Lv50: 巨龙（需连续100天）

## 视觉图层

从底到顶：
- Layer 0: 天空背景
- Layer 1: 海洋
- Layer 2: 岛屿地形
- Layer 3: 植被
- Layer 4: 建筑/装饰
- Layer 5: 动物
- Layer 6: 特效

## 经验值系统

- 基础：10经验/篇
- 字数：每100字+1，最多+20
- 心情：+5
- 天气：+3
- 连续倍率：3天1.1x，7天1.3x，14天1.5x，30天2.0x
- 升级公式：100 * level + 50 * level * (level-1) / 2

## 文件清单

### 新建文件
```
app/src/main/java/com/diary/app/data/IslandModels.kt
app/src/main/java/com/diary/app/data/IslandDao.kt
app/src/main/java/com/diary/app/data/IslandRepository.kt
app/src/main/java/com/diary/app/data/MoodEnvironmentMapper.kt
app/src/main/java/com/diary/app/ui/island/IslandScreen.kt
app/src/main/java/com/diary/app/ui/island/IslandViewModel.kt
app/src/main/java/com/diary/app/ui/island/IslandCanvas.kt
app/src/main/java/com/diary/app/ui/island/layers/SkyLayer.kt
app/src/main/java/com/diary/app/ui/island/layers/OceanLayer.kt
app/src/main/java/com/diary/app/ui/island/layers/TerrainLayer.kt
app/src/main/java/com/diary/app/ui/island/layers/VegetationLayer.kt
app/src/main/java/com/diary/app/ui/island/layers/BuildingLayer.kt
app/src/main/java/com/diary/app/ui/island/layers/AnimalLayer.kt
app/src/main/java/com/diary/app/ui/island/layers/EffectLayer.kt
```

### 修改文件
```
app/src/main/java/com/diary/app/data/DiaryDatabase.kt (注册Entity)
app/src/main/java/com/diary/app/ui/home/HomeScreen.kt (添加小岛入口)
app/src/main/java/com/diary/app/ui/navigation/DiaryNavHost.kt (添加路由)
app/src/main/java/com/diary/app/ui/editor/EditorViewModel.kt (保存后更新小岛)
```

---

# 工作进度追踪

## 阶段一：趣味称号（进行中）

- [ ] 创建工作计划文档
- [ ] 数据层（Entity, DAO, Migration, SeedData）
- [ ] 业务逻辑（TitleChecker, TitleManager）
- [ ] UI层（TitleScreen, TitleViewModel, 称号墙, 解锁动画）
- [ ] 集成测试

## 阶段二：情绪宠物（待开始）

- [ ] 数据层（PetModels, PetDao）
- [ ] 状态机和性格系统
- [ ] 反馈文案系统
- [ ] UI和动画
- [ ] 集成到首页

## 阶段三：心情小岛（待开始）

- [ ] 数据层（IslandModels, IslandDao）
- [ ] 心情环境映射
- [ ] Canvas渲染引擎
- [ ] 等级和解锁系统
- [ ] UI和动画
- [ ] 集成到首页
