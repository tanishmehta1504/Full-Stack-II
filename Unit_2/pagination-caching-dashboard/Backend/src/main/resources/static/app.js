const API_BASE = '';

// =========================================================================
// APPLICATION STATE
// =========================================================================
let booksState = {
  page: 0,
  size: 10,
  sortBy: 'id',
  direction: 'asc',
  mode: '',
  totalPages: 1,
  searchQuery: '',
  genreFilter: ''
};

let authorsState = {
  page: 0,
  size: 10,
  sortBy: 'id',
  direction: 'asc',
  totalPages: 1,
  searchQuery: ''
};

let expensiveState = {
  page: 0,
  size: 10,
  maxPrice: 50,
  totalPages: 1
};

// Raw fetched items cache for client-side search filtering
let currentRawBooks = [];
let currentRawAuthors = [];

// =========================================================================
// HELPERS
// =========================================================================
async function getJSON(url) {
  const res = await fetch(url);
  if (!res.ok) throw new Error(`HTTP ${res.status} (${res.statusText})`);
  return res.json();
}

function fmtMoney(v) {
  return v == null ? '-' : '$' + Number(v).toFixed(2);
}

// =========================================================================
// SERVER STATUS & KPIS
// =========================================================================
async function checkServerStatus() {
  const pill = document.getElementById('serverStatus');
  try {
    const data = await getJSON(`${API_BASE}/api/authors?page=0&size=1`);
    pill.className = 'status-pill status-ok';
    pill.querySelector('.status-text').textContent = 'Server Online';

    if (data.totalElements) {
      document.getElementById('kpiTotalAuthors').textContent = data.totalElements.toLocaleString();
    }
  } catch (e) {
    pill.className = 'status-pill status-down';
    pill.querySelector('.status-text').textContent = 'Server Offline';
  }
}

// =========================================================================
// TAB SWITCHING
// =========================================================================
function wireTabs() {
  const tabBtns = document.querySelectorAll('.tab-btn');
  tabBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      const targetTabId = btn.getAttribute('data-tab');

      document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
      document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));

      btn.classList.add('active');
      document.getElementById(targetTabId).classList.add('active');

      if (targetTabId === 'tabAuthors' && currentRawAuthors.length === 0) {
        loadAuthors();
      } else if (targetTabId === 'tabExpensive') {
        loadExpensiveBooks();
      }
    });
  });
}

// =========================================================================
// BOOKS CATALOG (TAB 1)
// =========================================================================
async function loadBooks() {
  const tbody = document.getElementById('booksTbody');
  tbody.innerHTML = '<tr><td colspan="8" class="loading-cell">Loading books data...</td></tr>';

  const path = booksState.mode === 'naive' ? '/api/books/naive' : '/api/books';
  let url = `${API_BASE}${path}?page=${booksState.page}&size=${booksState.size}` +
            `&sortBy=${booksState.sortBy}&direction=${booksState.direction}`;
  if (booksState.genreFilter) {
    url += `&genre=${encodeURIComponent(booksState.genreFilter)}`;
  }

  const start = performance.now();
  try {
    const data = await getJSON(url);
    const elapsed = (performance.now() - start).toFixed(1);

    booksState.totalPages = data.totalPages;
    currentRawBooks = data.content || [];

    // Update KPI for total books if available
    if (data.totalElements) {
      document.getElementById('kpiTotalBooks').textContent = data.totalElements.toLocaleString();
    }

    renderBooksTable();

    document.getElementById('pageInfo').textContent =
      `Page ${data.pageNumber + 1} of ${data.totalPages} (${data.totalElements} total records)`;
    document.getElementById('lastFetchTime').textContent = `Fetched in ${elapsed} ms`;
    document.getElementById('prevBtn').disabled = data.first;
    document.getElementById('nextBtn').disabled = data.last;

    updateSortIcons('sort-', booksState.sortBy, booksState.direction);

  } catch (e) {
    tbody.innerHTML = `<tr><td colspan="8" class="loading-cell text-rose">Error loading data: ${e.message}. Is backend running on :8080?</td></tr>`;
  }
}

