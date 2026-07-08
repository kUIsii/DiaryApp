# Desktop App Redesign Specification

## Design Direction

**Style:** Motion-driven with glassmorphism cards, animated gradient backgrounds, micro-interactions
**References:** Design Spells (micro-animations), Unicorn UI (card layout), Shader Gradient (dynamic gradients)
**No emoji as icons** — use Material Icons (Lucide-equivalent SVG)
**7 themes** — same as mobile (Fog Blue, Moss Green, Ocean Cyan, Petal Rose, Sand Amber, Clay, Ink)

---

## Architecture

### Sidebar — 5 items (no more)

| Icon | Label | Default? | Description |
|------|-------|----------|-------------|
| Edit | 日记 | Yes (default) | Left list + right plain-text editor |
| CheckBox | 待办 | No | Expanded task list with detail panel |
| CalendarMonth | 时间线 | No | Diary timeline + stats summary |
| AutoAwesome | AI 助手 | No | Chat overlay (existing mode, keep) |
| Settings | 设置 | No | Themes, account, sync, AI config |

### Layout

```
┌───────┬────────────────────────────────────────────────────┐
│       │                                                    │
│  S    │  Header: page title + action buttons               │
│  I    │                                                    │
│  D    │  Main content area                                 │
│  E    │  (changes per view)                                │
│  B    │                                                    │
│  A    │                                                    │
│  R    │                                                    │
│       │                                                    │
│       │  Footer: sync status (small, unobtrusive)          │
├───────┴────────────────────────────────────────────────────┤
│  Status bar: word count | connectivity | shortcut hints    │
└────────────────────────────────────────────────────────────┘
```

### Removed
- Dashboard view (metrics, heatmap, smart folders, readiness)
- Kanban board (drag-drop columns)
- Gantt chart
- Task planner modal
- Writing analysis panel
- Standalone sync page (moved to Settings)
- Standalone stats page (moved to Timeline)

---

## Page Designs

### 1. Diary Page (default)

```
┌───────────┬────────────────────────────────────────────────┐
│ Sidebar   │  日记                                           │
│           │                                                │
│           │  2026年7月                                     │
│           │    0712 今天天气不错，出门走了走                 │
│           │    0711 下午写了会代码                           │
│           │    0710 周末研究了下厨艺                         │
│           │                                                │
│           │  2026年6月                                     │
│           │    0628 半年总结                                │
│           │    0620 出差日记                                │
│           │                                                │
│           │                        [新建日记]               │
└───────────┴────────────────────────────────────────────────┘
```

**Left panel:**
- Month headers in muted theme color
- Each entry: date (gray) + first line of text
- Selected entry: subtle background (--accent-bg)
- No borders, no selection bars

**Right panel (editor):**
```
2026年7月12日 星期二

[B] [I] [H] [—]                                   保存  ↓

今天天气不错，出门走了走。

下午写了会代码，把桌面端重新设计了一下。
```

- Pure plain text input (textarea), no Markdown
- Minimal floating toolbar: Bold, Italic, Heading, Divider
- Auto-save on blur
- Character count in footer
- **Micro-interaction:** focus glow on textarea (Design Spells)
- **Background:** Shader Gradient subtle drift (matching current theme)

### 2. Todo Page

```
┌───────────┬────────────────────────────────────────────────┐
│ Sidebar   │  待办                   [新建]  [立即同步]     │
│           │                                                │
│           │  今天                                           │
│           │  □ 提交周报                      !! 高优先级   │
│           │  □ 买牛奶                        到期 今天     │
│           │    └ 记得买全脂的，顺便带点水果                  │
│           │                                                │
│           │  本周                                           │
│           │  □ 给妈妈打电话                  到期 周三     │
│           │  □ 预约牙医                      到期 周五     │
│           │                                                │
│           │  以后                                           │
│           │  □ 整理书架                       无截止日期   │
└───────────┴────────────────────────────────────────────────┘
```

**Features:**
- Three groups: 今天 / 本周 / 以后
- Click title to expand/collapse detail description
- Priority: ! = high (red), !! = urgent (amber), no mark = normal
- Due date shown as relative text
- Checkbox to complete (strikethrough + move to bottom)

**Different from mobile:**
- Desktop shows ALL tasks in one view (mobile is simpler)
- Desktop can expand to full description
- Desktop has quick capture bar at top

### 3. Timeline Page

```
┌───────────┬────────────────────────────────────────────────┐
│ Sidebar   │  时间线                                         │
│           │                                                │
│           │  2026年7月                    < 今天 >          │
│           │                                                │
│           │  7月12日                                       │
│           │  ● 下午写了会代码 [日记]                        │
│           │  ✓ 提交周报 [待办]                              │
│           │                                                │
│           │  7月11日                                       │
│           │  ● 今天没什么特别的 [日记]                      │
│           │                                                │
│           │  ─── 本月写作品质 ───                           │
│           │  写了 8 篇日记 · 平均 142 字/篇                 │
│           │  连续写作 5 天 · 本月累计 1,136 字              │
└───────────┴────────────────────────────────────────────────┘
```

- Mixed feed: diary entries + completed todos
- Bottom summary card: writing stats for current month
- Navigation: prev/next month

### 4. AI Assistant Page

Keep existing chat interface as-is. No changes.

### 5. Settings Page

```
┌───────────┬────────────────────────────────────────────────┐
│ Sidebar   │  设置                                           │
│           │                                                │
│           │  主题                                           │
│           │  [雾蓝] [苔绿] [海潮] [陶粉] [沙金] [陶土] [墨蓝]│
│           │  浅色 / 深色 / 跟随系统                          │
│           │                                                │
│           │  账号                                           │
│           │  手机号: 138****8000                            │
│           │  [修改登录密码]  [退出登录]                      │
│           │                                                │
│           │  同步                                           │
│           │  ● 已同步 · 2分钟前                             │
│           │  [立即同步]                                     │
│           │                                                │
│           │  AI 配置 (保留现有)                              │
│           │  字体大小                                       │
└───────────┴────────────────────────────────────────────────┘
```

---

## UX Guidelines (from Design Spells / Unicorn UI)

1. **Micro-interactions:** 
   - Button press: spring scale 0.97
   - Sidebar item hover: background color transition
   - Page switch: fade 150ms
   - Input focus: subtle glow ring

2. **Card design:**
   - Glass morphism: backdrop-filter blur + semi-transparent bg
   - Rounded corners 12px
   - Subtle border (0.5px solid with low opacity)
   - Hover: slight lift (translateY -1px + shadow)

3. **Gradient background:**
   - Shader Gradient style slow drift (90s cycle, matching current theme)
   - Applies behind all glass cards

4. **Typography (from UI/UX Pro Max):**
   - Headings: system font, semi-bold
   - Body: system font, regular
   - Mono: for code/date stamps
   - Font scaling supported

5. **Sync:**
   - Auto-sync every 2 hours after login
   - Manual sync via one button in Settings
   - Sync status text in settings + small indicator in footer
   - No separate sync page

---

## Color System (per theme)

Refer to existing 7-theme CSS variables already defined in `styles.css`. Each theme has:
- `--bg`, `--bg-surface`, `--bg-subtle`, `--bg-hover`
- `--text-primary`, `--text-secondary`, `--text-tertiary`
- `--border`
- `--accent`, `--accent-light`, `--accent-dark`, `--accent-bg`
- Gradient definitions

No changes needed — the existing CSS theme system is solid.
