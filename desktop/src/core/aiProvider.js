import { randomUUID } from "node:crypto";

const DEFAULT_SYSTEM_PROMPT = `你是一个桌面助手，帮助用户管理待办任务和记录日记。

## 你的能力
1. 创建、查询、更新待办任务
2. 记录和查询日记
3. 生成每日计划和总结
4. 回答关于用户数据的问题

## 行为准则
- 用自然温暖的中文对话，语气像贴心的朋友
- 回答简洁有重点，避免冗长
- 当用户提到要做的事，主动询问是否需要创建待办
- 当用户分享生活内容，可以建议写入日记
- 可以同时执行多个操作（如创建待办+写日记）

## 可用工具
当你需要操作数据时，使用 function_call 格式：
{"function": "create_task", "args": {"title": "...", "description": "...", "priority": 0, "tags": ["..."], "dueAt": "..."}}
{"function": "list_tasks", "args": {"filter": "all|active|done"}}
{"function": "complete_task", "args": {"taskId": "..."}}
{"function": "delete_task", "args": {"taskId": "..."}}
{"function": "create_diary", "args": {"title": "...", "text": "...", "mood": "...", "tags": ["..."]}}
{"function": "search_diaries", "args": {"query": "..."}}
{"function": "get_summary", "args": {}}
{"function": "get_today_plan", "args": {}}

用户不能直接看到 function_call，你要在执行后告诉用户结果。`;

const FUNCTION_MAP = {
  create_task: (service, args) => {
    const task = service.addTask({ ...args, tags: args.tags || [] });
    return `已创建待办：「${task.title}」`;
  },
  list_tasks: (service, args) => {
    const filter = args.filter || "active";
    const tasks = service.listTasks(filter);
    if (!tasks.length) return "当前没有待办任务。";
    return `你共有 ${tasks.length} 个待办：\n${tasks.map((t, i) => `${i + 1}. [${t.status === "done" ? "完成" : "待办"}] ${t.title}${t.dueAt ? ` (截止: ${new Date(t.dueAt).toLocaleDateString("zh-CN")})` : ""}${t.priority > 0 ? ` [P${t.priority}]` : ""}`).join("\n")}`;
  },
  complete_task: (service, args) => {
    service.completeTask(args.taskId);
    return `已完成待办。`;
  },
  delete_task: (service, args) => {
    service.deleteTask(args.taskId);
    return `已删除待办。`;
  },
  create_diary: (service, args) => {
    const entry = service.createDiaryEntry({ title: args.title, text: args.text, mood: args.mood || "neutral", tags: args.tags || [] });
    return `日记已保存：「${entry.title}」`;
  },
  search_diaries: (service, args) => {
    const results = service.searchDiaries(args.query || "");
    if (!results.length) return "没有找到匹配的日记。";
    return `找到 ${results.length} 篇相关日记：\n${results.map((d, i) => `${i + 1}. ${d.title || "无标题"} (${new Date(d.createdAt).toLocaleDateString("zh-CN")})`).join("\n")}`;
  },
  get_summary: (service) => {
    const tasks = service.listTasks("all");
    const active = tasks.filter((t) => t.status !== "done");
    const done = tasks.filter((t) => t.status === "done");
    const diaries = service.listTasks && typeof service.searchDiaries === "function" ? [] : [];
    return `数据概况\n待办：共 ${tasks.length} 个（已完成 ${done.length} 个，进行中 ${active.length} 个）`;
  },
  get_today_plan: (service) => {
    const tasks = service.listTasks("active");
    const overdue = tasks.filter((t) => t.dueAt && new Date(t.dueAt) < new Date());
    if (!tasks.length) return "今天没有待办任务，可以放松或规划新目标。";
    let msg = `今天有 ${tasks.length} 个待办`;
    if (overdue.length) msg += `，其中 ${overdue.length} 个已逾期`;
    msg += `：\n${tasks.slice(0, 5).map((t, i) => `${i + 1}. ${t.title}`).join("\n")}`;
    return msg;
  }
};

// ── Local Rule-Based Agent ──