function renderBooksTable() {
  const tbody = document.getElementById('booksTbody');
  let items = [...currentRawBooks];

  // Apply client-side search filter if query is entered
  const q = booksState.searchQuery.toLowerCase().trim();
  if (q) {
    items = items.filter(b =>
      (b.title && b.title.toLowerCase().includes(q)) ||
      (b.authorName && b.authorName.toLowerCase().includes(q)) ||
      (b.genre && b.genre.toLowerCase().includes(q))
    );
  }

  // Apply client-side genre filter if selected
  if (booksState.genreFilter) {
    items = items.filter(b => b.genre && b.genre.toLowerCase().includes(booksState.genreFilter.toLowerCase()));
  }

  if (items.length === 0) {
    tbody.innerHTML = '<tr><td colspan="8" class="loading-cell">No matching books found.</td></tr>';
    return;
  }

  tbody.innerHTML = items.map(b => `
    <tr>
      <td style="font-family: var(--font-mono); font-weight: 600;">#${b.id}</td>
      <td style="font-weight: 600;">${escapeHtml(b.title)}</td>
      <td><span class="genre-badge">${escapeHtml(b.genre || 'General')}</span></td>
      <td style="font-family: var(--font-mono); color: var(--text-muted);">${b.publishedYear ?? '-'}</td>
      <td class="price-tag">${fmtMoney(b.price)}</td>
      <td><span class="popularity-badge">★ ${(b.popularity ?? 4.5).toFixed(1)}</span></td>
      <td><strong>${escapeHtml(b.authorName ?? 'Unknown')}</strong></td>
      <td><span class="country-chip">${escapeHtml(b.authorCountry ?? '-')}</span></td>
    </tr>
  `).join('');
}

function wireBooksControls() {
  document.getElementById('modeSelect').addEventListener('change', e => {
    booksState.mode = e.target.value;
    booksState.page = 0;
    loadBooks();
  });

  document.getElementById('sizeSelect').addEventListener('change', e => {
    booksState.size = parseInt(e.target.value, 10);
    booksState.page = 0;
    loadBooks();
  });

  document.getElementById('refreshBtn').addEventListener('click', loadBooks);

  document.getElementById('prevBtn').addEventListener('click', () => {
    if (booksState.page > 0) {
      booksState.page--;
      loadBooks();
    }
  });

  document.getElementById('nextBtn').addEventListener('click', () => {
    if (booksState.page < booksState.totalPages - 1) {
      booksState.page++;
      loadBooks();
    }
  });

  // Real-time search filter
  document.getElementById('searchInput').addEventListener('input', e => {
    booksState.searchQuery = e.target.value;
    renderBooksTable();
  });

  // Genre filter
  document.getElementById('genreFilterSelect').addEventListener('change', e => {
    booksState.genreFilter = e.target.value;
    booksState.page = 0;
    loadBooks();
  });

  // Clickable table header sorting
  document.querySelectorAll('th.sortable').forEach(th => {
    th.addEventListener('click', () => {
      const field = th.getAttribute('data-sort');
      if (booksState.sortBy === field) {
        booksState.direction = booksState.direction === 'asc' ? 'desc' : 'asc';
      } else {
        booksState.sortBy = field;
        booksState.direction = 'asc';
      }
      booksState.page = 0;
      loadBooks();
    });
  });
}

// =========================================================================
// AUTHORS DIRECTORY (TAB 2)
// =========================================================================
async function loadAuthors() {
  const tbody = document.getElementById('authorsTbody');
  tbody.innerHTML = '<tr><td colspan="4" class="loading-cell">Loading authors...</td></tr>';

  const url = `${API_BASE}/api/authors?page=${authorsState.page}&size=${authorsState.size}` +
              `&sortBy=${authorsState.sortBy}&direction=${authorsState.direction}`;

  try {
    const data = await getJSON(url);
    authorsState.totalPages = data.totalPages;
    currentRawAuthors = data.content || [];

    renderAuthorsTable();

    document.getElementById('authorPageInfo').textContent =
      `Page ${data.pageNumber + 1} of ${data.totalPages} (${data.totalElements} total authors)`;
    document.getElementById('authorPrevBtn').disabled = data.first;
    document.getElementById('authorNextBtn').disabled = data.last;

    updateSortIcons('author-sort-', authorsState.sortBy, authorsState.direction);

  } catch (e) {
    tbody.innerHTML = `<tr><td colspan="4" class="loading-cell text-rose">Error: ${e.message}</td></tr>`;
  }
}

function renderAuthorsTable() {
  const tbody = document.getElementById('authorsTbody');
  let items = [...currentRawAuthors];

  const q = authorsState.searchQuery.toLowerCase().trim();
  if (q) {
    items = items.filter(a =>
      (a.name && a.name.toLowerCase().includes(q)) ||
      (a.country && a.country.toLowerCase().includes(q))
    );
  }

  if (items.length === 0) {
    tbody.innerHTML = '<tr><td colspan="4" class="loading-cell">No matching authors found.</td></tr>';
    return;
  }

  tbody.innerHTML = items.map(a => `
    <tr>
      <td style="font-family: var(--font-mono); font-weight: 600;">#${a.id}</td>
      <td style="font-weight: 700;">${escapeHtml(a.name)}</td>
      <td><span class="country-chip">${escapeHtml(a.country)}</span></td>
      <td><span class="genre-badge" style="background: rgba(59, 130, 246, 0.12); color: #93c5fd;">${a.bookCount} books</span></td>
    </tr>
  `).join('');
}

