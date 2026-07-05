# Login / Account 功能 & UI 拓展设计方案

## 参考资源
- [Design Spells](https://designspells.com) — 微交互动画灵感
- [Unicorn UI](https://unicornui.com) — UI Kit 布局参考
- [Shader Gradient](https://shadergradient.co) — 动态渐变背景

---

## 一、登录页面 UI 升级

### 当前状态
登录页面（LoginScreen.kt）是一个简单的 GradientBackground + 两个 OutlinedTextField + 一个 Button，功能完整但视觉平淡。

### 升级方案

**1. 登录页适配 7 主题家族**
每个主题的登录页有细微差异化，保持整体统一：
- Background：使用各主题的 GradientBackground（已有），叠加 Shader Gradient 风格的动态微渐变
- 输入框：聚焦时边框色 = 主题 accent 色，带微光晕动画（借鉴 Design Spells）
- 按钮：注册/登录按钮背景使用主题 gradient（从 primary → tertiary），按压时 spring 缩放（借鉴 GlassCard）
- PIN 输入：增加自定义数字键盘（Unicorn UI 风格的圆润大按键），替代系统键盘

**2. 登录过渡动画**
- 页面进入时：三个元素（标题、输入区、按钮）错开 fadeIn + slideUp，每个间隔 100ms
- 注册成功 → 进入主页：当前内容 dissolve 过渡到主界面
- 切换注册/登录模式：文字和按钮平滑 crossfade

**3. 可选：登录页品牌质感**
- 顶部 App 名称下方加一行极淡的装饰线 / 几何装饰（各主题不同图案）
- 底部显示版本号 + 小字"数据仅存于本地及你的云账户"

---

## 二、账号管理功能（新增 ProfileScreen）

### 当前状态
暂无账号管理界面，修改 PIN、查看同步状态等功能缺失。

### 新页面：个人中心

**入口**：设置页顶部新增「个人中心」卡片

**页面布局**（从上到下）：

```
┌─────────────────────────┐
│  [圆形头像]  昵称        │
│  手机号: 138****8000     │
│  🟢 已同步 2分钟前       │
├─────────────────────────┤
│  ┌─────────────────────┐│
│  │ 🔒 修改 PIN         ││
│  ├─────────────────────┤│
│  │ 📱 已登录设备 (2)   ││
│  ├─────────────────────┤│
│  │ ☁️ 云端备份状态     ││
│  ├─────────────────────┤│
│  │ 📤 导出全部数据     ││
│  ├─────────────────────┤│
│  │ 🚪 注销账号         ││
│  └─────────────────────┘│
└─────────────────────────┘
```

### 各功能详情

**1. 修改 PIN**
- 当前流程：输入旧 PIN → 输入新 PIN（2次）→ 确认
- 本地：更新 SharedPreferences 中的 PIN hash
- 云端：同步更新 Worker 中的用户凭证（需用旧 token 认证）

**2. 已登录设备管理**
- 后端新增 API：`GET /api/devices` + `DELETE /api/devices/{id}`
- Worker 在注册/登录时记录设备信息到 KV：`devices:{phone}` → `[{id, name, lastSeen, type}]`
- 列表展示：手机（Android）、桌面（Windows）
- 可点击「远程注销」删除设备 token

**3. 云端备份状态**
- 显示：数据量大小、最后同步时间、自动同步间隔（2h）
- 按钮：「立即同步」手动触发
- 加载状态：同步中显示进度指示

**4. 导出全部数据**
- 格式选择：JSON / Markdown / TXT
- 导出内容：日记 + 待办 + 标签
- 通过 Android Share Sheet 分享或保存到文件

**5. 注销账号**
- 确认弹窗（防止误触）
- 本地清除所有数据（日记、待办、设置保留可选项）
- 云端删除 token（API 调用）

---

## 三、同步状态指示器

### 全局同步状态栏
在以下位置显示同步状态：

| 位置 | 状态显示 | 点击行为 |
|------|----------|----------|
| 设置页顶部 | 🟢 已同步 / 🟡 同步中 / 🔴 未登录 | 进入个人中心 |
| 首页右上角 | 小型 icon（同色系圆点） | 快速同步 |
| 桌面端状态栏 | "最后同步: 2分钟前" | 手动同步 |

### 状态计算逻辑
```
isLoggedIn && lastSyncAt > 2h ago → 🟢 已同步
isLoggedIn && syncing → 🟡 同步中
!isLoggedIn → 🔴 未登录
isLoggedIn && lastSyncAt > 24h → 🟡 建议同步
```

---

## 四、云端功能拓展

### 现有 Worker API
- `POST /api/register` — 注册
- `POST /api/login` — 登录
- `GET/POST /api/backup` — 备份

### 新增 API

**设备管理**
```
GET /api/devices
  → 200 { devices: [{ id, name, type, lastSeen }] }
DELETE /api/devices/:id
  → 200 { message: "Device logged out" }
```

**账号操作**
```
PUT /api/pin
  → Body: { oldPin, newPin }
  → 200 { message: "PIN updated" }
DELETE /api/account
  → 200 { message: "Account deleted" }
```

**同步增强**
```
POST /api/sync
  → Body: { data: { todos, diaries } }
  → 200 { message, version }
  → 409 { conflict: true, serverVersion, localVersion }
```
返回冲突标记，客户端决定覆盖或合并。

---

## 五、UI 设计细节

### 颜色与主题
每个功能卡片使用主题色系：

| 主题 | 卡片强调色 | 装饰元素 |
|------|-----------|---------|
| BLUE | Blue-400 | 水平细线纹理 |
| GREEN | Green-400 | 圆点纹理 |
| CYAN | Cyan-300 | 波浪曲线 |
| ROSE | Rose-300 | 椭圆环 |
| AMBER | Amber-300 | 颗粒噪点 |
| CLAY | Warm-400 | 交叉斜线 |
| INK | Slate-300 | 网格点阵 |

### 微交互（Design Spells 风格）
- 卡片列表项：按压下沉 0.97x + 背景色微变
- 开关/按钮：spring 弹性动画（stiffness=300, damping=20）
- 页面切换：内容 crossfade 200ms
- 同步状态变化：圆点 pulse 动画（同步中闪烁）
- 数字 PIN 键盘：按键 press 反馈（缩放 + 颜色加深 + haptic）

### 布局（Unicorn UI 风格）
- 卡片圆角 16dp，轻微阴影（ambient=4dp, key=8dp）
- 列表项之间 8dp 间距
- 图标使用 Material Icons Outlined 风格（或者 Lucide 风格 SVG）
- 头像圆形占位区 64dp，首字母或默认图标

---

## 六、开发优先级

| 优先级 | 功能 | 预估工作量 |
|--------|------|-----------|
| P0 | 登录页 UI 升级 + 主题适配 | 1个文件修改 |
| P0 | 个人中心页面框架 + 入口 | 1-2个新文件 |
| P1 | 修改 PIN（本地） | AuthManager 扩展 |
| P1 | 同步状态指示器 | 2-3个文件修改 |
| P2 | 设备管理（Worker API + App UI） | 3-4个文件 |
| P2 | 数据导出 | 1-2个文件 |
| P3 | 云端冲突同步 | Worker + App |

---

## 七、桌面端互通对齐

桌面端（Electron）同步已有的功能：
- 个人中心页面对齐：相同的卡片布局、功能入口
- 设置页已有「互通同步」面板，可以扩展成完整的账号管理
- 状态栏显示同步状态、最后同步时间
- 自动同步 2h 定时器已在 main.js 添加

---

## 文件影响范围

| 文件 | 变更 |
|------|------|
| `app/.../ui/login/LoginScreen.kt` | 重写 UI + 动画 + 主题适配 |
| `app/.../ui/profile/ProfileScreen.kt` | **新建** — 个人中心 |
| `app/.../ui/profile/AccountSettingsScreen.kt` | **新建** — 修改PIN/设备管理 |
| `app/.../ui/settings/SettingsScreen.kt` | 添加个人中心入口 |
| `app/.../data/auth/AuthManager.kt` | 新增 changePin(), deleteAccount() |
| `app/.../data/sync/CloudSyncManager.kt` | 新增设备管理 API |
| `workers/src/index.js` | 新增设备管理 / 改PIN 接口 |
| `desktop/src/renderer/renderer.js` | 同步状态指示器 |
| `desktop/src/renderer/index.html` | 个人中心 UI |
