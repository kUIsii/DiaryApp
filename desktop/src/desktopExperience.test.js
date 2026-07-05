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
  assert.match(html, /class="[^"]*app-shell/);
  assert.match(html, /class="sidebar"/);
  assert.match(html, /class="main-content"/);
  assert.match(html, /data-view="dashboard"/);
  assert.match(html, /data-view="tasks"/);
  assert.match(html, /data-view="diary"/);
  assert.match(html, /data-view="chat"/);
  assert.match(html, /data-view="timeline"/);
  assert.doesNotMatch(html, /data-view="calendar"/);
  assert.doesNotMatch(html, /id="nav-calendar"/);
  assert.match(html, /data-view="stats"/);
  assert.match(html, /data-view="settings"/);
});

test("desktop opens on a visual dashboard with smart folders and page AI", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  const css = readProjectFile("src", "renderer", "styles.css");
  assert.match(html, /id="nav-dashboard"/);
  assert.match(html, /id="view-dashboard"/);
  assert.match(html, /class="[^"]*dashboard-page[^"]*"/);
  assert.match(html, /id="dashboard-trend-svg"/);
  assert.match(html, /id="dashboard-heatmap"/);
  assert.match(html, /class="[^"]*dashboard-legend[^"]*"/);
  assert.match(html, /id="dashboard-smart-folders"/);
  assert.match(html, /id="desktop-readiness-list"/);
  assert.match(html, /id="dashboard-ai-rail"/);
  assert.match(html, /data-smart-folder-query="status:overdue"/);
  assert.match(html, /data-smart-folder-query="date:today"/);
  assert.match(html, /data-smart-folder-query="tags:missing"/);
  assert.match(js, /renderDashboardPage/);
  assert.match(js, /renderDashboardMetrics/);
  assert.match(js, /renderSmartFolders/);
  assert.match(js, /applySmartFolder/);
  assert.match(js, /renderDesktopReadiness/);
  assert.match(js, /renderPageAiRail/);
  assert.match(css, /\.dashboard-page/);
  assert.match(css, /\.dashboard-card/);
  assert.match(css, /\.dashboard-legend/);
  assert.match(css, /\.smart-folder-list/);
  assert.match(css, /\.smart-folder-query/);
  assert.match(css, /\.desktop-readiness-list/);
  assert.match(css, /\.page-ai-rail/);
});

test("desktop shell supports keyboard skip navigation", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const css = readProjectFile("src", "renderer", "styles.css");
  assert.match(html, /class="skip-link"/);
  assert.match(html, /href="#main-content"/);
  assert.match(html, /id="main-content"/);
  assert.match(html, /tabindex="-1"/);
  assert.match(css, /\.skip-link/);
  assert.match(css, /\.skip-link:focus-visible/);
});

test("desktop home exposes quick capture and task/diary actions", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  assert.match(html, /class="[^"]*quick-capture[^"]*"/);
  assert.match(html, /写日记/);
  assert.match(html, /id="task-capture-input"/);
  assert.match(js, /captureTask/);
  assert.match(js, /renderTasks/);
  assert.match(js, /renderDiaries/);
  assert.match(js, /renderStats/);
});

test("assistant page is chat-only with proactive agent support", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  assert.match(html, /id="chat-messages"/);
  assert.match(html, /id="chat-input"/);
  assert.match(js, /sendChatMessage/);
  assert.match(js, /checkProactiveAgent/);
  assert.match(js, /appendChatMessage/);
});

test("AI settings support both local and API modes", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  assert.match(html, /本地助手（无需配置）/);
  assert.match(html, /DeepSeek/);
  assert.match(html, /OpenAI/);
  assert.match(html, /id="setting-provider"/);
  assert.match(html, /id="setting-api-key"/);
  assert.match(js, /toggleApiFields/);
});

test("desktop utility actions are grouped in settings view", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  assert.match(html, /导出数据/);
  assert.match(html, /导入数据/);
  assert.match(html, /互通同步/);
  assert.match(html, /id="btn-export"/);
  assert.match(html, /id="btn-import"/);
  assert.match(html, /id="btn-force-sync"/);
});

test("desktop UI uses inline SVG icons instead of emoji glyphs", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  const emojiPattern = /\p{Extended_Pictographic}/u;
  assert.doesNotMatch(html, emojiPattern);
  assert.doesNotMatch(js, emojiPattern);
  assert.match(html, /class="icon icon-chat"/);
  assert.match(html, /aria-label="立即同步"/);
  assert.match(js, /iconMarkup/);
});

