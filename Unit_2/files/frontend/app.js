/**
 * Post Desk Console — Application Logic & API Client
 * Experiment 2.1.1 & 2.1.2 REST API & MDC Request Tracing
 */

const $ = (id) => document.getElementById(id);
let editingPostId = null;
const traceLogs = [];
let selectedLogIndex = 0;

/**
 * Returns clean API Base URL from header input
 */
function getApiBase() {
  return $('apiBase').value.replace(/\/+$/, '');
}

/**
 * Formats JS object as syntax-highlighted HTML JSON string
 */
function syntaxHighlightJson(json) {
  if (typeof json !== 'string') {
    json = JSON.stringify(json, null, 2);
  }
  if (!json) return '';
  
  json = json.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  return json.replace(/("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g, function (match) {
    let cls = 'json-number';
    if (/^"/.test(match)) {
      if (/:$/.test(match)) {
        cls = 'json-key';
      } else {
        cls = 'json-string';
      }
    } else if (/true|false/.test(match)) {
      cls = 'json-boolean';
    } else if (/null/.test(match)) {
      cls = 'json-null';
    }
    return '<span class="' + cls + '">' + match + '</span>';
  });
}

/**
 * Renders data into the Live Request-Response Envelope Inspector
 */
function inspectLog(log) {
  if (!log) return;
  $('inspMethod').textContent = log.method;
  $('inspEndpoint').textContent = log.path;
  $('inspStatus').textContent = log.status || 'ERROR';
  $('inspStatus').className = `status-${log.status}`;
  $('inspCorrId').textContent = log.correlationId;
  $('inspLatency').textContent = `${log.durationMs} ms`;

  // Show @ControllerAdvice badge if status is an exception status (400, 404, 409, 500)
  const isException = log.status >= 400;
  $('inspectorAdviceBadge').style.display = isException ? 'inline-block' : 'none';

  // Syntax highlight envelope body
  if (log.body) {
    $('jsonViewer').innerHTML = syntaxHighlightJson(log.body);
  } else {
    $('jsonViewer').textContent = '// No JSON response body returned';
  }
}

/**
 * Appends a request trace entry to the MDC Diagnostic Drawer table
 */
function addTraceLog(method, path, status, correlationId, durationMs, body) {
  const timeStr = new Date().toLocaleTimeString();
  const entry = { timeStr, method, path, status, correlationId: correlationId || '—', durationMs, body };
  traceLogs.unshift(entry);
  selectedLogIndex = 0;
  renderTraceLogs();
  inspectLog(entry);
}

/**
 * Renders the MDC Correlation log rows in the table UI
 */
function renderTraceLogs() {
  const tbody = $('traceLogsBody');
  if (traceLogs.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--text-dim); padding: 20px;">No requests logged yet. Trigger an API call to observe correlation IDs.</td></tr>`;
    return;
  }
  tbody.innerHTML = traceLogs.map((l, index) => `
    <tr class="${index === selectedLogIndex ? 'selected' : ''}" onclick="selectTraceRow(${index})">
      <td>${l.timeStr}</td>
      <td><strong>${l.method}</strong></td>
      <td style="color: var(--text-muted);">${l.path}</td>
      <td class="status-${l.status}">${l.status}</td>
      <td>${l.durationMs} ms</td>
      <td style="color: var(--accent-cyan); font-weight: 600;">${l.correlationId}</td>
    </tr>
  `).join('');
}

function selectTraceRow(index) {
  selectedLogIndex = index;
  renderTraceLogs();
  inspectLog(traceLogs[index]);
}

/**
 * Core API Fetch Wrapper injecting X-Correlation-Id headers
 */
async function apiCall(path, opts = {}) {
  const startTime = performance.now();
  const customCorrId = 'req-' + Math.random().toString(36).substring(2, 9);
  
  const headers = {
    'Content-Type': 'application/json',
    'X-Correlation-Id': customCorrId,
    ...(opts.headers || {})
  };

  try {
    const res = await fetch(getApiBase() + path, { ...opts, headers });
    const duration = Math.round(performance.now() - startTime);
    const returnedCorrId = res.headers.get('X-Correlation-Id') || customCorrId;
    
    const body = await res.json().catch(() => null);

    addTraceLog(opts.method || 'GET', path, res.status, returnedCorrId, duration, body);

    return { ok: res.ok, status: res.status, body, correlationId: returnedCorrId };
  } catch (err) {
    const duration = Math.round(performance.now() - startTime);
    const errBody = {
      success: false,
      message: "Network error / Backend service unreachable",
      data: null,
      correlationId: customCorrId,
      timestamp: new Date().toISOString()
    };
    addTraceLog(opts.method || 'GET', path, 0, customCorrId, duration, errBody);
    throw err;
  }
}

