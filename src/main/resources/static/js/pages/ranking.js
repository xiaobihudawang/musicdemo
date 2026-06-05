/**
 * ============================================================
 * 页面：排行榜（pages/ranking.js）
 * ============================================================
 */
let currentType = 'likes';

function switchTab(type, btn) {
    currentType = type;
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    loadRanking();
}

async function loadRanking() {
    const res = await get('/api/ranking/' + currentType);
    const sk = document.getElementById('rankSkeleton');
    if (!res || res.code !== 200) {
        if (sk) sk.classList.add('hidden');
        return;
    }
    if (sk) sk.classList.add('hidden');

    const list = res.data;
    if (!list || list.length === 0) {
        document.getElementById('rankList').innerHTML = '<div class="tip">本周暂无排行数据</div>';
        return;
    }
    document.getElementById('rankList').innerHTML = list.map((item, index) => {
        const rankClass = index < 3 ? 'top3' : '';
        return `
            <div class="rank-item ${rankClass}">
                <div class="rank-num">${index + 1}</div>
                <div style="flex:1;">
                    <div><a href="/detail.html?id=${item.id}"><strong>${escapeHtml(item.title)}</strong></a></div>
                    <div style="color:var(--text-muted);font-size:13px;font-style:italic;">
                        ${escapeHtml(item.artist)} &mdash;
                        ♥ ${item.likeCount} &nbsp;💬 ${item.commentCount} &nbsp;⬇ ${item.downloadCount}
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

function init() { loadRanking(); }

document.addEventListener('DOMContentLoaded', init);
