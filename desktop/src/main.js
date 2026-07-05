import { app, BrowserWindow, dialog, ipcMain, Menu, Notification, Tray, globalShortcut } from "electron";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { resolveDesktopUserDataPath } from "./core/installPaths.js";
import { JsonStore } from "./core/jsonStore.js";
import { chatWithAI, testAiConnection, getProactiveAgentMessage } from "./core/aiProvider.js";
import * as Service from "./core/desktopService.js";
import { createDesktopState } from "./core/taskEngine.js";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

let state = createDesktopState();
let store = null;
let mainWindow = null;
let tray = null;

configureUserDataPath();

function createWindow() {
  const win = new BrowserWindow({
    width: 1200,
    height: 800,
    minWidth: 900,
    minHeight: 600,
    title: "DiaryApp Desktop",
    backgroundColor: "#F5F0EB",
    autoHideMenuBar: true,
    show: false,
    webPreferences: {
      preload: path.join(__dirname, "preload.cjs"),
      contextIsolation: true,
      nodeIntegration: false
    }
  });
  mainWindow = win;

  win.once("ready-to-show", () => win.show());
  win.loadFile(path.join(__dirname, "renderer", "index.html"));
}

function showMainWindow(route) {
  if (!mainWindow) createWindow();
  if (!mainWindow) return;
  if (mainWindow.isMinimized()) mainWindow.restore();
  mainWindow.show();
  mainWindow.focus();
  if (route) {
    mainWindow.webContents.send("desktop:shortcut", route);
  }
}

function createTray() {
  if (tray) return tray;
  const iconPath = path.join(__dirname, "assets", "icon.ico");
  tray = new Tray(iconPath);
  tray.setToolTip("DiaryApp Desktop");
  tray.setContextMenu(Menu.buildFromTemplate([
    { label: "打开 DiaryApp", click: () => showMainWindow() },
    { label: "快速写日记", click: () => showMainWindow("new-diary") },
    { label: "新建待办", click: () => showMainWindow("new-task") },
    { type: "separator" },
    { label: "退出", click: () => app.quit() }
  ]));
  tray.on("click", () => showMainWindow());
  return tray;
}

function registerGlobalShortcuts() {
  globalShortcut.register("Alt+Space", () => showMainWindow("search"));
  globalShortcut.register("CommandOrControl+Shift+D", () => showMainWindow("new-diary"));
  globalShortcut.register("CommandOrControl+Shift+T", () => showMainWindow("new-task"));
  globalShortcut.register("CommandOrControl+Shift+F", () => showMainWindow("search"));
}

// ── State Helpers ──
function buildViewModel() {
  return {
    tasks: state.tasks || [],
    diaries: state.diaries || [],
    archive: state.archive || [],
    aiDrafts: state.aiDrafts || [],
    account: state.account || {},
    lastSync: state.lastSync || state.account?.lastSyncAt || null,
    settings: state.settings || {}
  };
}

function persist() {
  if (store) store.save(state);
}

// ================================================================
// IPC Handlers
// ================================================================

// Get state
ipcMain.handle("desktop:get-state", () => buildViewModel());

// ── Settings ──
ipcMain.handle("desktop:save-settings", (_event, settings) => {
  state = Service.saveSettings(state, settings);
  persist();
  return { ok: true };
});

// ── AI Chat ──
ipcMain.handle("desktop:send-chat", async (_event, input) => {
  const settings = state.settings || {};
  const service = createAiServiceProxy();
  const result = await chatWithAI({
    messages: input.messages || [],
    settings,
    service
  });
  // Check if service calls modified state
  const stateChanged = await checkStateChanged();
  persist();
  return { ...result, stateChanged };
});

ipcMain.handle("desktop:test-ai", async (_event, settings) => {
  return await testAiConnection(settings);
});

// ── Tasks ──
ipcMain.handle("desktop:add-task", (_event, input) => {
  state = Service.addTask(state, input);
  persist();
  return buildViewModel();
});

