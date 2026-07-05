// ═══════════════════════════════════════════
// DiaryApp Desktop — Renderer
// ═══════════════════════════════════════════

const API = window.diaryDesktop;
let state = { tasks: [], diaries: [], aiDrafts: [], settings: {}, archive: [], account: {} };
let currentView = "dashboard";
let taskFilter = "active";
let timelineDate = new Date();
let selectedTaskPriority = 0;
let selectedTaskIds = new Set();
let activeDiaryId = null;
let selectedExportFormat = "json";
let selectedImportFormat = "json";
let pendingAttachments = [];
let shortcutProfile = "default";
let writingLayout = "split";

const ICONS = {
  emptyTask: '<svg class="icon icon-empty" viewBox="0 0 24 24" aria-hidden="true"><path d="M6.5 6.5h11M6.5 12h11M6.5 17.5h7"/><path d="m3.8 6.5.8.8 1.4-1.6M3.8 12l.8.8 1.4-1.6M3.8 17.5l.8.8 1.4-1.6"/></svg>',
  emptyDone: '<svg class="icon icon-empty" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12.5 9.5 17 19 7"/></svg>',
  diary: '<svg class="icon icon-empty" viewBox="0 0 24 24" aria-hidden="true"><path d="M7 4h9.2A1.8 1.8 0 0 1 18 5.8v12.4a1.8 1.8 0 0 1-1.8 1.8H7a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z"/><path d="M8.5 8h6M8.5 11.5h6M8.5 15h4"/></svg>',
  clock: '<svg class="icon icon-inline" viewBox="0 0 24 24" aria-hidden="true"><path d="M12 6v6l4 2"/><path d="M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Z"/></svg>',
  calendar: '<svg class="icon icon-inline" viewBox="0 0 24 24" aria-hidden="true"><path d="M6 5h12a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2Z"/><path d="M8 3v4M16 3v4M4 10h16"/></svg>',
  trash: '<svg class="icon icon-inline" viewBox="0 0 24 24" aria-hidden="true"><path d="M6 7h12M10 7V5h4v2M8 7l.7 12h6.6L16 7"/></svg>',
  user: '<svg class="icon icon-inline" viewBox="0 0 24 24" aria-hidden="true"><path d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z"/><path d="M5 20a7 7 0 0 1 14 0"/></svg>',
  bot: '<svg class="icon icon-inline" viewBox="0 0 24 24" aria-hidden="true"><path d="M8 9h8a3 3 0 0 1 3 3v3a3 3 0 0 1-3 3H8a3 3 0 0 1-3-3v-3a3 3 0 0 1 3-3Z"/><path d="M12 5v4M9.5 13h.1M14.5 13h.1"/></svg>',
  idea: '<svg class="icon icon-inline" viewBox="0 0 24 24" aria-hidden="true"><path d="M9 18h6M10 21h4"/><path d="M8 13.5a6 6 0 1 1 8 0c-.8.7-1.1 1.5-1.1 2.5H9.1c0-1-.3-1.8-1.1-2.5Z"/></svg>'
};

function iconMarkup(name) {
  return ICONS[name] || "";
}

// ═══ Init ═══

document.addEventListener("DOMContentLoaded", async () => {
  state = await API.getState();
  applyVisualPreferences(state.settings || {});
  renderAll();
  bindEvents();
  bindGlobalShortcuts();
  checkProactiveAgent();
});

// ═══ State Helpers ═══

async function refreshState() {
  state = await API.getState();
  return state;
}

async function mutate(fn) {
  await fn();
  await refreshState();
  renderAll();
}

function showToast(message, type = "info") {
  const container = document.getElementById("toast-container");
  const toast = document.createElement("div");
  toast.className = `toast ${type}`;
  toast.textContent = message;
  container.appendChild(toast);
  setTimeout(() => toast.remove(), 3000);
}

// ═══ Rendering ═══

function renderAll() {
  renderDashboardPage();
  renderTaskPage();
  renderDiaries();
  renderStats();
  renderTimelinePage();
  renderBadge();
  renderSyncStatus();
  renderSyncAccount();
  renderStudioStatus();
  renderWritingAnalysis();
  renderShortcutSettings();
  renderNativeStatus();
  loadSettings();
}

function renderDashboardPage() {
  renderDashboardMetrics();
  renderDashboardTrend();
  renderDashboardHeatmap();
  renderSmartFolders();
  renderDesktopReadiness();
  renderPageAiRail();
}

function renderDashboardMetrics() {
  const grid = document.getElementById("dashboard-metric-grid");
  if (!grid) return;
  const tasks = state.tasks || [];
  const diaries = state.diaries || [];
  const active = tasks.filter((task) => task.status !== "done").length;
  const done = tasks.filter((task) => task.status === "done").length;
  const completion = tasks.length ? Math.round((done / tasks.length) * 100) : 0;
  const todayWriting = diaries.filter((diary) => isSameDay(diary.createdAt, new Date())).length;
  const metrics = [
    ["未完成", active, "需要处理"],
    ["完成率", `${completion}%`, "任务闭环"],
    ["今日日记", todayWriting, "写作输入"],
    ["同步", state.account?.token ? "已连接" : "未登录", "手机号互通"]
  ];
  grid.innerHTML = metrics.map(([label, value, hint]) => `
    <div class="dashboard-metric">
      <span>${label}</span>
      <strong>${value}</strong>
      <small>${hint}</small>
    </div>
  `).join("");
}

function renderDashboardTrend() {
  const svg = document.getElementById("dashboard-trend-svg");
  if (!svg) return;
  const days = Array.from({ length: 7 }, (_, index) => {
    const date = new Date();
    date.setDate(date.getDate() - (6 - index));
    const key = date.toDateString();
    const taskCount = (state.tasks || []).filter((task) => task.updatedAt && new Date(task.updatedAt).toDateString() === key).length;
    const diaryCount = (state.diaries || []).filter((diary) => diary.createdAt && new Date(diary.createdAt).toDateString() === key).length;
    return { label: `${date.getMonth() + 1}/${date.getDate()}`, value: taskCount + diaryCount };
  });
  const max = Math.max(1, ...days.map((day) => day.value));
  const points = days.map((day, index) => {
    const x = 36 + index * 80;
    const y = 172 - (day.value / max) * 120;
    return { ...day, x, y };
  });
  const path = points.map((point, index) => `${index ? "L" : "M"} ${point.x} ${point.y}`).join(" ");
  const lastPoint = points[points.length - 1];
  svg.innerHTML = `
    <path class="dashboard-trend-area" d="${path} L ${lastPoint.x} 190 L ${points[0].x} 190 Z"></path>
    <path class="dashboard-trend-line" d="${path}"></path>
    ${points.map((point) => `
      <g>
        <circle class="dashboard-trend-dot" cx="${point.x}" cy="${point.y}" r="5"></circle>
        <text x="${point.x}" y="210" text-anchor="middle">${point.label}</text>
      </g>
    `).join("")}
  `;
}

function renderDashboardHeatmap() {
  const heatmap = document.getElementById("dashboard-heatmap");
  if (!heatmap) return;
  const diaries = state.diaries || [];
  const cells = Array.from({ length: 42 }, (_, index) => {
    const date = new Date();
    date.setDate(date.getDate() - (41 - index));
    const count = diaries.filter((diary) => isSameDay(diary.createdAt, date)).length;
    return `<span class="dashboard-heat-cell level-${Math.min(4, count)}" title="${date.toLocaleDateString()}"></span>`;
  });
  heatmap.innerHTML = cells.join("");
}

function renderSmartFolders() {
  const tasks = state.tasks || [];
  const diaries = state.diaries || [];
  const values = {
    overdue: tasks.filter(isOverdue).length,
    today: tasks.filter(isDueToday).length,
    untagged: [...tasks, ...diaries].filter((item) => !(item.tags || []).length).length,
    writing: diaries.filter((diary) => String(diary.text || "").replace(/\s+/g, "").length > 300).length
  };
  Object.entries(values).forEach(([key, value]) => {
    const el = document.getElementById(`smart-folder-${key}`);
    if (el) el.textContent = value;
  });
}

function renderDesktopReadiness() {
  const sync = document.getElementById("readiness-sync");
  if (!sync) return;
  sync.textContent = state.account?.token ? "已联通" : "去登录";
}

function renderNativeStatus() {
  const tray = document.getElementById("native-status-tray");
  const notification = document.getElementById("native-status-notification");
  const backgroundSync = document.getElementById("native-status-background-sync");
  if (tray) tray.textContent = "已启用";
  if (notification) {
    const value = state.settings?.notificationChoice || "deadline";
    const labels = { deadline: "截止提醒", sync: "同步结果", quiet: "安静模式" };
    notification.textContent = labels[value] || labels.deadline;
  }
  if (backgroundSync) backgroundSync.textContent = state.account?.token ? "2 小时" : "登录后启用";
}

function renderPageAiRail() {
  const rail = document.getElementById("dashboard-ai-rail");
  if (!rail) return;
  const insights = buildDashboardAiInsights();
  rail.innerHTML = `
    <div class="calendar-panel-title">页面 AI 辅助</div>
    <div class="page-ai-list">
      ${insights.map((item) => `<button class="page-ai-action" data-ai-dashboard-prompt="${escapeHtml(item.prompt)}"><strong>${escapeHtml(item.title)}</strong><span>${escapeHtml(item.detail)}</span></button>`).join("")}
    </div>
  `;
}

