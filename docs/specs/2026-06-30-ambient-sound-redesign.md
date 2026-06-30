# 环境音页面重设计

## 概述
重构 DiaryApp 的环境音功能，替换代码生成的假音频为真实音源（首次下载+缓存），重新设计 UI 为暗暖调沉浸式风格，新增全屏播放器、分类背景切换、进度/音量控制、睡眠定时等功能。

## 视觉方向

### 色调
- 基于 Clay Paper 主题深色模式的暖棕暗调
- 背景：深暖色渐变（`#1C1511` → `#17120F`）
- 卡片：半透明深色底（`#2A1F1ABB`）+ 圆角 14dp
- 主强调色：暖陶色（`#CCA090`）
- 主文字：`#F2E3DA` / 次要：`#9A8579`
- 分隔线：`#3A2D28`

### 图片素材
- 主背景：Unsplash 实景森林夜晚图片，运行时加载（不增加 APK 体积）
- 分类背景：每个分类对应不同实景图（助眠→夜晚、自然→森林、伴读→暖室、冥想→山雾）
- 音轨卡片图：每个音轨配一张 AI 生成的实景风格小图（非矢量、非渐变圆）
- 全屏播放器背景：当前播放音轨对应的大幅图片

### 图标
- 使用 Phosphor 图标库（play, pause, stop, skip-back, skip-forward, clock, speaker-high, arrow-down）
- 不引入 emoji、代码组合图标、简单矢量图

## 交互流程

### 浏览态
1. 用户进入环境音页面，看到分类标签（默认选中"助眠"）
2. 下方展示当前分类的 6-8 张音轨卡片
3. 每张卡片：实景小图 + 音轨名 + 时长 + 播放按钮
4. 首次播放某音轨 → 显示下载进度 → 缓存到本地 → 开始播放
5. 已缓存的音轨直接播放

### 播放态
1. 点击卡片播放按钮 → MiniBar 在底部出现，显示"正在播放"
2. 点击 MiniBar 或卡片 → 展开全屏播放器
3. 全屏播放器：背景图、专辑封面、进度条、音量条、控制按钮、睡眠定时器
4. 点击下拉箭头或返回 → 收起全屏，返回浏览页，MiniBar 继续显示

### 多分类
- 切换分类标签 → 背景图切换 + 音轨列表切换
- 当前播放音轨所属分类标签高亮

## 功能清单

### 核心功能
- [x] 模拟音频替换为真实音源（下载+缓存）
- [x] 4 个分类（助眠/自然/伴读/冥想），每个分类 6 个音轨
- [x] 分类标签切换
- [x] 每个分类切换时背景图随之变化
- [x] 音轨播放/暂停/停止
- [x] MiniBar 全局悬浮
- [x] 全屏播放器
- [x] 进度条拖动
- [x] 音量控制
- [x] 睡眠定时器（15/30/45/60/90 分钟，到时淡出停止）
- [x] 通知栏控制（复用现有 AmbientSoundService）

### 增强功能
- [x] 收藏音轨
- [x] 最近播放记录
- [x] 已缓存/未下载状态标识
- [x] 切歌（上一首/下一首）

## 架构设计

### 分层

```
UI Layer
  AmbientSoundScreen (浏览页 + 全屏播放器)
  AmbientSoundMiniBar (全局 MiniBar)
  AmbientSoundViewModel (状态管理)

Service Layer
  AmbientSoundService (通知栏 + 后台播放)
  AmbientSoundPlayer (音频播放控制)

Data Layer
  AudioRepository (音轨元数据)
  AudioCacheManager (下载 + 缓存管理)
  AmbientSoundDao (收藏/最近播放/缓存状态的数据库)
```

### 数据流
1. ViewModel 从 AudioRepository 获取音轨列表
2. 用户点击播放 → ViewModel 检查是否已缓存
3. 未缓存 → AudioCacheManager 下载 → 完成后回调 → AmbientSoundPlayer 播放
4. 已缓存 → AmbientSoundPlayer 直接播放
5. 播放状态同步到 ViewModel → UI 更新（MiniBar、全屏播放器）

### 音频缓存
- 音轨首次播放时下载到 `Context.filesDir/ambient_sounds/`
- 每个音轨用 `track_id.mp3` 命名
- 缓存版本号记录在 SharedPreferences，版本号变化时重新下载

## 文件变更清单

### 新增文件
- `data/ambientsound/AudioTrack.kt` — 音轨数据模型
- `data/ambientsound/AudioRepository.kt` — 音轨元数据提供
- `data/ambientsound/AudioCacheManager.kt` — 下载+缓存管理
- `data/ambientsound/AmbientSoundDao.kt` — 收藏/记录 DAO

### 修改文件
- `ui/ambientsound/AmbientSoundScreen.kt` — 完全重写
- `ui/ambientsound/AmbientSoundViewModel.kt` — 完全重写
- `ui/ambientsound/AmbientSoundPlayer.kt` — 重写，移除代码合成
- `ui/ambientsound/AmbientSoundMiniBar.kt` — 更新 UI
- `ui/ambientsound/AmbientSoundService.kt` — 微调（可能）

### 数据库
- 版本升到 37
- 新增 `ambient_sound_favorites` 表
- 新增 `ambient_sound_recent` 表

## 关键约束
- 必须保持所有已有功能不受影响
- 数据库迁移必须正确处理
- 全屏播放器的返回/关闭必须恢复浏览页状态
- 后台播放必须正确处理生命周期（复用现有 Service 架构）
- 图片从 Unsplash 加载，不增加 APK 大小
- 音频文件缓存后离线可用
