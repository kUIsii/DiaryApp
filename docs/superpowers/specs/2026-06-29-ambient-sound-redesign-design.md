# AmbientSound 功能重设计

## 目标
重新设计环境音播放功能：单列全宽卡片、永久可见控件、解决点击/播放矛盾、增加全局 MiniBar 快速控制、增加主音量/Meander/自动恢复等深度功能。

## 设计

### 1. 卡片布局 AmbientSoundScreen
- LazyColumn（单列）替代 LazyVerticalGrid(2)
- 每行一个 SoundCard，横向布局：左侧圆形图标 + 名称 | 播放/停止按钮 | 音量滑块
- 所有控件始终可见，无折叠/展开
- 点击卡片 = 播放/停止（toggle）
- 移除顶部"音效预设"行（presets 系统保持，但 UI 去掉预设 chips）
- 移除 "save current preset" 对话框（用户临时混音即可，无需显式保存）
- 保留底部定时器行 + 停止全部按钮

### 2. 卡片交互效果
- 播放时：图标区域脉冲呼吸动画（scale 1.0↔1.1），边框发光（color + alpha animate），背景微染色
- 停止时：静态灰白
- 点击反馈：ripple + haptic

### 3. MiniBar 快速控制
- 在 DiaryNavHost.kt 的 Scaffold 中，当 AmbientSoundPlayer 有活跃播放时，在底部导航栏上方插入一条浮动条
- 内容(从左到右)：播放中音效名（滚动文本） | 主音量滑块 | 暂停/继续按钮 | 停止全部按钮 | 展开箭头（点击→导航到 AmbientSoundScreen）
- 入场动画：从底部滑入 + 淡入（animateAnimatedVisibility + slideInVertically）
- 离场：反向动画
- 不遮挡底部导航栏

### 4. 主音量控制
- MiniBar 中包含一个 master volume slider
- AmbientSoundScreen 底部定时器行上方也增加一个 master volume slider
- 调整 master volume 时：等比例缩放所有活跃音效的音量（保持各音效之间的相对比例）
- AmbientSoundPlayer 新增 `masterVolume: Float` 属性

### 5. Meander 模式
- AmbientSoundScreen 底部新增 Meander 开关按钮（图标：波浪线）
- 开启后：每个活跃音效的音量以随机周期（5-15秒）在 0.5x~1.0x 之间正弦波动
- AmbientSoundPlayer 新增 `meanderEnabled: Boolean` 属性
- 通过 Handler postDelayed 循环更新音量

### 6. 自动恢复播放
- AmbientSoundViewModel.onCleared() 时保存当前 activeSounds + volumes + masterVolume + meanderEnabled 到 SharedPreferences
- AmbientSoundViewModel.init 时从 SharedPreferences 恢复并自动开始播放
- 标记位：如果上次退出时没有活跃播放则不恢复

### 7. 代码变更清单

#### AmbientSoundPlayer.kt
- 新增 `masterVolume: Float = 1f`
- 新增 `meanderEnabled: Boolean = false` + meander loop Handler
- `applyVol()` 计算最终音量时乘以 masterVolume
- `getVolume()` 改为返回比例值（不带 masterVolume 和 meander 影响）

#### AmbientSoundViewModel.kt
- 新增 masterVolume 状态
- 新增 meanderEnabled 状态
- `setMasterVolume()` 方法
- `toggleMeander()` 方法
- `onCleared()` 保存到 SharedPreferences（activeSounds keys + volumes + masterVolume + meanderEnabled）
- `init` 块中恢复保存的状态并自动播放

#### AmbientSoundScreen.kt
- LazyColumn 替换 LazyVerticalGrid
- SoundCard 改为横向 Row 布局
- 移除 presets chips 行 + save dialog
- 新增底部 master volume slider + Meander 开关
- 脉冲动画

#### DiaryNavHost.kt
- 添加 AmbientSoundMiniBar  composable
- 在 Scaffold 的 bottomBar 之后插入

#### AmbientSoundService.kt
- 更新 notification 显示状态（可选）

## 不做的
- 不删除 PresetStorage.kt 和 SoundPreset（保持代码可编译，但 UI 不再调用）
- 不从导航移除 AmbientSound 路由
