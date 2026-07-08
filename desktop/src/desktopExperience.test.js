import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import test from "node:test";

const projectRoot = path.resolve(import.meta.dirname, "..");

function readProjectFile(...parts) {
  return fs.readFileSync(path.join(projectRoot, ...parts), "utf8");
}

test("desktop uses desktop-native layout with sidebar navigation", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  assert.match(html, /class="desktop-shell"/);
  assert.match(html, /class="sidebar"/);
  assert.match(html, /class="main-content"/);
  assert.match(html, /data-view="diary"/);
  assert.match(html, /data-view="todo"/);
  assert.match(html, /data-view="timeline"/);
  assert.match(html, /data-view="chat"/);
  assert.match(html, /data-view="settings"/);
  assert.doesNotMatch(html, /data-view="dashboard"/);
  assert.doesNotMatch(html, /data-view="calendar"/);
  assert.doesNotMatch(html, /data-view="stats"/);
  assert.doesNotMatch(html, /data-view="sync"/);
});

test("desktop opens on diary-first view with nav icons", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const css = readProjectFile("src", "renderer", "styles.css");
  assert.match(html, /class="nav-item active" data-view="diary"/);
  assert.match(html, />日记<\/span>/);
  assert.match(html, />待办<\/span>/);
  assert.match(html, />时间线<\/span>/);
  assert.match(html, />AI 助手<\/span>/);
  assert.match(html, />设置<\/span>/);
  assert.match(html, /<svg class="icon"/);
  assert.match(html, /class="[^"]*view-diary[^"]*active/);
  assert.match(css, /\.nav-item/);
  assert.match(css, /\.nav-item\.active/);
});

test("desktop provides keyboard accessibility with focus-visible outlines", () => {
  const css = readProjectFile("src", "renderer", "styles.css");
  assert.match(css, /:focus-visible/);
  assert.match(css, /outline/);
});

test("desktop todo view has quick capture and three-group classification", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  assert.match(html, /id="todo-capture-input"/);
  assert.match(html, /id="btn-todo-capture"/);
  assert.match(html, /id="todo-today"/);
  assert.match(html, /id="todo-week"/);
  assert.match(html, /id="todo-later"/);
  assert.match(html, />今天<\/div>/);
  assert.match(html, />本周<\/div>/);
  assert.match(html, />以后<\/div>/);
  assert.match(js, /renderTodoPage/);
  assert.match(js, /addTask/);
  assert.match(js, /completeTask/);
});

test("assistant page is chat-based with proactive agent support", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  assert.match(html, /id="chat-messages"/);
  assert.match(html, /id="chat-input"/);
  assert.match(html, /id="chat-send"/);
  assert.match(js, /sendChatMessage/);
  assert.match(js, /appendChatMessage/);
  assert.match(js, /appendProactiveSuggestion/);
  assert.match(js, /appendProactiveSuggestion/);
});

test("AI settings support provider selection with local and API modes", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  assert.match(html, /id="setting-provider"/);
  assert.match(html, /data-provider="local"/);
  assert.match(html, /data-provider="deepseek"/);
  assert.match(html, /data-provider="openai"/);
  assert.match(html, /data-provider="custom"/);
  assert.match(html, /id="setting-api-key"/);
  assert.match(js, /toggleApiFields/);
});

test("desktop data actions are grouped in settings view", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  assert.match(html, /导出数据/);
  assert.match(html, /导入数据/);
  assert.match(html, /id="btn-export"/);
  assert.match(html, /id="btn-import"/);
  assert.match(html, /id="btn-sync-now"/);
  assert.match(html, /id="btn-sync-login"/);
  assert.match(html, /id="btn-sync-logout"/);
});

test("desktop UI uses inline SVG icons instead of emoji glyphs", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  const emojiPattern = /\p{Extended_Pictographic}/u;
  assert.doesNotMatch(html, emojiPattern);
  assert.doesNotMatch(js, emojiPattern);
  assert.match(html, /<svg class="icon"/);
  assert.match(html, /title="立即同步"/);
  assert.match(js, /iconMarkup/);
});