test("desktop user-visible assistant copy does not emit emoji glyphs", () => {
  const ai = readProjectFile("src", "core", "aiProvider.js");
  const emojiPattern = /\p{Extended_Pictographic}/u;
  assert.doesNotMatch(ai, emojiPattern);
});

test("desktop settings exposes phone login sync as a focused subpage", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  assert.match(html, /data-view="sync"/);
  assert.match(html, /id="nav-sync"/);
  assert.match(html, /id="view-sync"/);
  assert.match(html, /id="sync-phone"/);
  assert.match(html, /id="sync-pin"/);
  assert.match(html, /id="btn-sync-login"/);
  assert.match(html, /id="btn-sync-register"/);
  assert.match(js, /renderSyncAccount/);
  assert.match(js, /bindSyncAccountEvents/);
});

test("desktop exposes a focused task page beyond a simple list", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  assert.match(html, /class="task-page"/);
  assert.match(html, /id="task-focus-strip"/);
  assert.match(html, /id="task-lanes"/);
  assert.match(html, /data-lane="inbox"/);
  assert.match(html, /data-lane="today"/);
  assert.match(html, /data-lane="planned"/);
  assert.match(html, /id="btn-open-task-planner"/);
  assert.match(js, /renderTaskPage/);
  assert.match(js, /showTaskPlanner/);
  assert.match(js, /buildTaskInsights/);
});

test("desktop includes app-aligned theme and type scale controls", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  const css = readProjectFile("src", "renderer", "styles.css");
  assert.match(html, /id="theme-palette"/);
  assert.match(html, /data-theme-choice="moss"/);
  assert.match(html, /data-theme-choice="ocean"/);
  assert.match(html, /data-theme-choice="petal"/);
  assert.match(html, /data-theme-choice="sand"/);
  assert.match(html, /data-theme-choice="clay"/);
  assert.match(html, /data-theme-choice="ink"/);
  assert.match(html, /id="font-scale-control"/);
  assert.match(js, /applyVisualPreferences/);
  assert.match(js, /bindThemeControls/);
  assert.match(css, /body\[data-theme="moss"\]/);
  assert.match(css, /body\[data-theme="ocean"\]/);
  assert.match(css, /--font-scale/);
});

test("desktop theme system includes dark mode and removes violet defaults", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  const css = readProjectFile("src", "renderer", "styles.css");
  assert.match(html, /id="theme-mode-control"/);
  assert.match(html, /data-theme-mode="light"/);
  assert.match(html, /data-theme-mode="dark"/);
  assert.match(html, /data-theme-mode="system"/);
  assert.match(js, /resolveThemeMode/);
  assert.match(js, /matchMedia\("\(prefers-color-scheme: dark\)"\)/);
  assert.match(css, /body\[data-mode="dark"\]\[data-theme="fog"\]/);
  assert.match(css, /body\[data-mode="dark"\]\[data-theme="ink"\]/);
  assert.doesNotMatch(css, /124,\s*58,\s*237/);
  assert.doesNotMatch(css, /--accent:\s*#7C3AED/);
  assert.doesNotMatch(css, /--sidebar-bg:\s*#1C1917/);
});

test("desktop replaces calendar with a dedicated timeline page", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  const css = readProjectFile("src", "renderer", "styles.css");
  assert.match(html, /id="nav-timeline"/);
  assert.match(html, /id="view-timeline"/);
  assert.match(html, />时间线<\/span>/);
  assert.doesNotMatch(html, />日历<\/span>/);
  assert.doesNotMatch(html, /data-calendar-mode="month"/);
  assert.doesNotMatch(html, /id="cal-grid"/);
  assert.doesNotMatch(html, /id="view-calendar"/);
  assert.match(html, /class="[^"]*timeline-page[^"]*"/);
  assert.match(html, /id="timeline-stream"/);
  assert.match(html, /id="timeline-heatmap"/);
  assert.match(html, /id="timeline-milestones"/);
  assert.match(js, /renderTimelinePage/);
  assert.match(js, /buildTimelineItems/);
  assert.match(js, /renderTimelineHeatmap/);
  assert.match(js, /timeline: \["时间线"/);
  assert.match(js, /if \(view === "timeline"\) renderTimelinePage\(\)/);
  assert.match(css, /\.timeline-page/);
  assert.match(css, /\.timeline-stream/);
  assert.match(css, /\.calendar-heatmap/);
});

test("desktop uses page-owned layout without a global workbench shell", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  const css = readProjectFile("src", "renderer", "styles.css");
  assert.match(html, /class="[^"]*desktop-shell/);
  assert.match(html, /class="desktop-menubar"/);
  assert.match(html, /class="desktop-toolbar"/);
  assert.match(html, /class="[^"]*page-stage/);
  assert.doesNotMatch(html, /studio-workspace/);
  assert.doesNotMatch(html, /workspace-panel/);
  assert.doesNotMatch(html, /data-panel="left"/);
  assert.doesNotMatch(html, /data-panel="right"/);
  assert.doesNotMatch(html, /panel-resizer/);
  assert.doesNotMatch(html, /id="btn-toggle-ai-panel"/);
  assert.doesNotMatch(html, /ai-side-panel/);
  assert.match(html, /class="desktop-statusbar"/);
  assert.match(js, /renderStudioStatus/);
  assert.doesNotMatch(js, /bindPanelResizers/);
  assert.doesNotMatch(js, /renderAiSidePanel/);
  assert.match(css, /\.desktop-shell/);
  assert.match(css, /\.page-stage/);
  assert.doesNotMatch(css, /\.studio-workspace/);
  assert.doesNotMatch(css, /\.workspace-panel/);
  assert.doesNotMatch(css, /\.panel-resizer/);
  assert.doesNotMatch(css, /\.ai-side-panel/);
});

