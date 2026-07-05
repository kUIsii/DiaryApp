import test from "node:test";
import assert from "node:assert/strict";
import { processLocally, getProactiveAgentMessage, getDailyBriefingForProactive, testAiConnection } from "./aiProvider.js";

function createMockService() {
  return {
    addTask: (input) => ({ ...input, id: "mock-1" }),
    listTasks: (filter) => [],
    completeTask: (id) => {},
    deleteTask: (id) => {},
    createDiaryEntry: (input) => ({ ...input, id: "diary-1" }),
    searchDiaries: (query) => [],
    createDraftsFromDiary: (diaryId) => {}
  };
}

test("processLocally returns greeting on empty messages", () => {
  const result = processLocally({ messages: [], service: createMockService() });
  assert.match(result.content, /你好/);
});

test("processLocally classifies create task intent", () => {
  const service = createMockService();
  let created = null;
  service.addTask = (input) => { created = input; return { ...input, id: "t1" }; };
  const result = processLocally({
    messages: [{ role: "user", content: "帮我创建一个任务叫买牛奶" }],
    service
  });
  assert.match(result.content, /已创建待办/);
  assert.equal(created?.title, "买牛奶");
  assert.equal(result.stateChanged, true);
});

test("processLocally classifies list tasks intent", () => {
  const service = createMockService();
  service.listTasks = (filter) => {
    assert.equal(filter, "active");
    return [{ id: "1", title: "测试任务", status: "active" }];
  };
  const result = processLocally({
    messages: [{ role: "user", content: "列出我的待办" }],
    service
  });
  assert.match(result.content, /测试任务/);
});

test("processLocally classifies help intent", () => {
  const result = processLocally({
    messages: [{ role: "user", content: "你能做什么" }],
    service: createMockService()
  });
  assert.match(result.content, /我能帮你做什么/);
  assert.match(result.content, /任务管理/);
  assert.match(result.content, /日记/);
});

test("processLocally provides daily briefing on greeting", () => {
  const result = processLocally({
    messages: [{ role: "user", content: "早上好" }],
    service: createMockService()
  });
  assert.match(result.content, /早安/);
});

test("getProactiveAgentMessage returns suggestion with data", () => {
  const service = createMockService();
  const result = getProactiveAgentMessage(service);
  assert.ok(result);
  assert.match(result.content, /欢迎/);
  assert.equal(result.proactive, true);
});

test("testAiConnection returns ok for local mode without API key", async () => {
  const result = await testAiConnection({ provider: "local" });
  assert.equal(result.ok, true);
  assert.match(result.message, /本地模式已就绪/);
});

test("testAiConnection returns error when API endpoint fails", async () => {
  const originalFetch = global.fetch;
  global.fetch = async () => ({ ok: false, status: 401, text: async () => "Unauthorized" });
  try {
    const result = await testAiConnection({ provider: "deepseek", apiKey: "bad-key" });
    assert.equal(result.ok, false);
  } finally {
    global.fetch = originalFetch;
  }
});

test("getDailyBriefingForProactive returns content and suggestion", () => {
  const service = createMockService();
  const result = getDailyBriefingForProactive(service);
  assert.match(result.content, /没有待办任务/);
  assert.ok(result.suggestion);
});