/**
 * Checks connectivity status to REST API backend
 */
async function checkHealth() {
  const dot = $('statusDot');
  const text = $('statusText');
  dot.className = 'status-dot checking';
  text.textContent = 'Checking...';

  try {
    const { ok, status } = await apiCall('/api/posts');
    if (ok) {
      dot.className = 'status-dot online';
      text.textContent = 'Spring Boot / Node Connected';
    } else {
      dot.className = 'status-dot';
      text.textContent = `Backend HTTP ${status}`;
    }
  } catch (e) {
    dot.className = 'status-dot';
    text.textContent = 'Backend Offline';
  }
}

/**
 * Display alert banner in the post editor card
 */
function showBanner(type, message) {
  const b = $('formBanner');
  b.className = `banner banner-${type} show`;
  b.textContent = message;
}

function clearBanner() {
  $('formBanner').className = 'banner';
}

function clearFieldErrors() {
  ['title', 'content', 'scheduledAt'].forEach(f => {
    $('err-' + f).textContent = '';
  });
}

function formatDateTime(isoStr) {
  if (!isoStr) return '—';
  const d = new Date(isoStr);
  return d.toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}

/**
 * Preset date field to +24 Hours from now
 */
function setTomorrowPreset() {
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  tomorrow.setHours(10, 0, 0, 0);
  
  const pad = n => String(n).padStart(2, '0');
  const formatted = `${tomorrow.getFullYear()}-${pad(tomorrow.getMonth()+1)}-${pad(tomorrow.getDate())}T${pad(tomorrow.getHours())}:${pad(tomorrow.getMinutes())}`;
  $('scheduledAt').value = formatted;
}

/**
 * Loads and renders posts repository
 */
async function loadPosts() {
  clearBanner();
  try {
    const { ok, body } = await apiCall('/api/posts');
    const container = $('postsList');
    container.innerHTML = '';

    if (!ok || !body) {
      container.innerHTML = `<div class="empty-state" style="grid-column: 1/-1;">Could not load posts from server. Ensure backend is running.</div>`;
      return;
    }

    let posts = body.data || [];
    $('postCountBadge').textContent = `${posts.length} Post${posts.length === 1 ? '' : 's'}`;

    const filterVal = $('statusFilter').value;
    if (filterVal !== 'ALL') {
      posts = posts.filter(p => p.status === filterVal);
    }

    if (posts.length === 0) {
      container.innerHTML = `<div class="empty-state" style="grid-column: 1/-1;">No posts found. Create your first post using the form on the left.</div>`;
      return;
    }

    posts.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

    posts.forEach(post => {
      const card = document.createElement('div');
      card.className = `post-card ${post.status}`;
      card.innerHTML = `
        <div class="post-top">
          <div class="post-title">${escapeHtml(post.title)}</div>
          <span class="badge badge-${post.status}">${post.status}</span>
        </div>
        <div class="post-content">${escapeHtml(post.content)}</div>
        <div class="post-meta">
          <span>ID #${post.id}</span>
          <span>Sched: ${formatDateTime(post.scheduledAt)}</span>
          ${post.publishedAt ? `<span>Pub: ${formatDateTime(post.publishedAt)}</span>` : ''}
        </div>
        <div class="post-card-actions"></div>
      `;

      const actions = card.querySelector('.post-card-actions');
      
      if (post.status !== 'PUBLISHED') {
        const editBtn = document.createElement('button');
        editBtn.className = 'btn btn-secondary btn-sm';
        editBtn.textContent = 'Edit';
        editBtn.onclick = () => startEdit(post);

        const pubBtn = document.createElement('button');
        pubBtn.className = 'btn btn-success btn-sm';
        pubBtn.textContent = 'Publish Now';
        pubBtn.onclick = () => publishPost(post.id);

        actions.append(editBtn, pubBtn);
      }

      const delBtn = document.createElement('button');
      delBtn.className = 'btn btn-danger btn-sm';
      delBtn.textContent = 'Delete';
      delBtn.onclick = () => deletePost(post.id);

      actions.append(delBtn);
      container.appendChild(card);
    });
  } catch (err) {
    $('postsList').innerHTML = `<div class="empty-state" style="grid-column: 1/-1;">Error connecting to API service.</div>`;
  }
}

function escapeHtml(str) {
  if (!str) return '';
  const d = document.createElement('div');
  d.textContent = str;
  return d.innerHTML;
}