function buildDashboardAiInsights() {
  const tasks = state.tasks || [];
  const diaries = state.diaries || [];
  const overdue = tasks.filter(isOverdue).length;
  const recentDiary = diaries[0]?.title || "最近日记";
  return [
    {
      title: overdue ? "先处理逾期" : "生成今日路线",
      detail: overdue ? `${overdue} 个任务已过期，建议先收束。` : "基于任务和日记生成今天的执行顺序。",
      prompt: overdue ? "请帮我整理逾期任务的处理顺序。" : "请根据当前任务和日记生成今日执行路线。"
    },
    {
      title: "复盘写作主线",
      detail: diaries.length ? `从「${recentDiary}」开始提炼主题。` : "还没有日记时，先给我一个复盘模板。",
      prompt: "请根据最近日记提炼本周主线和可执行建议。"
    },
    {
      title: "整理智能文件夹",
      detail: "把未归档线索变成项目标签。",
      prompt: "请帮我为未归档任务和日记建议项目标签。"
    }
  ];
}

function applySmartFolder(folder) {
  if (folder === "writing") {
    switchView("diary");
    showToast("已打开写作素材，可继续沉淀长文线索", "info");
    return;
  }
  switchView("tasks");
  taskFilter = folder === "untagged" ? "all" : "active";
  document.querySelectorAll("[data-filter]").forEach((btn) => {
    btn.classList.toggle("active", btn.dataset.filter === taskFilter);
  });
  renderTaskPage();
  const labels = {
    overdue: "已应用保存搜索：status:overdue",
    today: "已应用保存搜索：date:today",
    untagged: "已应用保存搜索：tags:missing"
  };
  showToast(labels[folder] || "已打开智能文件夹", "info");
}

function renderTasks() {
  renderTaskPage();
}

function renderTaskPage() {
  renderTaskInsights();
  renderKanbanBoard();
  renderTaskGantt();
  renderTaskLanes();
  updateBulkBar();
  bindKanbanDrag();
}

function buildTaskInsights() {
  const tasks = state.tasks || [];
  const active = tasks.filter((t) => t.status !== "done");
  const today = active.filter(isDueToday);
  const overdue = active.filter(isOverdue);
  const inbox = active.filter((t) => (t.status || "inbox") === "inbox");
  const tagged = active.filter((t) => (t.tags || []).length);
  const focusScore = active.length ? Math.round((tagged.length / active.length) * 100) : 100;
  return [
    { label: "今日执行", value: today.length, detail: overdue.length ? `${overdue.length} 个已逾期` : "节奏正常" },
    { label: "Inbox 待整理", value: inbox.length, detail: inbox.length ? "需要明确下一步" : "入口干净" },
    { label: "项目清晰度", value: `${focusScore}%`, detail: "基于标签完整度" },
  ];
}

function renderTaskInsights() {
  const strip = document.getElementById("task-focus-strip");
  if (!strip) return;
  strip.innerHTML = buildTaskInsights().map((item) => `
    <div class="task-insight-card">
      <span>${item.label}</span>
      <strong>${item.value}</strong>
      <small>${item.detail}</small>
    </div>
  `).join("");
}

function renderTaskLanes() {
  const lanes = {
    inbox: document.getElementById("lane-inbox"),
    today: document.getElementById("lane-today"),
    planned: document.getElementById("lane-planned"),
  };
  if (!lanes.inbox || !lanes.today || !lanes.planned) return;

  const list = document.getElementById("task-list");
  const tasks = (state.tasks || [])
    .filter((t) => {
      if (taskFilter === "active") return t.status !== "done";
      if (taskFilter === "done") return t.status === "done";
      return true;
    })
    .sort((a, b) => {
      if (a.status === "done" && b.status !== "done") return 1;
      if (a.status !== "done" && b.status === "done") return -1;
      return (b.priority || 0) - (a.priority || 0);
    });

  if (!tasks.length) {
    const empty = `
      <div class="empty-state compact-empty">
        <div class="empty-state-icon">${taskFilter === "done" ? iconMarkup("emptyDone") : iconMarkup("emptyTask")}</div>
        <div class="empty-state-title">${taskFilter === "done" ? "还没有已完成的任务" : "还没有待办任务"}</div>
        <div class="empty-state-text">${taskFilter === "done" ? "完成一些任务后，它们会出现在这里" : "在上方输入框快速添加，或让 AI 助手帮你创建"}</div>
      </div>`;
    Object.values(lanes).forEach((lane) => { lane.innerHTML = ""; });
    lanes.inbox.innerHTML = empty;
    updateLaneCounts({ inbox: 0, today: 0, planned: 0 });
    if (list) list.innerHTML = empty;
    return;
  }

  const grouped = { inbox: [], today: [], planned: [] };
  for (const task of tasks) {
    if ((task.status || "inbox") === "inbox" && task.status !== "done") grouped.inbox.push(task);
    else if (isDueToday(task) || isOverdue(task)) grouped.today.push(task);
    else grouped.planned.push(task);
  }

  for (const [key, lane] of Object.entries(lanes)) {
    lane.innerHTML = grouped[key].length
      ? grouped[key].map(renderTaskCard).join("")
      : `<div class="lane-empty">${key === "inbox" ? "没有待整理输入" : key === "today" ? "今天没有明确执行项" : "后续计划为空"}</div>`;
  }
  updateLaneCounts({
    inbox: grouped.inbox.length,
    today: grouped.today.length,
    planned: grouped.planned.length,
  });
  if (list) list.innerHTML = tasks.map(renderTaskCard).join("");
}

