# 养成世界系统 - 开发进度文档

> 最后更新: 2026-06-24
> 当前版本: 2.64.91-experimental

---

## 一、系统概览

养成世界包含三个子系统，通过日记写作驱动养成：

| 系统 | 核心机制 | 入口 |
|------|----------|------|
| 趣味称号 | 写作行为解锁称号，展示收藏 | 个人页 -> 称号墙 |
| 情绪宠物 | 宠物随心情变化状态和表情 | 个人页 -> 情绪宠物 |
| 心情小岛 | 环境维度反映心情趋势 | 个人页 -> 心情小岛 |

---

## 二、趣味称号系统

### 完成度: 95%

### 已实现
- [x] 数据层: 35+称号定义、7分类(时间旅人/情绪画师/风雨行者/文字匠人/习惯先锋/隐藏彩蛋/全部)
- [x] 分层解锁: tier 1-3(普通/稀有/传说)，不同触发条件
- [x] 进度跟踪: UserTitle 表记录解锁状态和时间
- [x] TitleManager: 监听日记保存，自动检查称号解锁
- [x] UI: 称号墙列表 + 分类tabs + 筛选
- [x] 详情弹窗: BottomSheet 显示图标/名称/稀有度/描述/风味文字
- [x] 装备系统: 设为当前称号，PetScreen 显示"使用中"标签
- [x] 入场动画: 每个 TitleCard 交错 50ms 淡入+上滑
- [x] 解锁动画: 分级粒子效果 + 分阶段动画序列 + 脉冲光环
  - Tier 1 普通(1.5s): 简单 scale+fade
  - Tier 2 稀有(3.5s): 30粒子 + 8光线 + 聚光灯 + 涟漪
  - Tier 3 传说(5.5s): 60粒子彩虹偏移 + 16旋转光线 + 3层同心圆
- [x] 分类统计: tab 旁显示已解锁/总数

### 待探索
- [ ] 称号组合效果 (同时装备多个称号)
- [ ] 称号稀有度排行榜
- [ ] 限时称号 (节日/活动)

---

## 三、情绪宠物系统

### 完成度: 95%

### 已实现
- [x] 数据层: PetState(8种) + PetPersonality(Big Five) + PetProfile + PetStateRecord
- [x] PetStateMachine: 根据时间/心情/连续记录/上次写作时间决定状态
- [x] SentimentAnalyzer: 分析日记情感(-1.0 ~ 1.0)
- [x] PetPersonality: 根据关键词调整5维度，生成反馈风格
- [x] FeedbackGenerator: 10种触发 × 5种风格的模板文案
- [x] 视觉: 有机水滴形身体(贝塞尔曲线) + 8种状态颜色
- [x] 表情系统: 眉毛6种 + 眼睛7种(含眨眼) + 嘴巴7种
- [x] 粒子特效: 星星(兴奋) + 爱心(开心) + 雨滴(难过) + Zzz(困倦) + 问号(好奇) + 汗滴(担心) + 气泡(疲惫)
- [x] 动画: 呼吸(速度随状态) + 眨眼 + 晃动 + 弹跳
- [x] 交互: 点击弹跳+反馈 / 左右滑动梳毛+爱心 / 下拉喂食 / 长按旋转
- [x] 性格反馈: 5种风格(LIVELY/CURIOUS/ENCOURAGING/WARM/CALM)各有独特语气
- [x] 当前称号展示: 宠物名下方显示"使用中"标签
- [x] 小岛等级影响外观: Lv5星星 / Lv10光环 / Lv15翅膀
- [x] 心情历史图表: 7天折线图 + 心情分布甜甜圈图
- [x] 隐藏状态: 夜猫子(月牙眼+月亮) / 宝藏猎人(星星眼+光点) / 暖心守护者(暖黄光晕) / 深海潜水员(气泡) / 时间旅人(重影)
- [x] 画面精度: 渐变填充 + 高光效果 + 阴影 + 眼睛瞳孔细节

### 待探索
- [ ] 宠物互动音效
- [ ] 宠物外观商店

---

## 四、心情小岛系统

### 完成度: 95%

