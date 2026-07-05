import { createDesktopState } from "./taskEngine.js";

const DAY = 24 * 60 * 60 * 1000;
const DEFAULT_SYNC_ENDPOINT = "https://diary-app-sync.2453759261.workers.dev";

// ── Core State ──
export function createInitialState(input = {}) {
  return createDesktopState(input);
}

// ── Settings ──
export function saveSettings(state, patch) {
  const current = state.settings || {};
  return {
    ...state,
    settings: { ...current, ...patch }
  };
}

export function getSettings(state) {
  return state.settings || {};
}

// ── Phone Login + Cloud Sync ──
export async function loginSyncAccount(state, input, options = {}) {
  return authenticateSyncAccount(state, "/api/login", input, options);
}

export async function registerSyncAccount(state, input, options = {}) {
  return authenticateSyncAccount(state, "/api/register", input, options);
}

export function logoutSyncAccount(state, now = Date.now()) {
  return {
    ...state,
    account: {
      ...(state.account || {}),
      status: "guest",
      phone: "",
      maskedPhone: "",
      token: "",
      boundAt: null,
      unboundAt: now
    }
  };
}

export async function syncWithCloud(state, options = {}) {
  const account = state.account || {};
  if (account.status !== "linked" || !account.token) {
    return { ok: false, state, message: "请先使用手机号和 PIN 登录同步账号" };
  }

  const endpoint = normalizeEndpoint(account.syncEndpoint || DEFAULT_SYNC_ENDPOINT);
  const fetcher = options.fetcher || globalThis.fetch;
  const now = options.now ?? Date.now();
  if (typeof fetcher !== "function") {
    return { ok: false, state, message: "当前环境不支持网络同步" };
  }

  const payload = {
    data: {
      version: 2,
      exportedAt: new Date(now).toISOString(),
      syncMeta: {
        source: "desktop",
        phone: account.maskedPhone || maskPhone(account.phone),
        deviceId: account.deviceId || createDeviceId(),
        deviceName: account.deviceName || "DiaryApp Desktop",
        pushedAt: now
      },
      tasks: state.tasks || [],
      diaries: state.diaries || [],
      aiDrafts: state.aiDrafts || [],
      archive: state.archive || []
    }
  };

  try {
    const response = await fetcher(`${endpoint}/api/backup`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${account.token}`
      },
      body: JSON.stringify(payload)
    });
    const parsed = await parseJsonResponse(response);
    if (!response.ok || parsed.error) {
      return { ok: false, state, message: parsed.error || `同步失败 (${response.status})` };
    }

    const nextState = {
      ...state,
      lastSync: now,
      account: {
        ...account,
        status: "linked",
        syncEndpoint: endpoint,
        lastSyncAt: now,
        lastSyncDirection: "push",
        lastSyncDeviceId: payload.data.syncMeta.deviceId,
        lastSyncSummary: {
          message: parsed.message || "同步成功",
          version: parsed.version ?? null,
          taskCount: payload.data.tasks.length,
          diaryCount: payload.data.diaries.length
        }
      }
    };
    return { ok: true, state: nextState, message: parsed.message || "同步成功" };
  } catch (error) {
    return { ok: false, state, message: error.message || "同步失败" };
  }
}

// ── Tasks ──
export function addTask(state, input, now = Date.now()) {
  const task = normalizeTask(input, now);
  if (!task.title) return state;
  return { ...state, tasks: [task, ...(state.tasks || [])] };
}

export function updateTask(state, taskId, patch, now = Date.now()) {
  return {
    ...state,
    tasks: (state.tasks || []).map((t) =>
      t.id === taskId ? { ...t, ...patch, updatedAt: now } : t
    )
  };
}

export function deleteTask(state, taskId) {
  return {
    ...state,
    tasks: (state.tasks || []).filter((t) => t.id !== taskId)
  };
}

export function completeTask(state, taskId, now = Date.now()) {
  return updateTask(state, taskId, { status: "done", completedAt: now }, now);
}

export function listTasks(state, filter = "active") {
  const tasks = state.tasks || [];
  if (filter === "all") return tasks;
  if (filter === "active") return tasks.filter((t) => t.status !== "done");
  if (filter === "done") return tasks.filter((t) => t.status === "done");
  return tasks;
}

export function captureTasks(state, text, now = Date.now()) {
  const lines = String(text ?? "").split("\n").map((l) => l.trim()).filter(Boolean);
  const tasks = lines.map((line, i) => {
    let title = line;
    let priority = 0;
    let tags = [];

    // Priority markers
    if (title.startsWith("!!")) { priority = 2; title = title.slice(2).trim(); }
    else if (title.startsWith("!")) { priority = 1; title = title.slice(1).trim(); }

    // Tags
    const tagMatches = title.matchAll(/#([\p{L}\p{N}_-]+)/gu);
    tags = Array.from(tagMatches, (m) => m[1]);
    title = title.replace(/#[\p{L}\p{N}_-]+/gu, "").trim();

    // Due date parsing
    let dueAt = null;
    const dayMatch = title.match(/^(今天|明天|后天)\s*/);
    const timeMatch = title.match(/\b(\d{1,2}):(\d{2})\b/);
    if (dayMatch) {
      const offset = { "今天": 0, "明天": 1, "后天": 2 }[dayMatch[1]];
      const base = new Date(now + offset * DAY);
      const hour = timeMatch ? Number(timeMatch[1]) : 20;
      const minute = timeMatch ? Number(timeMatch[2]) : 0;
      base.setHours(hour, minute, 0, 0);
      dueAt = base.toISOString();
      title = title.replace(dayMatch[0], "");
    }
    if (timeMatch) title = title.replace(timeMatch[0], "");

    title = title.replace(/\s+/g, " ").trim();
    if (!title) return null;

    return normalizeTask({ title, priority, tags, dueAt, source: "capture" }, now + i);
  }).filter(Boolean);

  if (!tasks.length) return state;
  return { ...state, tasks: [...tasks, ...(state.tasks || [])] };
}

// ── Diary ──
export function createDiaryEntry(state, input, now = Date.now()) {
  const entry = {
    id: input.id || `diary-${now}-${randomSuffix()}`,
    title: String(input.title || "").trim() || `${new Date(now).toLocaleDateString("zh-CN")} 的日记`,
    text: String(input.text || "").trim(),
    mood: input.mood || "neutral",
    tags: Array.isArray(input.tags) ? input.tags : String(input.tags || "").split(/[,，]/).map((s) => s.trim()).filter(Boolean),
    attachments: Array.isArray(input.attachments) ? input.attachments : [],
    source: input.source || "manual",
    sourceName: input.sourceName || "",
    createdAt: input.createdAt || now,
    updatedAt: now
  };
  if (!entry.text) return state;
  return { ...state, diaries: [entry, ...(state.diaries || [])] };
}

export function listDiaries(state) {
  return (state.diaries || []).sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0));
}

export function updateDiaryEntry(state, diaryId, patch, now = Date.now()) {
  return {
    ...state,
    diaries: (state.diaries || []).map((d) =>
      d.id === diaryId ? { ...d, ...patch, updatedAt: now } : d
    )
  };
}

export function attachFilesToDiary(state, diaryId, files = [], now = Date.now()) {
  const normalized = normalizeAttachments(files, now);
  if (!normalized.length) return state;
  return {
    ...state,
    diaries: (state.diaries || []).map((diary) => {
      if (diary.id !== diaryId) return diary;
      return {
        ...diary,
        attachments: [...(diary.attachments || []), ...normalized],
        updatedAt: now
      };
    })
  };
}

export function searchDiaries(state, query) {
  const q = String(query || "").toLowerCase();
  if (!q) return listDiaries(state);
  return (state.diaries || []).filter((d) =>
    (d.title || "").toLowerCase().includes(q) ||
    (d.text || "").toLowerCase().includes(q) ||
    (d.tags || []).some((t) => t.toLowerCase().includes(q))
  ).sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0));
}

export function createDraftsFromDiary(state, diaryId) {
  const diary = (state.diaries || []).find((d) => d.id === diaryId);
  if (!diary) return state;
  const draft = {
    title: `整理: ${diary.title}`,
    reason: "从日记内容提取",
    sourceId: diary.id
  };
  return {
    ...state,
    aiDrafts: [draft, ...(state.aiDrafts || [])]
  };
}

// ── Data Summary ──
export function getDataSummary(state) {
  const tasks = state.tasks || [];
  const diaries = state.diaries || [];
  const activeTasks = tasks.filter((t) => t.status !== "done");
  const doneTasks = tasks.filter((t) => t.status === "done");
  return {
    totalTasks: tasks.length,
    activeTasks: activeTasks.length,
    doneTasks: doneTasks.length,
    totalDiaries: diaries.length,
    todayDiaries: diaries.filter((d) => {
      const dDate = new Date(d.createdAt).toDateString();
      return dDate === new Date().toDateString();
    }).length
  };
}

// ── Export / Import ──
export function exportState(state) {
  return JSON.stringify({
    version: 2,
    exportedAt: new Date().toISOString(),
    state: {
      tasks: state.tasks || [],
      diaries: state.diaries || [],
      aiDrafts: state.aiDrafts || [],
      archive: state.archive || [],
      account: sanitizeAccount(state.account || {}),
      settings: sanitizeSettings(state.settings || {})
    }
  }, null, 2);
}

export function importState(state, payload) {
  try {
    const parsed = typeof payload === "string" ? JSON.parse(payload) : payload;
    const data = parsed.state || parsed;
    return {
      ...createDesktopState({
        tasks: data.tasks || [],
        aiDrafts: data.aiDrafts || [],
        archive: data.archive || [],
        account: data.account || {},
        settings: data.settings || {}
      }),
      diaries: data.diaries || [],
    };
  } catch {
    return state;
  }
}

export function importMarkdownArchive(state, files = [], now = Date.now()) {
  const entries = (files || [])
    .map((file, index) => markdownFileToDiary(file, now + index))
    .filter(Boolean);
  if (!entries.length) return state;
  return {
    ...state,
    diaries: [...entries, ...(state.diaries || [])]
  };
}

export function importExternalDiaryArchive(state, files = [], source = "day-one", now = Date.now()) {
  const entries = (files || [])
    .flatMap((file, fileIndex) => parseExternalDiaryFile(file, source, now + fileIndex * 1000))
    .filter(Boolean);
  if (!entries.length) return state;
  return {
    ...state,
    diaries: [...entries, ...(state.diaries || [])]
  };
}

export function buildMonthlyReport(state, input = {}) {
  const nowDate = input.now ? new Date(input.now) : new Date();
  const year = Number(input.year) || nowDate.getFullYear();
  const month = Number(input.month) || nowDate.getMonth() + 1;
  const start = new Date(year, month - 1, 1).getTime();
  const end = new Date(year, month, 1).getTime();
  const diaries = (state.diaries || []).filter((diary) => inRange(diary.createdAt, start, end));
  const tasks = (state.tasks || []).filter((task) => {
    const anchor = task.completedAt || task.updatedAt || task.createdAt || (task.dueAt ? new Date(task.dueAt).getTime() : null);
    return inRange(anchor, start, end);
  });
  const completedTasks = tasks.filter((task) => task.status === "done").length;
  const openTasks = tasks.filter((task) => task.status !== "done").length;
  const wordCount = diaries.reduce((sum, diary) => sum + String(diary.text || "").replace(/\s+/g, "").length, 0);
  const topTags = collectTopTags([...diaries, ...tasks]);
  const periodLabel = `${year}年${month}月`;
  const focusTag = topTags[0]?.tag || "当前主题";
  const recommendations = [
    openTasks ? `还有 ${openTasks} 项未完成，建议先清理 Inbox 和临近截止事项。` : "本月任务闭环不错，可以整理经验模板。",
    diaries.length ? `围绕「${focusTag}」做一次主题复盘，提炼可复用流程。` : "本月写作样本不足，建议先补一篇月度回顾。",
    wordCount >= 1200 ? "写作素材充足，可以沉淀为长文或决策档案。" : "记录篇幅偏轻，下一阶段可以补充背景、判断和下一步。"
  ];
  return {
    periodLabel,
    diaryCount: diaries.length,
    taskCount: tasks.length,
    completedTasks,
    openTasks,
    wordCount,
    topTags,
    summary: `${periodLabel}写作 ${diaries.length} 篇，完成 ${completedTasks} 项任务，还有 ${openTasks} 项待推进。`,
    recommendations
  };
}

// ── Helpers ──
function normalizeTask(input, now) {
  return {
    id: input.id || `task-${now}-${randomSuffix()}`,
    title: String(input.title || "").trim(),
    description: String(input.description || "").trim(),
    status: input.status || "inbox",
    priority: Number(input.priority) || 0,
    dueAt: input.dueAt || null,
    tags: Array.isArray(input.tags) ? input.tags : String(input.tags || "").split(/[,，]/).map((s) => s.trim()).filter(Boolean),
    source: input.source || "manual",
    sourceId: input.sourceId,
    createdAt: input.createdAt || now,
    updatedAt: now,
    completedAt: null
  };
}

function normalizeAttachments(files, now) {
  return (files || [])
    .map((file, index) => {
      const sourcePath = String(file.path || file.sourcePath || "").trim();
      const name = String(file.name || sourcePath.split(/[\\/]/).pop() || "").trim();
      if (!name && !sourcePath) return null;
      return {
        id: file.id || `attachment-${now}-${index}-${randomSuffix()}`,
        name: name || "未命名附件",
        sourcePath,
        size: Number(file.size) || 0,
        type: file.type || file.mimeType || "",
        storageMode: "linked",
        attachedAt: now
      };
    })
    .filter(Boolean);
}

function markdownFileToDiary(file, now) {
  const content = String(file.content || file.text || "").trim();
  if (!content) return null;
  const title = extractMarkdownTitle(content) || cleanupMarkdownFileName(file.name) || `${new Date(now).toLocaleDateString("zh-CN")} 的导入日记`;
  return {
    id: file.id || `diary-${now}-${randomSuffix()}`,
    title,
    text: content,
    mood: "neutral",
    tags: extractMarkdownTags(content),
    attachments: [],
    source: "markdown-import",
    sourceName: file.name || "",
    createdAt: file.createdAt || now,
    updatedAt: now
  };
}

function parseExternalDiaryFile(file, source, now) {
  const payload = parseJsonPayload(file?.content || file?.text || "");
  if (!payload) return [];
  const items = source === "bear" ? extractBearItems(payload) : extractDayOneItems(payload);
  return items
    .map((item, index) => externalItemToDiary(item, file, source, now + index))
    .filter(Boolean);
}

function parseJsonPayload(content) {
  try {
    return JSON.parse(String(content || ""));
  } catch {
    return null;
  }
}

function extractDayOneItems(payload) {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.entries)) return payload.entries;
  if (Array.isArray(payload?.journalEntries)) return payload.journalEntries;
  if (Array.isArray(payload?.items)) return payload.items;
  return [];
}

function extractBearItems(payload) {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.notes)) return payload.notes;
  if (Array.isArray(payload?.entries)) return payload.entries;
  if (Array.isArray(payload?.items)) return payload.items;
  return [];
}

function externalItemToDiary(item, file, source, now) {
  const text = String(item?.text || item?.markdown || item?.content || item?.body || "").trim();
  if (!text) return null;
  const createdAt = parseTimestamp(
    item?.creationDate || item?.createdDate || item?.createdAt || item?.created || item?.date,
    now
  );
  const title = String(item?.title || extractMarkdownTitle(text) || cleanupMarkdownFileName(file?.name) || "").trim()
    || `${new Date(createdAt).toLocaleDateString("zh-CN")} 的导入日记`;
  return {
    id: item?.id || `diary-${createdAt}-${randomSuffix()}`,
    title,
    text,
    mood: "neutral",
    tags: normalizeExternalTags(item?.tags, text),
    attachments: [],
    source: source === "bear" ? "bear-import" : "day-one-import",
    sourceName: file?.name || "",
    createdAt,
    updatedAt: now
  };
}

function extractMarkdownTitle(content) {
  const heading = String(content).split(/\r?\n/).find((line) => /^#{1,3}\s+/.test(line.trim()));
  return heading ? heading.replace(/^#{1,3}\s+/, "").trim() : "";
}

function normalizeExternalTags(tags, content) {
  const explicitTags = Array.isArray(tags)
    ? tags
    : String(tags || "").split(/[,，\s]+/);
  return [...new Set([
    ...explicitTags.map((tag) => String(tag).replace(/^#/, "").trim()).filter(Boolean),
    ...extractMarkdownTags(content)
  ])];
}

function parseTimestamp(value, fallback) {
  const time = value ? new Date(value).getTime() : NaN;
  return Number.isFinite(time) ? time : fallback;
}

function extractMarkdownTags(content) {
  const inlineTags = Array.from(String(content).matchAll(/#([\p{L}\p{N}_-]+)/gu), (match) => match[1]);
  const tagsLine = String(content).match(/^Tags:\s*(.+)$/im);
  const listedTags = tagsLine ? tagsLine[1].split(/[,，\s]+/).map((tag) => tag.trim()).filter(Boolean) : [];
  return [...new Set([...inlineTags, ...listedTags])];
}

function cleanupMarkdownFileName(name = "") {
  return String(name)
    .replace(/\.[^.]+$/, "")
    .replace(/^\d{4}[-_]\d{2}[-_]\d{2}[-_]?/, "")
    .replace(/[-_]+/g, " ")
    .trim();
}

function inRange(value, start, end) {
  if (!value) return false;
  const time = value instanceof Date ? value.getTime() : new Date(value).getTime();
  return Number.isFinite(time) && time >= start && time < end;
}

function collectTopTags(items) {
  const counts = new Map();
  for (const item of items) {
    for (const tag of item.tags || []) {
      counts.set(tag, (counts.get(tag) || 0) + 1);
    }
  }
  return [...counts.entries()]
    .map(([tag, count]) => ({ tag, count }))
    .sort((a, b) => b.count - a.count || a.tag.localeCompare(b.tag))
    .slice(0, 5);
}

function sanitizeSettings(settings) {
  const safe = { ...settings };
  delete safe.apiKey;
  return safe;
}

function sanitizeAccount(account) {
  const safe = { ...account };
  delete safe.token;
  return safe;
}

async function authenticateSyncAccount(state, path, input, options = {}) {
  const phone = normalizePhone(input.phone);
  const pin = String(input.pin ?? "").trim();
  const endpoint = normalizeEndpoint(input.endpoint || state.account?.syncEndpoint || DEFAULT_SYNC_ENDPOINT);
  const fetcher = options.fetcher || globalThis.fetch;
  const now = options.now ?? Date.now();

  if (!phone || pin.length < 4) {
    return { ok: false, state, message: "请输入手机号和至少 4 位 PIN" };
  }
  if (typeof fetcher !== "function") {
    return { ok: false, state, message: "当前环境不支持网络登录" };
  }

  try {
    const response = await fetcher(`${endpoint}${path}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ phone, pin })
    });
    const parsed = await parseJsonResponse(response);
    const token = parsed.token;
    if (!response.ok || !token) {
      return { ok: false, state, message: parsed.error || parsed.message || `登录失败 (${response.status})` };
    }

    const nextState = {
      ...state,
      account: {
        ...(state.account || {}),
        status: "linked",
        phone,
        maskedPhone: maskPhone(phone),
        token,
        syncEndpoint: endpoint,
        deviceId: state.account?.deviceId || createDeviceId(),
        deviceName: input.deviceName || state.account?.deviceName || "DiaryApp Desktop",
        boundAt: now,
        unboundAt: null
      }
    };
    return { ok: true, state: nextState, message: parsed.message || "账号已连接" };
  } catch (error) {
    return { ok: false, state, message: error.message || "登录失败" };
  }
}

async function parseJsonResponse(response) {
  try {
    if (typeof response.json === "function") return await response.json();
  } catch {}
  try {
    const text = typeof response.text === "function" ? await response.text() : "";
    return text ? JSON.parse(text) : {};
  } catch {
    return {};
  }
}

function normalizeEndpoint(endpoint) {
  return String(endpoint || DEFAULT_SYNC_ENDPOINT).trim().replace(/\/+$/, "");
}

function normalizePhone(phone) {
  return String(phone ?? "").replace(/\D/g, "");
}

function maskPhone(phone) {
  const normalized = normalizePhone(phone);
  if (normalized.length <= 7) return normalized;
  return `${normalized.slice(0, 3)}****${normalized.slice(-4)}`;
}

function createDeviceId() {
  return `desktop-${Math.random().toString(36).slice(2, 10)}`;
}

function randomSuffix() {
  return Math.random().toString(36).slice(2, 8);
}
