// ═══════════════════════════════════════════
// DiaryApp Desktop — Renderer
// ═══════════════════════════════════════════

const API = window.diaryDesktop;
let state = { tasks: [], diaries: [], settings: {}, account: {} };
let currentView = 'diary';
let timelineDate = new Date();
let activeDiaryId = null;
let diarySaveTimer = null;
let chatHistory = [];
let isProcessing = false;

const ICONS = {
  diary: '<svg class="icon icon-empty" viewBox="0 0 24 24" aria-hidden="true"><path d="M7 4h9.2A1.8 1.8 0 0 1 18 5.8v12.4a1.8 1.8 0 0 1-1.8 1.8H7a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z"/><path d="M8.5 8h6M8.5 11.5h6M8.5 15h4"/></svg>',
  user: '<svg class="icon icon-inline" viewBox="0 0 24 24" aria-hidden="true"><path d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z"/><path d="M5 20a7 7 0 0 1 14 0"/></svg>',
  bot: '<svg class="icon icon-inline" viewBox="0 0 24 24" aria-hidden="true"><path d="M8 9h8a3 3 0 0 1 3 3v3a3 3 0 0 1-3 3H8a3 3 0 0 1-3-3v-3a3 3 0 0 1 3-3Z"/><path d="M12 5v4M9.5 13h.1M14.5 13h.1"/></svg>',
  idea: '<svg class="icon icon-inline" viewBox="0 0 24 24" aria-hidden="true"><path d="M9 18h6M10 21h4"/><path d="M8 13.5a6 6 0 1 1 8 0c-.8.7-1.1 1.5-1.1 2.5H9.1c0-1-.3-1.8-1.1-2.5Z"/></svg>',
  clock: '<svg class="icon icon-inline" viewBox="0 0 24 24" aria-hidden="true"><path d="M12 6v6l4 2"/><path d="M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Z"/></svg>',
  check: '<svg class="icon icon-inline" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12.5 9.5 17 19 7"/></svg>'
};

function iconMarkup(name) { return ICONS[name] || ''; }

// ═══ Init ═══