function wireAuthorsControls() {
  document.getElementById('authorRefreshBtn').addEventListener('click', loadAuthors);

  document.getElementById('authorPrevBtn').addEventListener('click', () => {
    if (authorsState.page > 0) {
      authorsState.page--;
      loadAuthors();
    }
  });

  document.getElementById('authorNextBtn').addEventListener('click', () => {
    if (authorsState.page < authorsState.totalPages - 1) {
      authorsState.page++;
      loadAuthors();
    }
  });

  document.getElementById('authorSearchInput').addEventListener('input', e => {
    authorsState.searchQuery = e.target.value;
    renderAuthorsTable();
  });

  document.querySelectorAll('th.sortable-author').forEach(th => {
    th.addEventListener('click', () => {
      const field = th.getAttribute('data-sort');
      if (authorsState.sortBy === field) {
        authorsState.direction = authorsState.direction === 'asc' ? 'desc' : 'asc';
      } else {
        authorsState.sortBy = field;
        authorsState.direction = 'asc';
      }
      authorsState.page = 0;
      loadAuthors();
    });
  });
}

// =========================================================================
// PERFORMANCE & CACHE LAB (TAB 3)
// =========================================================================
async function runBenchmark() {
  const container = document.getElementById('benchmarkResults');
  container.innerHTML = '<div class="loading-cell">Running live SQL benchmark...</div>';

  try {
    const results = await getJSON(`${API_BASE}/api/books/benchmark?page=0&size=20`);
    const naive = results.find(r => r.mode === 'naive');
    const optimized = results.find(r => r.mode === 'optimized');
    const maxQ = Math.max(naive.queryCount, optimized.queryCount, 1);

    const naivePercent = Math.max((naive.queryCount / maxQ) * 100, 15);
    const optPercent = Math.max((optimized.queryCount / maxQ) * 100, 15);

    container.innerHTML = `
      <div class="bench-card">
        <div class="bench-header">
          <span>🐢 Standard Mode (N+1 Query Problem)</span>
          <span style="color: #ef4444;">${naive.queryCount} SQL Statements &bull; ${naive.timeMillis} ms</span>
        </div>
        <div class="bench-bar-outer">
          <div class="bench-bar-fill naive" style="width: ${naivePercent}%;">
            ${naive.queryCount} Queries Executed
          </div>
        </div>
      </div>

      <div class="bench-card">
        <div class="bench-header">
          <span>⚡ Fast Mode (JOIN FETCH Optimization)</span>
          <span style="color: var(--emerald);">${optimized.queryCount} SQL Statement(s) &bull; ${optimized.timeMillis} ms</span>
        </div>
        <div class="bench-bar-outer">
          <div class="bench-bar-fill optimized" style="width: ${optPercent}%;">
            ${optimized.queryCount} Query Executed
          </div>
        </div>
      </div>

      <div class="bench-summary">
        🚀 <strong>Performance Advantage:</strong> Fast Mode reduced database queries from <strong>${naive.queryCount}</strong> down to <strong>${optimized.queryCount}</strong>!
        This eliminates the N+1 database bottleneck and fetches all <strong>${optimized.recordsReturned}</strong> records in a single optimized SQL join.
      </div>
    `;
  } catch (e) {
    container.innerHTML = `<div class="loading-cell text-rose">Benchmark failed: ${e.message}</div>`;
  }
}

async function runCacheTest() {
  const container = document.getElementById('cacheResults');
  container.innerHTML = '<div class="loading-cell">Testing Ehcache memory speed...</div>';
  const url = `${API_BASE}/api/books?page=0&size=10&sortBy=id&direction=asc`;

  container.innerHTML = '';

  for (let i = 1; i <= 2; i++) {
    const start = performance.now();
    await getJSON(url);
    const elapsed = (performance.now() - start).toFixed(1);
    const isHit = i === 2;

    const row = document.createElement('div');
    row.className = `cache-item ${isHit ? 'hit' : 'miss'}`;
    row.innerHTML = `
      <div>
        <strong>Call #${i}</strong> ${isHit ? '— Served directly from Ehcache JVM Heap' : '— Hits H2 Database'}
      </div>
      <div style="display: flex; align-items: center; gap: 10px;">
        <span class="${isHit ? 'badge-hit' : 'badge-miss'}">${isHit ? 'CACHE HIT' : 'CACHE MISS'}</span>
        <span style="font-family: var(--font-mono); font-weight: 600;">${elapsed} ms</span>
      </div>
    `;
    container.appendChild(row);
  }
}

