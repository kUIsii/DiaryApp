# v2.74.01-experimental

修复：product flavor 版本号覆盖导致更新提示重复出现的问题。

- 移除 experimental flavor 中过期的手动 versionCode/versionName 配置
- 现统一继承 defaultConfig 的版本号，确保 API 版本检测与实际 APK 版本一致
