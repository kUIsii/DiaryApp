## Experimental update
- 重构日记编辑器的滚动与光标可见逻辑，减少连续输入、有序列表换行、点回编辑器时的上跳与跑位
- 增加“专注书写 / 完整编辑”切换，写字时可以收起大部分编辑功能，同时保留字号调节
- 调整记录页标签布局为“心情 / 天气 / 分类”第一行，“位置”第二行居中显示
- 修复无序列表圆点与复选框位置偏移问题，让它们和有序列表的文字基线更一致
- 修复插入图片后只显示小图标或占位的问题，并优化插图后继续输入时的稳定性

## Verification
- `:app:compileExperimentalDebugKotlin` passed
- `:app:testExperimentalDebugUnitTest --tests com.diary.app.ui.editor.EditorUtilsTest` could not complete on this machine because the existing Gradle worker bootstrap issue under Java 21 is still present

## Known issue
- 本机在 Java 21 下运行部分 Gradle 单测任务仍可能出现 `worker.org.gradle.process.internal.worker.GradleWorkerMain` 启动失败
- 当前已确认编译通过并可继续生成 experimental release APK