function renderTaskCard(t) {
  const checked = selectedTaskIds.has(t.id) ? "checked" : "";
  return `
    <div class="task-item task-card ${t.status === "done" ? "completed" : ""} priority-${t.priority || 0}" data-id="${t.id}">
      <button class="task-select ${checked}" data-action="select" data-id="${t.id}" aria-label="选择任务"></button>
      <div class="task-checkbox ${t.status === "done" ? "checked" : ""}" data-action="toggle" data-id="${t.id}"></div>
      <div class="task-body">
        <div class="task-title">${escapeHtml(t.title)}</div>
        ${t.description ? `<div class="task-description">${escapeHtml(t.description)}</div>` : ""}
        <div class="task-meta">
          ${t.priority > 0 ? `<span class="task-tag priority-${t.priority >= 2 ? "high" : "medium"}">P${t.priority}</span>` : ""}
          ${t.tags ? t.tags.map((tag) => `<span class="task-tag tag-default">#${escapeHtml(tag)}</span>`).join("") : ""}
          ${t.dueAt ? `<span>${iconMarkup("clock")} ${formatDate(t.dueAt)}</span>` : ""}
          ${t.createdAt ? `<span>${iconMarkup("calendar")} ${formatDate(t.createdAt)}</span>` : ""}
        </div>
      </div>
      <div class="task-actions">
        <button class="btn btn-ghost btn-icon" data-action="delete" data-id="${t.id}" title="删除" aria-label="删除">${iconMarkup("trash")}</button>
      </div>
    </div>`;
}

function renderKanbanBoard() {
  const board = document.getElementById("kanban-board");
  if (!board) return;
  const columns = {
    inbox: document.getElementById("kanban-inbox"),
    todo: document.getElementById("kanban-todo"),
    doing: document.getElementById("kanban-doing"),
    done: document.getElementById("kanban-done"),
  };
  if (Object.values(columns).some((column) => !column)) return;

  const grouped = { inbox: [], todo: [], doing: [], done: [] };
  for (const task of state.tasks || []) {
    const status = task.status === "done" ? "done" : task.status === "doing" ? "doing" : task.status === "planned" ? "todo" : "inbox";
    grouped[status].push(task);
  }

  for (const [key, column] of Object.entries(columns)) {
    column.innerHTML = grouped[key].length
      ? grouped[key].map(renderKanbanCard).join("")
      : `<div class="lane-empty">暂无内容</div>`;
  }
}

function renderKanbanCard(task) {
  const selected = selectedTaskIds.has(task.id) ? "selected" : "";
  return `
    <article class="kanban-card ${selected}" draggable="true" data-id="${task.id}">
      <button class="task-select ${selected ? "checked" : ""}" data-action="select" data-id="${task.id}" aria-label="选择任务"></button>
      <strong>${escapeHtml(task.title)}</strong>
      <span>${task.dueAt ? formatDate(task.dueAt) : "未排期"}</span>
    </article>`;
}

function bindKanbanDrag() {
  const board = document.getElementById("kanban-board");
  if (!board || board.dataset.bound) return;
  board.dataset.bound = "true";
  board.addEventListener("dragstart", (event) => {
    const card = event.target.closest(".kanban-card");
    if (!card) return;
    event.dataTransfer.setData("text/plain", card.dataset.id);
    card.classList.add("dragging");
  });
  board.addEventListener("dragend", (event) => {
    event.target.closest(".kanban-card")?.classList.remove("dragging");
  });
  board.addEventListener("dragover", (event) => {
    if (event.target.closest("[data-kanban-column]")) event.preventDefault();
  });
  board.addEventListener("drop", async (event) => {
    const column = event.target.closest("[data-kanban-column]");
    if (!column) return;
    event.preventDefault();
    const id = event.dataTransfer.getData("text/plain");
    const targetStatus = column.dataset.kanbanColumn === "done" ? "done" : column.dataset.kanbanColumn === "doing" ? "doing" : column.dataset.kanbanColumn === "todo" ? "planned" : "inbox";
    await mutate(() => API.updateTask(id, { status: targetStatus }));
  });
}

function renderTaskGantt() {
  const gantt = document.getElementById("task-gantt");
  if (!gantt) return;
  const dated = (state.tasks || []).filter((task) => task.dueAt).slice(0, 8);
  gantt.innerHTML = dated.length
    ? dated.map((task, index) => `
      <div class="gantt-row">
        <span>${escapeHtml(task.title)}</span>
        <div class="gantt-track"><i class="gantt-fill" data-offset="${Math.min(72, index * 9)}" data-width="${task.status === "done" ? 36 : 22}"></i></div>
        <time>${formatDate(task.dueAt)}</time>
      </div>`).join("")
    : `<div class="lane-empty">给任务添加截止时间后，这里会形成桌面端甘特计划。</div>`;
  applyRangeStyles(gantt.querySelectorAll(".gantt-fill"), "offset", "--offset");
  applyRangeStyles(gantt.querySelectorAll(".gantt-fill"), "width", "--width");
}

function updateBulkBar() {
  const count = document.getElementById("task-bulk-count");
  if (count) count.textContent = `已选 ${selectedTaskIds.size} 项`;
}

async function runBulkTaskAction(action) {
  const ids = [...selectedTaskIds];
  if (!ids.length) {
    showToast("请先选择任务", "info");
    return;
  }
  for (const id of ids) {
    if (action === "doing") await API.updateTask(id, { status: "doing" });
    if (action === "complete") await API.completeTask(id);
    if (action === "delete") await API.deleteTask(id);
  }
  selectedTaskIds = new Set();
  await refreshState();
  renderTaskPage();
  const labels = {
    doing: "已移动所选任务",
    complete: "已完成所选任务",
    delete: "已删除所选任务"
  };
  showToast(labels[action] || "批量操作已完成", action === "delete" ? "info" : "success");
}

function updateLaneCounts(counts) {
  for (const [key, value] of Object.entries(counts)) {
    const el = document.getElementById(`lane-count-${key}`);
    if (el) el.textContent = value;
  }
}

function isDueToday(task) {
  if (!task.dueAt) return false;
  const due = new Date(task.dueAt);
  const now = new Date();
  return due.getFullYear() === now.getFullYear() && due.getMonth() === now.getMonth() && due.getDate() === now.getDate();
}

function isOverdue(task) {
  return task.status !== "done" && task.dueAt && new Date(task.dueAt).getTime() < Date.now();
}

function renderDiaries() {
  const list = document.getElementById("diary-list");
  const diaries = state.diaries || [];

  if (!diaries.length) {
    list.innerHTML = `
      <div class="empty-state">
        <div class="empty-state-icon">${iconMarkup("diary")}</div>
        <div class="empty-state-title">还没有日记</div>
        <div class="empty-state-text">点击右上角「写日记」开始记录你的生活</div>
      </div>`;
    return;
  }

  list.innerHTML = [...diaries]
    .reverse()
    .slice(0, 50)
    .map(
      (d) => `
    <div class="diary-entry" data-id="${d.id}">
      <div class="diary-date">${formatDate(d.createdAt)} ${d.mood ? `<span class="diary-mood ${d.mood}">${getMoodLabel(d.mood)}</span>` : ""}</div>
      <div class="diary-title">${escapeHtml(d.title || "无标题")}</div>
      <div class="diary-excerpt">${escapeHtml((d.text || "").slice(0, 200))}</div>
      ${d.tags && d.tags.length ? `<div class="diary-tag-row">${d.tags.map((tag) => `<span class="badge badge-accent">#${escapeHtml(tag)}</span>`).join("")}</div>` : ""}
    </div>`
    )
    .join("");
}

function formatDate(ts) {
  if (!ts) return "";
  const d = new Date(ts);
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`;
}

function getMoodLabel(mood) {
  return { happy: "开心", sad: "难过", neutral: "平静", angry: "生气", excited: "兴奋", grateful: "感恩" }[mood] || mood;
}

function renderMoodSegments(selectedMood = "neutral") {
  return ["neutral", "happy", "sad", "angry", "excited", "grateful"].map((mood) => `
    <button class="segment ${mood === selectedMood ? "active" : ""}" data-mood-choice="${mood}" type="button">
      ${getMoodLabel(mood)}
    </button>
  `).join("");
}

function renderStats() {
  const tasks = state.tasks || [];
  const diaries = state.diaries || [];
  const total = tasks.length;
  const done = tasks.filter((t) => t.status === "done").length;
  const active = total - done;
  const completion = total > 0 ? Math.round((done / total) * 100) : 0;

  document.getElementById("stat-tasks-total").textContent = total;
  document.getElementById("stat-tasks-done").textContent = done;
  document.getElementById("stat-tasks-active").textContent = active;
  document.getElementById("stat-diaries").textContent = diaries.length;
  document.getElementById("stat-streak").textContent = calcStreak(diaries);
  document.getElementById("stat-completion").textContent = `${completion}%`;
  renderStatsCharts({ tasks, diaries, completion, active, done });
  renderMonthlyReportPanel();
}

function buildStatsInsights({ tasks, diaries, completion, active, done }) {
  const overdue = tasks.filter((task) => task.status !== "done" && isOverdue(task)).length;
  const planned = tasks.filter((task) => task.status !== "done" && task.dueAt).length;
  const diaryDays = new Set(diaries.map((diary) => new Date(diary.createdAt).toDateString())).size;
  return [
    `完成率 ${completion}%，已完成 ${done} 项，还有 ${active} 项待推进。`,
    overdue ? `${overdue} 项已经逾期，建议先清理阻塞。` : "没有逾期任务，当前节奏稳定。",
    planned ? `${planned} 项任务已经绑定日期，可以在时间线页面里安排。` : "多数任务还没有时间锚点，可以补充截止时间。",
    diaryDays ? `已覆盖 ${diaryDays} 个记录日，适合做周复盘。` : "还没有日记记录，复盘样本不足。"
  ];
}

function renderStatsCharts(metrics) {
  const ring = document.getElementById("stats-completion-ring");
  const ringValue = document.getElementById("stats-ring-value");
  const chart = document.getElementById("stats-trend-chart");
  const panel = document.getElementById("stats-insight-panel");
  if (!ring || !chart || !panel) return;

  ring.style.setProperty("--completion", `${metrics.completion}%`);
  ringValue.textContent = `${metrics.completion}%`;

  const today = new Date();
  const days = Array.from({ length: 7 }, (_, index) => {
    const date = new Date(today);
    date.setDate(today.getDate() - (6 - index));
    const key = date.toDateString();
    const taskCount = metrics.tasks.filter((task) => task.updatedAt && new Date(task.updatedAt).toDateString() === key).length;
    const diaryCount = metrics.diaries.filter((diary) => diary.createdAt && new Date(diary.createdAt).toDateString() === key).length;
    return { label: `${date.getMonth() + 1}/${date.getDate()}`, value: taskCount + diaryCount };
  });
  const max = Math.max(1, ...days.map((day) => day.value));
  chart.innerHTML = `
    <div class="trend-chart-head">
      <strong>近 7 天行动趋势</strong>
      <span>任务更新 + 日记记录</span>
    </div>
    <div class="trend-bars">
      ${days.map((day) => `
        <div class="trend-bar">
          <span class="trend-bar-fill" data-height="${Math.max(10, Math.round((day.value / max) * 100))}"></span>
          <small>${day.label}</small>
        </div>
      `).join("")}
    </div>`;
  applyRangeStyles(chart.querySelectorAll(".trend-bar-fill"), "height", "--bar-height");
  panel.innerHTML = `
    <div class="calendar-panel-title">执行洞察</div>
    ${buildStatsInsights(metrics).map((text) => `<div class="insight-line">${escapeHtml(text)}</div>`).join("")}`;
}

function applyRangeStyles(nodes, dataKey, cssVar) {
  nodes.forEach((node) => {
    const value = Number(node.dataset[dataKey]);
    if (Number.isFinite(value)) node.style.setProperty(cssVar, `${value}%`);
  });
}

function buildMonthlyReport() {
  const now = new Date();
  const year = now.getFullYear();
  const month = now.getMonth() + 1;
  const start = new Date(year, month - 1, 1).getTime();
  const end = new Date(year, month, 1).getTime();
  const inMonth = (value) => {
    if (!value) return false;
    const time = new Date(value).getTime();
    return Number.isFinite(time) && time >= start && time < end;
  };
  const diaries = (state.diaries || []).filter((diary) => inMonth(diary.createdAt));
  const tasks = (state.tasks || []).filter((task) => inMonth(task.completedAt || task.updatedAt || task.createdAt || task.dueAt));
  const completedTasks = tasks.filter((task) => task.status === "done").length;
  const openTasks = tasks.filter((task) => task.status !== "done").length;
  const words = diaries.reduce((sum, diary) => sum + String(diary.text || "").replace(/\s+/g, "").length, 0);
  const tags = new Map();
  [...diaries, ...tasks].forEach((item) => {
    (item.tags || []).forEach((tag) => tags.set(tag, (tags.get(tag) || 0) + 1));
  });
  const topTags = [...tags.entries()].sort((a, b) => b[1] - a[1]).slice(0, 4);
  return {
    periodLabel: `${year}年${month}月`,
    summary: `${year}年${month}月写作 ${diaries.length} 篇，完成 ${completedTasks} 项任务，还有 ${openTasks} 项待推进。`,
    recommendations: [
      openTasks ? `先收束 ${openTasks} 项未完成任务，避免下月继续堆积。` : "任务闭环不错，可以沉淀为复盘模板。",
      words >= 1200 ? "写作素材充足，适合整理成长文或决策记录。" : "写作样本偏少，建议补一篇月度回顾。",
      topTags.length ? `本月主线集中在「${topTags[0][0]}」，可以按主题归档。` : "标签还不够明确，可以给日记和任务补项目标签。"
    ],
    topTags
  };
}

function renderMonthlyReportPanel() {
  const panel = document.getElementById("monthly-report-panel");
  const summary = document.getElementById("monthly-report-summary");
  const actions = document.getElementById("monthly-report-actions");
  if (!panel || !summary || !actions) return;
  const report = buildMonthlyReport();
  summary.textContent = report.summary;
  actions.innerHTML = report.recommendations
    .map((item) => `<div class="monthly-report-action">${escapeHtml(item)}</div>`)
    .join("");
}

function buildWritingAnalysis(text = document.getElementById("markdown-editor-body")?.value || "") {
  const trimmed = text.trim();
  const words = trimmed.replace(/\s+/g, "").length;
  const taskHints = (trimmed.match(/- \[[ x]\]/g) || []).length;
  const headings = (trimmed.match(/^#{1,3}\s/gm) || []).length;
  const focus = Math.min(100, Math.round((Math.min(words, 900) / 900) * 70 + Math.min(headings + taskHints, 6) * 5));
  return {
    words,
    taskHints,
    headings,
    focus,
    summary: words
      ? `当前 ${words} 字，${headings} 个结构标题，${taskHints} 条待办线索。`
      : "开始写作后，这里会显示节奏、结构和可执行线索。"
  };
}

function renderWritingAnalysis() {
  const panel = document.getElementById("writing-analysis-panel");
  if (!panel) return;
  const analysis = buildWritingAnalysis();
  const summary = document.getElementById("writing-analysis-summary");
  const meter = document.getElementById("writing-focus-meter");
  if (summary) summary.textContent = analysis.summary;
  if (meter) meter.style.setProperty("--focus", `${analysis.focus}%`);
}

function calcStreak(diaries) {
  if (!diaries.length) return 0;
  const dates = [...new Set(diaries.map((d) => new Date(d.createdAt).toDateString()))].sort();
  let streak = 1;
  for (let i = dates.length - 1; i > 0; i--) {
    const diff = (new Date(dates[i]) - new Date(dates[i - 1])) / 86400000;
    if (diff <= 1.5) streak++;
    else break;
  }
  return streak;
}

function renderBadge() {
  const active = (state.tasks || []).filter((t) => t.status !== "done").length;
  const badge = document.getElementById("task-badge");
  badge.textContent = active;
  badge.style.display = active > 0 ? "inline" : "none";
}

function renderSyncStatus() {
  const el = document.getElementById("sync-text");
  const account = state.account || {};
  if (account.status === "linked") {
    el.textContent = account.lastSyncAt ? `已连接 ${account.maskedPhone} · ${formatDate(account.lastSyncAt)}` : `已连接 ${account.maskedPhone}`;
  } else {
    el.textContent = "未连接同步";
  }
}

function renderSyncAccount() {
  const summary = document.getElementById("sync-account-summary");
  if (!summary) return;
  const account = state.account || {};
  const endpoint = account.syncEndpoint || "https://diary-app-sync.2453759261.workers.dev";
  document.getElementById("sync-phone").value = account.status === "linked" ? account.phone : "";
  document.getElementById("sync-endpoint").value = endpoint;
  summary.innerHTML = account.status === "linked"
    ? `<div class="sync-status-card connected"><strong>已连接 ${escapeHtml(account.maskedPhone || account.phone)}</strong><span>${account.lastSyncAt ? `上次同步 ${formatDate(account.lastSyncAt)}` : "尚未同步"}</span></div>`
    : `<div class="sync-status-card"><strong>未连接 APP 端账号</strong><span>使用 Android 端同一个手机号和 PIN 登录。</span></div>`;
}

function resolveThemeMode(mode = "light") {
  if (mode === "system") {
    return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
  }
  return mode === "dark" ? "dark" : "light";
}

function applyVisualPreferences(settings = {}) {
  const theme = settings.themeChoice || "fog";
  const fontScale = String(settings.fontScale || "1.1");
  const themeMode = settings.themeMode || "light";
  document.body.dataset.theme = theme;
  document.body.dataset.mode = resolveThemeMode(themeMode);
  document.documentElement.style.setProperty("--font-scale", fontScale);
}

// ═══ Timeline ═══

function renderTimelinePage() {
  const label = document.getElementById("cal-month-label");
  if (label) label.textContent = `${timelineDate.getFullYear()}年 ${timelineDate.getMonth() + 1}月 · 时间线`;
  const stream = document.getElementById("timeline-stream");
  if (!stream) return;
  const items = buildTimelineItems();
  stream.innerHTML = items.length
    ? items.map((item) => `
      <article class="timeline-item ${item.type}">
        <time>${item.dateLabel}</time>
        <div>
          <strong>${escapeHtml(item.title)}</strong>
          <p>${escapeHtml(item.detail)}</p>
        </div>
      </article>`).join("")
    : `<div class="empty-state"><div class="empty-state-title">暂无时间线内容</div><div class="empty-state-text">添加带日期的任务或日记后，这里会自动形成复盘流。</div></div>`;
  renderTimelineHeatmap();
  renderTimelineMilestones(items);
}

function buildTimelineItems() {
  const taskItems = (state.tasks || []).filter((task) => task.dueAt).map((task) => ({
    type: "task",
    date: new Date(task.dueAt),
    title: task.title,
    detail: task.status === "done" ? "任务已完成" : "待办计划",
  }));
  const diaryItems = (state.diaries || []).map((diary) => ({
    type: "diary",
    date: new Date(diary.createdAt || Date.now()),
    title: diary.title || "无标题日记",
    detail: (diary.text || "").slice(0, 120) || "日记记录",
  }));
  return [...taskItems, ...diaryItems]
    .filter((item) => !Number.isNaN(item.date.getTime()))
    .sort((a, b) => b.date.getTime() - a.date.getTime())
    .slice(0, 40)
    .map((item) => ({ ...item, dateLabel: formatDate(item.date.toISOString()) }));
}

function calendarKey(date) {
  return `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`;
}

function isSameDay(value, compareDate) {
  if (!value || !compareDate) return false;
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return false;
  return calendarKey(date) === calendarKey(compareDate);
}

function buildCalendarActivity() {
  const activity = new Map();
  const ensure = (key) => {
    if (!activity.has(key)) activity.set(key, { tasks: [], diaries: [] });
    return activity.get(key);
  };
  (state.tasks || []).forEach((task) => {
    if (!task.dueAt) return;
    ensure(calendarKey(new Date(task.dueAt))).tasks.push(task);
  });
  (state.diaries || []).forEach((diary) => {
    if (!diary.createdAt) return;
    ensure(calendarKey(new Date(diary.createdAt))).diaries.push(diary);
  });
  return activity;
}

function buildCalendarAgenda(date) {
  const key = calendarKey(date);
  const activity = buildCalendarActivity().get(key) || { tasks: [], diaries: [] };
  return [
    ...activity.tasks.map((task) => ({ type: "task", title: task.title, meta: task.status === "done" ? "已完成" : "待执行" })),
    ...activity.diaries.map((diary) => ({ type: "diary", title: diary.title || "无标题日记", meta: "日记记录" }))
  ];
}

function renderCalendarAgenda(date = timelineDate) {
  const agenda = document.getElementById("calendar-agenda");
  if (!agenda) return;
  const items = buildCalendarAgenda(date);
  agenda.innerHTML = items.length
    ? items.map((item) => `<div class="agenda-item ${item.type}"><strong>${escapeHtml(item.title)}</strong><span>${item.meta}</span></div>`).join("")
    : `<div class="agenda-empty">这一天没有任务或日记，可以从待办规划器安排一个时间块。</div>`;
}

function renderTimelineHeatmap() {
  const heatmap = document.getElementById("timeline-heatmap");
  if (!heatmap) return;
  const activity = buildCalendarActivity();
  const today = new Date();
  heatmap.innerHTML = Array.from({ length: 35 }, (_, index) => {
    const date = new Date(today);
    date.setDate(today.getDate() - (34 - index));
    const load = (activity.get(calendarKey(date))?.tasks.length || 0) + (activity.get(calendarKey(date))?.diaries.length || 0);
    return `<span class="heatmap-cell" data-level="${Math.min(4, load)}" title="${date.getMonth() + 1}/${date.getDate()} · ${load} 条"></span>`;
  }).join("");
}

function renderTimelineMilestones(items) {
  const milestones = document.getElementById("timeline-milestones");
  if (!milestones) return;
  const completed = (state.tasks || []).filter((task) => task.status === "done").length;
  const diaries = (state.diaries || []).length;
  milestones.innerHTML = `
    <div class="milestone-item"><strong>${items.length}</strong><span>条时间线记录</span></div>
    <div class="milestone-item"><strong>${completed}</strong><span>项任务完成</span></div>
    <div class="milestone-item"><strong>${diaries}</strong><span>篇日记沉淀</span></div>`;
}

// ═══ View Switching ═══

function switchView(view) {
  currentView = view;
  document.querySelectorAll(".view").forEach((el) => el.classList.remove("active"));
  const viewEl = document.getElementById(`view-${view}`);
  if (viewEl) viewEl.classList.add("active");

  document.querySelectorAll(".nav-item").forEach((el) => el.classList.remove("active"));
  const navEl = document.getElementById(`nav-${view}`);
  if (navEl) navEl.classList.add("active");

  const titles = {
    dashboard: ["仪表盘", "今天的执行、写作和同步状态"],
    chat: ["AI 助手", "你的智能桌面助理"],
    tasks: ["待办任务", "管理你的待办事项"],
    diary: ["日记", "记录生活的点滴"],
    timeline: ["时间线", "任务、日记和里程碑回顾"],
    stats: ["统计", "数据概览与分析"],
    sync: ["互通同步", "连接 Android 端账号与云端备份"],
    settings: ["设置", "应用配置"],
  };
  const [title, sub] = titles[view] || ["", ""];
  document.getElementById("view-title").textContent = title;
  document.getElementById("view-subtitle").textContent = sub;

  if (view === "chat") {
    document.getElementById("chat-input").focus();
  }
  if (view === "dashboard") renderDashboardPage();
  if (view === "stats") renderStats();
  if (view === "timeline") renderTimelinePage();
}

// ═══ AI Chat ═══

let chatHistory = [];
let isProcessing = false;

async function sendChatMessage(text) {
  if (isProcessing || !text.trim()) return;
  isProcessing = true;

  const input = document.getElementById("chat-input");
  input.value = "";
  input.disabled = true;

  appendChatMessage("user", text);
  chatHistory.push({ role: "user", content: text });

  showTypingIndicator();

  try {
    const result = await API.sendChat({ messages: chatHistory });

    hideTypingIndicator();

    if (result.stateChanged) {
      await refreshState();
      renderTasks();
      renderDiaries();
      renderStats();
      renderBadge();
    }

    const content = result.content || "抱歉，我没有理解你的意思。";
    appendChatMessage("assistant", content);
    chatHistory.push({ role: "assistant", content });

    if (result.action === "ask_task_title") {
      setTimeout(() => {
        appendProactiveSuggestion("直接告诉我任务名称，比如「买牛奶」");
      }, 500);
    }
    if (result.action === "ask_diary_content") {
      setTimeout(() => {
        appendProactiveSuggestion("写下今天想记录的内容就好");
      }, 500);
    }
  } catch (err) {
    hideTypingIndicator();
    appendChatMessage("assistant", `出错了: ${err.message}`);
  }

  input.disabled = false;
  input.focus();
  isProcessing = false;
}

function appendChatMessage(role, content) {
  const container = document.getElementById("chat-messages");
  const div = document.createElement("div");
  div.className = `chat-message ${role}`;
  div.innerHTML = `
    <div class="chat-avatar ${role}">${role === "user" ? iconMarkup("user") : iconMarkup("bot")}</div>
    <div class="chat-bubble">${escapeHtml(content).replace(/\n/g, "<br>")}</div>`;
  container.appendChild(div);
  container.scrollTop = container.scrollHeight;
}

function showTypingIndicator() {
  const container = document.getElementById("chat-messages");
  const div = document.createElement("div");
  div.className = "chat-message assistant";
  div.id = "typing-indicator";
  div.innerHTML = `
    <div class="chat-avatar assistant">${iconMarkup("bot")}</div>
    <div class="chat-bubble">
      <div class="loading-dots">
        <span></span><span></span><span></span>
      </div>
    </div>`;
  container.appendChild(div);
  container.scrollTop = container.scrollHeight;
}

function hideTypingIndicator() {
  const el = document.getElementById("typing-indicator");
  if (el) el.remove();
}

function appendProactiveSuggestion(text) {
  const container = document.getElementById("chat-messages");
  const div = document.createElement("div");
  div.className = "chat-proactive";
  div.innerHTML = `${iconMarkup("idea")} <span>${escapeHtml(text)}</span>`;
  div.onclick = () => {
    document.getElementById("chat-input").value = text;
    document.getElementById("chat-input").focus();
  };
  container.appendChild(div);
  container.scrollTop = container.scrollHeight;
}

async function checkProactiveAgent() {
  try {
    const result = await API.getProactiveSuggestion();
    if (result && result.content) {
      setTimeout(() => {
        appendChatMessage("assistant", result.content);
        chatHistory.push({ role: "assistant", content: result.content, proactive: true });
      }, 600);
    }
  } catch {
    // ignore
  }
}

// ═══ Settings ═══

function loadSettings() {
  const s = state.settings || {};
  shortcutProfile = s.shortcutProfile || "default";
  applyVisualPreferences(s);
  setSegmentActive("setting-provider", "providerChoice", s.provider || "local");
  setSegmentActive("font-scale-control", "fontScale", String(s.fontScale || "1.1"));
  setSegmentActive("theme-mode-control", "themeMode", s.themeMode || "light");
  setSegmentActive("shortcut-profile-control", "shortcutProfile", shortcutProfile);
  document.querySelectorAll("[data-theme-choice]").forEach((btn) => {
    btn.classList.toggle("active", btn.dataset.themeChoice === (s.themeChoice || "fog"));
  });
  document.getElementById("setting-api-key").value = s.apiKey || "";
  document.getElementById("setting-endpoint").value = s.endpoint || "";
  document.getElementById("setting-model").value = s.model || "";
  toggleApiFields(s.provider || "local");
  renderShortcutSettings();
  renderNativeStatus();
}

function selectedSegmentValue(containerId, dataKey, fallback) {
  const active = document.querySelector(`#${containerId} .segment.active`);
  return active?.dataset[dataKey] || fallback;
}

function setSegmentActive(containerId, dataKey, value) {
  document.querySelectorAll(`#${containerId} .segment`).forEach((btn) => {
    btn.classList.toggle("active", btn.dataset[dataKey] === String(value));
  });
}

function toggleApiFields(provider) {
  const isLocal = provider === "local";
  document.getElementById("setting-api-key-row").style.display = isLocal ? "none" : "";
  document.getElementById("setting-endpoint-row").style.display = isLocal ? "none" : "";
  document.getElementById("setting-model-row").style.display = isLocal ? "none" : "";
}

function renderShortcutSettings() {
  const grid = document.getElementById("shortcut-settings-grid");
  if (!grid) return;
  const profiles = {
    default: [
      ["global-search", "全局搜索", "Ctrl+K", "跨任务、日记和命令快速定位"],
      ["new-task", "新建任务", "Ctrl+N", "打开任务规划子页面"],
      ["new-diary", "写日记", "Ctrl+Shift+D", "进入 Markdown 写作"],
      ["settings", "设置", "Ctrl+,", "打开偏好设置"]
    ],
    writer: [
      ["global-search", "全局搜索", "Ctrl+K", "查找素材和旧日记"],
      ["new-diary", "新建日记", "Ctrl+Shift+D", "直接进入写作"],
      ["writing-focus", "全屏写作", "Ctrl+Shift+F", "切换专注编辑器"],
      ["save-diary", "保存日记", "Ctrl+S", "保存当前 Markdown"]
    ],
    planner: [
      ["global-search", "全局搜索", "Ctrl+K", "检索任务和项目"],
      ["new-task", "任务规划", "Ctrl+N", "打开深度任务表单"],
      ["dashboard", "回到仪表盘", "Ctrl+1", "查看今日状态"],
      ["sync", "互通同步", "Ctrl+5", "检查手机端联通"]
    ]
  };
  const rows = profiles[shortcutProfile] || profiles.default;
  grid.innerHTML = rows.map(([action, label, keys, detail]) => `
    <button class="shortcut-row" data-shortcut-action="${action}">
      <span>
        <strong>${escapeHtml(label)}</strong>
        <small>${escapeHtml(detail)}</small>
      </span>
      <kbd>${escapeHtml(keys)}</kbd>
    </button>
  `).join("");
}

function collectShortcutPreferences() {
  return [...document.querySelectorAll("#shortcut-settings-grid [data-shortcut-action]")].map((row) => ({
    action: row.dataset.shortcutAction,
    keys: row.querySelector("kbd")?.textContent?.trim() || ""
  }));
}

// ═══ Markdown Writing Studio ═══

function showMarkdownEditor(editData) {
  switchView("diary");
  showDiaryWriter();
  activeDiaryId = editData?.id || null;
  const title = document.getElementById("markdown-editor-title");
  const body = document.getElementById("markdown-editor-body");
  if (!title || !body) return;
  title.value = editData?.title || "";
  body.value = editData?.text || "";
  renderMarkdownPreview();
  setTimeout(() => (editData ? body : title).focus(), 50);
}

function showDiaryWriter() {
  const shell = document.getElementById("diary-writer-shell");
  if (!shell) return;
  shell.classList.add("open");
  shell.setAttribute("aria-hidden", "false");
  setWritingLayout(writingLayout);
}

function hideDiaryWriter() {
  const shell = document.getElementById("diary-writer-shell");
  if (!shell) return;
  shell.classList.remove("open", "fullscreen");
  shell.setAttribute("aria-hidden", "true");
  const button = document.getElementById("btn-toggle-writing-focus");
  if (button) button.textContent = "全屏写作";
}

function toggleWritingFocus() {
  const shell = document.getElementById("diary-writer-shell");
  if (!shell) return;
  shell.classList.add("open");
  shell.setAttribute("aria-hidden", "false");
  const isFullscreen = shell.classList.toggle("fullscreen");
  const button = document.getElementById("btn-toggle-writing-focus");
  if (button) button.textContent = isFullscreen ? "退出全屏" : "全屏写作";
  document.getElementById("markdown-editor-body")?.focus();
}

function setWritingLayout(layout = "split") {
  writingLayout = ["split", "focus", "analysis"].includes(layout) ? layout : "split";
  const editor = document.getElementById("markdown-editor");
  if (!editor) return;
  editor.classList.remove("layout-split", "layout-focus", "layout-analysis");
  editor.classList.add(`layout-${writingLayout}`);
  setSegmentActive("writing-layout-control", "writingLayout", writingLayout);
}

function renderMarkdownPreview() {
  const body = document.getElementById("markdown-editor-body");
  const preview = document.getElementById("markdown-preview");
  const wordCount = document.getElementById("writing-word-count");
  const readTime = document.getElementById("writing-read-time");
  if (!body || !preview) return;
  const text = body.value || "";
  const words = text.replace(/\s+/g, "").length;
  if (wordCount) wordCount.textContent = `${words} 字`;
  if (readTime) readTime.textContent = `约 ${Math.max(1, Math.ceil(words / 450))} 分钟`;
  preview.innerHTML = markdownToHtml(text);
  renderWritingAnalysis();
  renderStudioStatus();
}

function markdownToHtml(text) {
  const lines = escapeHtml(text).split(/\n/);
  return lines.map((line) => {
    if (line.startsWith("### ")) return `<h3>${line.slice(4)}</h3>`;
    if (line.startsWith("## ")) return `<h2>${line.slice(3)}</h2>`;
    if (line.startsWith("# ")) return `<h1>${line.slice(2)}</h1>`;
    if (line.startsWith("- [ ] ")) return `<p class="markdown-task"><span class="markdown-check pending"></span>${line.slice(6)}</p>`;
    if (line.startsWith("- [x] ")) return `<p class="markdown-task done"><span class="markdown-check done"></span>${line.slice(6)}</p>`;
    if (line.startsWith("- ")) return `<p class="markdown-list"><span class="markdown-bullet"></span>${line.slice(2)}</p>`;
    if (line.startsWith("> ")) return `<blockquote>${line.slice(2)}</blockquote>`;
    return line ? `<p>${line.replace(/\*\*(.*?)\*\*/g, "<strong>$1</strong>")}</p>` : `<br>`;
  }).join("");
}

async function saveMarkdownDiary() {
  const title = document.getElementById("markdown-editor-title").value.trim() || "无标题";
  const text = document.getElementById("markdown-editor-body").value.trim();
  if (!text) {
    showToast("请先写下日记内容", "error");
    return;
  }
  let savedDiaryId = activeDiaryId;
  await mutate(async () => {
    if (activeDiaryId) {
      await API.updateDiaryEntry(activeDiaryId, { title, text, mood: "neutral", tags: [] });
    } else {
      const nextState = await API.createDiaryEntry({ title, text, mood: "neutral", tags: [] });
      savedDiaryId = nextState.diaries?.[0]?.id || null;
    }
  });
  if (savedDiaryId && pendingAttachments.length) {
    await saveAttachmentReferences(savedDiaryId);
  }
  activeDiaryId = null;
  pendingAttachments = [];
  renderAttachmentSummary();
  showToast("日记已保存", "success");
}

function insertDiaryTemplate(templateId) {
  const templates = {
    "daily-review": "# 每日复盘\n\n## 今天完成\n- [ ] \n\n## 关键阻塞\n\n## 明天第一步\n- [ ] ",
    "weekly-review": "# 周度回顾\n\n## 本周推进\n\n## 反复出现的问题\n\n## 下周计划\n- [ ] ",
    "decision-log": "# 决策记录\n\n## 背景\n\n## 选项\n\n## 决定\n\n## 后续验证\n- [ ] "
  };
  const body = document.getElementById("markdown-editor-body");
  if (!body) return;
  const template = templates[templateId] || templates["daily-review"];
  const prefix = body.value.trim() ? "\n\n" : "";
  body.value = `${body.value}${prefix}${template}`;
  renderMarkdownPreview();
  body.focus();
}

// ═══ Event Binding ═══

function bindEvents() {
  // ── Sidebar Navigation ──
  document.querySelectorAll(".nav-item").forEach((el) => {
    el.addEventListener("click", () => switchView(el.dataset.view));
  });
  bindContextMenu();
  bindAttachmentDropZone();

  document.querySelectorAll("[data-dashboard-action]").forEach((button) => {
    button.addEventListener("click", () => {
      if (button.dataset.dashboardAction === "open-search") openGlobalSearch();
      if (button.dataset.dashboardAction === "new-task") {
        switchView("tasks");
        showTaskPlanner();
      }
    });
  });
  document.getElementById("dashboard-smart-folders")?.addEventListener("click", (event) => {
    const item = event.target.closest("[data-smart-folder]");
    if (!item) return;
    applySmartFolder(item.dataset.smartFolder);
  });
  document.getElementById("dashboard-ai-rail")?.addEventListener("click", (event) => {
    const action = event.target.closest("[data-ai-dashboard-prompt]");
    if (!action) return;
    const input = document.getElementById("chat-input");
    if (input) input.value = action.dataset.aiDashboardPrompt || "";
    switchView("chat");
    input?.focus();
  });
  document.getElementById("desktop-readiness-list")?.addEventListener("click", (event) => {
    const item = event.target.closest("[data-readiness-action]");
    if (!item) return;
    const action = item.dataset.readinessAction;
    if (action === "sync") switchView("sync");
    if (action === "shortcuts") switchView("settings");
    if (action === "import") {
      switchView("settings");
      document.getElementById("data-import-center")?.classList.add("open");
      document.getElementById("data-import-center")?.setAttribute("aria-hidden", "false");
    }
  });

  // ── Chat ──
  const chatInput = document.getElementById("chat-input");
  const chatSend = document.getElementById("chat-send");
  chatSend.addEventListener("click", () => sendChatMessage(chatInput.value));
  chatInput.addEventListener("keydown", (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      sendChatMessage(chatInput.value);
    }
  });

  // ── Tasks ──
  document.getElementById("task-capture-btn").addEventListener("click", captureTask);
  document.getElementById("task-capture-input").addEventListener("keydown", (e) => {
    if (e.key === "Enter") captureTask();
  });
  document.getElementById("btn-open-task-planner").addEventListener("click", () => showTaskPlanner());
  document.getElementById("btn-close-task-planner").addEventListener("click", () => hideTaskPlanner());
  document.getElementById("btn-save-task-planner").addEventListener("click", saveTaskPlanner);
  document.querySelectorAll("[data-priority-choice]").forEach((btn) => {
    btn.addEventListener("click", () => {
      document.querySelectorAll("[data-priority-choice]").forEach((b) => b.classList.remove("active"));
      btn.classList.add("active");
      selectedTaskPriority = Number(btn.dataset.priorityChoice) || 0;
    });
  });

  document.querySelectorAll("[data-filter]").forEach((btn) => {
    btn.addEventListener("click", () => {
      document.querySelectorAll("[data-filter]").forEach((b) => b.classList.remove("active"));
      btn.classList.add("active");
      taskFilter = btn.dataset.filter;
      renderTasks();
    });
  });
  document.getElementById("task-list").addEventListener("click", async (e) => {
    await handleTaskAction(e);
  });
  document.getElementById("task-lanes").addEventListener("click", async (e) => {
    await handleTaskAction(e);
  });
  document.getElementById("kanban-board").addEventListener("click", async (e) => {
    await handleTaskAction(e);
  });
  document.getElementById("btn-bulk-clear").addEventListener("click", () => {
    selectedTaskIds = new Set();
    renderTaskPage();
  });
  document.getElementById("btn-bulk-doing").addEventListener("click", () => runBulkTaskAction("doing"));
  document.getElementById("btn-bulk-complete").addEventListener("click", () => runBulkTaskAction("complete"));
  document.getElementById("btn-bulk-delete").addEventListener("click", () => runBulkTaskAction("delete"));

  async function handleTaskAction(e) {
    const target = e.target.closest("[data-action]");
    if (!target) return;
    const id = target.dataset.id;
    const action = target.dataset.action;
    if (action === "select") {
      if (selectedTaskIds.has(id)) selectedTaskIds.delete(id);
      else selectedTaskIds.add(id);
      renderTaskPage();
    } else if (action === "toggle") {
      const task = (state.tasks || []).find((t) => t.id === id);
      if (task && task.status === "done") {
        await mutate(() => API.updateTask(id, { status: "active" }));
      } else {
        await mutate(() => API.completeTask(id));
      }
    } else if (action === "delete") {
      await mutate(() => API.deleteTask(id));
      showToast("已删除任务", "info");
    }
  }

  // ── Diary ──
  document.getElementById("btn-open-diary-writer").addEventListener("click", () => showMarkdownEditor(null));
  document.getElementById("btn-close-diary-writer").addEventListener("click", hideDiaryWriter);
  document.getElementById("btn-toggle-writing-focus").addEventListener("click", toggleWritingFocus);
  document.querySelectorAll("[data-writing-layout]").forEach((button) => {
    button.addEventListener("click", () => setWritingLayout(button.dataset.writingLayout));
  });
  document.getElementById("markdown-editor-title").addEventListener("keydown", (event) => {
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "s") {
      event.preventDefault();
      saveMarkdownDiary();
    }
  });
  document.getElementById("markdown-editor-body").addEventListener("input", renderMarkdownPreview);
  document.getElementById("markdown-editor-body").addEventListener("keydown", (event) => {
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "s") {
      event.preventDefault();
      saveMarkdownDiary();
    }
  });
  document.querySelectorAll("[data-template-id]").forEach((button) => {
    button.addEventListener("click", () => insertDiaryTemplate(button.dataset.templateId));
  });
  document.querySelectorAll("[data-ai-action]").forEach((button) => {
    button.addEventListener("click", () => handleAiContextAction(button.dataset.aiAction));
  });
  bindSyncAccountEvents();
  bindThemeControls();
  document.getElementById("btn-global-search").addEventListener("click", openGlobalSearch);
  document.getElementById("global-search-input").addEventListener("input", (event) => {
    renderGlobalSearchResults(event.target.value);
  });
  document.getElementById("global-search-overlay").addEventListener("click", (event) => {
    if (event.target.id === "global-search-overlay") closeGlobalSearch();
  });
  document.getElementById("global-search-results").addEventListener("click", (event) => {
    const item = event.target.closest("[data-search-view]");
    if (!item) return;
    const action = item.dataset.searchAction;
    closeGlobalSearch();
    switchView(item.dataset.searchView);
    if (action === "task-planner") showTaskPlanner();
    if (action === "new-diary") showMarkdownEditor(null);
  });

  document.getElementById("diary-list").addEventListener("click", (e) => {
    const entry = e.target.closest(".diary-entry");
    if (entry) {
      const id = entry.dataset.id;
      const diary = (state.diaries || []).find((d) => d.id === id);
      if (diary) showMarkdownEditor(diary);
    }
  });

  // ── Timeline ──
  document.getElementById("timeline-prev").addEventListener("click", () => {
    timelineDate.setMonth(timelineDate.getMonth() - 1);
    renderTimelinePage();
  });
  document.getElementById("timeline-next").addEventListener("click", () => {
    timelineDate.setMonth(timelineDate.getMonth() + 1);
    renderTimelinePage();
  });
  document.getElementById("btn-timeline-today").addEventListener("click", () => {
    timelineDate = new Date();
    renderTimelinePage();
  });
  // ── Settings ──
  document.querySelectorAll("[data-provider-choice]").forEach((btn) => {
    btn.addEventListener("click", () => {
      setSegmentActive("setting-provider", "providerChoice", btn.dataset.providerChoice);
      toggleApiFields(btn.dataset.providerChoice);
    });
  });
  document.querySelectorAll("[data-export-format]").forEach((btn) => {
    btn.addEventListener("click", () => {
      setSegmentActive("export-format-control", "exportFormat", btn.dataset.exportFormat);
      selectedExportFormat = btn.dataset.exportFormat;
    });
  });
  document.querySelectorAll("[data-import-format]").forEach((btn) => {
    btn.addEventListener("click", () => {
      setSegmentActive("import-format-control", "importFormat", btn.dataset.importFormat);
      selectedImportFormat = btn.dataset.importFormat;
    });
  });
  document.getElementById("btn-open-import-center").addEventListener("click", () => {
    const center = document.getElementById("data-import-center");
    center?.classList.toggle("open");
    center?.setAttribute("aria-hidden", center.classList.contains("open") ? "false" : "true");
  });
  document.querySelectorAll("[data-notification-choice]").forEach((btn) => {
    btn.addEventListener("click", () => {
      setSegmentActive("notification-control", "notificationChoice", btn.dataset.notificationChoice);
      renderNativeStatus();
    });
  });
  document.querySelectorAll("[data-shortcut-profile]").forEach((btn) => {
    btn.addEventListener("click", () => {
      shortcutProfile = btn.dataset.shortcutProfile || "default";
      setSegmentActive("shortcut-profile-control", "shortcutProfile", shortcutProfile);
      renderShortcutSettings();
    });
  });
  document.querySelectorAll("[data-native-action]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const action = btn.dataset.nativeAction;
      if (action === "phone") switchView("sync");
      if (action === "export") document.getElementById("export-format-control")?.scrollIntoView({ behavior: "smooth", block: "center" });
      if (action === "tray") showToast("系统托盘和全局热键已由桌面主进程托管", "info");
    });
  });

  document.getElementById("btn-save-settings").addEventListener("click", async () => {
    const settings = {
      provider: selectedSegmentValue("setting-provider", "providerChoice", "local"),
      apiKey: document.getElementById("setting-api-key").value,
      endpoint: document.getElementById("setting-endpoint").value,
      model: document.getElementById("setting-model").value,
      themeChoice: document.querySelector("[data-theme-choice].active")?.dataset.themeChoice || "fog",
      themeMode: selectedSegmentValue("theme-mode-control", "themeMode", "light"),
      fontScale: selectedSegmentValue("font-scale-control", "fontScale", "1.1"),
      notificationChoice: selectedSegmentValue("notification-control", "notificationChoice", "deadline"),
      shortcutProfile: selectedSegmentValue("shortcut-profile-control", "shortcutProfile", shortcutProfile),
      shortcuts: collectShortcutPreferences(),
    };
    await mutate(() => API.saveSettings(settings));
    showToast("设置已保存", "success");
  });

  document.getElementById("btn-test-ai").addEventListener("click", async () => {
    const settings = {
      provider: selectedSegmentValue("setting-provider", "providerChoice", "local"),
      apiKey: document.getElementById("setting-api-key").value,
      endpoint: document.getElementById("setting-endpoint").value,
      model: document.getElementById("setting-model").value,
    };
    const result = await API.testAiConnection(settings);
    showToast(result.message, result.ok ? "success" : "error");
  });

  document.getElementById("btn-export").addEventListener("click", async () => {
    const result = await API.exportData(collectExportFormat());
    if (!result.canceled) showToast("数据已导出", "success");
  });

  document.getElementById("btn-import").addEventListener("click", async () => {
    await mutate(() => API.importData(collectImportFormat()));
    showToast("数据已导入", "success");
  });

  // ── Sync ──
  document.getElementById("btn-sync-now").addEventListener("click", async () => {
    await doSync();
  });
  document.getElementById("btn-force-sync").addEventListener("click", async () => {
    await doSync();
  });
}

