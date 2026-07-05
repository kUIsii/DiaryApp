import test from "node:test";
import assert from "node:assert/strict";
import {
  addTask,
  updateTask,
  deleteTask,
  completeTask,
  listTasks,
  captureTasks,
  createDiaryEntry,
  updateDiaryEntry,
  searchDiaries,
  saveSettings,
  loginSyncAccount,
  logoutSyncAccount,
  registerSyncAccount,
  syncWithCloud,
  exportState,
  importState,
  getDataSummary,
  attachFilesToDiary,
  importMarkdownArchive,
  importExternalDiaryArchive,
  buildMonthlyReport
} from "./desktopService.js";
import { createDesktopState } from "./taskEngine.js";

function emptyState() {
  return createDesktopState({ tasks: [], diaries: [], aiDrafts: [], archive: [] });
}

function now() {
  return 1710000000000;
}

test("addTask adds a task to state", () => {
  const state = addTask(emptyState(), { title: "测试任务" }, now());
  assert.equal(state.tasks.length, 1);
  assert.equal(state.tasks[0].title, "测试任务");
});

test("addTask rejects empty title", () => {
  const state = addTask(emptyState(), { title: "" }, now());
  assert.equal(state.tasks.length, 0);
});

test("completeTask marks task as done", () => {
  const s1 = addTask(emptyState(), { title: "测试" }, now());
  const s2 = completeTask(s1, s1.tasks[0].id, now());
  assert.equal(s2.tasks[0].status, "done");
  assert.ok(s2.tasks[0].completedAt);
});

test("deleteTask removes a task", () => {
  const s1 = addTask(emptyState(), { title: "删除测试" }, now());
  const s2 = deleteTask(s1, s1.tasks[0].id);
  assert.equal(s2.tasks.length, 0);
});

test("updateTask patches a task", () => {
  const s1 = addTask(emptyState(), { title: "旧标题" }, now());
  const s2 = updateTask(s1, s1.tasks[0].id, { title: "新标题" }, now());
  assert.equal(s2.tasks[0].title, "新标题");
});

test("listTasks filters by status", () => {
  const s1 = addTask(emptyState(), { title: "待办1" }, now());
  const s2 = addTask(s1, { title: "待办2" }, now + 1);
  const s3 = completeTask(s2, s2.tasks[1].id, now + 2);
  assert.equal(listTasks(s3, "active").length, 1);
  assert.equal(listTasks(s3, "done").length, 1);
  assert.equal(listTasks(s3, "all").length, 2);
});

test("captureTasks parses priority markers", () => {
  const s1 = captureTasks(emptyState(), "!!紧急任务\n!普通任务\n一般任务", now());
  assert.equal(s1.tasks.length, 3);
  assert.equal(s1.tasks[0].priority, 2);
  assert.equal(s1.tasks[1].priority, 1);
  assert.equal(s1.tasks[2].priority, 0);
});

test("captureTasks parses tags with #", () => {
  const s1 = captureTasks(emptyState(), "买牛奶 #购物 #生活", now());
  assert.deepEqual(s1.tasks[0].tags, ["购物", "生活"]);
});

test("createDiaryEntry creates a diary entry", () => {
  const state = createDiaryEntry(emptyState(), {
    title: "测试日记",
    text: "今天测试了应用程序。",
    mood: "happy",
    tags: ["测试"]
  }, now());
  assert.equal(state.diaries.length, 1);
  assert.equal(state.diaries[0].title, "测试日记");
  assert.equal(state.diaries[0].mood, "happy");
});

test("updateDiaryEntry patches a diary entry", () => {
  const s1 = createDiaryEntry(emptyState(), { title: "旧标题", text: "内容" }, now());
  const s2 = updateDiaryEntry(s1, s1.diaries[0].id, { title: "新标题" }, now());
  assert.equal(s2.diaries[0].title, "新标题");
  assert.equal(s2.diaries[0].text, "内容");
});

test("searchDiaries finds matching entries", () => {
  const s1 = createDiaryEntry(emptyState(), { title: "工作记录", text: "完成了一个项目", tags: ["工作"] }, now());
  const s2 = createDiaryEntry(s1, { title: "生活随笔", text: "去公园散步了", tags: ["生活"] }, now + 1);
  assert.equal(searchDiaries(s2, "项目").length, 1);
  assert.equal(searchDiaries(s2, "公园").length, 1);
  assert.equal(searchDiaries(s2, "不存在的").length, 0);
});

test("saveSettings updates settings", () => {
  const state = saveSettings(emptyState(), { provider: "deepseek" });
  assert.equal(state.settings.provider, "deepseek");
});

