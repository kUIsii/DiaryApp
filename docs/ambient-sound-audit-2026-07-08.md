# 背景音（场景环境音）功能全面审查报告

> 审查时间：2026-07-08 ｜ 审查范围：全部 `ui/ambientsound`、`data/ambientsound`、`AmbientSoundService`、专注模式的声音模块、导航集成
> 结论：**体验感差是系统性的，不是个别小 bug**。根因集中在「音频源脆弱」「主线程阻塞」「两套互不连通的声音子系统」「播放器生命周期反模式」「七主题被硬编码颜色打破」五个层面。

---

## 一、总览：问题地图（按影响力）

| 维度 | 严重度 | 问题 |
|---|---|---|
| 内容/资源 | 【致命】 | 24 首全部强依赖 Freesound 远程下载，本地零兜底；下载失败会把错误页当 mp3 永久缓存 |
| 架构 | 【严重】 | `MediaPlayer.prepare()` 在主线程同步阻塞，首次播放冻结 UI，加载态形同虚设 |
| 架构 | 【严重】 | 后台 `ForegroundService` 不持有 `MediaPlayer`，播放器绑在单例的首个 Context（常为 Activity），后台不稳/泄漏 |
| 内容/资源 | 【严重】 | 两套独立「环境音」子系统互不连通（AmbientSound 24 首 vs 专注模式 rain/cafe/whitenoise） |
| UI 设计 | 【严重】 | 主题适配被硬编码颜色打破：收藏心形 `Color(0xFFE07070)` 写死，七主题失效 |
| 交互 | 【严重】 | 同一屏出现**两个播放器**（页面内联播放条 + 全局 MiniBar 重叠），控件冗余冲突 |
| 交互 | 【中等】 | 打开 App 自动恢复并外放上次声音（含音量），不打招呼 |
| 交互 | 【中等】 | 进度条对「30s 循环片段」无意义，拖动体验差 |
| 交互 | 【中等】 | 睡眠定时依赖 ViewModel 存活；`checkAndExpireSleepTimer()` 写成却从未调用（死代码） |
| 交互 | 【中等】 | 双轮询（500ms 进度 + 1s 睡眠）反复整状态拷贝，重组合过剩 |
| UI 设计 | 【轻微】 | 「律动」音量正弦抖动与用户音量设置互相打架；通知用系统图标无品牌 |
| 代码质量 | 【轻微】 | `TrackCard` 标题 `if (isActive) onSurface else onSurface` 死分支；无内容校验/重试 |

---

## 二、内容 / 资源维度

### 【致命】音频源单点依赖远程且无可兜底（AudioRepository.kt / AudioCacheManager.kt）
- 24 首曲目 `audioUrl` 全部是 `https://cdn.freesound.org/previews/...`。Freesound 预览 CDN 接口对鉴权/来源敏感，在真实设备上**经常 403/不稳定**，且属于带版权要求的采样片段，长期可用性无保障。
- `AudioCacheManager.prepare()` 的查找顺序是：① 本地 asset `ambient_sounds/{id}.mp3`（**工程里根本没有打包这些文件**）→ ② 远程下载。也就是说：**离线、弱网、或 CDN 抽风时，所有声音 100% 放不出来**。
- **更隐蔽的毒缓存 bug**（`AudioCacheManager.kt:33-43`）：远程下载若返回的是错误页/HTML（如 403 页面），`input.copyTo(output)` 会把这段 HTML 写成 `{id}.mp3` 并 `return success`。之后 `isCached()` 永远为 true，**这条音频被永久污染，除非手动清缓存**。没有任何 content-type 校验、没有下载后大小/格式检查。
- 后果：用户看到列表、点播放 → 显示「音频文件缺失 / 无法加载音频」→ 实际是 CDN 拉不到或被污染缓存。这正是「体验感极差」的最可能主因。

### 【严重】内容本身是「凑数」的（AudioRepository.kt）
- 全部 24 首 `durationSeconds = 30` 且播放器 `isLooping = true`，即每段都是 30 秒循环，无真实时长信息。
- 中文名与英文名映射、subtitle 均为模板化文案（如「白噪音助眠」「温暖柴火声」），无真正描述、无分类里的「推荐/心情」等用户价值。
- 封面走 Unsplash 远程图，失败即降级为 MusicNote 占位，弱网下整页「图都加载不出」。

