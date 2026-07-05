const DAY = 24 * 60 * 60 * 1000;

const DEFAULT_ASSISTANT_ROLES = Object.freeze([
  {
    id: "role-diary-executive",
    name: "DiaryApp 执行助理",
    purpose: "把日记、捕获和待办转成可执行计划；只有用户允许时才写入待办。",
    permissions: ["read_tasks", "suggest_tasks", "create_tasks", "update_tasks", "breakdown_tasks"]
  },
  {
    id: "role-review-analyst",
    name: "复盘分析师",
    purpose: "检查逾期、模糊任务和项目失焦风险，输出可审计建议。",
    permissions: ["read_tasks", "suggest_tasks"]
  },
  {
    id: "role-sync-curator",
    name: "同步整理员",
    purpose: "整理手机同步、批量捕获和导入内容，保持 Inbox 干净。",
    permissions: ["read_tasks", "suggest_tasks", "create_tasks", "update_tasks"]
  }
]);

export function getDefaultAssistantRoles() {
  return DEFAULT_ASSISTANT_ROLES.map((role) => ({
    ...role,
    permissions: [...role.permissions]
  }));
}

export function createDesktopState(input = {}) {
  const now = input.now ?? Date.now();
  const tasks = (input.tasks ?? []).map(normalizeTask);
  const habits = (input.habits ?? []).map(normalizeHabit);
  const notes = (input.notes ?? []).map(normalizeNote);
  const diaryEntries = input.diaryEntries ?? [];
  const aiDrafts = input.aiDrafts ?? [];
  const assistant = normalizeAssistant(input.assistant, now);
  const account = normalizeAccount(input.account);
  const reminderLog = input.reminderLog ?? {};
  const archive = input.archive ?? [];
  const templates = input.templates ?? [];
  const checkin = input.checkin ?? { currentStreak: 0, bestStreak: 0, lastDate: null, history: [] };
  const settings = {
    theme: "focus",
    density: "comfortable",
    board: {
      query: "",
      status: "all",
      tag: "",
      sort: "smart"
    },
    ai: {
      provider: "local",
      model: "local-rules",
      endpoint: "",
      apiKey: "",
      apiKeyEnv: "",
      autoDraftFromDiary: true
    },
    notifications: {
      enabled: true,
      reminderWindowMinutes: 30
    },
    ...(input.settings ?? {}),
    board: {
      query: "",
      status: "all",
      tag: "",
      sort: "smart",
      ...(input.settings?.board ?? {})
    },
    ai: {
      provider: "local",
      model: "local-rules",
      endpoint: "",
      apiKey: "",
      apiKeyEnv: "",
      autoDraftFromDiary: true,
      ...(input.settings?.ai ?? {})
    },
    notifications: {
      enabled: true,
      reminderWindowMinutes: 30,
      ...(input.settings?.notifications ?? {})
    }
  };
  return { now, tasks, habits, notes, diaryEntries, aiDrafts, assistant, account, settings, reminderLog, archive, templates, checkin };
}

export function summarizeTaskBuckets(state, now = Date.now()) {
  const tasks = state.tasks.map(normalizeTask);
  const isOverdue = (task) => task.status !== "done" && task.dueAt && new Date(task.dueAt).getTime() < now;
  const inbox = tasks.filter((task) => task.type !== "habit" && task.status === "inbox");
  const overdue = tasks.filter(isOverdue);
  const planned = tasks.filter((task) => task.type !== "habit" && task.status !== "done" && task.status !== "inbox" && !isOverdue(task));
  const done = tasks.filter((task) => task.status === "done");
  const habits = [
    ...tasks.filter((task) => task.type === "habit"),
    ...(state.habits ?? [])
  ];

  return {
    inbox,
    planned,
    overdue,
    done,
    habits,
    counts: {
      inbox: inbox.length,
      planned: planned.length,
      overdue: overdue.length,
      done: done.length,
      habits: habits.length
    }
  };
}

export function buildTaskDraftFromDiary(text, sourceId = `diary-${Date.now()}`) {
  if (!text || !text.trim()) return [];
  const parts = text
    .split(/[。！？!?；;，,\n]/)
    .map((item) => item.trim())
    .filter(Boolean);
  const triggers = ["要", "需要", "别忘", "记得", "准备", "整理", "完成", "做", "备份"];

  return parts
    .filter((part) => triggers.some((trigger) => part.includes(trigger)))
    .slice(0, 6)
    .map((part, index) => ({
      id: `${sourceId}-draft-${index + 1}`,
      title: cleanupDraftTitle(part),
      source: "diary",
      sourceId,
      status: "draft",
      confidence: clamp(0.62 + index * 0.07, 0.62, 0.9),
      reason: "从日记中的行动语气识别，等待用户确认后写入待办。"
    }));
}

