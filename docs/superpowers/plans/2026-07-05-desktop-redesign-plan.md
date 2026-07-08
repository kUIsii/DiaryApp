# Desktop App Redesign Implementation Plan

> **For agentic workers:** Use direct execution with verification after each task.

**Goal:** Redesign desktop app to focus on Diary/Todo/Timeline/AI/Settings, remove dashboard/Kanban/Gantt, apply Design Spells + Unicorn UI + Shader Gradient styling

**Architecture:** Three files to modify: `index.html` (structure), `styles.css` (styling), `renderer.js` (logic). One file to tweak: `main.js` (remove tray items).

**Tech Stack:** Electron, vanilla JS, CSS custom properties

---

## File Changes

| File | Action | Lines changed |
|------|--------|---------------|
| `desktop/src/renderer/index.html` | Rewrite | ~200 → ~150 lines |
| `desktop/src/renderer/styles.css` | Rewrite | 3356 → ~1500 lines |
| `desktop/src/renderer/renderer.js` | Rewrite | 1793 → ~800 lines |
| `desktop/src/main.js` | Modify | Remove tray items, keep sync |

---

## Task 1: Rewrite index.html

**Goal:** Minimal HTML with new sidebar (5 items), view containers, no dashboard/Kanban/Gantt elements.

Key structure:
```
desktop-shell (CSS grid)
├── desktop-sidebar
│   ├── logo
│   ├── nav items (日记/待办/时间线/AI助手/设置)
│   └── sync status indicator
├── desktop-main
│   ├── view-header (title + actions)
│   ├── view-diary
│   │   ├── diary-list (left)
│   │   └── diary-editor (right)
│   ├── view-todo
│   │   ├── quick-capture
│   │   └── task-groups (today/week/later)
│   ├── view-timeline
│   │   ├── month-nav
│   │   ├── feed
│   │   └── stats-summary
│   ├── view-chat (keep existing)
│   └── view-settings
│       ├── theme
│       ├── account
│       ├── sync
│       └── ai-config
└── status-bar
```

## Task 2: Rewrite styles.css

**Goal:** Keep existing 7-theme CSS variable system, remove unused component styles, add glass cards + micro-interactions.

Keep from existing:
- Theme variables (7 themes, light/dark)
- Typography (Inter + Playfair Display)
- Sidebar layout
- Buttons (primary, secondary, ghost)
- Scrollbar styling
- Status bar

Remove:
- Dashboard styles
- Kanban board styles
- Gantt chart styles
- Task planner modal styles
- Writing analysis panel styles
- Timeline heatmap styles
- Diary writer layout variants (split/focus/analysis)
- Template chips
- Attachment drop zone

Add:
- Glass card: `background: var(--bg-surface); backdrop-filter: blur(12px); border: 0.5px solid var(--border); border-radius: 12px;`
- Diary editor: full-height textarea, minimal toolbar
- Todo groups: three sections with clear visual separation
- Micro-interactions: hover lift, focus glow, button spring (via CSS transforms + transitions)

## Task 3: Rewrite renderer.js

**Keep from existing:**
- `renderChatPage()` — AI chat stays as-is
- `renderSettings()` — keep theme/sync/AI config, add account section
- `bindEvents()` — keep sidebar nav, chat, settings events
- `renderSyncStatus()` — keep sync status display
- State management (state, refreshState, mutate)
- API communication (window.diaryDesktop)
- Markdown-to-HTML converter (for diary preview in list)

**Remove:**
- `renderDashboardPage()` and all sub-renderers
- `renderTaskPage()` kanban/gantt/planner/lanes
- `renderDiaries()` — replace with simplified version
- `renderStats()` — move monthly summary to timeline
- `renderTimelinePage()` — simplify (remove heatmap/milestones)
- `renderWritingAnalysis()`
- All event handlers for removed components

**Simplify:**
- `renderDiaryPage()` — new: left list + right editor
- `renderTodoPage()` — new: three groups with expandable descriptions
- `renderTimelinePage()` — new: diary feed + bottom stats
- `renderSettingsPage()` — add account section, sync button

## Task 4: Modify main.js

- Remove `createTray()` items for removed views
- Keep auto-sync interval
- Keep all IPC handlers for sync/account
- Remove IPC handlers for removed features (kanban/gantt)

## Task 5: Build & Verify

- Run: `cd desktop && npm test`
- Run: `cd desktop && npm run smoke` (if available)
- Manual: verify main window loads without errors