ipcMain.handle("desktop:update-task", (_event, id, patch) => {
  state = Service.updateTask(state, id, patch);
  persist();
  return buildViewModel();
});

ipcMain.handle("desktop:delete-task", (_event, id) => {
  state = Service.deleteTask(state, id);
  persist();
  return buildViewModel();
});

ipcMain.handle("desktop:complete-task", (_event, id) => {
  state = Service.completeTask(state, id);
  persist();
  return buildViewModel();
});

ipcMain.handle("desktop:capture-tasks", (_event, text) => {
  state = Service.captureTasks(state, text);
  persist();
  return buildViewModel();
});

// ── Diary ──
ipcMain.handle("desktop:create-diary-entry", (_event, input) => {
  state = Service.createDiaryEntry(state, input);
  persist();
  return buildViewModel();
});

ipcMain.handle("desktop:create-drafts-from-diary", (_event, diaryId) => {
  state = Service.createDraftsFromDiary(state, diaryId);
  persist();
  return buildViewModel();
});

ipcMain.handle("desktop:update-diary-entry", (_event, diaryId, patch) => {
  state = Service.updateDiaryEntry(state, diaryId, patch);
  persist();
  return buildViewModel();
});

ipcMain.handle("desktop:attach-files", (_event, diaryId, files = []) => {
  state = Service.attachFilesToDiary(state, diaryId, files);
  persist();
  return buildViewModel();
});

ipcMain.handle("desktop:get-proactive-suggestion", () => {
  const service = createAiServiceProxy();
  return getProactiveAgentMessage(service);
});

ipcMain.handle("desktop:sync-now", async () => {
  const result = await Service.syncWithCloud(state);
  state = result.state;
  persist();
  if (!result.ok) throw new Error(result.message);
  return { ok: true, message: result.message };
});

ipcMain.handle("desktop:sync-login", async (_event, input) => {
  const result = await Service.loginSyncAccount(state, input);
  state = result.state;
  persist();
  return { ok: result.ok, message: result.message, state: buildViewModel() };
});

ipcMain.handle("desktop:sync-register", async (_event, input) => {
  const result = await Service.registerSyncAccount(state, input);
  state = result.state;
  persist();
  return { ok: result.ok, message: result.message, state: buildViewModel() };
});

ipcMain.handle("desktop:sync-logout", () => {
  state = Service.logoutSyncAccount(state);
  persist();
  return { ok: true, state: buildViewModel() };
});

// ── Data Export/Import ──
ipcMain.handle("desktop:export-file", async (_event, exportFormat = "json") => {
  const format = ["json", "md", "html"].includes(exportFormat) ? exportFormat : "json";
  const extension = format === "md" ? "md" : format === "html" ? "html" : "json";
  const result = await dialog.showSaveDialog({
    title: "导出数据",
    defaultPath: `DiaryApp-${new Date().toISOString().slice(0, 10)}.${extension}`,
    filters: [
      { name: "DiaryApp Export", extensions: [extension] },
      { name: "All Files", extensions: ["*"] }
    ]
  });
  if (result.canceled || !result.filePath) return { canceled: true };
  fs.writeFileSync(result.filePath, buildExportPayload(format), "utf8");
  if (Notification.isSupported()) {
    new Notification({ title: "DiaryApp Desktop", body: "导出完成" }).show();
  }
  return { canceled: false };
});

