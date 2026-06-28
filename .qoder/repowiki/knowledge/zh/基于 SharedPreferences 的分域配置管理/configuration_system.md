## 1. 系统概述
该应用采用 **Android SharedPreferences** 作为核心的运行时配置存储方案。配置逻辑并未集中在全局单例中，而是根据业务领域（Domain）分散在多个 `object` 或管理类中。这种设计实现了配置的模块化隔离，不同功能模块（如 AI、通知、主题、实验性功能）拥有独立的配置键值空间和管理接口。

## 2. 核心架构与约定
### 2.1 分域管理模式 (Domain-Specific Managers)
配置管理遵循“谁使用，谁管理”的原则，每个功能模块提供专用的配置管理器：
- **AI 配置**: `AiConfigStore` 管理 API Key、Endpoint、Model 及 Provider 切换。支持多 Provider 配置及从旧版单 Key 的自动迁移。
- **通知配置**: `NotificationPreferencesManager` 管理每日提醒、成就通知、天气预警开关及免打扰时段（Quiet Hours）。
- **主题配置**: `ThemePreferences` 管理应用的主题模式（ThemeMode）。
- **实验性功能**: `ExperimentalFeaturesPreferences` 以 Feature Flag 的形式管理尚未全量开放的功能（如主屏滑动、AI 传记等）。
- **安全配置**: `BiometricHelper` 管理生物识别锁、PIN 码哈希及盐值、失败尝试次数等敏感配置。
- **备份配置**: `BackupManager` 管理自动备份开关、频率、历史记录及最大备份数。

### 2.2 存储实现细节
- **持久化层**: 统一使用 `Context.getSharedPreferences()`。
- **命名约定**: 
  - 通用配置通常使用 `"diary_prefs"`。
  - 特定领域使用独立名称，如 `"notification_preferences"`、`"diary_backup_prefs"`、`"ai_cache"`。
- **异步写入**: 所有写操作均使用 `.apply()` 进行异步提交，避免阻塞主线程。
- **默认值处理**: 读取时提供合理的默认值（如 `false`、`0` 或枚举的默认项），确保应用在首次安装或配置缺失时能正常运行。

### 2.3 状态封装
- **数据类映射**: 复杂配置（如实验性功能）通过 `ExperimentalFeaturesState` 数据类一次性加载，便于在 UI 层（Compose）进行状态传递和比对。
- **逻辑内聚**: 配置管理器不仅负责存取，还包含相关的业务逻辑。例如 `NotificationPreferencesManager` 包含 `isInQuietHours()` 判断逻辑，`BiometricHelper` 包含 PIN 码验证及指数退避锁定逻辑。

## 3. 关键文件清单
| 文件路径 | 职责描述 |
| :--- | :--- |
| `app/src/main/java/com/diary/app/ai/AiConfigStore.kt` | AI 服务配置中心，支持多 Provider 及 Legacy 迁移 |
| `app/src/main/java/com/diary/app/reminder/NotificationPreferencesManager.kt` | 通知偏好设置，包括免打扰时段计算 |
| `app/src/main/java/com/diary/app/ui/theme/ThemePreferences.kt` | 主题模式持久化 |
| `app/src/main/java/com/diary/app/ui/experimental/ExperimentalFeaturesPreferences.kt` | 实验性功能开关管理 |
| `app/src/main/java/com/diary/app/biometric/BiometricHelper.kt` | 安全锁配置及 PIN 码验证逻辑 |
| `app/src/main/java/com/diary/app/data/BackupManager.kt` | 备份策略配置及历史记录管理 |

## 4. 开发者指南
1. **新增配置**: 若为新功能添加配置，建议在对应模块下创建或扩展专用的 `PreferencesManager` / `ConfigStore`，避免直接在全局 `diary_prefs` 中随意添加键值。
2. **键值命名**: 使用清晰的常量定义 Key（如 `KEY_AI_ENABLED`），并加上模块前缀以防冲突。
3. **迁移兼容**: 若修改配置结构（如 `AiConfigStore` 中的 Provider 迁移），需在读取逻辑中保留对旧 Key 的兼容处理。
4. **UI 集成**: 在 Jetpack Compose 中，建议将配置读取封装在 `ViewModel` 或 `StateHolder` 中，利用 `SnapshotFlow` 或手动触发重绘来响应配置变更。