export function parseTaskCapture(text, now = Date.now()) {
  return String(text ?? "")
    .split(/\n+/)
    .map((line) => parseCaptureLine(line, now))
    .filter(Boolean);
}

export function buildDayPlan(state, now = Date.now()) {
  const buckets = summarizeTaskBuckets(state, now);
  const actionable = [...buckets.overdue, ...buckets.planned, ...buckets.inbox]
    .filter((task) => task.status !== "done")
    .sort((a, b) => taskScore(b, now) - taskScore(a, now))
    .slice(0, 6);

  const blocks = actionable.map((task, index) => ({
    id: `block-${task.id}`,
    taskId: task.id,
    kind: "task",
    title: task.title,
    window: chooseWindow(index),
    energy: index <= 1 ? "deep" : "light",
    risk: taskRisk(task, now),
    aiReason: explainTask(task, now)
  }));

  const habitBlocks = (state.habits ?? [])
    .filter((habit) => !habit.doneToday)
    .slice(0, 2)
    .map((habit, index) => ({
      id: `habit-block-${habit.id}`,
      taskId: habit.id,
      kind: "habit",
      title: habit.title,
      window: index === 0 ? "08:40-08:55" : "21:20-21:35",
      energy: "light",
      risk: habit.streak >= 5 ? "medium" : "low",
      aiReason: `保持 ${habit.streak ?? 0} 天连续记录，建议用短时段完成。`
    }));

  const allBlocks = [...blocks, ...habitBlocks];
  return {
    generatedAt: now,
    summary: allBlocks.length
      ? `优先处理 ${allBlocks[0].title}，再安排 ${Math.max(0, allBlocks.length - 1)} 个轻重搭配事项。`
      : "今天没有必须处理的事项，可以做回顾和整理。",
    blocks: allBlocks
  };
}

export function buildHabitEvidence({ diary, habits }) {
  if (!diary || !Array.isArray(habits)) return [];
  const text = diary.text ?? "";
  const tagIds = diary.tagIds ?? [];

  return habits.flatMap((habit) => {
    const linkedTags = habit.linkedTagIds ?? [];
    const tagHit = linkedTags.some((tag) => tagIds.includes(tag));
    const titleHit = habit.title && text.includes(habit.title.replace(/^每日/, ""));
    if (!tagHit && !titleHit) return [];
    return [{
      id: `${diary.id}-${habit.id}-evidence`,
      habitId: habit.id,
      diaryId: diary.id,
      action: "propose-checkin",
      requiresConfirmation: true,
      confidence: tagHit && titleHit ? 0.86 : 0.72,
      reason: tagHit && titleHit ? "标签和内容同时命中，建议确认打卡。" : "标签或内容命中，建议确认后打卡。",
      quote: pickQuote(text, habit.title)
    }];
  });
}

export function buildTaskDependencyHints(tasks = []) {
  const active = tasks.filter((task) => task.type !== "habit" && task.status !== "done");
  const blockers = active.filter((task) => /准备|整理|设计|确认|检查|拆解|规划/.test(task.title ?? ""));
  const dependents = active.filter((task) => /发布|提交|上线|交付|验证|复盘|完成/.test(task.title ?? ""));

  return dependents.flatMap((task) => {
    const keywords = titleKeywords(task.title);
    const action = dependentAction(task.title);
    const blocker = blockers.find((candidate) => candidate.id !== task.id
      && (sharesKeyword(keywords, titleKeywords(candidate.title)) || (action && String(candidate.title ?? "").includes(action))));
    if (!blocker) return [];
    return [{
      taskId: task.id,
      blockedBy: blocker.id,
      reason: `可能需要先完成「${blocker.title}」，再处理「${task.title}」。`
    }];
  });
}

export function buildTaskHealthReport(tasks = [], now = Date.now()) {
  const active = tasks.filter((task) => task.type !== "habit" && task.status !== "done");
  const missingDue = active.filter((task) => !task.dueAt).length;
  const missingTags = active.filter((task) => !(task.tags ?? []).length).length;
  const staleInbox = active.filter((task) => task.status === "inbox").length;
  const urgent = active.filter((task) => task.dueAt && new Date(task.dueAt).getTime() - now <= DAY).length;
  const healthy = active.filter((task) => task.dueAt && (task.tags ?? []).length && task.status !== "inbox").length;
  const focusScore = active.length ? Math.round((healthy / active.length) * 100) : 100;

  const nextFixes = active
    .map((task) => taskFixReason(task))
    .filter(Boolean)
    .slice(0, 5);

  return {
    active: active.length,
    missingDue,
    missingTags,
    staleInbox,
    urgent,
    focusScore,
    nextFixes
  };
}