test("desktop user-visible assistant copy does not emit emoji glyphs", () => {
  const ai = readProjectFile("src", "core", "aiProvider.js");
  const emojiPattern = /\p{Extended_Pictographic}/u;
  assert.doesNotMatch(ai, emojiPattern);
});

test("desktop settings expose phone login sync as a settings card", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  assert.match(html, /账号与同步/);
  assert.match(html, /id="settings-phone"/);
  assert.match(html, /id="settings-pin"/);
  assert.match(html, /id="btn-sync-login"/);
  assert.match(html, /id="btn-sync-logout"/);
  assert.match(js, /renderSyncStatus/);
  assert.match(js, /doSync/);
});

test("desktop todo uses three-group classification with expandable descriptions", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  assert.match(html, /class="todo-groups"/);
  assert.match(html, /class="todo-group"/);
  assert.match(html, /class="todo-item"/);
  assert.match(html, /class="todo-checkbox"/);
  assert.doesNotMatch(html, /kanban/);
  assert.doesNotMatch(html, /gantt/);
  assert.doesNotMatch(html, /task-lanes/);
  assert.doesNotMatch(html, /task-planning-board/);
  assert.match(js, /renderTodoPage/);
  assert.match(js, /groups\.today/);
  assert.match(js, /groups\.week/);
  assert.match(js, /groups\.later/);
});

test("desktop includes theme palette with all 7 themes", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  const css = readProjectFile("src", "renderer", "styles.css");
  assert.match(html, /id="theme-palette"/);
  assert.match(html, /data-theme="fog"/);
  assert.match(html, /data-theme="moss"/);
  assert.match(html, /data-theme="ocean"/);
  assert.match(html, /data-theme="petal"/);
  assert.match(html, /data-theme="sand"/);
  assert.match(html, /data-theme="clay"/);
  assert.match(html, /data-theme="ink"/);
  assert.match(js, /applyVisualPreferences/);
  assert.match(css, /body\[data-theme="fog"\]/);
  assert.match(css, /body\[data-theme="moss"\]/);
  assert.match(css, /body\[data-theme="ocean"\]/);
  assert.match(css, /body\[data-theme="petal"\]/);
  assert.match(css, /body\[data-theme="sand"\]/);
  assert.match(css, /body\[data-theme="clay"\]/);
  assert.match(css, /body\[data-theme="ink"\]/);
});