const INTENT_PATTERNS = [
  { keywords: ["创建", "添加", "新建", "增加", "写一个", "加一个"], context: ["任务", "待办", "todo"], intent: "create_task", required: "title" },
  { keywords: ["完成", "搞定", "做完了", "做好了", "标记完成"], context: ["任务", "待办", "todo"], intent: "complete_task", required: "query" },
  { keywords: ["列出", "查看", "显示", "有什么", "有哪些", "所有", "全部"], context: ["任务", "待办", "todo", "未完成", "已完成"], intent: "list_tasks" },
  { keywords: ["删除", "移除", "取消"], context: ["任务", "待办", "todo"], intent: "delete_task", required: "query" },
  { keywords: ["写", "记录", "创建", "保存"], context: ["日记", "日志", "日常", "今天"], intent: "create_diary", required: "text" },
  { keywords: ["搜索", "找", "查找", "查询"], context: ["日记", "日志"], intent: "search_diaries", required: "query" },
  { keywords: ["总结", "概况", "统计", "数据", "报告"], intent: "get_summary" },
  { keywords: ["计划", "今日", "今天", "日程"], intent: "get_today_plan" },
  { keywords: ["帮助", "help", "功能", "你会什么", "你能做什么"], intent: "help" },
  { keywords: ["早安", "早上好", "早啊", "上午好"], intent: "greeting_morning" },
  { keywords: ["午安", "中午好", "下午好"], intent: "greeting_afternoon" },
  { keywords: ["晚安", "晚上好", "晚好"], intent: "greeting_evening" },
];

function classifyIntent(text) {
  const lower = text.toLowerCase();
  for (const pattern of INTENT_PATTERNS) {
    const hasKeyword = pattern.keywords.some((k) => lower.includes(k));
    const hasContext = !pattern.context || pattern.context.some((c) => lower.includes(c));
    if (hasKeyword && hasContext) return pattern;
  }
  return null;
}

function extractArg(text, patterns) {
  for (const p of patterns) {
    const m = text.match(p);
    if (m) return m[1] || m[0];
  }
  return "";
}

function extractTaskTitle(text) {
  return extractArg(text, [
    /(?:叫|名为|标题为|标题叫)["""'']?(.+?)["""'']?(?:\s|$|的)/,
    /(?:创建|添加|新建|写)(?:一个|个)?(?:任务|待办|todo)?[:：\s]*["""'']?(.+?)["""'']?(?:\s|$|[，。！？])/,
    /(?:创建|添加|新建|写)(?:一个|个)?(?:任务|待办|todo)?\s+(.+)/,
  ]);
}

function extractDiaryText(text) {
  return extractArg(text, [
    /(?:内容|正文)[:：\s]*["""'']?(.+?)["""'']?(?:\s|$|[，。！？])/,
    /(?:写|记录|创建)(?:一篇|个)?(?:日记|日志)?[:：\s]*(?:关于|标题[为叫])?["""'']?(.+?)["""'']?(?:\s|$)/,
    /(?:写|记录)(?:下|了)?(.+)/,
  ]);
}

function extractSearchQuery(text) {
  return extractArg(text, [
    /(?:搜索|找|查找|查询)(?:关于|一下|下)?["""'']?(.+?)["""'']?(?:\s|$|[，。！？])/,
    /(?:关于|有关)(.+?)(?:的|的日记|的日志)/,
    /["""''](.+?)["""'']/,
  ]);
}