test("desktop pages are focused and avoid workbench-like clutter", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const css = readProjectFile("src", "renderer", "styles.css");
  assert.doesNotMatch(html, /Task Page|Timeline Page|Insight Page|Planner/);
  assert.doesNotMatch(html, /style="/);
  assert.match(html, /class="[^"]*task-command-row[^"]*"/);
  assert.match(html, /class="task-planning-board"/);
  assert.match(html, /class="diary-page"/);
  assert.match(html, /class="diary-subpage-shell"/);
  assert.match(html, /id="btn-open-diary-writer"/);
  assert.match(html, /class="[^"]*timeline-review-page[^"]*"/);
  assert.match(html, /class="[^"]*stats-review-page[^"]*"/);
  assert.match(html, /class="sync-verify-page"/);
  assert.match(css, /\.page-hero/);
  assert.match(css, /\.page-action-row/);
  assert.match(css, /\.diary-subpage-shell/);
  assert.match(css, /\.task-planning-board/);
});

test("desktop diary uses a markdown writing studio instead of modal editing", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  const css = readProjectFile("src", "renderer", "styles.css");
  assert.match(html, /class="markdown-editor"/);
  assert.match(html, /id="markdown-editor-title"/);
  assert.match(html, /id="markdown-editor-body"/);
  assert.match(html, /id="markdown-preview"/);
  assert.match(html, /id="writing-word-count"/);
  assert.match(html, /id="btn-toggle-writing-focus"/);
  assert.match(html, /id="writing-layout-control"/);
  assert.match(html, /data-writing-layout="split"/);
  assert.match(html, /data-writing-layout="focus"/);
  assert.match(html, /data-writing-layout="analysis"/);
  assert.match(js, /showMarkdownEditor/);
  assert.match(js, /toggleWritingFocus/);
  assert.match(js, /setWritingLayout/);
  assert.match(js, /writingLayout/);
  assert.match(js, /renderMarkdownPreview/);
  assert.match(js, /saveMarkdownDiary/);
  assert.doesNotMatch(js, /function showDiaryModal/);
  assert.match(css, /\.diary-subpage-shell\.fullscreen/);
  assert.match(css, /\.markdown-editor\.layout-focus/);
  assert.match(css, /\.markdown-editor\.layout-analysis/);
  assert.match(css, /\.markdown-editor/);
  assert.match(css, /\.markdown-preview/);
});

test("desktop task page includes kanban bulk and gantt planning surfaces", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  const css = readProjectFile("src", "renderer", "styles.css");
  assert.match(html, /class="kanban-board"/);
  assert.match(html, /data-kanban-column="inbox"/);
  assert.match(html, /data-kanban-column="todo"/);
  assert.match(html, /data-kanban-column="doing"/);
  assert.match(html, /data-kanban-column="done"/);
  assert.match(html, /id="task-bulk-bar"/);
  assert.match(html, /id="btn-bulk-doing"/);
  assert.match(html, /id="btn-bulk-complete"/);
  assert.match(html, /id="btn-bulk-delete"/);
  assert.match(html, /id="task-gantt"/);
  assert.match(js, /renderKanbanBoard/);
  assert.match(js, /bindKanbanDrag/);
  assert.match(js, /selectedTaskIds/);
  assert.match(js, /runBulkTaskAction/);
  assert.match(js, /completeTask/);
  assert.match(js, /deleteTask/);
  assert.match(css, /\.task-bulk-bar/);
  assert.match(css, /\.task-bulk-actions/);
  assert.match(css, /\.kanban-board/);
  assert.match(css, /\.task-gantt/);
});