function collectExportFormat() {
  selectedExportFormat = selectedSegmentValue("export-format-control", "exportFormat", selectedExportFormat);
  return selectedExportFormat;
}

function collectImportFormat() {
  selectedImportFormat = selectedSegmentValue("import-format-control", "importFormat", selectedImportFormat);
  return selectedImportFormat;
}

function bindContextMenu() {
  const menu = document.getElementById("desktop-context-menu");
  if (!menu || menu.dataset.bound) return;
  menu.dataset.bound = "true";
  document.addEventListener("contextmenu", (event) => {
    const target = event.target.closest(".task-card, .kanban-card, .diary-entry, .timeline-item");
    if (!target) return;
    event.preventDefault();
    showDesktopContextMenu(event.clientX, event.clientY, target.dataset.id || "");
  });
  document.addEventListener("click", () => hideDesktopContextMenu());
  menu.addEventListener("click", (event) => {
    const action = event.target.closest("[data-context-action]")?.dataset.contextAction;
    if (!action) return;
    showToast(`已选择${event.target.textContent}`, "info");
    hideDesktopContextMenu();
  });
}

function showDesktopContextMenu(x, y, targetId = "") {
  const menu = document.getElementById("desktop-context-menu");
  if (!menu) return;
  menu.dataset.targetId = targetId;
  menu.style.left = `${x}px`;
  menu.style.top = `${y}px`;
  menu.classList.add("open");
  menu.setAttribute("aria-hidden", "false");
}

