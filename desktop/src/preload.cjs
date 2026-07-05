const { contextBridge, ipcRenderer } = require("electron");

contextBridge.exposeInMainWorld("diaryDesktop", {
  // State
  getState: () => ipcRenderer.invoke("desktop:get-state"),

  // AI Chat
  sendChat: (input) => ipcRenderer.invoke("desktop:send-chat", input),
  testAiConnection: (settings) => ipcRenderer.invoke("desktop:test-ai", settings),
  saveSettings: (settings) => ipcRenderer.invoke("desktop:save-settings", settings),
  getProactiveSuggestion: () => ipcRenderer.invoke("desktop:get-proactive-suggestion"),

  // Tasks
  addTask: (input) => ipcRenderer.invoke("desktop:add-task", input),
  updateTask: (id, patch) => ipcRenderer.invoke("desktop:update-task", id, patch),
  deleteTask: (id) => ipcRenderer.invoke("desktop:delete-task", id),
  completeTask: (id) => ipcRenderer.invoke("desktop:complete-task", id),
  captureTasks: (text) => ipcRenderer.invoke("desktop:capture-tasks", text),

  // Diary
  createDiaryEntry: (input) => ipcRenderer.invoke("desktop:create-diary-entry", input),
  updateDiaryEntry: (id, patch) => ipcRenderer.invoke("desktop:update-diary-entry", id, patch),
  createDraftsFromDiary: (diaryId) => ipcRenderer.invoke("desktop:create-drafts-from-diary", diaryId),

  // Data
  exportData: (format) => ipcRenderer.invoke("desktop:export-file", format),
  importData: (format) => ipcRenderer.invoke("desktop:import-file", format),
  attachFilesToDiary: (diaryId, files) => ipcRenderer.invoke("desktop:attach-files", diaryId, files),

  // Sync
  syncNow: () => ipcRenderer.invoke("desktop:sync-now"),
  syncLogin: (input) => ipcRenderer.invoke("desktop:sync-login", input),
  syncRegister: (input) => ipcRenderer.invoke("desktop:sync-register", input),
  syncLogout: () => ipcRenderer.invoke("desktop:sync-logout")
});