document.addEventListener('DOMContentLoaded', async () => {
  state = await API.getState();
  applyVisualPreferences(state.settings || {});
  renderAll();
  bindEvents();
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

function showToast(message, type = 'info') {
  const container = document.getElementById('toast-container');
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.textContent = message;
  container.appendChild(toast);
  setTimeout(() => toast.remove(), 3000);
}

// ═══ Utilities ═══

function escapeHtml(text) {
  if (!text) return '';
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

function formatDate(ts) {
  if (!ts) return '';
  const d = new Date(ts);
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`;
}

function formatShortDate(ts) {
  if (!ts) return '';
  const d = new Date(ts);
  return `${d.getMonth() + 1}月${d.getDate()}日`;
}

function isSameDay(a, b) {
  if (!a || !b) return false;
  const da = a instanceof Date ? a : new Date(a);
  const db = b instanceof Date ? b : new Date(b);
  if (isNaN(da) || isNaN(db)) return false;
  return da.getFullYear() === db.getFullYear() && da.getMonth() === db.getMonth() && da.getDate() === db.getDate();
}

function calcStreak(diaries) {
  if (!diaries.length) return 0;
  const dates = [...new Set(diaries.map(d => new Date(d.createdAt).toDateString()))].sort();
  let streak = 1;
  for (let i = dates.length - 1; i > 0; i--) {
    const diff = (new Date(dates[i]) - new Date(dates[i - 1])) / 86400000;
    if (diff <= 1.5) streak++;
    else break;
  }
  return streak;
}

function resolveThemeMode(mode = 'light') {
  if (mode === 'system') return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  return mode === 'dark' ? 'dark' : 'light';
}

function applyVisualPreferences(settings = {}) {
  const theme = settings.themeChoice || 'fog';
  const themeMode = settings.themeMode || 'light';
  document.body.dataset.theme = theme;
  document.body.dataset.mode = resolveThemeMode(themeMode);
}

function debounce(fn, ms) {
  let timer;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), ms);
  };
}

function markdownToHtml(text) {
  const lines = escapeHtml(text).split(/\n/);
  return lines.map(line => {
    if (line.startsWith('### ')) return `<h3>${line.slice(4)}</h3>`;
    if (line.startsWith('## ')) return `<h2>${line.slice(3)}</h2>`;
    if (line.startsWith('# ')) return `<h1>${line.slice(2)}</h1>`;
    if (line.startsWith('- [ ] ')) return `<p class="markdown-task"><span class="markdown-check pending"></span>${line.slice(6)}</p>`;
    if (line.startsWith('- [x] ')) return `<p class="markdown-task done"><span class="markdown-check done"></span>${line.slice(6)}</p>`;
    if (line.startsWith('- ')) return `<p class="markdown-list"><span class="markdown-bullet"></span>${line.slice(2)}</p>`;
    if (line.startsWith('> ')) return `<blockquote>${line.slice(2)}</blockquote>`;
    return line ? `<p>${line.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')}</p>` : '<br>';
  }).join('');
}

// ═══ View Switching ═══

function switchView(viewName) {
  currentView = viewName;
  document.querySelectorAll('.view').forEach(el => el.classList.remove('active'));
  const view = document.querySelector(`.view-${viewName}`);
  if (view) view.classList.add('active');

  document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
  const nav = document.querySelector(`[data-view="${viewName}"]`);
  if (nav) nav.classList.add('active');

  const titles = {
    diary: '日记', todo: '待办', timeline: '时间线', chat: 'AI 助手', settings: '设置'
  };
  document.getElementById('view-title').textContent = titles[viewName] || viewName;

  if (viewName === 'chat') document.getElementById('chat-input')?.focus();
}

// ═══ Rendering ═══

function renderAll() {
  renderDiaryList();
  renderTodoPage();
  renderTimelinePage();
  renderSettingsPage();
  renderSyncStatus();
  updateStatusBar();
  loadSettings();
}

function renderDiaryList() {
  const container = document.getElementById('diary-list');
  if (!container) return;
  const diaries = state.diaries || [];
  if (!diaries.length) {
    container.innerHTML = `<div class="empty-state"><div class="empty-state-title">还没有日记</div><div class="empty-state-text">点击右侧开始写第一篇日记</div></div>`;
    return;
  }
  const sorted = [...diaries].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
  const groups = {};
  for (const d of sorted) {
    const date = new Date(d.createdAt);
    const key = `${date.getFullYear()}年${date.getMonth() + 1}月`;
    if (!groups[key]) groups[key] = [];
    groups[key].push(d);
  }
  container.innerHTML = Object.entries(groups).map(([month, entries]) => `
    <div class="diary-month-header">${month}</div>
    ${entries.map(d => `
      <div class="diary-entry-item ${d.id === activeDiaryId ? 'selected' : ''}" data-id="${d.id}">
        <div class="diary-entry-date">${formatShortDate(d.createdAt)}</div>
        <div class="diary-entry-title">${escapeHtml(d.title || '无标题')}</div>
        <div class="diary-entry-preview">${escapeHtml((d.text || d.content || '').slice(0, 60))}</div>
      </div>
    `).join('')}
  `).join('');
}

function renderDiaryEditor(diaryId) {
  const dateEl = document.querySelector('.diary-editor-date');
  const body = document.getElementById('diary-editor-body');
  if (!dateEl || !body) return;

  if (diaryId) {
    const diary = (state.diaries || []).find(d => d.id === diaryId);
    if (diary) {
      dateEl.textContent = formatDate(diary.createdAt);
      body.value = diary.text || diary.content || '';
      return;
    }
  }
  dateEl.textContent = formatDate(new Date().toISOString());
  body.value = '';
}

function renderTodoPage() {
  const todayList = document.getElementById('todo-today');
  const weekList = document.getElementById('todo-week');
  const laterList = document.getElementById('todo-later');
  if (!todayList) return;

  const tasks = (state.tasks || []).filter(t => t.status !== 'done');
  const now = new Date();
  now.setHours(0, 0, 0, 0);

  const weekStart = new Date(now);
  weekStart.setDate(now.getDate() - now.getDay() + (now.getDay() === 0 ? -6 : 1));
  const weekEnd = new Date(weekStart);
  weekEnd.setDate(weekStart.getDate() + 6);
  weekEnd.setHours(23, 59, 59, 999);

  const groups = { today: [], week: [], later: [] };
  for (const task of tasks) {
    if (!task.dueAt) { groups.later.push(task); continue; }
    const due = new Date(task.dueAt);
    if (isSameDay(due, now)) { groups.today.push(task); }
    else if (due >= weekStart && due <= weekEnd) { groups.week.push(task); }
    else { groups.later.push(task); }
  }

  const renderTask = (task) => `
    <div class="todo-item" data-id="${task.id}">
      <span class="todo-checkbox" data-action="complete" data-id="${task.id}"></span>
      <span class="todo-text" data-action="expand" data-id="${task.id}">${escapeHtml(task.title)}</span>
      ${task.priority > 0 ? `<span class="todo-priority p${task.priority}">P${task.priority}</span>` : ''}
      ${task.dueAt ? `<span class="todo-due">${formatShortDate(task.dueAt)}</span>` : ''}
      ${task.description ? `<div class="todo-desc" data-id="${task.id}" style="display:none">${escapeHtml(task.description)}</div>` : ''}
    </div>`;

  const renderEmpty = (text) => `<div class="todo-empty">${text}</div>`;

  todayList.innerHTML = groups.today.length ? groups.today.map(renderTask).join('') : renderEmpty('今天没有待办');
  weekList.innerHTML = groups.week.length ? groups.week.map(renderTask).join('') : renderEmpty('本周没有其他待办');
  laterList.innerHTML = groups.later.length ? groups.later.map(renderTask).join('') : renderEmpty('以后没有待办');
}

function renderTimelinePage() {
  const label = document.getElementById('timeline-month');
  const feed = document.getElementById('timeline-feed');
  const statDiaries = document.getElementById('stat-diaries');
  const statTasks = document.getElementById('stat-tasks');
  const statStreak = document.getElementById('stat-streak');
  if (!feed) return;

  if (label) label.textContent = `${timelineDate.getFullYear()}年${timelineDate.getMonth() + 1}月`;

  const year = timelineDate.getFullYear();
  const month = timelineDate.getMonth();
  const monthStart = new Date(year, month, 1).getTime();
  const monthEnd = new Date(year, month + 1, 1).getTime();

  const inMonth = (ts) => ts && new Date(ts).getTime() >= monthStart && new Date(ts).getTime() < monthEnd;

  const diaryItems = (state.diaries || [])
    .filter(d => inMonth(d.createdAt))
    .map(d => ({ type: 'diary', date: new Date(d.createdAt), title: d.title || '无标题日记', detail: (d.text || d.content || '').slice(0, 80) }));

  const taskItems = (state.tasks || [])
    .filter(t => t.status === 'done' && inMonth(t.completedAt))
    .map(t => ({ type: 'task', date: new Date(t.completedAt), title: t.title, detail: '' }));

  const items = [...diaryItems, ...taskItems]
    .sort((a, b) => b.date - a.date)
    .slice(0, 40);

  feed.innerHTML = items.length
    ? items.map(item => `
      <div class="timeline-feed-item">
        <span class="timeline-dot ${item.type}"></span>
        <div class="timeline-item-body">
          <time class="timeline-time">${formatShortDate(item.date.toISOString())}</time>
          <p class="timeline-text">${item.type === 'diary' ? '● ' : '✓ '}${escapeHtml(item.title)}</p>
        </div>
      </div>
    `).join('')
    : '<div class="todo-empty">该月暂无时间线内容</div>';

  const monthDiaries = (state.diaries || []).filter(d => inMonth(d.createdAt));
  const monthTasks = (state.tasks || []).filter(t => inMonth(t.completedAt || t.createdAt));
  const words = monthDiaries.reduce((sum, d) => sum + (d.text || d.content || '').replace(/\s+/g, '').length, 0);

  if (statDiaries) statDiaries.textContent = monthDiaries.length;
  if (statTasks) statTasks.textContent = monthTasks.length;
  if (statStreak) statStreak.textContent = calcStreak(state.diaries || []);
}

function renderSettingsPage() {
  const s = state.settings || {};
  setSegmentActive('theme-mode-control', 'mode', s.themeMode || 'light');
  document.querySelectorAll('#theme-palette .theme-swatch').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.theme === (s.themeChoice || 'fog'));
  });
  setSegmentActive('setting-provider', 'provider', s.provider || 'local');
  document.getElementById('setting-api-key').value = s.apiKey || '';
  document.getElementById('setting-endpoint').value = s.endpoint || '';
  document.getElementById('setting-model').value = s.model || '';
  toggleApiFields(s.provider || 'local');

  const account = state.account || {};
  document.getElementById('settings-phone').value = account.phone || '';
  document.getElementById('settings-pin').value = '';
  document.getElementById('settings-endpoint').value = account.syncEndpoint || 'https://diary-app-sync.workers.dev';
}