test("desktop theme system includes dark mode and removes violet defaults", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  const css = readProjectFile("src", "renderer", "styles.css");
  assert.match(html, /id="theme-mode-control"/);
  assert.match(html, /data-mode="light"/);
  assert.match(html, /data-mode="dark"/);
  assert.match(html, /data-mode="system"/);
  assert.match(js, /resolveThemeMode/);
  assert.match(js, /matchMedia\(['"]\(prefers-color-scheme: dark\)['"]\)/);
  assert.match(css, /body\[data-mode="dark"\]\[data-theme="fog"\]/);
  assert.match(css, /body\[data-mode="dark"\]\[data-theme="ink"\]/);
  assert.doesNotMatch(css, /124,\s*58,\s*237/);
  assert.doesNotMatch(css, /--accent:\s*#7C3AED/);
  assert.doesNotMatch(css, /--sidebar-bg:\s*#1C1917/);
});

test("desktop uses dedicated timeline page instead of calendar", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  assert.match(html, /data-view="timeline"/);
  assert.match(html, /id="timeline-feed"/);
  assert.match(html, />时间线<\/span>/);
  assert.doesNotMatch(html, /data-view="calendar"/);
  assert.doesNotMatch(html, /data-calendar-mode="month"/);
  assert.doesNotMatch(html, /id="cal-grid"/);
  assert.doesNotMatch(html, /id="view-calendar"/);
  assert.match(js, /renderTimelinePage/);
  assert.match(js, /calcStreak/);
  assert.match(js, /timelineDate/);
});

test("desktop uses shell layout without workbench", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  const css = readProjectFile("src", "renderer", "styles.css");
  assert.match(html, /class="desktop-shell"/);
  assert.match(html, /class="main-content"/);
  assert.doesNotMatch(html, /class="desktop-menubar"/);
  assert.doesNotMatch(html, /class="desktop-toolbar"/);
  assert.doesNotMatch(html, /studio-workspace/);
  assert.doesNotMatch(html, /workspace-panel/);
  assert.doesNotMatch(html, /data-panel="left"/);
  assert.doesNotMatch(html, /data-panel="right"/);
  assert.doesNotMatch(html, /panel-resizer/);
  assert.doesNotMatch(html, /id="btn-toggle-ai-panel"/);
  assert.doesNotMatch(html, /ai-side-panel/);
  assert.match(html, /class="status-bar"/);
  assert.doesNotMatch(js, /bindPanelResizers/);
  assert.doesNotMatch(js, /renderAiSidePanel/);
  assert.match(css, /\.desktop-shell/);
  assert.doesNotMatch(css, /\.studio-workspace/);
  assert.doesNotMatch(css, /\.workspace-panel/);
  assert.doesNotMatch(css, /\.panel-resizer/);
  assert.doesNotMatch(css, /\.ai-side-panel/);
});

test("desktop pages are focused and avoid cluttered layouts", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const css = readProjectFile("src", "renderer", "styles.css");
  assert.doesNotMatch(html, /Task Page|Timeline Page|Insight Page|Planner/);
  assert.doesNotMatch(html, /style="/);
  assert.doesNotMatch(html, /id="btn-open-diary-writer"/);
  assert.doesNotMatch(html, /id="writing-analysis-panel"/);
  assert.doesNotMatch(html, /id="data-import-center"/);
  assert.doesNotMatch(html, /id="monthly-report-panel"/);
  assert.doesNotMatch(html, /id="global-search-overlay"/);
  assert.match(html, /class="[^"]*view-diary[^"]*"/);
  assert.match(html, /class="[^"]*view-todo[^"]*"/);
  assert.match(html, /class="[^"]*view-timeline[^"]*"/);
  assert.match(html, /class="[^"]*view-chat[^"]*"/);
  assert.match(html, /class="[^"]*view-settings[^"]*"/);
  assert.match(css, /\.view\s*\{ display: none; \}/);
  assert.match(css, /\.view\.active/);
});

test("desktop diary uses simple text editor with toolbar", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  const css = readProjectFile("src", "renderer", "styles.css");
  assert.match(html, /id="diary-editor-body"/);
  assert.match(html, /<textarea/);
  assert.match(html, /class="diary-editor-toolbar"/);
  assert.match(html, /data-format="bold"/);
  assert.match(html, /data-format="italic"/);
  assert.match(html, /data-format="heading"/);
  assert.match(html, /id="btn-save-diary"/);
  assert.doesNotMatch(html, /class="markdown-editor"/);
  assert.doesNotMatch(html, /id="markdown-preview"/);
  assert.match(js, /saveDiary/);
  assert.match(js, /renderDiaryEditor/);
  assert.doesNotMatch(js, /showMarkdownEditor/);
  assert.match(css, /#diary-editor-body/);
  assert.match(css, /\.diary-editor-toolbar/);
});

test("desktop task page has no kanban/gantt — just three-group todo list", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  const css = readProjectFile("src", "renderer", "styles.css");
  assert.doesNotMatch(html, /kanban-board/);
  assert.doesNotMatch(html, /task-lanes/);
  assert.doesNotMatch(html, /task-gantt/);
  assert.doesNotMatch(html, /task-bulk-bar/);
  assert.doesNotMatch(js, /renderKanbanBoard/);
  assert.doesNotMatch(js, /renderGantt/);
  assert.doesNotMatch(js, /selectedTaskIds/);
  assert.doesNotMatch(css, /\.kanban-board/);
  assert.doesNotMatch(css, /\.task-gantt/);
  assert.doesNotMatch(css, /\.task-bulk-bar/);
});

test("desktop keeps AI inside its own page and preserves native tray hotkey hooks", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const main = readProjectFile("src", "main.js");
  assert.match(html, /class="[^"]*view-chat[^"]*"/);
  assert.match(html, /id="chat-messages"/);
  assert.doesNotMatch(html, /id="ai-side-messages"/);
  assert.doesNotMatch(html, /id="btn-toggle-ai-panel"/);
  assert.doesNotMatch(html, /ai-side-panel/);
  assert.match(main, /Tray/);
  assert.match(main, /globalShortcut/);
  assert.match(main, /registerGlobalShortcuts/);
  assert.match(main, /createTray/);
});

