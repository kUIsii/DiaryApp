## 1. 构建系统概览
该项目采用标准的 **Android Gradle** 构建体系，基于 **Kotlin DSL** (`build.gradle.kts`) 进行配置。项目使用 **Gradle Wrapper** (版本 8.2) 确保构建环境的一致性，并集成了 **KSP** (Kotlin Symbol Processing) 用于 Room 数据库的代码生成。

## 2. 核心配置文件
- **根目录 `build.gradle.kts`**: 声明了全局插件及其版本，包括 Android Application Plugin (8.2.0)、Kotlin Android (1.9.20) 和 KSP (1.9.20-1.0.14)。
- **`settings.gradle.kts`**: 定义了仓库源（Google, MavenCentral）并包含 `:app` 模块。
- **`app/build.gradle.kts`**: 核心构建逻辑，定义了 SDK 版本（Compile 35, Min 26）、依赖项以及产品风味。
- **`gradle.properties`**: 配置了 JVM 参数、AndroidX 支持及非传递性 R 类优化。

## 3. 版本管理与产品风味
项目采用了**双风味（Flavor）**策略来区分稳定版和实验版：
- **`stable`**: 面向正式发布的版本，应用 ID 为 `com.diary.app`。
- **`experimental`**: 面向测试和新功能验证的版本，应用 ID 为 `com.diary.app.experimental`，版本号后缀带有 `-experimental`。

**版本号规范**：
- `versionCode`: 整数递增，用于内部版本追踪。
- `versionName`: 语义化版本（如 `2.71.23-experimental`），便于用户识别。

## 4. 自动化发布脚本
项目提供了一个 PowerShell 脚本 `scripts/release-experimental.ps1` 用于自动化实验版本的发布流程：
1. **版本自动递增**: 解析当前 `build.gradle.kts` 中的版本号并自动增加 Patch 位或 Version Code。
2. **构建 APK**: 调用 `gradlew.bat :app:assembleExperimentalRelease` 进行混淆打包。
3. **Git 操作**: 自动提交版本变更、创建 Git Tag 并推送到远程仓库。
4. **GitHub Release**: 利用 `gh` CLI 工具创建 GitHub Release 并上传生成的 APK 文件。

## 5. 代码混淆与安全
- **ProGuard/R8**: 在 Release 模式下启用 `isMinifyEnabled` 和 `isShrinkResources`。
- **规则配置**: `app/proguard-rules.pro` 中保留了 Gson 序列化所需的实体类（如 `DiaryBackup` 系列）以及高德地图 SDK 的相关类，防止运行时反射错误。
- **敏感信息**: 通过 `local.properties` 注入 `GITHUB_TOKEN` 和 `AMAP_API_KEY`，避免硬编码在版本控制系统中。

## 6. 开发者指南
- **构建命令**: 使用 `./gradlew assembleDebug` 进行日常开发调试，使用 `./gradlew assembleExperimentalRelease` 打包测试版。
- **密钥管理**: 必须在项目根目录创建 `local.properties` 并填入必要的 API Key 才能成功编译和运行地图相关功能。
- **发布流程**: 严禁手动修改 `versionCode`，应统一通过 `release-experimental.ps1` 脚本执行发布，以确保 Git 标签与构建产物的一致性。