function hideDesktopContextMenu() {
  const menu = document.getElementById("desktop-context-menu");
  if (!menu) return;
  menu.classList.remove("open");
  menu.setAttribute("aria-hidden", "true");
}

function bindAttachmentDropZone() {
  const zone = document.getElementById("attachment-drop-zone");
  if (!zone || zone.dataset.bound) return;
  zone.dataset.bound = "true";
  const activate = () => zone.classList.add("dragging");
  const deactivate = () => zone.classList.remove("dragging");
  zone.addEventListener("dragover", (event) => {
    event.preventDefault();
    activate();
  });
  zone.addEventListener("dragleave", deactivate);
  zone.addEventListener("drop", (event) => {
    event.preventDefault();
    deactivate();
    pendingAttachments = [...event.dataTransfer.files].map((file) => ({
      name: file.name,
      path: file.path || "",
      size: file.size || 0,
      type: file.type || ""
    })).slice(0, 6);
    renderAttachmentSummary();
  });
}

async function saveAttachmentReferences(diaryId) {
  if (!diaryId || !pendingAttachments.length) return;
  await API.attachFilesToDiary(diaryId, pendingAttachments);
  await refreshState();
}

function renderAttachmentSummary() {
  const summary = document.getElementById("attachment-summary");
  if (!summary) return;
  const names = pendingAttachments.map((file) => file.name).filter(Boolean);
  summary.textContent = names.length
    ? `已挂载 ${names.length} 个附件：${names.join("、")}`
    : "支持把设计稿、截图、Markdown 文件作为本地线索挂到当前日记。";
}