test("exportState produces valid JSON with version", () => {
  const json = exportState(emptyState());
  const parsed = JSON.parse(json);
  assert.equal(parsed.version, 2);
  assert.ok(parsed.state);
});

test("importState restores state from JSON", () => {
  const original = createDiaryEntry(emptyState(), { title: "导入测试", text: "内容" }, now());
  const json = exportState(original);
  const restored = importState(emptyState(), json);
  assert.equal(restored.diaries.length, 1);
  assert.equal(restored.diaries[0].title, "导入测试");
});

test("importState sanitizes apiKey on export", () => {
  const s1 = saveSettings(emptyState(), { apiKey: "secret-123" });
  const json = exportState(s1);
  const parsed = JSON.parse(json);
  assert.equal(parsed.state.settings.apiKey, undefined);
});

test("getDataSummary returns counts", () => {
  const s1 = addTask(emptyState(), { title: "A" }, now());
  const s2 = addTask(s1, { title: "B" }, now + 1);
  const s3 = completeTask(s2, s2.tasks[0].id, now + 2);
  const sum = getDataSummary(s3);
  assert.equal(sum.totalTasks, 2);
  assert.equal(sum.activeTasks, 1);
  assert.equal(sum.doneTasks, 1);
});

test("loginSyncAccount links desktop account with masked phone and token", async () => {
  const calls = [];
  const fetcher = async (url, options) => {
    calls.push({ url, options });
    return {
      ok: true,
      json: async () => ({ token: "token-123", message: "login ok" }),
      text: async () => JSON.stringify({ token: "token-123" })
    };
  };

  const result = await loginSyncAccount(emptyState(), {
    phone: "13812345678",
    pin: "2468",
    endpoint: "https://diary-app-sync.workers.dev",
    deviceName: "Desktop Studio"
  }, { fetcher, now: now() });

  assert.equal(result.ok, true);
  assert.equal(result.state.account.status, "linked");
  assert.equal(result.state.account.maskedPhone, "138****5678");
  assert.equal(result.state.account.token, "token-123");
  assert.equal(result.state.account.syncEndpoint, "https://diary-app-sync.workers.dev");
  assert.equal(calls[0].url, "https://diary-app-sync.workers.dev/api/login");
  assert.deepEqual(JSON.parse(calls[0].options.body), { phone: "13812345678", pin: "2468" });
});

test("registerSyncAccount uses the same Worker auth contract as Android", async () => {
  const calls = [];
  const fetcher = async (url, options) => {
    calls.push({ url, options });
    return {
      ok: true,
      json: async () => ({ token: "registered-token" }),
      text: async () => JSON.stringify({ token: "registered-token" })
    };
  };

  const result = await registerSyncAccount(emptyState(), {
    phone: "13900001111",
    pin: "1357"
  }, { fetcher, now: now() });

  assert.equal(result.ok, true);
  assert.equal(result.state.account.status, "linked");
  assert.equal(calls[0].url, "https://diary-app-sync.workers.dev/api/register");
});

test("syncWithCloud uploads desktop data and stores sync metadata", async () => {
  const starting = {
    ...addTask(emptyState(), { title: "同步测试" }, now()),
    account: {
      status: "linked",
      phone: "13812345678",
      maskedPhone: "138****5678",
      token: "token-123",
      syncEndpoint: "https://diary-app-sync.workers.dev",
      deviceId: "desktop-test",
      deviceName: "Desktop"
    }
  };
  const calls = [];
  const fetcher = async (url, options) => {
    calls.push({ url, options });
    return {
      ok: true,
      json: async () => ({ message: "同步成功", version: 2 }),
      text: async () => JSON.stringify({ message: "同步成功", version: 2 })
    };
  };

  const result = await syncWithCloud(starting, { fetcher, now: now() });

  assert.equal(result.ok, true);
  assert.equal(calls[0].url, "https://diary-app-sync.workers.dev/api/backup");
  assert.equal(calls[0].options.headers.Authorization, "Bearer token-123");
  const body = JSON.parse(calls[0].options.body);
  assert.equal(body.data.syncMeta.deviceId, "desktop-test");
  assert.equal(body.data.tasks.length, 1);
  assert.equal(result.state.account.lastSyncDirection, "push");
  assert.equal(result.state.account.lastSyncSummary.message, "同步成功");
});

test("logoutSyncAccount clears token without deleting local data", () => {
  const starting = {
    ...addTask(emptyState(), { title: "保留本地任务" }, now()),
    account: {
      status: "linked",
      phone: "13812345678",
      maskedPhone: "138****5678",
      token: "secret"
    }
  };
  const result = logoutSyncAccount(starting, now());
  assert.equal(result.account.status, "guest");
  assert.equal(result.account.token, "");
  assert.equal(result.tasks.length, 1);
});

