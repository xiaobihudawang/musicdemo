/**
 * ============================================================
 * 页面：音乐列表首页（pages/index.js）
 *
 * 功能：分页加载音乐列表、关键词搜索、跳页。
 * ============================================================
 */
const PAGE_SIZE = 12;
let currentPage = 1;
let currentKeyword = '';

async function loadList(page, keyword) {
    currentPage = page;
    currentKeyword = keyword || '';
    let url = `/api/music/list?page=${page}&size=${PAGE_SIZE}`;
    if (currentKeyword) url += `&keyword=${encodeURIComponent(currentKeyword)}`;

    const skeleton = document.getElementById('listSkeleton');
    if (skeleton) skeleton.classList.remove('hidden');

    const res = await get(url);
    if (!res || res.code !== 200) {
        if (skeleton) skeleton.classList.add('hidden');
        return;
    }

    if (skeleton) skeleton.classList.add('hidden');
    renderList(res.data.list);
    renderPagination(res.data.total, page);
    const totalPages = Math.max(1, Math.ceil(res.data.total / PAGE_SIZE));
    document.getElementById('pageMeta').textContent =
        `第 ${page} 页，共 ${totalPages} 页 — 每页 ${PAGE_SIZE} 首`;
}

function renderList(list) {
    const container = document.getElementById('trackList');
    if (!list || list.length === 0) {
        container.innerHTML = '<div class="tip">暂无音乐，快去上传一首吧！</div>';
        return;
    }

    container.innerHTML = list.map((item, index) => {
        const hasCover = !!item.coverPath;
        const coverUrl = `/api/music/cover/${item.coverPath}`;
        const realIndex = (currentPage - 1) * PAGE_SIZE + index + 1;
        return `
            <div class="track" onclick="location.href='/detail.html?id=${item.id}'">
                <span class="track-index">${String(realIndex).padStart(2, '0')}</span>
                <div class="track-cover">
                    ${hasCover
                        ? `<img src="${coverUrl}" alt="${escapeHtml(item.title)}" loading="lazy">`
                        : `<div class="placeholder">♪</div>`
                    }
                </div>
                <div class="track-meta">
                    <div class="track-title">${escapeHtml(item.title)}</div>
                    <div class="track-artist">${escapeHtml(item.artist)}</div>
                </div>
                <button class="track-play" onclick="event.stopPropagation();location.href='/detail.html?id=${item.id}'">
                    <svg viewBox="0 0 24 24"><polygon points="8,5 20,12 8,19"/></svg>
                </button>
            </div>
        `;
    }).join('');
}

function renderPagination(total, page) {
    const totalPages = Math.ceil(total / PAGE_SIZE);
    const container = document.getElementById('pagination');
    if (totalPages <= 1) { container.innerHTML = ''; return; }

    let html = `<button class="page-arrow" ${page <= 1 ? 'disabled' : ''} onclick="goPage(${page - 1})">&lsaquo;</button>`;
    for (let i = 1; i <= totalPages; i++) {
        html += `<button class="page-btn ${i === page ? 'active' : ''}" onclick="goPage(${i})">${i}</button>`;
    }
    html += `<button class="page-arrow" ${page >= totalPages ? 'disabled' : ''} onclick="goPage(${page + 1})">&rsaquo;</button>`;
    container.innerHTML = html;
}

function goPage(page) {
    if (page < 1) return;
    loadList(page, currentKeyword);
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function search() {
    loadList(1, document.getElementById('keyword').value.trim());
}

async function handleAutoLogin() {
    const token = getToken();
    if (!token || isTokenExpired(token)) return;
    const res = await get('/api/auth/verify');
    if (!res || res.code !== 200) clearToken();
}

function init() {
    document.getElementById('keyword').addEventListener('keydown', e => {
        if (e.key === 'Enter') search();
    });
    handleAutoLogin().then(() => loadList(1, ''));
}

document.addEventListener('DOMContentLoaded', init);