function handleAiContextAction(action) {
  const body = document.getElementById("markdown-editor-body")?.value || "";
  const prompts = {
    summarize: "请总结当前日记的主线和情绪变化。",
    "extract-tasks": "请从当前内容提取可以执行的待办。",
    rewrite: "请把当前内容整理成更清晰的复盘。"
  };
  const input = document.getElementById("chat-input");
  if (input) input.value = `${prompts[action] || prompts.summarize}\n\n${body.slice(0, 800)}`;
  switchView("chat");
}

function bindThemeControls() {
  document.querySelectorAll("[data-theme-choice]").forEach((btn) => {
    btn.addEventListener("click", () => {
      document.querySelectorAll("[data-theme-choice]").forEach((b) => b.classList.remove("active"));
      btn.classList.add("active");
      applyVisualPreferences({ ...(state.settings || {}), themeChoice: btn.dataset.themeChoice });
    });
  });
  document.querySelectorAll("[data-theme-mode]").forEach((btn) => {
    btn.addEventListener("click", () => {
      setSegmentActive("theme-mode-control", "themeMode", btn.dataset.themeMode);
      applyVisualPreferences({ ...(state.settings || {}), themeMode: btn.dataset.themeMode });
    });
  });
  document.querySelectorAll("[data-font-scale]").forEach((btn) => {
    btn.addEventListener("click", () => {
      setSegmentActive("font-scale-control", "fontScale", btn.dataset.fontScale);
      applyVisualPreferences({ ...(state.settings || {}), fontScale: btn.dataset.fontScale });
    });
  });
  window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", () => {
    applyVisualPreferences(state.settings || {});
  });
}