test("desktop keeps AI inside its own page and preserves native tray hotkey hooks", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const main = readProjectFile("src", "main.js");
  assert.match(html, /id="view-chat"/);
  assert.match(html, /id="chat-messages"/);
  assert.doesNotMatch(html, /id="ai-side-messages"/);
  assert.doesNotMatch(html, /id="btn-toggle-ai-panel"/);
  assert.doesNotMatch(html, /ai-side-panel/);
  assert.match(main, /Tray/);
  assert.match(main, /globalShortcut/);
  assert.match(main, /registerGlobalShortcuts/);
  assert.match(main, /createTray/);
});

test("desktop exposes global command search and shortcut hints", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  const css = readProjectFile("src", "renderer", "styles.css");
  assert.match(html, /id="global-search-overlay"/);
  assert.match(html, /id="global-search-input"/);
  assert.match(html, /Ctrl\+K/);
  assert.match(html, /Ctrl\+N/);
  assert.match(js, /openGlobalSearch/);
  assert.match(js, /renderGlobalSearchResults/);
  assert.match(js, /bindGlobalShortcuts/);
  assert.match(css, /\.global-search-overlay/);
  assert.match(css, /\.shortcut-grid/);
});

test("desktop stats page includes visual insight panels", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  const css = readProjectFile("src", "renderer", "styles.css");
  assert.match(html, /class="[^"]*stats-page[^"]*"/);
  assert.match(html, /id="stats-trend-chart"/);
  assert.match(html, /id="stats-completion-ring"/);
  assert.match(html, /id="stats-insight-panel"/);
  assert.match(js, /renderStatsCharts/);
  assert.match(js, /buildStatsInsights/);
  assert.match(css, /\.stats-page/);
  assert.match(css, /\.trend-chart/);
  assert.match(css, /\.completion-ring/);
});

test("desktop replaces default selects with designed segmented controls", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const css = readProjectFile("src", "renderer", "styles.css");
  assert.doesNotMatch(html, /<select/);
  assert.match(html, /class="segmented-control"/);
  assert.match(html, /data-provider-choice="local"/);
  assert.match(html, /data-priority-choice="2"/);
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
  assert.match(css, /\.task-item/);
  assert.match(css, /\.diary-entry/);
  assert.match(css, /\.modal-overlay/);
  assert.match(css, /\.loading-dots/);
  assert.match(css, /\.toast/);
  assert.match(css, /\.icon/);
  assert.match(css, /\.sync-page-grid/);
  assert.match(css, /\.sync-status-card/);
  assert.match(css, /\.task-page/);
  assert.match(css, /\.task-lane/);
  assert.match(css, /\.task-planner-panel/);
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

test("desktop page-owned surfaces include context menu templates and attachments", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  const css = readProjectFile("src", "renderer", "styles.css");
  assert.match(html, /id="desktop-context-menu"/);
  assert.match(html, /id="diary-template-rail"/);
  assert.match(html, /data-template-id="daily-review"/);
  assert.match(html, /id="attachment-drop-zone"/);
  assert.match(html, /id="dashboard-smart-folders"/);
  assert.match(html, /data-smart-folder="overdue"/);
  assert.match(js, /bindContextMenu/);
  assert.match(js, /showDesktopContextMenu/);
  assert.match(js, /insertDiaryTemplate/);
  assert.match(js, /bindAttachmentDropZone/);
  assert.match(js, /renderSmartFolders/);
  assert.match(css, /\.desktop-context-menu/);
  assert.match(css, /\.template-chip/);
  assert.match(css, /\.attachment-drop-zone/);
  assert.match(css, /\.smart-folder-list/);
});

test("desktop settings expose customizable shortcut profiles", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  const css = readProjectFile("src", "renderer", "styles.css");
  assert.match(html, /id="shortcut-settings"/);
  assert.match(html, /id="shortcut-profile-control"/);
  assert.match(html, /data-shortcut-profile="default"/);
  assert.match(html, /data-shortcut-profile="writer"/);
  assert.match(html, /data-shortcut-action="global-search"/);
  assert.match(js, /renderShortcutSettings/);
  assert.match(js, /collectShortcutPreferences/);
  assert.match(js, /shortcutProfile/);
  assert.match(css, /\.shortcut-settings-grid/);
  assert.match(css, /\.shortcut-row/);
});