test("attachFilesToDiary persists desktop attachment metadata on a diary", () => {
  const s1 = createDiaryEntry(emptyState(), { title: "附件测试", text: "记录设计稿" }, now());
  const diaryId = s1.diaries[0].id;
  const s2 = attachFilesToDiary(s1, diaryId, [
    { name: "wireframe.png", path: "D:\\DiaryApp\\assets\\wireframe.png", size: 2048, type: "image/png" },
    { name: "meeting.md", path: "D:\\DiaryApp\\notes\\meeting.md", size: 512, type: "text/markdown" }
  ], now() + 1);

  assert.equal(s2.diaries[0].attachments.length, 2);
  assert.equal(s2.diaries[0].attachments[0].name, "wireframe.png");
  assert.equal(s2.diaries[0].attachments[0].sourcePath, "D:\\DiaryApp\\assets\\wireframe.png");
  assert.equal(s2.diaries[0].attachments[0].storageMode, "linked");
  assert.equal(s2.diaries[0].updatedAt, now() + 1);
});

test("importMarkdownArchive creates diary entries from markdown documents", () => {
  const imported = importMarkdownArchive(emptyState(), [
    {
      name: "2026-07-01-weekly-review.md",
      content: "# 周度复盘\n\n完成了桌面端计划。\n\n#desktop #review"
    },
    {
      name: "decision-log.md",
      content: "## 决策记录\n\n采用多面板桌面工作台。\n\nTags: product, desktop"
    }
  ], now());

  assert.equal(imported.diaries.length, 2);
  assert.equal(imported.diaries[0].title, "周度复盘");
  assert.deepEqual(imported.diaries[0].tags, ["desktop", "review"]);
  assert.equal(imported.diaries[0].source, "markdown-import");
  assert.equal(imported.diaries[1].title, "决策记录");
  assert.deepEqual(imported.diaries[1].tags, ["product", "desktop"]);
});

test("importExternalDiaryArchive converts Day One and Bear exports into diary entries", () => {
  const dayOne = importExternalDiaryArchive(emptyState(), [
    {
      name: "day-one.json",
      content: JSON.stringify({
        entries: [
          {
            text: "# 旅行复盘\n\n今天整理了桌面端导入。",
            creationDate: "2026-07-04T08:00:00Z",
            tags: ["travel", "desktop"]
          }
        ]
      })
    }
  ], "day-one", now());
  const bear = importExternalDiaryArchive(emptyState(), [
    {
      name: "bear-notes.json",
      content: JSON.stringify([
        {
          title: "Bear 决策记录",
          text: "采用页面自有布局，不做混乱工作台。",
          created: "2026-07-05T08:00:00Z",
          tags: ["product", "writing"]
        }
      ])
    }
  ], "bear", now());

  assert.equal(dayOne.diaries.length, 1);
  assert.equal(dayOne.diaries[0].title, "旅行复盘");
  assert.equal(dayOne.diaries[0].source, "day-one-import");
  assert.deepEqual(dayOne.diaries[0].tags, ["travel", "desktop"]);
  assert.equal(bear.diaries.length, 1);
  assert.equal(bear.diaries[0].title, "Bear 决策记录");
  assert.equal(bear.diaries[0].source, "bear-import");
  assert.deepEqual(bear.diaries[0].tags, ["product", "writing"]);
});

test("buildMonthlyReport summarizes writing and execution for desktop review", () => {
  const base = emptyState();
  const s1 = createDiaryEntry(base, {
    title: "七月第一篇",
    text: "今天完成桌面端写作分析，并准备继续打磨导入中心。",
    tags: ["desktop"]
  }, Date.parse("2026-07-01T09:00:00+08:00"));
  const s2 = addTask(s1, {
    title: "完成导入中心",
    status: "done",
    tags: ["desktop"],
    dueAt: "2026-07-03T18:00:00+08:00"
  }, Date.parse("2026-07-02T09:00:00+08:00"));
  const s3 = addTask(s2, {
    title: "整理月报",
    status: "inbox",
    tags: ["review"]
  }, Date.parse("2026-07-03T09:00:00+08:00"));

  const report = buildMonthlyReport(s3, { year: 2026, month: 7 });

  assert.equal(report.periodLabel, "2026年7月");
  assert.equal(report.diaryCount, 1);
  assert.equal(report.completedTasks, 1);
  assert.equal(report.openTasks, 1);
  assert.equal(report.topTags[0].tag, "desktop");
  assert.match(report.summary, /写作 1 篇/);
  assert.ok(report.recommendations.length >= 2);
});