ipcMain.handle("desktop:import-file", async (_event, importFormat = "json") => {
  const format = ["json", "markdown", "day-one", "bear"].includes(importFormat) ? importFormat : "json";
  const filtersByFormat = {
    json: [
      { name: "JSON", extensions: ["json"] },
      { name: "All Files", extensions: ["*"] }
    ],
    markdown: [
      { name: "Markdown", extensions: ["md", "markdown", "txt"] },
      { name: "All Files", extensions: ["*"] }
    ],
    "day-one": [
      { name: "Day One Export", extensions: ["json", "zip"] },
      { name: "All Files", extensions: ["*"] }
    ],
    bear: [
      { name: "Bear Export", extensions: ["json", "md", "markdown", "txt", "bear"] },
      { name: "All Files", extensions: ["*"] }
    ]
  };
  const filters = filtersByFormat[format] || filtersByFormat.json;
  const result = await dialog.showOpenDialog({
    title: "导入数据",
    properties: format === "json" ? ["openFile"] : ["openFile", "multiSelections"],
    filters
  });
  if (result.canceled || !result.filePaths[0]) return buildViewModel();
  if (format === "markdown") {
    const files = result.filePaths.map((filePath) => ({
      name: path.basename(filePath),
      path: filePath,
      content: fs.readFileSync(filePath, "utf8")
    }));
    state = Service.importMarkdownArchive(state, files);
  } else if (format === "day-one" || format === "bear") {
    const files = result.filePaths.map((filePath) => ({
      name: path.basename(filePath),
      path: filePath,
      content: fs.readFileSync(filePath, "utf8")
    }));
    state = Service.importExternalDiaryArchive(state, files, format);
  } else {
    const payload = fs.readFileSync(result.filePaths[0], "utf8");
    state = Service.importState(state, payload);
  }
  persist();
  return buildViewModel();
});

// ================================================================
// AI Service Proxy (for function calling from AI)
// ================================================================
function createAiServiceProxy() {
  return {
    addTask: (input) => {
      state = Service.addTask(state, input);
      const tasks = state.tasks || [];
      return tasks[0] || input;
    },
    listTasks: (filter) => {
      return Service.listTasks(state, filter || "active");
    },
    completeTask: (id) => {
      state = Service.completeTask(state, id);
    },
    deleteTask: (id) => {
      state = Service.deleteTask(state, id);
    },
    createDiaryEntry: (input) => {
      state = Service.createDiaryEntry(state, input);
      const diaries = state.diaries || [];
      return diaries[0] || input;
    },
    searchDiaries: (query) => {
      return Service.searchDiaries(state, query);
    },
    createDraftsFromDiary: (diaryId) => {
      state = Service.createDraftsFromDiary(state, diaryId);
    }
  };
}

function buildExportPayload(exportFormat) {
  if (exportFormat === "md") {
    const diaries = (state.diaries || []).map((diary) => `# ${diary.title || "无标题"}\n\n${diary.text || ""}`).join("\n\n---\n\n");
    const tasks = (state.tasks || []).map((task) => `- [${task.status === "done" ? "x" : " "}] ${task.title}`).join("\n");
    return `# DiaryApp Export\n\n## Diaries\n\n${diaries || "暂无日记"}\n\n## Tasks\n\n${tasks || "暂无任务"}\n`;
  }
  if (exportFormat === "html") {
    const body = Service.exportState(state)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;");
    return `<!doctype html><html lang="zh-CN"><meta charset="utf-8"><title>DiaryApp Export</title><body><pre>${body}</pre></body></html>`;
  }
  return Service.exportState(state);
}

async function checkStateChanged() {
  // Simple check: if we modified state during AI processing,
  // the renderer needs to refresh
  return true; // Always refresh after AI chat
}

// ================================================================
// App Lifecycle
// ================================================================
app.whenReady().then(() => {
  store = new JsonStore(path.join(app.getPath("userData"), "state.json"));
  state = createDesktopState(store.load(state) || {});
  createWindow();
  createTray();
  registerGlobalShortcuts();
  // Auto-sync every 2 hours
  setInterval(() => {
    if (state.account?.token) {
      Service.syncWithCloud(state).then((r) => {
        if (r.state) { state = r.state; persist(); }
      }).catch(() => {});
    }
  }, 2 * 60 * 60 * 1000);
});

app.on("window-all-closed", () => {
  app.quit();
});

app.on("before-quit", () => {
  persist();
  globalShortcut.unregisterAll();
});

function configureUserDataPath() {
  const preferredPath = resolveDesktopUserDataPath();
  if (!preferredPath) return;
  try {
    fs.mkdirSync(preferredPath, { recursive: true });
    app.setPath("userData", preferredPath);
  } catch {
    // ignore
  }
}