function renderSyncStatus() {
  const el = document.getElementById('sync-text');
  const syncState = document.getElementById('status-sync-state');
  const account = state.account || {};
  if (account.status === 'linked') {
    const text = account.lastSyncAt ? `已连接 · ${formatDate(account.lastSyncAt)}` : `已连接 ${account.maskedPhone || account.phone}`;
    if (el) el.textContent = text;
    if (syncState) syncState.textContent = '同步已连接';
  } else {
    if (el) el.textContent = '未登录';
    if (syncState) syncState.textContent = '同步未登录';
  }
}

function updateStatusBar() {
  const body = document.getElementById('diary-editor-body');
  const wordCount = document.getElementById('status-word-count');
  if (body && wordCount) {
    wordCount.textContent = `字数 ${(body.value || '').replace(/\s+/g, '').length}`;
  }
}

// ═══ Chat ═══

async function sendChatMessage(text) {
  if (isProcessing || !text.trim()) return;
  isProcessing = true;
  const input = document.getElementById('chat-input');
  input.value = '';
  input.disabled = true;
  appendChatMessage('user', text);
  chatHistory.push({ role: 'user', content: text });
  showTypingIndicator();
  try {
    const result = await API.sendChat({ messages: chatHistory });
    hideTypingIndicator();
    if (result.stateChanged) {
      await refreshState();
      renderAll();
    }
    const content = result.content || '抱歉，我没有理解你的意思。';
    appendChatMessage('assistant', content);
    chatHistory.push({ role: 'assistant', content });
  } catch (err) {
    hideTypingIndicator();
    appendChatMessage('assistant', `出错了: ${err.message}`);
  }
  input.disabled = false;
  input.focus();
  isProcessing = false;
}