### 已实现
- [x] 数据层: IslandEnvironment(4维度) + IslandProfile + IslandDecoration(16种) + IslandUpdate
- [x] MoodEnvironmentMapper: 心情->环境映射 + 天气/内容影响
- [x] IslandRepository: 处理日记保存、装饰解锁、初始化
- [x] 7层渲染:
  - Layer 0 天空: 3色渐变 + 贝塞尔云朵 + 太阳/月亮光芒 + warmth色调
  - Layer 1 海洋: 4层正弦波浪 + 水面高光线 + 小浪花 + 泡沫气泡
  - Layer 2 地形: 多段贝塞尔海岸线 + 50个沙滩纹理点 + 21根草叶
  - Layer 3 植被: 8层树冠叠加 + 叶脉细节 + 椭圆形花瓣 + 6种花色
  - Layer 4 建筑: 小木屋(烟雾/木纹/窗格) + 灯塔(旋转光束) + 喷泉(水花)
  - Layer 5 动物: 7种动物(鸟/蝴蝶/松鼠/猫头鹰/猫/青蛙/萤火虫)昼夜行为
  - Layer 6 特效: 雨滴(brightness<0.3) / 风效(tranquility<0.3) / 阳光光斑 / 萤火虫
- [x] 交互: 装饰点击详情 + 装备管理(最多5个) + 动画进度条 + 升级庆祝
- [x] 环境维度: 茂盛度/明亮度/宁静度/温暖度 动画进度条

### 已实现(续)
- [x] 昼夜循环: 基于 LocalTime 真实时间，5段天空过渡(夜空/日出/白天/日落/夜空)
- [x] 太阳/月亮: 椭圆轨迹移动 + 光晕效果
- [x] 星星: 40颗预分配位置 + 独立闪烁频率
- [x] 天气增强: 雨滴溅水(水花+扩散环) / 树叶飘落(4色+旋转) / 光斑漂浮
- [x] 水面反光: 日出日落金色 / 夜间银白月光 / 白天微弱白色
- [x] 截图分享: drawToBitmap 截图 + FileProvider 分享 + MediaStore 保存到相册
- [x] 截图预览: 弹窗预览 + 分享/保存/取消按钮
- [x] 动物行为: 7种动物昼夜行为 + 互动规则(猫追蝴蝶/鸟停灯塔等)
- [x] 隐藏发现: 7种稀有元素(满月狼人/彩虹桥/精灵之光/记忆树/烟花/极光) + 9种季节场景
- [x] 组合效果: 5种装饰搭配(温馨家园/守望灯塔/生态乐园/风之谷/静谧水岸)
- [x] 画面精度: 云朵/光芒/波浪/树冠/建筑细节提升
- [ ] 岛屿历史记录时间线

---

## 五、跨功能联动

### 已实现
- [x] CrossSystemManager: Kotlin object 单例事件总线
  - petState: StateFlow<PetState> - 宠物状态供小岛读取
  - islandLevel: StateFlow<Int> - 小岛等级供宠物外观显示
  - titleUnlockEvents: SharedFlow - 称号解锁事件流

- [x] 宠物 -> 小岛:
  - HAPPY/EXCITED: brightness +0.1, warmth +0.05
  - SAD/WORRIED: brightness -0.1, tranquility -0.05
  - SLEEPY: tranquility +0.1

- [x] 小岛 -> 宠物:
  - Lv5-14: 头顶三颗金色星星
  - Lv10+: 身体金色光环
  - Lv15+: 两侧白色翅膀

- [x] 称号 -> 宠物:
  - 传说级: 宠物进入 EXCITED
  - 稀有级: 宠物进入 HAPPY
  - 普通级: 好感度+5

- [x] 连续记录buff:
  - streak >= 7: "勤奋之光" brightness +0.15
  - streak >= 30: 解锁"荣誉旗杆"装饰

---

## 六、文件清单

