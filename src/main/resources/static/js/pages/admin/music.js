/**
 * ============================================================
 * 页面：后台音乐管理（pages/admin/music.js）
 * 权限：ROLE_ADMIN
 * ============================================================
 */
function guard() {
    if (getUserRole() !== 'admin') {
        showToast('无权访问', 'error');
        setTimeout(() => { window.location.href = '/'; }, 1000);
        return false;
    }
    return true;
}

async function loadAdminMusic() {
    const res = await get('/api/music/list?page=1&size=100');
    if (!res || res.code !== 200) return;
    document.getElementById('musicTableSkeleton').classList.add('hidden');
    const list = res.data.list;
    renderAdminMusic(list);
}

function renderAdminMusic(list) {
    const tbody = document.getElementById('musicTable');
    if (!list || list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" class="tip">暂无音乐</td></tr>';
        return;
    }
    tbody.innerHTML = list.map(m => `
        <tr data-id="${m.id}">
            <td>${m.id}</td>
            <td><a href="/detail.html?id=${m.id}">${escapeHtml(m.title)}</a></td>
            <td>${escapeHtml(m.artist)}</td>
            <td>${escapeHtml(m.username)}</td>
            <td>${m.likeCount}</td>
            <td>${m.commentCount}</td>
            <td>${m.downloadCount}</td>
            <td>${formatDate(m.createTime)}</td>
            <td>
                <button class="btn btn-danger btn-sm" data-action="remove" data-id="${m.id}">删除</button>
            </td>
        </tr>
    `).join('');
}

/**
 * 局部刷新指定行（用于删除成功后立即移除该行，无需整表重渲）
 */
function removeRowFromTable(id) {
    const tr = document.querySelector(`#musicTable tr[data-id="${id}"]`);
    if (tr) tr.remove();
    // 表空时显示占位
    const tbody = document.getElementById('musicTable');
    if (tbody && tbody.querySelectorAll('tr[data-id]').length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" class="tip">暂无音乐</td></tr>';
    }
}

async function removeAdminMusic(id, btn) {
    if (!confirm('确定删除这首音乐吗？删除后不可恢复！')) return;
    if (btn) { btn.disabled = true; btn.textContent = '删除中...'; }

    const res = await del('/api/admin/music/' + id);

    if (res && res.code === 200) {
        showToast('删除成功', 'success');
        // 局部移除该行（视觉立即生效，无需等服务端下次拉取）
        removeRowFromTable(id);
        // 后台静默重拉一次，保证与服务器一致（不阻塞 UI）
        loadAdminMusic().catch(() => {});
    } else {
        showToast((res && res.message) || '删除失败', 'error');
        if (btn) { btn.disabled = false; btn.textContent = '删除'; }
    }
}

/**
 * 事件委托：处理"删除"按钮点击。
 * 每次重新渲染 tbody 后，旧的 onclick 已丢失（innerHTML 重写），
 * 用委托避免每次重新绑定。
 */
document.addEventListener('click', function (e) {
    const btn = e.target.closest('button[data-action="remove"]');
    if (!btn) return;
    const id = parseInt(btn.dataset.id);
    removeAdminMusic(id, btn);
});

function init() { if (guard()) loadAdminMusic(); }

document.addEventListener('DOMContentLoaded', init);