function appendChatMessage(role, content) {
  const container = document.getElementById('chat-messages');
  const div = document.createElement('div');
  div.className = `chat-message ${role}`;
  div.innerHTML = `
    <div class="chat-avatar ${role}">${role === 'user' ? iconMarkup('user') : iconMarkup('bot')}</div>
    <div class="chat-bubble">${escapeHtml(content).replace(/\n/g, '<br>')}</div>`;
  container.appendChild(div);
  container.scrollTop = container.scrollHeight;
}

function showTypingIndicator() {
  const container = document.getElementById('chat-messages');
  const div = document.createElement('div');
  div.className = 'chat-message assistant';
  div.id = 'typing-indicator';
  div.innerHTML = `
    <div class="chat-avatar assistant">${iconMarkup('bot')}</div>
    <div class="chat-bubble"><div class="loading-dots"><span></span><span></span><span></span></div></div>`;
  container.appendChild(div);
  container.scrollTop = container.scrollHeight;
}

function hideTypingIndicator() {
  const el = document.getElementById('typing-indicator');
  if (el) el.remove();
}

function appendProactiveSuggestion(text) {
  const container = document.getElementById('chat-messages');
  const div = document.createElement('div');
  div.className = 'chat-proactive';
  div.innerHTML = `${iconMarkup('idea')} <span>${escapeHtml(text)}</span>`;
  div.onclick = () => {
    document.getElementById('chat-input').value = text;
    document.getElementById('chat-input').focus();
  };
  container.appendChild(div);
  container.scrollTop = container.scrollHeight;
}