### 【严重】两套互不连通的声音系统（专注模式 vs 场景环境音）
- `FocusModeScreen` 里有独立的 `SoundChip`：`rain / cafe / whitenoise` 三个**硬编码字符串**（`FocusModeScreen.kt:243-256`），由 `FocusModeViewModel.selectedSound` 保存为偏好。
- 但 `FocusModeViewModel` 里**完全没有 `AmbientSoundPlayer`、没有 `MediaPlayer`、没有音频资源引用**（grep 确认无 player/audio 调用）。也就是说专注模式这 3 个声音**大概率根本不会出声**，或走完全不同的、未接好的播放路径。
- 用户体验上的割裂：在「场景环境音」里精心选了「雨打芭蕉」并收藏，进专注模式却只能选一个不知道有没有声音的「rain」；两套列表、两套状态、两套历史，互不可见。

---

## 三、架构维度

### 【严重】`MediaPlayer.prepare()` 主线程同步阻塞（AmbientSoundPlayer.kt:72-114）
- `play()` 中 `mp.prepare()` 是**同步阻塞调用**，运行在调用方线程（UI 线程）。而 `AudioCacheManager.prepare` 又是 `suspend` 在 `Dispatchers.IO` 下载，下载完后 `player.play` 回到 ViewModel 协程（默认 `Dispatchers.Main.immediate`）→ 在**主线程做 `prepare()`**。
- 对远程 30s mp3，缓冲/解码会卡住主线程几百毫秒到数秒，期间 UI 冻结。屏幕上那个「加载中…」的 `isPreparing` 状态**根本来不及渲染就被阻塞**，所以用户感知到的就是「点一下卡一下才出声」甚至 ANR 风险。
- 正确做法：`prepareAsync()` + `OnPreparedListener`，或全面切到 `androidx.media3`（ExoPlayer），支持流式播放、缓存、无缝循环。

### 【严重】播放器与 Service 生命周期错配
- `AmbientSoundService`（前台服务）本应「持有并管理播放」，但代码里它只是 `player = AmbientSoundPlayer.getInstance()` 的**代理壳**：通知、MediaSession 回调都转调单例。
- 真正的 `MediaPlayer` 创建在 `AmbientSoundPlayer` 单例里，其 `Context` 是**第一次调用 `play(context, ...)` 的那个 context**（通常是某个 Activity/屏幕）。Activity 销毁后，这个 MediaPlayer 的 context 引用存在泄漏风险；且播放完全依赖单例存活，而不是服务的生命周期。
- 设计上应是：**Service 拥有 MediaPlayer（或把单例的 context 固定为 ApplicationContext）**，并让 sleep timer / 停止逻辑由 Service 驱动，而不是绑定在 UI 的 ViewModel 上。
- 顺带：`MediaPlayer` 是 Android 已标记 legacy 的 API，用于「需要长时间后台循环的环境音」并不合适（无缝循环、音频焦点、省电都不如 Media3）。

### 【中等】音频焦点处理简陋（`AmbientSoundPlayer.kt:40-47`）
- 失去焦点（如来电、其他 App 出声）只做 `pause()`，恢复焦点 `AUDIOFOCUS_GAIN` 时**没有自动 resume**（只 `applyVolume()`）。用户接完电话回来，声音不会自己续上，需手动点。
- duck（被压低）逻辑正确，但整体缺乏「电话/闹钟优先级」考虑。

---

## 四、UI 设计维度

### 【严重】七主题适配被硬编码颜色打破
- 你之前明确要求的「不硬编码颜色、每个主题对应」在这里被违反：`TrackCard.kt:455` 与 `FullscreenPlayer.kt:568` 的收藏心形用了 `Color(0xFFE07070)`——一个固定的粉红，不受任何主题控制。在「墨石板」「沙琥珀」等深色/暖色主题下它依然是这个红，与其他强调色脱节。
- 应改为 `MaterialTheme.colorScheme` 里某个语义色（如 `error` 或扩展色 `favorite`），或在 `ExtendedColors` 里加一个随主题变的收藏色。

### 【严重】同一屏出现两个播放器（重叠冗余）
- `DiaryNavHost.kt:829` 在**所有路由**下都挂了全局 `AmbientSoundMiniBar`（当 `currentTrack != null` 时显示，且 `align(BottomCenter)` 浮在最上层）。
- 而 `AmbientSoundScreen` 的 `BrowseView`（`AmbientSoundScreen.kt:277-351`）**内部**已经有一个内联播放条（音量 + 律动 + 睡眠 + 停止）。
- 结果：进入「场景环境音」页面播放时，屏幕底部**同时出现内联条和浮层 MiniBar 两层控件**，既重叠又浪费空间，还容易点错。MiniBar 本应只在「离开该页面后」常驻，但当前逻辑在页面内也显示。

### 【轻微】全屏播放器强调色不一致
- `BrowseView` 用 `colorScheme.primary` 作强调色，`FullscreenPlayer` 却用 `colorScheme.tertiary`（`:501`）。同一功能的强调色不统一，视觉语言分裂。