function startEdit(post) {
  editingPostId = post.id;
  $('formTitle').textContent = `Edit Post #${post.id}`;
  $('editingIdBadge').textContent = `Editing ID #${post.id}`;
  $('title').value = post.title;
  $('content').value = post.content;
  
  if (post.scheduledAt) {
    const d = new Date(post.scheduledAt);
    const pad = n => String(n).padStart(2, '0');
    $('scheduledAt').value = `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }

  $('submitBtn').textContent = 'Save Changes';
  $('cancelEditBtn').style.display = 'inline-flex';
  clearBanner();
  clearFieldErrors();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function resetForm() {
  editingPostId = null;
  $('formTitle').textContent = 'New Scheduled Post';
  $('editingIdBadge').textContent = '';
  $('title').value = '';
  $('content').value = '';
  $('scheduledAt').value = '';
  $('submitBtn').textContent = 'Create Post';
  $('cancelEditBtn').style.display = 'none';
  clearBanner();
  clearFieldErrors();
}

/**
 * Handle Post Create & Update form submission
 */
$('postForm').onsubmit = async (e) => {
  e.preventDefault();
  clearBanner();
  clearFieldErrors();

  const payload = {
    title: $('title').value,
    content: $('content').value,
    scheduledAt: $('scheduledAt').value ? $('scheduledAt').value + ':00' : null
  };

  const path = editingPostId ? `/api/posts/${editingPostId}` : '/api/posts';
  const method = editingPostId ? 'PUT' : 'POST';

  try {
    const { ok, status, body, correlationId } = await apiCall(path, {
      method,
      body: JSON.stringify(payload)
    });

    if (ok) {
      showBanner('success', `Post ${editingPostId ? 'updated' : 'scheduled'} successfully! (Correlation ID: ${correlationId.substring(0, 10)}...)`);
      resetForm();
      loadPosts();
      return;
    }

    if (status === 400 && body?.data) {
      for (const [field, msg] of Object.entries(body.data)) {
        const el = $('err-' + field);
        if (el) el.textContent = msg;
      }
      showBanner('error', body.message || 'Validation failed. Please correct the fields above.');
    } else {
      showBanner('error', (body?.message || 'Request failed') + ` (HTTP ${status})`);
    }
  } catch (err) {
    showBanner('error', 'Network error connecting to backend API.');
  }
};

/**
 * Publish post action handler
 */
async function publishPost(id) {
  try {
    const { ok, body, status } = await apiCall(`/api/posts/${id}/publish`, { method: 'PATCH' });
    if (!ok) {
      alert(`Publish failed: ${body?.message || 'Unknown error'} (HTTP ${status})`);
      return;
    }
    loadPosts();
  } catch (err) {
    alert('Network error while publishing post.');
  }
}

/**
 * Delete post action handler
 */
async function deletePost(id) {
  if (!confirm(`Are you sure you want to delete post #${id}?`)) return;
  try {
    const { ok, body, status } = await apiCall(`/api/posts/${id}`, { method: 'DELETE' });
    if (!ok) {
      alert(`Delete failed: ${body?.message || 'Unknown error'} (HTTP ${status})`);
      return;
    }
    loadPosts();
  } catch (err) {
    alert('Network error while deleting post.');
  }
}

// Global Exception Trigger Helpers (@ControllerAdvice validation & 404 tests)
$('trigger400Btn').onclick = () => {
  resetForm();
  $('submitBtn').click(); // Submit empty form to trigger 400 validation error
};

$('trigger404Btn').onclick = async () => {
  await apiCall('/api/posts/999999');
};

// Architecture Modal Handlers
const modal = $('archModal');
$('archModalBtn').onclick = () => modal.classList.add('open');
$('closeArchModal').onclick = () => modal.classList.remove('open');
$('closeArchModal2').onclick = () => modal.classList.remove('open');
modal.onclick = (e) => { if (e.target === modal) modal.classList.remove('open'); };

// Event Bindings
$('presetDateBtn').onclick = setTomorrowPreset;
$('cancelEditBtn').onclick = resetForm;
$('statusFilter').onchange = loadPosts;
$('refreshBtn').onclick = () => { checkHealth(); loadPosts(); };
$('pingBtn').onclick = checkHealth;
$('apiBase').onchange = () => { checkHealth(); loadPosts(); };
$('clearLogsBtn').onclick = () => { traceLogs.length = 0; renderTraceLogs(); };

// Initial Startup
setTomorrowPreset();
checkHealth();
loadPosts();