// ═══ Settings Helpers ═══

function loadSettings() {
  const s = state.settings || {};
  applyVisualPreferences(s);
  setSegmentActive('setting-provider', 'provider', s.provider || 'local');
  setSegmentActive('theme-mode-control', 'mode', s.themeMode || 'light');
  document.querySelectorAll('#theme-palette .theme-swatch').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.theme === (s.themeChoice || 'fog'));
  });
  document.getElementById('setting-api-key').value = s.apiKey || '';
  document.getElementById('setting-endpoint').value = s.endpoint || '';
  document.getElementById('setting-model').value = s.model || '';
  toggleApiFields(s.provider || 'local');
  window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
    applyVisualPreferences(state.settings || {});
  });
}

function selectedSegmentValue(containerId, dataKey, fallback) {
  const active = document.querySelector(`#${containerId} .segment.active`);
  return active?.dataset[dataKey] || fallback;
}

function setSegmentActive(containerId, dataKey, value) {
  document.querySelectorAll(`#${containerId} .segment`).forEach(btn => {
    btn.classList.toggle('active', btn.dataset[dataKey] === String(value));
  });
}

function toggleApiFields(provider) {
  const isLocal = provider === 'local';
  document.getElementById('setting-api-key').closest('.settings-row').style.display = isLocal ? 'none' : '';
  document.getElementById('setting-endpoint').closest('.settings-row').style.display = isLocal ? 'none' : '';
  document.getElementById('setting-model').closest('.settings-row').style.display = isLocal ? 'none' : '';
}

// ═══ Event Binding ═══