### 【轻微】通知图标用系统 drawable
- `AmbientSoundService.kt:119` 等用 `android.R.drawable.ic_media_play`、`ic_menu_close_clear_cancel`——系统图标，无品牌感（天气预警你也曾指出同样问题）。

---

## 五、交互维度

### 【中等】打开 App 自动恢复并外放
- `AmbientSoundViewModel.init`（`:70-99`）在 `shouldRestoreAmbientTrack` 为真时，会**自动 `player.play` 恢复上次声音**。打开 App 即突然出声（且用上次音量），若未戴耳机会很尴尬；同时会立刻拉起前台通知。
- 建议改为：恢复「上次在听」状态但**默认暂停**，进入页面再手动续播；或至少只在用户回到该页时提示「是否继续」。

### 【中等】进度条对循环片段无意义
- 所有片段 30s 且 `isLooping`，但全屏页有「进度」滑块 + `HH:MM/HH:MM` 时间（`AmbientSoundScreen.kt:604-616`）。拖动后 30s 一到又从头循环，滑块位置与「真实进度感」脱节，交互价值很低，反而显得「功能有但不好用」。
- 对纯循环环境音，应隐藏进度条、改用「已播放时长」或干脆去掉。

### 【中等】睡眠定时脆弱 + 死代码
- `startSleepTimer` 在 ViewModel 里起一个 `viewModelScope` 协程每秒刷新剩余时间（`:252-260`）；同时 `startProgressUpdates` 每 500ms 也刷一次——**两个循环都在 `syncStateWithPlayer`**，状态拷贝翻倍。
- 真正到期停止依赖 `startSleepTimer` 协程里的 `if (player.isSleepExpired()) stop()`。但 `AmbientSoundPlayer.checkAndExpireSleepTimer()`（`:199-204`）**全文没有任何地方调用**——纯粹死代码，说明作者曾想在服务侧兜底，但最终没接上。一旦 ViewModel 因某种原因被清理，睡眠定时就失效，音乐无限播。

### 【中等】「律动」与用户音量打架
- `meanderEnabled`（律动）开启后，用 `Handler` 每 200ms 按正弦改 `meanderFactor` 调音量（`:206-221`）。它会覆盖/乘算用户设定的 `volume`，用户拖音量条时律动还在后台改音量，体验混乱。且这是 200ms 一次的 main-thread handler 持续运行，省电不友好。

### 【轻微】双循环状态轮询导致重组合过剩
- `startProgressUpdates` 的 `while(true){ delay(500); syncStateWithPlayer() }` 在播放期间**永久每 0.5s 重建整个 `AmbientSoundState`**（含 `currentTrack` 新引用），会触发整棵订阅树重组合。虽然是 0.5s 一次不算离谱，但结合 sleep 循环、pulse 动画、meander handler，多套定时器叠加，后台耗电与卡顿隐患明显。

---

## 六、修复优先级建议（供决策）

**P0（不修就谈不上可用）**
1. 音频源：把音频**打包进 `res/raw` 或 `assets/ambient_sounds/`**（24 首本地化），彻底去掉对 Freesound 的运行时依赖；保留远程仅作可选扩展。
2. 修毒缓存：`AudioCacheManager` 下载后校验 content-type/大小，失败不写盘、已污染的能重试。
3. `prepare()` 改 `prepareAsync()`（或迁移 Media3），杜绝主线程阻塞。

**P1（体验质变）**
4. 合并两套声音系统：专注模式的 rain/cafe/whitenoise 直接复用 AmbientSound 的曲目/播放器，或明确接好播放。
5. 去掉同屏双播放器：MiniBar 仅在非「场景环境音」路由显示。
6. 收藏色改为主题语义色（修 `0xFFE07070`）。
7. 打开 App 不再自动外放，改为恢复但暂停。

**P2（打磨）**
8. 隐藏/重做进度条（循环片段无意义）。
9. 睡眠定时改由 Service 驱动，删死代码 `checkAndExpireSleepTimer` 或接上。
10. 全屏播放器强调色统一为 primary；通知换品牌图标；律动与音量解耦。
11. 合并双轮询为单一播放状态流（用 `StateFlow`/回调驱动，而非定时轮询）。

---

## 七、小结
这套背景音功能「功能清单看起来齐全」（分类/收藏/最近/音量/律动/睡眠/全屏/通知控制都有），但**地基是错的**：音频源在云端且会自我污染、播放阻塞主线程、后台模型不稳、主题被写死、两套系统打架、同屏双控件。所以用户感受到的「极差」是必然的，不是调几个参数能解决的——按 P0→P1 顺序重构资源与播放链路，体验才会真正可用。