function executeIntent(intent, text, service) {
  switch (intent) {
    case "create_task": {
      const title = extractTaskTitle(text) || "";
      if (!title) return { content: "好的，想创建什么任务呢？请告诉我任务名称。", action: "ask_task_title" };
      const task = service.addTask({ title: title.trim(), tags: [] });
      return { content: `已创建待办：「${task.title}」\n还有其他需要帮忙的吗？`, stateChanged: true };
    }
    case "complete_task": {
      const query = extractSearchQuery(text) || text.replace(/完成|搞定|做完了|做好了|标记完成/g, "").trim();
      const tasks = service.listTasks("active");
      if (!tasks.length) return { content: "当前没有未完成的待办任务。" };
      const match = tasks.find((t) => t.title.includes(query));
      if (match) {
        service.completeTask(match.id);
        return { content: `已完成「${match.title}」。干得漂亮。`, stateChanged: true };
      }
      if (tasks.length <= 3) {
        return { content: `没找到匹配「${query}」的待办。当前待办：\n${tasks.map((t) => `- ${t.title}`).join("\n")}\n请问要完成哪一个？`, action: "ask_which_task" };
      }
      return { content: `没找到匹配「${query}」的待办。试试说出更具体的任务名称？` };
    }
    case "list_tasks": {
      const filter = text.includes("已完成") || text.includes("done") ? "done" : "active";
      const tasks = service.listTasks(filter);
      if (!tasks.length) return { content: filter === "done" ? "还没有已完成的待办，加油。" : "当前没有待办任务。" };
      const label = filter === "done" ? "已完成" : "进行中";
      const lines = tasks.map((t, i) => `${i + 1}. ${t.title}${t.priority > 0 ? ` [P${t.priority}]` : ""}${t.dueAt ? ` (${new Date(t.dueAt).toLocaleDateString("zh-CN")})` : ""}`);
      return { content: `${label}待办（共 ${tasks.length} 个）：\n${lines.join("\n")}` };
    }
    case "delete_task": {
      const query = extractSearchQuery(text) || text.replace(/删除|移除|取消/g, "").trim();
      const tasks = service.listTasks("all");
      const match = tasks.find((t) => t.title.includes(query));
      if (match) {
        service.deleteTask(match.id);
        return { content: `已删除「${match.title}」`, stateChanged: true };
      }
      return { content: `没找到匹配「${query}」的待办。` };
    }
    case "create_diary": {
      const diaryText = extractDiaryText(text) || text.replace(/写|记录|创建|一篇|个|日记|日志/g, "").trim();
      if (!diaryText || diaryText.length < 3) return { content: "好的，想记录什么呢？告诉我今天发生了什么。", action: "ask_diary_content" };
      const entry = service.createDiaryEntry({
        title: diaryText.slice(0, 30) + (diaryText.length > 30 ? "..." : ""),
        text: diaryText,
        mood: "neutral",
        tags: []
      });
      const insight = generateDiaryInsight(entry, service);
      return { content: `日记已保存\n\n${insight}`, stateChanged: true };
    }
    case "search_diaries": {
      const query = extractSearchQuery(text) || text.replace(/搜索|找|查找|查询/g, "").trim();
      if (!query) return { content: "想搜索什么内容呢？告诉我关键词。" };
      const results = service.searchDiaries(query);
      if (!results.length) return { content: `没有找到包含「${query}」的日记。` };
      const lines = results.map((d, i) => `${i + 1}. ${d.title || "无标题"} (${new Date(d.createdAt).toLocaleDateString("zh-CN")})`);
      return { content: `找到 ${results.length} 篇相关日记：\n${lines.join("\n")}` };
    }
    case "get_summary": {
      const allTasks = service.listTasks("all");
      const active = allTasks.filter((t) => t.status !== "done");
      const done = allTasks.filter((t) => t.status === "done");
      return {
        content: `**数据概况**\n\n**待办任务**\n- 总计：${allTasks.length} 个\n- 进行中：${active.length} 个\n- 已完成：${done.length} 个${active.length > 5 ? `\n- 待办较多，建议优先处理高优先级任务。` : ""}\n\n${done.length > 10 ? "最近完成效率不错，继续保持。" : done.length > 0 ? "已有一些完成记录，可以回顾一下成就。" : "还没有完成记录，今天开始行动起来吧。"}`
      };
    }
    case "get_today_plan": {
      const tasks = service.listTasks("active");
      const overdue = tasks.filter((t) => t.dueAt && new Date(t.dueAt) < new Date());
      if (!tasks.length) return { content: "今天没有待办任务，可以放松一下或规划新目标。" };
      let msg = `**今日待办（共 ${tasks.length} 个）**`;
      if (overdue.length) msg += `\n其中 ${overdue.length} 个已逾期`;
      msg += `\n\n${tasks.slice(0, 8).map((t, i) => `${i + 1}. ${t.title}${t.priority > 0 ? ` [P${t.priority}]` : ""}${t.dueAt ? ` [截止 ${new Date(t.dueAt).toLocaleDateString("zh-CN")}]` : ""}${t.status === "done" ? " [完成]" : ""}`).join("\n")}`;
      if (tasks.length > 8) msg += `\n...还有 ${tasks.length - 8} 个`;
      return { content: msg };
    }
    case "help": {
      return { content: `**我能帮你做什么？**\n\n**任务管理**\n- "帮我创建一个任务叫..."\n- "完成...任务"\n- "列出我的待办"\n- "删除...任务"\n\n**日记**\n- "记录一下今天..."\n- "搜索关于...的日记"\n\n**数据**\n- "总结一下我的数据"\n- "今天有什么计划"\n\n也可以直接和我聊天。` };
    }
    case "greeting_morning":
      return { content: `早安！新的一天开始了。\n\n${getDailyBriefing(service)}` };
    case "greeting_afternoon":
      return { content: `下午好！今天过得怎么样？\n\n${getDailyBriefing(service)}` };
    case "greeting_evening":
      return { content: `晚上好！今天辛苦了。\n\n${getDailyBriefing(service)}` };
    default:
      return null;
  }
}