function bindEvents() {
  // Navigation
  document.querySelectorAll('.nav-item').forEach(el => {
    el.addEventListener('click', () => switchView(el.dataset.view));
  });

  // Diary: entry click
  document.getElementById('diary-list').addEventListener('click', e => {
    const entry = e.target.closest('.diary-entry-item');
    if (entry) {
      const id = entry.dataset.id;
      activeDiaryId = id;
      document.querySelectorAll('.diary-entry-item').forEach(el => el.classList.remove('selected'));
      entry.classList.add('selected');
      renderDiaryEditor(id);
    }
  });

  // Diary: toolbar buttons (wrap selection with markers)
  document.querySelectorAll('.editor-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const body = document.getElementById('diary-editor-body');
      if (!body) return;
      const start = body.selectionStart;
      const end = body.selectionEnd;
      const text = body.value;
      const selected = text.substring(start, end);
      const format = btn.dataset.format;
      let wrapped, cursorOffset;
      if (format === 'bold') {
        wrapped = `**${selected || '粗体'}**`;
        cursorOffset = selected ? 0 : -2;
      } else if (format === 'italic') {
        wrapped = `*${selected || '斜体'}*`;
        cursorOffset = selected ? 0 : -1;
      } else if (format === 'heading') {
        const lineStart = text.lastIndexOf('\n', start - 1) + 1;
        wrapped = `### ${selected || '标题'}`;
        cursorOffset = selected ? 0 : -3;
      }
      body.value = text.substring(0, start) + wrapped + text.substring(end);
      const newPos = selected ? end + wrapped.length - (end - start) : start + wrapped.length + (cursorOffset || 0);
      body.setSelectionRange(newPos, newPos);
      body.focus();
    });
  });

  // Diary: save button
  document.getElementById('btn-save-diary')?.addEventListener('click', saveDiary);

  // Diary: auto-save on blur with debounce
  const diaryBody = document.getElementById('diary-editor-body');
  if (diaryBody) {
    diaryBody.addEventListener('blur', debounce(async () => {
      if (diaryBody.value.trim()) await saveDiary();
    }, 500));
    diaryBody.addEventListener('input', updateStatusBar);
  }

  // Todo: quick capture
  const captureInput = document.getElementById('todo-capture-input');
  const captureBtn = document.getElementById('btn-todo-capture');
  if (captureBtn) {
    captureBtn.addEventListener('click', async () => {
      const text = captureInput.value.trim();
      if (!text) return;
      captureInput.value = '';
      await mutate(() => API.addTask(text));
      showToast('已添加待办', 'success');
    });
  }
  if (captureInput) {
    captureInput.addEventListener('keydown', async e => {
      if (e.key === 'Enter') {
        const text = captureInput.value.trim();
        if (!text) return;
        captureInput.value = '';
        await mutate(() => API.addTask(text));
        showToast('已添加待办', 'success');
      }
    });
  }

  // Todo: checkbox (complete) and title (expand)
  document.querySelectorAll('.todo-groups').forEach(group => {
    group.addEventListener('click', async e => {
      const checkbox = e.target.closest('.todo-checkbox[data-action="complete"]');
      if (checkbox) {
        const id = checkbox.dataset.id;
        await mutate(() => API.completeTask(id));
        return;
      }
      const title = e.target.closest('.todo-text[data-action="expand"]');
      if (title) {
        const id = title.dataset.id;
        const desc = document.querySelector(`.todo-desc[data-id="${id}"]`);
        if (desc) desc.style.display = desc.style.display === 'none' ? 'block' : 'none';
      }
    });
  });

  // Timeline: navigation
  document.getElementById('timeline-prev')?.addEventListener('click', () => {
    timelineDate.setMonth(timelineDate.getMonth() - 1);
    renderTimelinePage();
  });
  document.getElementById('timeline-next')?.addEventListener('click', () => {
    timelineDate.setMonth(timelineDate.getMonth() + 1);
    renderTimelinePage();
  });

  // Chat
  const chatInput = document.getElementById('chat-input');
  const chatSend = document.getElementById('chat-send');
  if (chatSend) chatSend.addEventListener('click', () => sendChatMessage(chatInput.value));
  if (chatInput) {
    chatInput.addEventListener('keydown', e => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendChatMessage(chatInput.value);
      }
    });
  }

  // Settings: theme palette
  document.querySelectorAll('#theme-palette .theme-swatch').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('#theme-palette .theme-swatch').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      applyVisualPreferences({ ...(state.settings || {}), themeChoice: btn.dataset.theme });
    });
  });

  // Settings: theme mode
  document.querySelectorAll('#theme-mode-control .segment').forEach(btn => {
    btn.addEventListener('click', () => {
      setSegmentActive('theme-mode-control', 'mode', btn.dataset.mode);
      applyVisualPreferences({ ...(state.settings || {}), themeMode: btn.dataset.mode });
    });
  });

  // Settings: AI provider
  document.querySelectorAll('#setting-provider .segment').forEach(btn => {
    btn.addEventListener('click', () => {
      setSegmentActive('setting-provider', 'provider', btn.dataset.provider);
      toggleApiFields(btn.dataset.provider);
    });
  });

  // Settings: save settings
  document.getElementById('btn-save-settings')?.addEventListener('click', async () => {
    const settings = {
      provider: selectedSegmentValue('setting-provider', 'provider', 'local'),
      apiKey: document.getElementById('setting-api-key').value,
      endpoint: document.getElementById('setting-endpoint').value,
      model: document.getElementById('setting-model').value,
      themeChoice: document.querySelector('#theme-palette .theme-swatch.active')?.dataset.theme || 'fog',
      themeMode: selectedSegmentValue('theme-mode-control', 'mode', 'light')
    };
    await API.saveSettings(settings);
    state.settings = settings;
    applyVisualPreferences(settings);
    showToast('设置已保存', 'success');
  });

  // Settings: test AI
  document.getElementById('btn-test-ai')?.addEventListener('click', async () => {
    const settings = {
      provider: selectedSegmentValue('setting-provider', 'provider', 'local'),
      apiKey: document.getElementById('setting-api-key').value,
      endpoint: document.getElementById('setting-endpoint').value,
      model: document.getElementById('setting-model').value
    };
    try {
      const result = await API.testAiConnection?.(settings) || await API.sendChat({ messages: [{ role: 'user', content: 'test' }], context: settings });
      showToast(result.ok ? '连接成功' : '连接失败', result.ok ? 'success' : 'error');
    } catch {
      showToast('连接失败', 'error');
    }
  });

  // Settings: export/import
  document.getElementById('btn-export')?.addEventListener('click', async () => {
    await API.exportFile(state, 'json');
    showToast('数据已导出', 'success');
  });
  document.getElementById('btn-import')?.addEventListener('click', async () => {
    await mutate(() => API.importFile('json'));
    showToast('数据已导入', 'success');
  });

  // Settings: sync login
  const loginBtn = document.getElementById('btn-sync-login');
  if (loginBtn) {
    loginBtn.addEventListener('click', async () => {
      const phone = document.getElementById('settings-phone').value.trim();
      const pin = document.getElementById('settings-pin').value.trim();
      if (!phone || !pin) { showToast('请输入手机号和 PIN', 'error'); return; }
      try {
        const result = await API.syncLogin({ phone, pin });
        if (result.state) state = result.state;
        await refreshState();
        document.getElementById('settings-pin').value = '';
        renderAll();
        showToast('登录并连接成功', 'success');
      } catch (err) {
        showToast(err.message || '连接失败', 'error');
      }
    });
  }

  // Settings: sync logout
  document.getElementById('btn-sync-logout')?.addEventListener('click', async () => {
    try {
      const result = await API.syncLogout();
      if (result?.state) state = result.state;
      await refreshState();
      renderAll();
      showToast('已断开同步', 'info');
    } catch {
      showToast('断开失败', 'error');
    }
  });

  // Toolbar sync button
  document.getElementById('btn-sync-now')?.addEventListener('click', doSync);
}

