# DiaryApp 项目笔记

## 桌面版构建
```bash
cd C:\Users\陈仕杰\Desktop\DiaryApp\desktop
npm run dist
```
输出到 `D:\DiaryApp\Desktop\dist\`，桌面快捷方式 `DiaryApp Desktop.lnk` 指向该目录。

## Android 构建
```bash
cd C:\Users\陈仕杰\Desktop\DiaryApp
.\gradlew.bat :app:assembleExperimentalDebug
```

## 测试
```bash
.\gradlew.bat :app:testExperimentalDebugUnitTest
```

## 设计资源
- https://designspells.com — 微交互动画参考
- https://unicornui.com — UI Kit 布局参考
- https://shadergradient.co — 动态渐变背景

## 登录 & 同步
- APP 启动先进登录页（手机号+PIN）
- 自动每2小时同步待办到云端
- Workers 后端: https://diary-app-sync.workers.dev
