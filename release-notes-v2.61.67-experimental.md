## 优化

- 稳定图片链路：统一 `diary_media` 路径转换为 `https://appassets/diary_media/...`，减少编辑器、详情页、图片查看器之间的路径不一致问题。
- 详情页支持点击正文图片进入全屏查看器，并保留左右切换的图片列表。
- 首页增加搜索入口、本周摘要和“历史今天”；搜索时会直接展示结果，避免被日历和摘要卡片压到下面。
- “历史今天”没有内容时不再显示空卡片，保持首页首屏简练。
- 时间线改为更轻的日期锚点和月份摘要，减少厚重竖线和卡片套卡片的视觉负担。
- 待办页增加备忘/待办快速输入；待办支持今天、明天快速选择，并按过期、今天、明天、之后、已完成分组。

## 验证

- `:app:compileExperimentalDebugKotlin` 通过。
- `:app:assembleExperimentalRelease` 通过。
- 单元测试任务仍受本机 Gradle Test Executor 环境异常影响，失败点为 `worker.org.gradle.process.internal.worker.GradleWorkerMain` 找不到，不是业务断言失败。