test("desktop exposes keyboard shortcut hints in status bar", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  assert.match(html, /Ctrl\+K 搜索/);
  assert.match(html, /Ctrl\+N 新建待办/);
  assert.doesNotMatch(html, /id="global-search-overlay"/);
  assert.doesNotMatch(js, /openGlobalSearch/);
});

test("desktop timeline includes stats summary panel", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  const css = readProjectFile("src", "renderer", "styles.css");
  assert.match(html, /id="stat-diaries"/);
  assert.match(html, /id="stat-tasks"/);
  assert.match(html, /id="stat-streak"/);
  assert.match(html, /class="[^"]*timeline-stats[^"]*"/);
  assert.match(html, /篇日记/);
  assert.match(html, /个待办/);
  assert.match(html, /连续天数/);
  assert.doesNotMatch(html, /stats-page/);
  assert.doesNotMatch(html, /stats-trend-chart/);
  assert.match(css, /\.timeline-stats/);
  assert.match(css, /\.stat-value/);
});

test("desktop replaces default selects with designed segmented controls", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const css = readProjectFile("src", "renderer", "styles.css");
  assert.doesNotMatch(html, /<select/);
  assert.match(html, /class="segmented-control"/);
  assert.match(html, /data-mode="light"/);
  assert.match(html, /data-provider="local"/);
  assert.match(css, /\.segmented-control/);
  assert.match(css, /\.theme-swatch/);
});

test("desktop package is configured for win build output", () => {
  const pkg = JSON.parse(readProjectFile("package.json"));
  assert.equal(pkg.build.win.icon, "assets/icon.ico");
  assert.equal(pkg.build.directories.output, "D:/DiaryApp/Desktop/dist");
});

test("desktop style uses design system with rich theme colors and micro-interactions", () => {
  const css = readProjectFile("src", "renderer", "styles.css");
  assert.match(css, /--radius-lg:\s*16px/);
  assert.match(css, /--radius-xl:\s*24px/);
  assert.match(css, /--accent-gradient/);
  assert.match(css, /--sidebar-bg:\s*color-mix/);
  assert.match(css, /--font-heading/);
  assert.match(css, /--font-body/);
  assert.match(css, /@keyframes fadeInUp/);
  assert.match(css, /@keyframes slideUp/);
  assert.match(css, /@keyframes scaleIn/);
  assert.match(css, /\.nav-item/);
  assert.match(css, /\.chat-bubble/);
  assert.match(css, /\.todo-item/);
  assert.match(css, /\.diary-entry-item/);
  assert.match(css, /\.modal-overlay/);
  assert.match(css, /\.loading-dots/);
  assert.match(css, /\.toast/);
  assert.match(css, /\.icon/);
  assert.match(css, /\.glass-card/);
  assert.match(css, /\.segmented-control/);
  assert.match(css, /\.settings-card/);
  assert.match(css, /prefers-reduced-motion/);
});