function openGlobalSearch() {
  const overlay = document.getElementById("global-search-overlay");
  overlay.classList.add("open");
  overlay.setAttribute("aria-hidden", "false");
  const input = document.getElementById("global-search-input");
  input.value = "";
  renderGlobalSearchResults("");
  setTimeout(() => input.focus(), 50);
}

function closeGlobalSearch() {
  const overlay = document.getElementById("global-search-overlay");
  overlay.classList.remove("open");
  overlay.setAttribute("aria-hidden", "true");
}

function renderGlobalSearchResults(query = "") {
  const results = document.getElementById("global-search-results");
  const q = query.trim().toLowerCase();
  const commands = [
    { title: "打开任务规划器", meta: "Ctrl+N", view: "tasks", action: "task-planner" },
    { title: "写一篇日记", meta: "Ctrl+Shift+D", view: "diary", action: "new-diary" },
    { title: "查看时间线页面", meta: "Timeline", view: "timeline" },
    { title: "打开设置", meta: "Ctrl+,", view: "settings" }
  ];
  const taskItems = (state.tasks || []).map((task) => ({ title: task.title, meta: "待办", view: "tasks" }));
  const diaryItems = (state.diaries || []).map((diary) => ({ title: diary.title || "无标题日记", meta: "日记", view: "diary" }));
  const items = [...commands, ...taskItems, ...diaryItems]
    .filter((item) => !q || `${item.title} ${item.meta}`.toLowerCase().includes(q))
    .slice(0, 8);
  results.innerHTML = items.length
    ? items.map((item) => `<button class="global-search-item" data-search-view="${item.view}" ${item.action ? `data-search-action="${item.action}"` : ""}><strong>${escapeHtml(item.title)}</strong><span>${escapeHtml(item.meta)}</span></button>`).join("")
    : `<div class="agenda-empty">没有匹配内容。试试搜索任务标题、日记标题，或直接输入命令。</div>`;
}

