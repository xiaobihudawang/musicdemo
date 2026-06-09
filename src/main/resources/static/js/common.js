/**
 * ============================================================
 * 公共模块（common.js）
 *
 * 提供：
 *   - 认证：getToken / setToken / clearToken / saveUserInfo / getUsername /
 *           getUserRole / isLogined / parseJwt / isTokenExpired
 *   - 提示：showToast
 *   - 格式化：formatDate / formatSize / escapeHtml
 *   - 组件：renderNavbar / fadeOutLoading / logout
 *
 * 全局函数（不依赖任何命名空间）。
 * 必须先于各页面脚本加载。
 * ============================================================
 */

// ============================================================
// 认证
// ============================================================
const TOKEN_KEY    = 'music_token';
const USERNAME_KEY = 'music_username';
const ROLE_KEY     = 'music_role';

function getToken() {
    return localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY);
}

function setToken(token, persistent = true) {
    (persistent ? localStorage : sessionStorage).setItem(TOKEN_KEY, token);
}

function clearToken() {
    [localStorage, sessionStorage].forEach(s => {
        s.removeItem(TOKEN_KEY);
        s.removeItem(USERNAME_KEY);
        s.removeItem(ROLE_KEY);
    });
}

function saveUserInfo(username, role, persistent = true) {
    const storage = persistent ? localStorage : sessionStorage;
    storage.setItem(USERNAME_KEY, username);
    storage.setItem(ROLE_KEY, role);
}

function getUsername() {
    return localStorage.getItem(USERNAME_KEY) || sessionStorage.getItem(USERNAME_KEY);
}

function getUserRole() {
    return localStorage.getItem(ROLE_KEY) || sessionStorage.getItem(ROLE_KEY);
}

function isLogined() {
    const token = getToken();
    if (!token) return false;
    return !isTokenExpired(token);
}

/**
 * 解码 JWT Payload（不验签，仅读取 exp / sub 等公开字段）。
 * 用于客户端做"是否过期"的快速判断。
 */
function parseJwt(token) {
    try {
        const payloadBase64 = token.split('.')[1];
        const base64 = payloadBase64.replace(/-/g, '+').replace(/_/g, '/');
        const jsonStr = decodeURIComponent(atob(base64).split('')
            .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join(''));
        return JSON.parse(jsonStr);
    } catch (e) {
        return null;
    }
}

function isTokenExpired(token) {
    const payload = parseJwt(token);
    if (!payload || !payload.exp) return true;
    return payload.exp * 1000 < Date.now();
}

// ============================================================
// Toast 提示
// ============================================================
function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = 'toast ' + type;
    toast.textContent = message;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 2500);
}

// ============================================================
// 格式化
// ============================================================
function formatDate(dateStr) {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    const pad = n => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function formatSize(bytes) {
    if (!bytes || bytes === 0) return '0 B';
    const units = ['B', 'KB', 'MB', 'GB'];
    let unitIndex = 0;
    let s = bytes;
    while (s >= 1024 && unitIndex < units.length - 1) {
        s /= 1024;
        unitIndex++;
    }
    return s.toFixed(1) + ' ' + units[unitIndex];
}

function escapeHtml(text) {
    if (text === null || text === undefined) return '';
    const div = document.createElement('div');
    div.textContent = String(text);
    return div.innerHTML;
}

// ============================================================
// 导航栏
// ============================================================
function renderNavbar() {
    const nav = document.createElement('nav');
    nav.className = 'navbar';
    nav.innerHTML = `
        <div class="logo">
            <a href="/"><img src="/video/logo.png" alt="音乐分享" style="height:36px;vertical-align:middle;"></a>
        </div>
        <div class="nav-links">
            <a href="/">首页</a>
            <a href="/ranking.html">排行榜</a>
        </div>
    `;

    const links = nav.querySelector('.nav-links');
    if (isLogined()) {
        const userInfo = document.createElement('div');
        userInfo.className = 'user-info';
        let inner = '<a href="/upload.html">上传音乐</a><a href="/bilibili.html">B站下载</a>';
        if (getUserRole() === 'admin') {
            inner += '<a href="/admin/users.html">用户管理</a><a href="/admin/music.html">音乐管理</a>';
        }
        inner += `<span>${escapeHtml(getUsername() || '')}</span>`;
        inner += '<button class="btn-logout" id="navLogoutBtn">退出</button>';
        userInfo.innerHTML = inner;
        links.appendChild(userInfo);
    } else {
        links.innerHTML += '<a href="/login.html">登录</a><a href="/register.html">注册</a>';
    }

    return nav;
}

function logout() {
    clearToken();
    window.location.href = '/login.html';
}

// 委托处理退出按钮点击（navbar 是动态插入的，不能直接绑 onclick）
document.addEventListener('click', function (e) {
    if (e.target && e.target.id === 'navLogoutBtn') logout();
});

// ============================================================
// 全屏加载遮罩
// ============================================================
// 记录 common.js 加载时刻，作为"页面开始"用于 fadeOutLoading 的最短显示时间计算
const PAGE_LOAD_TS = performance.now();

function fadeOutLoading() {
    const overlay = document.getElementById('loadingOverlay');
    if (!overlay) return;
    overlay.style.display = 'none';
}