function generateDiaryInsight(entry, service) {
  const text = entry.text || "";
  const insights = [];
  if (text.length > 50) insights.push("记录得很详细，坚持写日记是很好的习惯。");
  else insights.push("可以多写一些细节，以后回顾会更有意思。");

  if (text.includes("开心") || text.includes("快乐") || text.includes("高兴")) insights.push("看起来今天有开心的事，真为你高兴。");
  if (text.includes("累") || text.includes("疲惫") || text.includes("辛苦")) insights.push("今天辛苦了，记得好好休息。");
  if (text.includes("工作") || text.includes("项目") || text.includes("任务")) insights.push("工作上又有了新进展，继续保持。");
  if (text.includes("朋友") || text.includes("家人") || text.includes("聚会")) insights.push("和身边的人度过了美好的时光。");

  const taskMention = extractPotentialTask(text);
  if (taskMention) insights.push(`我注意到你提到了「${taskMention}」，需要帮你创建待办吗？`);
  return insights.join("\n\n");
}

function extractPotentialTask(text) {
  const patterns = [
    /(?:要|需要|得|该|应该|必须)(.+?)(?:了|一下|的|吧|\.|。|！|？)/,
    /(?:明天|下周|下个月|改天|回头)(.+?)(?:吧|哦|哈|\.|。)/,
    /(?:忘了|差点忘了|记得)(.+?)(?:\.|。|！|？)/,
  ];
  for (const p of patterns) {
    const m = text.match(p);
    if (m) return m[1].trim().slice(0, 30);
  }
  return "";
}

function getDailyBriefing(service) {
  const activeTasks = service.listTasks("active");
  const allTasks = service.listTasks("all");
  const done = allTasks.filter((t) => t.status === "done");
  const overdue = activeTasks.filter((t) => t.dueAt && new Date(t.dueAt) < new Date());

  const parts = [];
  if (activeTasks.length === 0) {
    parts.push("今天没有待办任务，可以轻松度过一天。");
  } else {
    parts.push(`今天有 **${activeTasks.length}** 个待办任务${overdue.length > 0 ? `（其中 ${overdue.length} 个已逾期）` : ""}`);
  }
  if (done.length > 0) {
    parts.push(`最近已完成 **${done.length}** 个任务，效率不错。`);
  }
  return parts.join("\n\n");
}

