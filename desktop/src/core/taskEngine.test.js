import test from "node:test";
import assert from "node:assert/strict";
import {
  buildDayPlan,
  buildHabitEvidence,
  buildTaskDependencyHints,
  buildTaskHealthReport,
  buildTaskDraftFromDiary,
  groupTasksByProject,
  parseTaskCapture,
  createDesktopState,
  summarizeTaskBuckets
} from "./taskEngine.js";

test("summarizeTaskBuckets separates inbox, planned, overdue, done, and habits", () => {
  const now = new Date("2026-07-04T09:00:00+08:00").getTime();
  const state = createDesktopState({
    now,
    tasks: [
      { id: "t1", title: "写周报", status: "planned", dueAt: "2026-07-04T18:00:00+08:00", type: "task" },
      { id: "t2", title: "整理照片", status: "inbox", type: "task" },
      { id: "t3", title: "过期材料", status: "planned", dueAt: "2026-07-03T18:00:00+08:00", type: "task" },
      { id: "t4", title: "已完成", status: "done", type: "task" },
      { id: "h1", title: "每日记录", status: "active", type: "habit" }
    ]
  });

  const result = summarizeTaskBuckets(state, now);

  assert.deepEqual(result.counts, { inbox: 1, planned: 1, overdue: 1, done: 1, habits: 1 });
  assert.equal(result.overdue[0].id, "t3");
});

test("buildTaskDraftFromDiary extracts actionable diary sentences into confirmable AI drafts", () => {
  const drafts = buildTaskDraftFromDiary(
    "今天要把网页版待办中心做出来。别忘了晚上整理照片，还需要周日之前备份数据。"
  );

  assert.equal(drafts.length, 3);
  assert.equal(drafts[0].source, "diary");
  assert.equal(drafts[0].status, "draft");
  assert.match(drafts[0].title, /网页版待办中心/);
  assert.ok(drafts.every((draft) => draft.confidence >= 0.62 && draft.confidence <= 0.9));
});

test("parseTaskCapture turns multiline natural language into task inputs", () => {
  const now = new Date("2026-07-04T08:00:00+08:00").getTime();

  const tasks = parseTaskCapture(`
    !! 今天 18:30 完成桌面端 AI 设置 #desktop #ai
    ! 明天 整理发布清单 #release
    后天 09:00 复盘待办流程
  `, now);

  assert.equal(tasks.length, 3);
  assert.equal(tasks[0].title, "完成桌面端 AI 设置");
  assert.equal(tasks[0].priority, 2);
  assert.deepEqual(tasks[0].tags, ["desktop", "ai"]);
  assert.equal(tasks[0].dueAt, "2026-07-04T18:30:00.000+08:00");
  assert.equal(tasks[1].title, "整理发布清单");
  assert.equal(tasks[1].priority, 1);
  assert.equal(tasks[1].dueAt, "2026-07-05T20:00:00.000+08:00");
  assert.equal(tasks[2].dueAt, "2026-07-06T09:00:00.000+08:00");
});

test("buildTaskDependencyHints identifies likely blocked tasks by shared subject", () => {
  const hints = buildTaskDependencyHints([
    { id: "a", title: "准备发布清单", status: "planned", type: "task" },
    { id: "b", title: "发布桌面端版本", status: "planned", type: "task" },
    { id: "c", title: "整理旅行照片", status: "done", type: "task" }
  ]);

  assert.equal(hints.length, 1);
  assert.equal(hints[0].taskId, "b");
  assert.equal(hints[0].blockedBy, "a");
  assert.match(hints[0].reason, /准备发布清单/);
});

test("buildTaskHealthReport summarizes actionable task quality", () => {
  const now = new Date("2026-07-04T08:00:00+08:00").getTime();
  const report = buildTaskHealthReport([
    { id: "a", title: "模糊任务", status: "inbox", type: "task", tags: [] },
    { id: "b", title: "发布桌面端", status: "planned", type: "task", priority: 2, dueAt: "2026-07-04T09:00:00+08:00", tags: ["desktop"] },
    { id: "c", title: "整理资料", status: "done", type: "task", tags: ["archive"] }
  ], now);

  assert.equal(report.active, 2);
  assert.equal(report.missingDue, 1);
  assert.equal(report.missingTags, 1);
  assert.equal(report.focusScore, 50);
  assert.equal(report.nextFixes[0].taskId, "a");
  assert.match(report.nextFixes[0].reason, /截止时间/);
});

test("groupTasksByProject builds project lanes from tags and sources", () => {
  const groups = groupTasksByProject([
    { id: "a", title: "桌面端 UI", status: "planned", type: "task", tags: ["desktop", "ui"] },
    { id: "b", title: "AI 设置", status: "inbox", type: "task", tags: ["ai"] },
    { id: "c", title: "无标签", status: "planned", type: "task", tags: [] }
  ]);

  assert.deepEqual(groups.map((group) => group.key), ["desktop", "ai", "uncategorized"]);
  assert.equal(groups[0].tasks[0].id, "a");
  assert.equal(groups[2].label, "未归档");
});

test("buildDayPlan creates ordered plan with energy, risk, and AI reasoning", () => {
  const now = new Date("2026-07-04T09:00:00+08:00").getTime();
  const state = createDesktopState({
    now,
    tasks: [
      { id: "a", title: "低优先级备忘", status: "inbox", priority: 0, type: "task" },
      { id: "b", title: "今天截止高优先级", status: "planned", priority: 2, dueAt: "2026-07-04T12:00:00+08:00", type: "task" },
      { id: "c", title: "已经完成", status: "done", priority: 2, type: "task" }
    ],
    habits: [
      { id: "h", title: "每日记录", streak: 6, doneToday: false }
    ]
  });

  const plan = buildDayPlan(state, now);

  assert.equal(plan.blocks[0].taskId, "b");
  assert.equal(plan.blocks[0].risk, "high");
  assert.match(plan.summary, /今天截止高优先级/);
  assert.ok(plan.blocks.some((block) => block.kind === "habit"));
});

test("buildHabitEvidence links diary tags and content to habits without silently completing them", () => {
  const evidence = buildHabitEvidence({
    diary: {
      id: "d1",
      text: "今天完成了 200 字记录，也散步了二十分钟。",
      tagIds: ["writing", "walk"]
    },
    habits: [
      { id: "h1", title: "每日记录", linkedTagIds: ["writing"] },
      { id: "h2", title: "散步", linkedTagIds: ["walk"] }
    ]
  });

  assert.equal(evidence.length, 2);
  assert.equal(evidence[0].action, "propose-checkin");
  assert.equal(evidence[0].requiresConfirmation, true);
  assert.match(evidence[0].reason, /标签|内容/);
});