### 新增文件
```
data/
  PetModels.kt          - PetState, PetPersonality, PetStateRecord, PetProfile
  PetDao.kt             - 宠物数据库操作
  PetStateMachine.kt    - 状态决定逻辑
  PetPersonality.kt     - 性格维度更新+反馈风格
  SentimentAnalyzer.kt  - 情感分析
  FeedbackGenerator.kt  - 反馈文案生成
  TitleManager.kt       - 称号管理器
  TitleModels.kt        - TitleDefinition, UserTitle, TitleProfile
  TitleDao.kt           - 称号数据库操作
  IslandModels.kt       - IslandEnvironment, IslandProfile, IslandDecoration
  IslandDao.kt          - 小岛数据库操作
  MoodEnvironmentMapper.kt - 心情->环境映射
  IslandRepository.kt   - 小岛业务逻辑
  CrossSystemManager.kt - 跨系统事件总线
  PetMemoryRepository.kt - 宠物记忆系统
  PetAiGenerator.kt - AI深度内容生成

ui/title/
  TitleScreen.kt        - 称号墙主界面
  TitleUnlockAnimation.kt - 称号解锁动画(含粒子系统)
  TitleViewModel.kt     - 称号 ViewModel

ui/pet/
  PetScreen.kt          - 宠物主界面
  PetComposable.kt      - 宠物 Canvas 绘制组件
  PetViewModel.kt       - 宠物 ViewModel

ui/island/
  IslandScreen.kt       - 小岛主界面
  IslandCanvas.kt       - 小岛 7层 Canvas 渲染
  IslandViewModel.kt    - 小岛 ViewModel
  IslandDiscoveryScreen.kt - 发现档案界面
```

### 修改的文件
```
data/DiaryDatabase.kt   - version 24->26, 新增 entities 和 migrations
ui/navigation/DiaryNavHost.kt - 新增路由 TitleWall/Pet/Island
ui/profile/ProfileScreen.kt  - 新增三个入口
ui/editor/EditorViewModel.kt - 保存后触发称号/宠物/小岛检查
```

---

## 七、技术要点

- **Canvas 绘制**: 所有视觉元素用 Compose Canvas + Path 贝塞尔曲线绘制，零外部资源
- **动画**: InfiniteTransition(循环) + animateFloatAsState(状态变化) + LaunchedEffect(序列)
- **状态管理**: StateFlow + ViewModel，跨系统通过 CrossSystemManager 通信
- **数据库**: Room，version 28，4次 migration(24->25->26->27->28)
- **性能**: Path对象复用、粒子池(上限50)、分层缓存
- **风格**: 宠物=几何极简(有机水滴形)，小岛=柔和像素扁平(纯色块无轮廓)

---

## 八、设计文档

### 已完成的设计研究
- [小岛深度生态系统设计](island-ecosystem-design.md) - 动物行为/隐藏发现/记忆碎片/叙事章节/组合效果
- [宠物深度扩展设计](pet-depth-design.md) - 成长阶段/记忆系统/隐藏状态/性格深度/关键词反应

### 核心设计理念
**从"静态装饰画"变成"活的生态世界"**，关键是内容深度而非动画复杂度。

---

## 九、下一步方向

### 优先级 P0
- [x] ~~小岛昼夜循环~~ (已完成)
- [x] ~~天气粒子增强~~ (已完成)

### 优先级 P1
- [x] ~~宠物心情历史图表~~ (已完成)
- [x] ~~小岛截图分享功能~~ (已完成)

### 优先级 P2 (内容深度 - 优先)
- [x] ~~宠物成长阶段系统~~ (已完成 - 三阶段+进化条件+外观变化)
- [x] ~~宠物记忆系统~~ (已完成 - 纪念记忆+习惯记忆+记忆衰减)
- [x] ~~AI 深度内容集成~~ (已完成 - PetAiGenerator 4个AI生成能力)
- [x] ~~宠物隐藏状态~~ (已完成 - 5种隐藏状态触发检测+特殊视觉效果)
- [x] ~~小岛动物行为系统~~ (已完成 - 7种动物昼夜行为+互动规则)
- [x] ~~小岛隐藏发现系统~~ (已完成 - 7种稀有元素+9种季节场景+发现档案)
- [x] ~~小岛组合效果~~ (已完成 - 5种装饰搭配配方+视觉增强)

### 优先级 P3 (增强体验)
- [ ] 称号组合效果
- [ ] 宠物互动音效
- [ ] 摇晃手机触发小岛特效
- [ ] 岛屿历史记录时间线
- [ ] 限时称号 (节日/活动)