function getProactiveSuggestion(service) {
  const activeTasks = service.listTasks("active");
  const allTasks = service.listTasks("all");
  const done = allTasks.filter((t) => t.status === "done");

  const hour = new Date().getHours();

  if (activeTasks.length === 0 && done.length === 0) {
    return "欢迎使用 DiaryApp！想创建第一个任务或记录第一篇日记吗？";
  }
  if (activeTasks.length === 0 && done.length > 0) {
    return "所有任务都完成了！要不要回顾一下今天的日记，或者规划一下明天的任务？";
  }
  if (hour < 11 && activeTasks.length > 0) {
    const urgent = activeTasks.filter((t) => t.priority >= 2);
    if (urgent.length > 0) {
      return `早上好！今天有 ${urgent.length} 个高优先级任务需要优先处理，加油！`;
    }
    return `早上好！今天有 ${activeTasks.length} 个待办任务等着你。`;
  }
  if (hour >= 11 && hour < 14 && activeTasks.length > 0) {
    return `中午好！上午进展如何？还有 ${activeTasks.length} 个待办要处理。`;
  }
  if (hour >= 17 && hour < 19) {
    const unfinished = activeTasks.length;
    if (unfinished === 0) return "傍晚了，今天任务都完成了，真棒！好好享受晚上吧。";
    return `傍晚了，还有 ${unfinished} 个待办未完成。今天能搞定吗？加油！`;
  }
  if (hour >= 19) {
    if (activeTasks.length === 0) return "晚安！今天过得很充实，早点休息。";
    return `夜深了，还有 ${activeTasks.length} 个待办。如果太累了就明天再做吧，休息也很重要。`;
  }
  return null;
}

function analyzeSentiment(text) {
  const positive = ["开心", "快乐", "高兴", "幸福", "棒", "好", "不错", "顺利", "成功", "感谢", "感动", "喜欢", "爱", "温暖", "美好", "进步"];
  const negative = ["累", "疲惫", "辛苦", "难过", "伤心", "烦", "焦虑", "担心", "压力", "失败", "差", "不好", "糟糕", "生气", "失望", "孤独"];
  let score = 0;
  for (const w of positive) if (text.includes(w)) score++;
  for (const w of negative) if (text.includes(w)) score--;
  return score;
}

function generateChatResponse(text, service) {
  const lower = text.toLowerCase();
  if (lower.includes("你好") || lower.includes("hi") || lower.includes("hello")) {
    return `你好呀！${getDailyBriefing(service)}`;
  }
  if (lower.includes("谢谢") || lower.includes("感谢")) {
    return "不客气！有什么需要随时找我。";
  }
  if (lower.includes("你是谁") || lower.includes("你是什么")) {
    return "我是你的桌面助手，可以帮你管理任务、记录日记、分析数据。说「帮助」查看我能做什么。";
  }
  if (/不错|很好|开心|棒/.test(lower)) {
    return "太好了！保持好心情，一天都会很顺利。";
  }
  if (/累|疲惫|辛苦|烦/.test(lower)) {
    return "听起来今天不太轻松。记得休息一下，泡杯茶放松放松。";
  }
  if (text.length > 20 && !classifyIntent(text)) {
    const sentiment = analyzeSentiment(text);
    if (sentiment > 0) return "听起来今天过得不错！想把这些写进日记吗？";
    if (sentiment < 0) return "听起来有些困扰。如果你愿意，可以详细说说，或者写进日记里释放一下。";
    return "了解了。想把这个记录成日记或待办吗？我可以帮你。";
  }
  return null;
}

// ── Public API ──