---

## 八、本轮已落地修复（2026-07-08 第二轮）

> 代码侧修复已完成，并经 `:app:compileExperimentalDebugKotlin` 复验 **BUILD SUCCESSFUL**（2026-07-09：首轮曾因在 `Result.onSuccess` 非挂起回调内误用 `withContext` 且漏导入 `Color` 导致 5 个编译错误，已改为协程体内直接取值并补回导入后通过）。音频文件由用户自行打包进 `app/src/main/assets/ambient_sounds/{id}.mp3`。

| 审计条目 | 严重度 | 修复动作 | 文件 |
|---|---|---|---|
| 音频源单点依赖远程 | 【致命】 | 5 分类 × 3 首 = 15 首（2026-07-09 由 30 精简），`audioUrl` 全置 `null` → 强制走本地 `assets` 优先；真实音频由用户放入 APK | `AudioRepository.kt` |
| 毒缓存 | 【致命】 | `prepare()` 改用 `HttpURLConnection`，校验 HTTP 200 + content-type 含 audio + 体积 > 1KB，失败不写盘并删除残片 | `AudioCacheManager.kt` |
| `prepare()` 主线程阻塞 | 【严重】 | `player.play`（含同步 `prepare()`）移入 `withContext(Dispatchers.IO)`，主线程不再冻结 | `AmbientSoundViewModel.kt` |
| 主题被硬编码打破 | 【严重】 | 收藏心形 `Color(0xFFE07070)` → `MaterialTheme.colorScheme.error`（TrackCard + FullscreenPlayer 两处）；TrackCard 改用主题化 `GlassCard` | `AmbientSoundScreen.kt` |
| 同屏双播放器 | 【严重】 | 全局 `AmbientSoundMiniBar` 仅在 `currentRoute != Screen.AmbientSound.route` 时显示 | `DiaryNavHost.kt` |
| 打开 App 自动外放 | 【中等】 | `init` 仅恢复上次曲目元数据并置 `isPlaying=false`，不再自动 `player.play` | `AmbientSoundViewModel.kt` |
| 进度条对循环段无意义 | 【中等】 | 全屏页隐藏「进度」滑块及 `formatDuration`，保留音量 | `AmbientSoundScreen.kt` |
| 睡眠定时死代码 | 【中等】 | 删除从未调用的 `checkAndExpireSleepTimer()` | `AmbientSoundPlayer.kt` |
| 全屏强调色不一致 | 【轻微】 | 全屏页强调色由 `tertiary` 统一为 `primary`，与列表一致；背景色调 `when` 适配新分类 id | `AmbientSoundScreen.kt` |
| 远程图片依赖 | 【严重】 | 分类 `backgroundImageUrl` 全置 `null`，全屏页改走主题化色调背景，去网络依赖 | `AudioRepository.kt` |
| `TrackCard` 死分支 | 【轻微】 | `if (isActive) onSurface else onSurface` → 激活态用 `primary` 强调；播放时 `GlassCard` 抬升阴影 + 浅色叠加 | `AmbientSoundScreen.kt` |
| Service 生命周期线程 | 【严重】 | `onPlaybackChanged` 中 `AmbientSoundService.start/stop` 改由 `Handler(Looper.getMainLooper())` 派发 | `AmbientSoundViewModel.kt` |
| 通知用系统图标 | 【轻微】 | 新增品牌矢量图标 `ic_ambient_play/pause/stop`，smallIcon 复用 `ic_notification`；通知控制不再用系统 drawable | `AmbientSoundService.kt` + `res/drawable/` |
| 双轮询状态拷贝 | 【中等】 | 500ms 进度轮询与 1s 睡眠轮询合并为单一 `startProgressUpdates` 监视循环（含睡眠到期自动停止），删独立 `sleepTimerJob` | `AmbientSoundViewModel.kt` |
| 两套声音系统互不连通 | 【严重】 | 专注模式 `雨声/咖啡厅/白噪音` 经 `soundTrackId` 映射复用场景环境音同一播放器与曲目（`focus_rain`/`write_cafe`/`focus_white`），绑定到专注会话生命周期（开始出声、暂停/停止/退出停止），全程 IO 线程、缺失文件静默降级；计时逻辑不动 | `FocusModeViewModel.kt` |

### 仅剩**未做**（独立大任务，非阻塞）
- **#3 彻底迁移 Media3/ExoPlayer**：当前用 `Dispatchers.IO` 包裹同步 `prepare()` 已消除用户可感知的主线程卡顿；完整迁移为独立的较大重构，留作后续专项，不影响当前可用性。