// ═══ Diary Save ═══

async function saveDiary() {
  const body = document.getElementById('diary-editor-body');
  const title = (state.diaries || []).find(d => d.id === activeDiaryId)?.title || '无标题日记';
  const content = body?.value?.trim();
  if (!content) return;
  if (activeDiaryId && API.updateDiaryEntry) {
    await mutate(() => API.updateDiaryEntry(activeDiaryId, { title, text: content }));
  } else {
    const result = await API.createDiaryEntry(title, content);
    if (result?.state) state = result.state;
    await refreshState();
    activeDiaryId = null;
    renderAll();
  }
  showToast('日记已保存', 'success');
}

// ═══ Sync ═══

async function doSync() {
  const status = document.getElementById('sync-status');
  const syncText = document.getElementById('sync-text');
  if (status) status.className = 'sync-indicator syncing';
  if (syncText) syncText.textContent = '同步中...';
  try {
    const result = await API.syncNow();
    await refreshState();
    if (status) status.className = 'sync-indicator synced';
    if (syncText) syncText.textContent = '同步完成';
    showToast(result?.message || '同步成功', 'success');
    renderAll();
  } catch (err) {
    if (status) status.className = 'sync-indicator error';
    if (syncText) syncText.textContent = '同步失败';
    showToast(err.message || '同步失败，请稍后重试', 'error');
  }
  setTimeout(renderSyncStatus, 3000);
}