export async function chatWithAI({ messages, settings, service }) {
  const { provider, endpoint, apiKey, model } = resolveSettings(settings);

  // Local mode: rule-based agent
  if (!apiKey || provider === "local") {
    return processLocally({ messages, service });
  }

  // Remote API mode
  const systemMsg = { role: "system", content: DEFAULT_SYSTEM_PROMPT };
  const apiMessages = [systemMsg, ...messages.map((m) => ({ role: m.role, content: m.content }))];

  try {
    const response = await fetch(endpoint, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${apiKey}`
      },
      body: JSON.stringify({
        model: model || "deepseek-chat",
        messages: apiMessages,
        temperature: 0.7,
        max_tokens: 2048,
        stream: false
      })
    });

    if (!response.ok) {
      const errText = await response.text().catch(() => "unknown error");
      return { role: "assistant", content: `API 请求失败 (${response.status}): ${errText}` };
    }

    const data = await response.json();
    const reply = data.choices?.[0]?.message?.content || "";

    const functionCalls = parseFunctionCalls(reply);
    if (functionCalls.length > 0) {
      const cleanReply = reply.replace(/\{"function":.*?\}\s*/gs, "").trim();
      const results = [];
      for (const fc of functionCalls) {
        const handler = FUNCTION_MAP[fc.function];
        if (handler && service) {
          try {
            const result = handler(service, fc.args);
            results.push(result);
          } catch (e) {
            results.push(`执行 ${fc.function} 失败: ${e.message}`);
          }
        }
      }
      const combinedContent = [cleanReply, ...results].filter(Boolean).join("\n\n");
      return { role: "assistant", content: combinedContent || results.join("\n"), stateChanged: true };
    }

    return { role: "assistant", content: reply };
  } catch (err) {
    return { role: "assistant", content: `连接失败: ${err.message}。已切换到本地模式。`, localFallback: true };
  }
}

export function processLocally({ messages, service }) {
  const lastMsg = messages[messages.length - 1];
  if (!lastMsg || lastMsg.role !== "user") {
    return { role: "assistant", content: "你好！有什么可以帮你的吗？" };
  }

  const text = lastMsg.content.trim();
  if (!text) return { role: "assistant", content: "请说点什么吧。" };

  // Try intent classification first
  const intentMatch = classifyIntent(text);
  if (intentMatch) {
    const result = executeIntent(intentMatch.intent, text, service);
    if (result) {
      return { role: "assistant", content: result.content, stateChanged: result.stateChanged || false, action: result.action || null };
    }
  }

  // Fallback to chat response
  const chatReply = generateChatResponse(text, service);
  if (chatReply) {
    return { role: "assistant", content: chatReply };
  }

  // Generic response
  return { role: "assistant", content: `嗯，我在听。可以试试说「帮助」查看我能做什么，或者直接告诉我你想创建什么任务或记录什么日记。` };
}

export function getDailyBriefingForProactive(service) {
  return {
    content: getDailyBriefing(service),
    suggestion: getProactiveSuggestion(service)
  };
}

export function getProactiveAgentMessage(service) {
  const suggestion = getProactiveSuggestion(service);
  if (!suggestion) return null;
  return { role: "assistant", content: suggestion, proactive: true };
}

export async function testAiConnection(settings) {
  const { provider, endpoint, apiKey, model } = resolveSettings(settings);
  if (!apiKey) {
    return { ok: true, message: "本地模式已就绪（未配置 API Key 时使用本地智能助手）" };
  }
  if (!endpoint) return { ok: false, message: "请填写 API 地址" };

  try {
    const response = await fetch(endpoint, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${apiKey}`
      },
      body: JSON.stringify({
        model: model || "deepseek-chat",
        messages: [{ role: "user", content: "Hello" }],
        max_tokens: 10,
        stream: false
      })
    });
    if (!response.ok) {
      const text = await response.text().catch(() => "");
      return { ok: false, message: `HTTP ${response.status}: ${text.slice(0, 200)}` };
    }
    return { ok: true, message: "连接成功（已配置 API，将使用 AI 模型）" };
  } catch (err) {
    return { ok: false, message: `连接失败: ${err.message}` };
  }
}

function parseFunctionCalls(text) {
  const regex = /\{"function":\s*"([^"]+)",\s*"args":\s*(\{.*?\})\}/gs;
  const calls = [];
  let match;
  while ((match = regex.exec(text)) !== null) {
    try {
      calls.push({ function: match[1], args: JSON.parse(match[2]) });
    } catch { /* skip invalid JSON */ }
  }
  return calls;
}

function resolveSettings(settings = {}) {
  const provider = settings.provider || "local";
  const presets = {
    deepseek: {
      endpoint: "https://api.deepseek.com/chat/completions",
      model: "deepseek-chat"
    },
    openai: {
      endpoint: "https://api.openai.com/v1/chat/completions",
      model: "gpt-4o-mini"
    },
    local: {
      endpoint: "",
      model: ""
    },
    custom: {
      endpoint: "",
      model: ""
    }
  };
  const preset = presets[provider] || presets.local;
  return {
    provider,
    endpoint: settings.endpoint || preset.endpoint,
    apiKey: settings.apiKey || "",
    model: settings.model || preset.model
  };
}