test("desktop settings expose native desktop capability shortcuts", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  const css = readProjectFile("src", "renderer", "styles.css");
  assert.match(html, /id="desktop-native-settings"/);
  assert.match(html, /id="native-status-grid"/);
  assert.match(html, /class="[^"]*desktop-native-grid[^"]*"/);
  assert.match(html, /data-native-action="tray"/);
  assert.match(html, /data-native-action="export"/);
  assert.match(html, /data-native-action="phone"/);
  assert.match(html, /id="native-status-tray"/);
  assert.match(html, /id="native-status-notification"/);
  assert.match(html, /id="native-status-background-sync"/);
  assert.match(js, /data-native-action/);
  assert.match(js, /renderNativeStatus/);
  assert.match(css, /\.desktop-native-grid/);
  assert.match(css, /\.desktop-native-card/);
  assert.match(css, /\.native-status-grid/);
  assert.match(css, /\.native-status-item/);
});

test("desktop writing page exposes analysis and context-aware AI actions", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  const css = readProjectFile("src", "renderer", "styles.css");
  assert.match(html, /id="writing-analysis-panel"/);
  assert.match(html, /id="writing-focus-meter"/);
  assert.match(html, /id="ai-context-actions"/);
  assert.match(html, /data-ai-action="summarize"/);
  assert.match(html, /data-ai-action="extract-tasks"/);
  assert.match(js, /buildWritingAnalysis/);
  assert.match(js, /renderWritingAnalysis/);
  assert.match(js, /handleAiContextAction/);
  assert.match(css, /\.writing-analysis-panel/);
  assert.match(css, /\.focus-meter/);
  assert.match(css, /\.ai-context-action/);
});

test("desktop settings support multi-format export and native notification preferences", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  const main = readProjectFile("src", "main.js");
  const preload = readProjectFile("src", "preload.cjs");
  assert.match(html, /id="export-format-control"/);
  assert.match(html, /data-export-format="json"/);
  assert.match(html, /data-export-format="md"/);
  assert.match(html, /data-export-format="html"/);
  assert.match(html, /id="notification-control"/);
  assert.match(html, /data-notification-choice="deadline"/);
  assert.match(js, /selectedExportFormat/);
  assert.match(js, /collectExportFormat/);
  assert.match(preload, /exportData: \(format\)/);
  assert.match(main, /desktop:export-file/);
  assert.match(main, /exportFormat/);
  assert.match(main, /Notification/);
});

test("desktop persists attachments through preload and main IPC", () => {
  const js = readProjectFile("src", "renderer", "renderer.js");
  const main = readProjectFile("src", "main.js");
  const preload = readProjectFile("src", "preload.cjs");
  const svc = readProjectFile("src", "core", "desktopService.js");
  assert.match(js, /pendingAttachments/);
  assert.match(js, /saveAttachmentReferences/);
  assert.match(js, /API\.attachFilesToDiary/);
  assert.match(preload, /attachFilesToDiary: \(diaryId, files\)/);
  assert.match(main, /desktop:attach-files/);
  assert.match(main, /attachFilesToDiary/);
  assert.match(svc, /export function attachFilesToDiary/);
  assert.match(svc, /storageMode: "linked"/);
});

test("desktop exposes a focused import center for markdown archives", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  const main = readProjectFile("src", "main.js");
  const preload = readProjectFile("src", "preload.cjs");
  const svc = readProjectFile("src", "core", "desktopService.js");
  assert.match(html, /id="data-import-center"/);
  assert.match(html, /id="import-format-control"/);
  assert.match(html, /data-import-format="json"/);
  assert.match(html, /data-import-format="markdown"/);
  assert.match(html, /data-import-format="day-one"/);
  assert.match(html, /data-import-format="bear"/);
  assert.match(html, /id="btn-open-import-center"/);
  assert.match(js, /selectedImportFormat/);
  assert.match(js, /collectImportFormat/);
  assert.match(preload, /importData: \(format\)/);
  assert.match(main, /desktop:import-file/);
  assert.match(main, /importFormat/);
  assert.match(main, /day-one/);
  assert.match(main, /bear/);
  assert.match(svc, /export function importMarkdownArchive/);
  assert.match(svc, /export function importExternalDiaryArchive/);
});

test("desktop stats include local AI-style monthly report panel", () => {
  const html = readProjectFile("src", "renderer", "index.html");
  const js = readProjectFile("src", "renderer", "renderer.js");
  const svc = readProjectFile("src", "core", "desktopService.js");
  assert.match(html, /id="monthly-report-panel"/);
  assert.match(html, /id="monthly-report-summary"/);
  assert.match(html, /id="monthly-report-actions"/);
  assert.match(js, /buildMonthlyReport/);
  assert.match(js, /renderMonthlyReportPanel/);
  assert.match(js, /monthly-report-panel/);
  assert.match(svc, /export function buildMonthlyReport/);
});
