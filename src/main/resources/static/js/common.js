/**
 * 通用工具函数
 */

/** 格式化日期 */
function formatDate(dateStr) {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    const hour = String(d.getHours()).padStart(2, '0');
    const min = String(d.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day} ${hour}:${min}`;
}

/** 格式化文件大小 */
function formatSize(bytes) {
    if (!bytes || bytes === 0) return '0 B';
    const units = ['B', 'KB', 'MB', 'GB'];
    let unitIndex = 0;
    let size = bytes;
    while (size >= 1024 && unitIndex < units.length - 1) {
        size /= 1024;
        unitIndex++;
    }
    return size.toFixed(1) + ' ' + units[unitIndex];
}

/** HTML 转义（防止 XSS） */
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/** 退出登录 */
function logout() {
    clearToken();
    window.location.href = '/login.html';
}

/** 创建导航栏（在每个页面的 body 开头调用） */
function renderNavbar() {
    const nav = document.createElement('nav');
    nav.className = 'navbar';

    const logo = document.createElement('div');
    logo.className = 'logo';
    logo.innerHTML = '<a href="/"><img src="/video/logo.png" alt="音乐分享" style="height:36px;vertical-align:middle;"></a>';
    nav.appendChild(logo);

    const links = document.createElement('div');
    links.className = 'nav-links';
    links.innerHTML = '<a href="/">首页</a><a href="/ranking.html">排行榜</a>';

    if (isLogined()) {
        links.innerHTML += '<a href="/upload.html">上传音乐</a>';

        const userInfo = document.createElement('div');
        userInfo.className = 'user-info';

        // 如果是管理员，显示管理入口
        if (getUserRole() === 'admin') {
            userInfo.innerHTML += '<a href="/admin/users.html">管理中心</a>';
        }

        userInfo.innerHTML += '<span>' + getUsername() + '</span>';
        const logoutBtn = document.createElement('button');
        logoutBtn.className = 'btn-logout';
        logoutBtn.textContent = '退出';
        logoutBtn.onclick = logout;
        userInfo.appendChild(logoutBtn);
        links.appendChild(userInfo);
    } else {
        links.innerHTML += '<a href="/login.html">登录</a><a href="/register.html">注册</a>';
    }

    nav.appendChild(links);
    return nav;
}

/** Loading overlay fade-out */
function fadeOutLoading() {
    const overlay = document.getElementById('loadingOverlay');
    if (!overlay) return;

    const minDuration = 1200;
    const elapsed = performance.now();
    const remaining = Math.max(0, minDuration - elapsed);

    setTimeout(function() {
        overlay.classList.add('fade-out');
        setTimeout(function() {
            if (overlay.parentNode) overlay.parentNode.removeChild(overlay);
        }, 600);
    }, remaining);
}

/** 页面加载完成后自动插入导航栏 */
document.addEventListener('DOMContentLoaded', function() {
    // 在 body 最前面插入导航栏
    const navbar = renderNavbar();
    document.body.insertBefore(navbar, document.body.firstChild);

    // 把原有的 body 内容包在一个容器里
    const existingContent = document.body.children;
    const container = document.createElement('div');
    container.className = 'container';
    while (navbar.nextSibling) {
        container.appendChild(navbar.nextSibling);
    }
    document.body.appendChild(container);

    // Fade out loading overlay
    fadeOutLoading();
});