该项目采用标准的 Android Gradle 构建系统来管理依赖和构建流程。

### 1. 构建系统与版本
- **构建工具**: Gradle 8.2 (通过 `gradle/wrapper/gradle-wrapper.properties` 指定)。
- **Android Gradle Plugin (AGP)**: 8.2.0 (在根目录 `build.gradle.kts` 中声明)。
- **Kotlin**: 1.9.20 (与 AGP 和 Compose Compiler 兼容)。
- **KSP (Kotlin Symbol Processing)**: 1.9.20-1.0.14 (用于 Room 等注解处理)。

### 2. 依赖声明与管理
- **集中式版本管理**: 根目录 `build.gradle.kts` 使用 `plugins` 块集中管理插件版本，避免子模块版本冲突。
- **BOM (Bill of Materials)**: 在 `app/build.gradle.kts` 中使用 `androidx.compose:compose-bom:2023.10.01` 来统一管理 Jetpack Compose 相关库的版本，确保兼容性。
- **仓库配置**: `settings.gradle.kts` 中配置了 `google()`、`mavenCentral()` 和 `gradlePluginPortal()` 作为依赖来源。未使用私有仓库或本地缓存代理。
- **主要第三方库**:
  - **数据库**: Room 2.6.1 (配合 KSP)。
  - **网络/JSON**: Gson 2.10.1。
  - **图片加载**: Coil 2.5.0。
  - **地图服务**: 高德地图 SDK (`com.amap.api:3dmap:10.0.600`)。
  - **其他**: WorkManager, Biometric, Health Connect, WebKit。

### 3. 构建变体与配置
- **产品风味 (Product Flavors)**: 定义了 `stable` (稳定版) 和 `experimental` (实验版) 两个风味，拥有不同的 `applicationId` 和版本号策略，便于并行开发和测试。
- **敏感信息管理**: 通过 `local.properties` 文件注入 `GITHUB_TOKEN` 和 `AMAP_API_KEY` 到 `BuildConfig` 和 `manifestPlaceholders` 中，避免硬编码敏感信息。
- **代码混淆**: Release 构建启用 `minifyEnabled` 和 `shrinkResources`，并使用 `proguard-rules.pro` 针对 Gson 数据模型和高德地图 SDK 进行了专门的混淆规则配置。

### 4. 开发者规范
- **依赖添加**: 新依赖应优先检查是否有对应的 BOM 管理（如 Compose），若无则需在 `app/build.gradle.kts` 中明确指定版本。
- **密钥管理**: 严禁将 API Key 或 Token 硬编码在源码中，必须通过 `local.properties` 配置并在 `.gitignore` 中排除该文件。
- **版本对齐**: 修改 Kotlin 或 AGP 版本时，需同步检查 KSP 和 Compose Compiler 的兼容性矩阵。