export function groupTasksByProject(tasks = []) {
  const buckets = new Map();
  tasks
    .filter((task) => task.type !== "habit")
    .forEach((task) => {
      const key = (task.tags ?? [])[0] || "uncategorized";
      if (!buckets.has(key)) {
        buckets.set(key, { key, label: key === "uncategorized" ? "未归档" : key, tasks: [] });
      }
      buckets.get(key).tasks.push(task);
    });

  return [...buckets.values()].sort((a, b) => {
    if (a.key === "uncategorized") return 1;
    if (b.key === "uncategorized") return -1;
    return 0;
  });
}

function parseCaptureLine(line, now) {
  let working = String(line ?? "").trim();
  if (!working) return null;

  const priority = working.startsWith("!!") ? 2 : working.startsWith("!") ? 1 : 0;
  working = working.replace(/^!!?\s*/, "");

  const tags = Array.from(working.matchAll(/#([\p{L}\p{N}_-]+)/gu), (match) => match[1]);
  working = working.replace(/#[\p{L}\p{N}_-]+/gu, "").trim();

  const parsedDue = parseCaptureDue(working, now);
  working = parsedDue.title.trim();
  if (!working) return null;

  return {
    title: working,
    status: "inbox",
    priority,
    dueAt: parsedDue.dueAt,
    tags,
    source: "capture"
  };
}

function parseCaptureDue(text, now) {
  let working = text;
  const dayMatch = working.match(/^(今天|明天|后天)\s*/);
  const timeMatch = working.match(/\b([01]?\d|2[0-3]):([0-5]\d)\b/);
  const dayOffset = dayMatch ? { "今天": 0, "明天": 1, "后天": 2 }[dayMatch[1]] : null;
  let dueAt = null;

  if (dayOffset !== null && dayOffset !== undefined) {
    const base = new Date(now + dayOffset * DAY);
    const hour = timeMatch ? Number(timeMatch[1]) : 20;
    const minute = timeMatch ? Number(timeMatch[2]) : 0;
    dueAt = formatLocalIso(base, hour, minute);
    working = working.replace(dayMatch[0], "");
  }

  if (timeMatch) working = working.replace(timeMatch[0], "");
  return { title: working.replace(/\s+/g, " ").trim(), dueAt };
}

function formatLocalIso(base, hour, minute) {
  const date = new Date(base);
  date.setHours(hour, minute, 0, 0);
  const offsetMinutes = -date.getTimezoneOffset();
  const sign = offsetMinutes >= 0 ? "+" : "-";
  const abs = Math.abs(offsetMinutes);
  const offset = `${sign}${String(Math.floor(abs / 60)).padStart(2, "0")}:${String(abs % 60).padStart(2, "0")}`;
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:00.000${offset}`;
}

function pad(value) {
  return String(value).padStart(2, "0");
}

function titleKeywords(title = "") {
  return String(title)
    .replace(/准备|整理|设计|确认|检查|拆解|规划|发布|提交|上线|交付|验证|复盘|完成/g, " ")
    .split(/[\s,，。:：#/-]+/)
    .map((word) => word.trim())
    .filter((word) => word.length >= 2);
}

function sharesKeyword(left, right) {
  return left.some((word) => right.includes(word));
}

function dependentAction(title = "") {
  return String(title).match(/发布|提交|上线|交付|验证|复盘|完成/)?.[0] ?? "";
}

function taskFixReason(task) {
  if (!task.dueAt) {
    return { taskId: task.id, title: task.title, reason: "缺少截止时间，无法进入可靠计划。" };
  }
  if (!(task.tags ?? []).length) {
    return { taskId: task.id, title: task.title, reason: "缺少项目标签，后续复盘和筛选会失真。" };
  }
  if (task.status === "inbox") {
    return { taskId: task.id, title: task.title, reason: "仍停留在 Inbox，需要确认下一步。" };
  }
  return null;
}

function normalizeTask(task) {
  return {
    id: task.id,
    title: task.title ?? "",
    description: task.description ?? "",
    type: task.type ?? "task",
    status: task.status ?? (task.isCompleted ? "done" : "planned"),
    priority: task.priority ?? 0,
    dueAt: task.dueAt ?? null,
    tags: task.tags ?? [],
    source: task.source ?? "manual",
    sourceId: task.sourceId,
    subtasks: task.subtasks ?? [],
    starred: Boolean(task.starred),
    aiBreakdown: task.aiBreakdown
  };
}

function normalizeHabit(habit) {
  return {
    id: habit.id,
    title: habit.title ?? "",
    streak: habit.streak ?? 0,
    doneToday: Boolean(habit.doneToday),
    linkedTagIds: habit.linkedTagIds ?? []
  };
}

function normalizeNote(note) {
  const now = Date.now();
  return {
    id: note.id ?? `note-${now}-${Math.random().toString(36).slice(2, 8)}`,
    title: String(note.title ?? "").trim(),
    text: String(note.text ?? "").trim(),
    tagIds: Array.isArray(note.tagIds)
      ? note.tagIds
      : String(note.tags ?? "").split(/[,，]/).map((tag) => tag.trim()).filter(Boolean),
    pinned: Boolean(note.pinned),
    createdAt: note.createdAt ?? now,
    updatedAt: note.updatedAt ?? note.createdAt ?? now
  };
}

function normalizeAssistant(input = {}, now = Date.now()) {
  const roles = input.roles?.length ? input.roles : getDefaultAssistantRoles();
  const normalizedRoles = roles.map(normalizeAssistantRole);
  const activeRoleId = normalizedRoles.some((role) => role.id === input.activeRoleId)
    ? input.activeRoleId
    : normalizedRoles[0]?.id;
  return {
    activeRoleId,
    permissionMode: input.permissionMode ?? "ask",
    roles: normalizedRoles,
    runs: input.runs ?? []
  };
}

function normalizeAssistantRole(role) {
  return {
    id: role.id,
    name: role.name ?? "桌面助手",
    purpose: role.purpose ?? "整理待办、拆解下一步、提醒风险。",
    permissions: role.permissions ?? ["read_tasks"]
  };
}

function normalizeAccount(account = {}) {
  const phone = String(account.phone ?? "").trim();
  const status = account.status === "linked" && phone ? "linked" : "guest";
  return {
    status,
    phone: status === "linked" ? phone : "",
    maskedPhone: status === "linked" ? (account.maskedPhone ?? maskPhone(phone)) : "",
    token: status === "linked" ? String(account.token ?? "") : "",
    deviceId: status === "linked" ? String(account.deviceId ?? "") : "",
    deviceName: status === "linked" ? String(account.deviceName ?? "") : String(account.deviceName ?? ""),
    syncEndpoint: String(account.syncEndpoint ?? ""),
    boundAt: status === "linked" ? (account.boundAt ?? null) : null,
    unboundAt: account.unboundAt ?? null,
    lastSyncAt: account.lastSyncAt ?? null,
    lastSyncDirection: account.lastSyncDirection ?? "",
    lastSyncDeviceId: account.lastSyncDeviceId ?? "",
    lastSyncSummary: account.lastSyncSummary ?? null
  };
}

function maskPhone(phone) {
  const normalized = String(phone ?? "").replace(/\D/g, "");
  if (normalized.length <= 7) return normalized;
  return `${normalized.slice(0, 3)}****${normalized.slice(-4)}`;
}

function cleanupDraftTitle(text) {
  return text
    .replace(/^今天/, "")
    .replace(/^还/, "")
    .replace(/^并且/, "")
    .replace(/^以及/, "")
    .trim();
}

function clamp(value, min, max) {
  return Math.max(min, Math.min(max, Number(value.toFixed(2))));
}

function taskScore(task, now) {
  const due = task.dueAt ? new Date(task.dueAt).getTime() : now + 7 * DAY;
  const urgency = Math.max(0, 7 - Math.ceil((due - now) / DAY));
  const statusBoost = task.status === "inbox" ? -1 : 0;
  return urgency * 2 + (task.priority ?? 0) * 3 + statusBoost;
}

function taskRisk(task, now) {
  if (!task.dueAt) return task.status === "inbox" ? "medium" : "low";
  const diff = new Date(task.dueAt).getTime() - now;
  if (diff < 0) return "high";
  if (diff < DAY) return "high";
  if (diff < 2 * DAY) return "medium";
  return "low";
}

function chooseWindow(index) {
  const windows = ["09:30-10:15", "10:30-11:10", "14:30-15:00", "16:00-16:25", "19:40-20:10", "20:20-20:40"];
  return windows[index] ?? "稍后";
}

function explainTask(task, now) {
  const risk = taskRisk(task, now);
  if (risk === "high") return "截止时间近或已过期，建议放在最高能量时段。";
  if (task.status === "inbox") return "仍在 Inbox，建议先明确下一步再执行。";
  if ((task.subtasks ?? []).length === 0 && (task.title ?? "").length > 8) return "任务较大，建议拆成 3 个小步骤。";
  return "按优先级和当前计划顺序安排。";
}

function pickQuote(text, habitTitle) {
  if (!text) return "";
  const sentences = text.split(/[。！？!?；;\n]/).map((item) => item.trim()).filter(Boolean);
  return sentences.find((sentence) => sentence.includes(habitTitle?.replace(/^每日/, "") ?? "")) ?? sentences[0] ?? "";
}