function bindGlobalShortcuts() {
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") {
      closeGlobalSearch();
      hideTaskPlanner();
    }
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "k") {
      event.preventDefault();
      openGlobalSearch();
    }
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "n") {
      event.preventDefault();
      switchView("tasks");
      showTaskPlanner();
    }
    if ((event.ctrlKey || event.metaKey) && event.shiftKey && event.key.toLowerCase() === "d") {
      event.preventDefault();
      switchView("diary");
      showMarkdownEditor(null);
    }
    if ((event.ctrlKey || event.metaKey) && event.key === ",") {
      event.preventDefault();
      switchView("settings");
    }
  });
}

function renderStudioStatus() {
  const wordCount = document.getElementById("status-word-count");
  const syncState = document.getElementById("status-sync-state");
  const text = document.getElementById("markdown-editor-body")?.value || "";
  if (wordCount) wordCount.textContent = `字数 ${text.replace(/\s+/g, "").length}`;
  if (syncState) syncState.textContent = state.account?.token ? "同步已连接" : "同步未登录";
}

async function captureTask() {
  const input = document.getElementById("task-capture-input");
  const text = input.value.trim();
  if (!text) return;
  input.value = "";
  await mutate(async () => {
    await API.captureTasks(text);
  });
  showToast("已添加待办", "success");
}

function showTaskPlanner() {
  const panel = document.getElementById("task-planner-panel");
  panel.classList.add("open");
  panel.setAttribute("aria-hidden", "false");
  document.getElementById("task-planner-title").focus();
}

function hideTaskPlanner() {
  const panel = document.getElementById("task-planner-panel");
  panel.classList.remove("open");
  panel.setAttribute("aria-hidden", "true");
}

async function saveTaskPlanner() {
  const title = document.getElementById("task-planner-title").value.trim();
  if (!title) {
    showToast("请先填写任务标题", "error");
    return;
  }
  const due = document.getElementById("task-planner-due").value;
  const tags = document.getElementById("task-planner-tags").value.split(/[,，]/).map((s) => s.trim()).filter(Boolean);
  const description = document.getElementById("task-planner-description").value.trim();
  await mutate(() => API.addTask({
    title,
    description,
    priority: selectedTaskPriority,
    dueAt: due ? new Date(due).toISOString() : null,
    tags,
    status: due ? "planned" : "inbox",
    source: "planner"
  }));
  ["task-planner-title", "task-planner-due", "task-planner-tags", "task-planner-description"].forEach((id) => {
    document.getElementById(id).value = "";
  });
  selectedTaskPriority = 0;
  setSegmentActive("task-priority-control", "priorityChoice", "0");
  hideTaskPlanner();
  showToast("深度待办已保存", "success");
}

function bindSyncAccountEvents() {
  const login = document.getElementById("btn-sync-login");
  const register = document.getElementById("btn-sync-register");
  const logout = document.getElementById("btn-sync-logout");
  if (!login || login.dataset.bound) return;
  login.dataset.bound = "true";
  register.dataset.bound = "true";
  logout.dataset.bound = "true";

  login.addEventListener("click", () => submitSyncAccount("login"));
  register.addEventListener("click", () => submitSyncAccount("register"));
  logout.addEventListener("click", async () => {
    const result = await API.syncLogout();
    if (result.state) state = result.state;
    await refreshState();
    renderAll();
    showToast("已断开同步账号", "info");
  });
}

async function submitSyncAccount(mode) {
  const input = {
    phone: document.getElementById("sync-phone").value.trim(),
    pin: document.getElementById("sync-pin").value.trim(),
    endpoint: document.getElementById("sync-endpoint").value.trim(),
    deviceName: "DiaryApp Desktop"
  };
  const action = mode === "register" ? API.syncRegister : API.syncLogin;
  const result = await action(input);
  if (!result.ok) {
    showToast(result.message || "连接失败", "error");
    return;
  }
  if (result.state) state = result.state;
  await refreshState();
  document.getElementById("sync-pin").value = "";
  renderAll();
  showToast(mode === "register" ? "注册并连接成功" : "登录并连接成功", "success");
}

async function doSync() {
  const status = document.getElementById("sync-status");
  status.className = "sync-indicator syncing";
  document.getElementById("sync-text").textContent = "同步中...";
  try {
    const result = await API.syncNow();
    await refreshState();
    status.className = "sync-indicator synced";
    document.getElementById("sync-text").textContent = "同步完成";
    showToast(result.message || "同步成功", "success");
    renderAll();
  } catch (error) {
    status.className = "sync-indicator error";
    document.getElementById("sync-text").textContent = "同步失败";
    showToast(error.message || "同步失败，请稍后重试", "error");
  }
  setTimeout(() => renderSyncStatus(), 3000);
}

// ═══ Utilities ═══

function escapeHtml(text) {
  if (!text) return "";
  const div = document.createElement("div");
  div.textContent = text;
  return div.innerHTML;
}
