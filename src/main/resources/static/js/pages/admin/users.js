/**
 * ============================================================
 * 页面：后台用户管理（pages/admin/users.js）
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

async function loadAdminUsers() {
    const res = await get('/api/admin/users');
    if (!res || res.code !== 200) return;
    document.getElementById('usersTableSkeleton').classList.add('hidden');
    renderAdminUsers(res.data);
}

function renderAdminUsers(users) {
    const tbody = document.getElementById('userTable');
    if (!users || users.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="tip">暂无用户</td></tr>';
        return;
    }
    tbody.innerHTML = users.map(u => `
        <tr data-id="${u.id}">
            <td>${u.id}</td>
            <td>${escapeHtml(u.username)}</td>
            <td>${escapeHtml(u.name || '-')}</td>
            <td>${escapeHtml(u.email || '-')}</td>
            <td>${u.role === 'admin' ? '<span style="color:#E6A23C;">管理员</span>' : '普通用户'}</td>
            <td class="status-cell">${u.enabled
                ? '<span style="color:#67C23A;">启用</span>'
                : '<span style="color:#F56C6C;">禁用</span>'}</td>
            <td>${formatDate(u.createTime)}</td>
            <td>
                <button class="btn btn-sm ${u.enabled ? 'btn-danger' : 'btn-success'}"
                        data-action="toggle-status"
                        data-id="${u.id}"
                        data-next-enabled="${!u.enabled}">
                    ${u.enabled ? '禁用' : '启用'}
                </button>
            </td>
        </tr>
    `).join('');
}

/**
 * 局部更新指定行的状态显示 + 按钮文案。
 */
function updateUserRowStatus(id, enabled) {
    const tr = document.querySelector(`#userTable tr[data-id="${id}"]`);
    if (!tr) return;
    const statusCell = tr.querySelector('.status-cell');
    if (statusCell) {
        statusCell.innerHTML = enabled
            ? '<span style="color:#67C23A;">启用</span>'
            : '<span style="color:#F56C6C;">禁用</span>';
    }
    const btn = tr.querySelector('button[data-action="toggle-status"]');
    if (btn) {
        btn.className = 'btn btn-sm ' + (enabled ? 'btn-danger' : 'btn-success');
        btn.textContent = enabled ? '禁用' : '启用';
        btn.dataset.nextEnabled = String(!enabled);
        btn.disabled = false;
    }
}

async function toggleUserStatus(id, enabled, btn) {
    const action = enabled ? '启用' : '禁用';
    if (!confirm(`确定${action}该用户吗？`)) {
        if (btn) { btn.disabled = false; }
        return;
    }
    if (btn) { btn.disabled = true; btn.textContent = action + '中...'; }

    const res = await put('/api/admin/users/' + id + '/status', { enabled });

    if (res && res.code === 200) {
        showToast('操作成功', 'success');
        // 局部更新该行（视觉立即生效）
        updateUserRowStatus(id, enabled);
        // 后台静默重拉一次，保证与服务器一致
        loadAdminUsers().catch(() => {});
    } else {
        showToast((res && res.message) || '操作失败', 'error');
        if (btn) { btn.disabled = false; btn.textContent = enabled ? '启用' : '禁用'; }
    }
}

/**
 * 事件委托：处理"启用/禁用"按钮点击。
 */
document.addEventListener('click', function (e) {
    const btn = e.target.closest('button[data-action="toggle-status"]');
    if (!btn) return;
    const id = parseInt(btn.dataset.id);
    const enabled = btn.dataset.nextEnabled === 'true';
    toggleUserStatus(id, enabled, btn);
});

function init() { if (guard()) loadAdminUsers(); }

document.addEventListener('DOMContentLoaded', init);