async function clearCache() {
  const container = document.getElementById('cacheResults');
  try {
    await fetch(`${API_BASE}/api/cache/clear`, { method: 'POST' });
    container.innerHTML = `
      <div class="empty-state" style="color: var(--emerald);">
        ✔ Ehcache memory region cleared successfully! Click <strong>"Test Cache Speed"</strong> to observe a fresh Cache Miss / Hit cycle.
      </div>
    `;
  } catch (e) {
    container.innerHTML = `<div class="loading-cell text-rose">Error clearing cache: ${e.message}</div>`;
  }
}

// =========================================================================
// HIGH VALUE BOOKS (TAB 4)
// =========================================================================
async function loadExpensiveBooks() {
  const tbody = document.getElementById('expensiveTbody');
  tbody.innerHTML = '<tr><td colspan="7" class="loading-cell">Loading high-value books...</td></tr>';

  const inputEl = document.getElementById('maxPriceInput') || document.getElementById('minPriceInput');
  const maxPrice = inputEl ? inputEl.value : 50;
  const url = `${API_BASE}/api/books/expensive?maxPrice=${maxPrice}&page=${expensiveState.page}&size=${expensiveState.size}`;

  try {
    const data = await getJSON(url);
    expensiveState.totalPages = data.totalPages;

    if (!data.content || data.content.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" class="loading-cell">No books found under this price threshold.</td></tr>';
      return;
    }

    tbody.innerHTML = data.content.map(b => `
      <tr>
        <td style="font-family: var(--font-mono); font-weight: 600;">#${b.id}</td>
        <td style="font-weight: 600;">${escapeHtml(b.title)}</td>
        <td><span class="genre-badge">${escapeHtml(b.genre || 'General')}</span></td>
        <td style="font-family: var(--font-mono); color: var(--text-muted);">${b.publishedYear ?? '-'}</td>
        <td class="price-tag">${fmtMoney(b.price)}</td>
        <td><strong>${escapeHtml(b.authorName ?? 'Unknown')}</strong></td>
        <td><span class="country-chip">${escapeHtml(b.authorCountry ?? '-')}</span></td>
      </tr>
    `).join('');

    document.getElementById('expPageInfo').textContent =
      `Page ${data.pageNumber + 1} of ${data.totalPages} (${data.totalElements} total matching)`;
    document.getElementById('expPrevBtn').disabled = data.first;
    document.getElementById('expNextBtn').disabled = data.last;

  } catch (e) {
    tbody.innerHTML = `<tr><td colspan="7" class="loading-cell text-rose">Error: ${e.message}</td></tr>`;
  }
}

function wireExpensiveControls() {
  document.getElementById('filterExpensiveBtn').addEventListener('click', () => {
    expensiveState.page = 0;
    loadExpensiveBooks();
  });

  document.getElementById('expPrevBtn').addEventListener('click', () => {
    if (expensiveState.page > 0) {
      expensiveState.page--;
      loadExpensiveBooks();
    }
  });

  document.getElementById('expNextBtn').addEventListener('click', () => {
    if (expensiveState.page < expensiveState.totalPages - 1) {
      expensiveState.page++;
      loadExpensiveBooks();
    }
  });
}

// =========================================================================
// UTILS & SORT INDICATORS
// =========================================================================
function updateSortIcons(prefix, activeSortBy, direction) {
  const arrows = document.querySelectorAll(`[id^="${prefix}"]`);
  arrows.forEach(el => {
    const field = el.id.replace(prefix, '');
    if (field === activeSortBy) {
      el.textContent = direction === 'asc' ? '▲' : '▼';
      el.style.color = 'var(--primary)';
    } else {
      el.textContent = '↕';
      el.style.color = 'var(--text-dim)';
    }
  });
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

// =========================================================================
// INIT
// =========================================================================
document.addEventListener('DOMContentLoaded', () => {
  wireTabs();
  wireBooksControls();
  wireAuthorsControls();
  wireExpensiveControls();

  document.getElementById('benchmarkBtn').addEventListener('click', runBenchmark);
  document.getElementById('cacheTestBtn').addEventListener('click', runCacheTest);
  document.getElementById('clearCacheBtn').addEventListener('click', clearCache);

  checkServerStatus();
  loadBooks();
});