test("aiProvider has local rule-based agent mode", () => {
  const ai = readProjectFile("src", "core", "aiProvider.js");
  assert.match(ai, /processLocally/);
  assert.match(ai, /classifyIntent/);
  assert.match(ai, /getProactiveAgentMessage/);
  assert.match(ai, /getDailyBriefingForProactive/);
  assert.match(ai, /analyzeSentiment/);
  assert.match(ai, /INTENT_PATTERNS/);
});

test("desktopService supports diary update", () => {
  const svc = readProjectFile("src", "core", "desktopService.js");
  assert.match(svc, /updateDiaryEntry/);
});

test("desktop has no context menu, no template rail, no attachments UI", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  assert.doesNotMatch(html, /id="desktop-context-menu"/);
  assert.doesNotMatch(html, /diary-template-rail/);
  assert.doesNotMatch(html, /attachment-drop-zone/);
  assert.doesNotMatch(html, /smart-folders/);
  assert.doesNotMatch(js, /bindContextMenu/);
  assert.doesNotMatch(js, /showDesktopContextMenu/);
  assert.doesNotMatch(js, /insertDiaryTemplate/);
  assert.doesNotMatch(js, /bindAttachmentDropZone/);
});

test("desktop settings has no shortcut profiles UI", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  assert.doesNotMatch(html, /id="shortcut-settings"/);
  assert.doesNotMatch(html, /shortcut-profile/);
  assert.doesNotMatch(js, /renderShortcutSettings/);
  assert.doesNotMatch(js, /shortcutProfile/);
});

test("desktop settings has no native desktop capability shortcuts UI", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  assert.doesNotMatch(html, /desktop-native-settings/);
  assert.doesNotMatch(html, /native-status-grid/);
  assert.doesNotMatch(js, /renderNativeStatus/);
});

test("desktop writing page has no analysis panel or AI actions", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  assert.doesNotMatch(html, /writing-analysis-panel/);
  assert.doesNotMatch(html, /writing-focus-meter/);
  assert.doesNotMatch(html, /ai-context-actions/);
  assert.doesNotMatch(js, /buildWritingAnalysis/);
  assert.doesNotMatch(js, /renderWritingAnalysis/);
  assert.doesNotMatch(js, /handleAiContextAction/);
});

test("desktop settings supports export and desktop Notification", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const main = readProjectFile("src", "main.js");
  const preload = readProjectFile("src", "preload.cjs");
  assert.match(html, /id="btn-export"/);
  assert.match(html, /id="btn-import"/);
  assert.match(html, /导出数据/);
  assert.match(html, /导入数据/);
  assert.match(preload, /exportData/);
  assert.match(main, /desktop:export-file/);
  assert.match(main, /Notification/);
});

test("desktop main.js handles import with multi-format support", () => {
  const main = readProjectFile("src", "main.js");
  const svc = readProjectFile("src", "core", "desktopService.js");
  assert.match(main, /desktop:import-file/);
  assert.match(main, /day-one/);
  assert.match(main, /bear/);
  assert.match(svc, /importMarkdownArchive/);
  assert.match(svc, /importExternalDiaryArchive/);
});

test("desktopService supports attachment persistence", () => {
  const svc = readProjectFile("src", "core", "desktopService.js");
  const main = readProjectFile("src", "main.js");
  const preload = readProjectFile("src", "preload.cjs");
  assert.match(preload, /attachFilesToDiary: \(diaryId, files\)/);
  assert.match(main, /desktop:attach-files/);
  assert.match(main, /attachFilesToDiary/);
  assert.match(svc, /attachFilesToDiary/);
  assert.match(svc, /storageMode: "linked"/);
});

test("desktop has no import center or monthly report panel", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  assert.doesNotMatch(html, /id="data-import-center"/);
  assert.doesNotMatch(html, /id="monthly-report-panel"/);
  assert.doesNotMatch(html, /import-format-control/);
  assert.doesNotMatch(js, /monthly-report-panel/);
  assert.doesNotMatch(js, /renderMonthlyReportPanel/);
  assert.doesNotMatch(js, /buildMonthlyReport/);
});
