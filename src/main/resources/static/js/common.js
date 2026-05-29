/**
 * ============================================================
 * 通用工具函数模块（common.js）
 *
 * 本模块提供所有页面共享的工具函数和 UI 组件：
 *
 *   1. 格式化函数
 *      - formatDate()    : 日期格式化为 "YYYY-MM-DD HH:mm"
 *      - formatSize()    : 文件大小格式化为 "XX.X MB" 等
 *
 *   2. 安全函数
 *      - escapeHtml()    : HTML 转义，防止 XSS 攻击
 *
 *   3. 导航栏
 *      - renderNavbar()  : 动态创建自适应导航栏
 *      - logout()        : 退出登录
 *
 *   4. 页面生命周期
 *      - fadeOutLoading(): 淡出全屏加载遮罩
 *      - DOMContentLoaded: 自动插入导航栏和页面容器
 * ============================================================
 */

// ============================================================
// formatDate —— 将日期字符串格式化为 "YYYY-MM-DD HH:mm"
// 参数：dateStr - ISO 日期字符串（如 "2024-01-15T10:30:00"）
// 返回值：格式化后的字符串，空值返回空字符串
// 示例：formatDate("2024-01-15T10:30:00") → "2024-01-15 10:30"
// ============================================================
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

// ============================================================
// formatSize —— 将字节数格式化为人类可读的文件大小
// 参数：bytes - 文件字节数（数字）
// 返回值：格式化字符串，如 "3.2 MB"、"500.0 B"
// 支持单位：B、KB、MB、GB（二进制 1024 进制）
// ============================================================
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

// ============================================================
// escapeHtml —— HTML 转义函数
// 将特殊字符（< > & " '）转为 HTML 实体，防止 XSS 攻击
// 原理：利用 DOM 元素的 textContent 赋值后读取 innerHTML
// 参数：text - 原始字符串
// 返回值：转义后的安全 HTML 字符串
// ============================================================
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ============================================================
// logout —— 退出登录
// 清除 Token 和用户信息，跳转到登录页
// ============================================================
function logout() {
    clearToken();                       // 清除所有存储的登录信息
    window.location.href = '/login.html'; // 跳转登录页
}

// ============================================================
// renderNavbar —— 创建顶部导航栏
// 根据用户登录状态和角色动态渲染不同的导航链接：
//
//   未登录：
//     [首页] [排行榜] [登录] [注册]
//
//   已登录（普通用户）：
//     [首页] [排行榜] [上传音乐] [B站下载] | {用户名} [退出]
//
//   已登录（管理员）：
//     [首页] [排行榜] [上传音乐] [B站下载] | [用户管理] [音乐管理] | {用户名} [退出]
//
// 返回值：<nav> DOM 元素
// ============================================================
function renderNavbar() {
    const nav = document.createElement('nav');
    nav.className = 'navbar';

    // ----- Logo（左侧） -----
    const logo = document.createElement('div');
    logo.className = 'logo';
    logo.innerHTML = '<a href="/"><img src="/video/logo.png" alt="音乐分享" style="height:36px;vertical-align:middle;"></a>';
    nav.appendChild(logo);

    // ----- 导航链接（右侧） -----
    const links = document.createElement('div');
    links.className = 'nav-links';

    // 所有用户都可见的公开页面链接
    links.innerHTML = '<a href="/">首页</a><a href="/ranking.html">排行榜</a>';

    if (isLogined()) {
        // 已登录用户额外可见的链接
        links.innerHTML += '<a href="/upload.html">上传音乐</a><a href="/bilibili.html">B站下载</a>';

        // 用户信息区
        const userInfo = document.createElement('div');
        userInfo.className = 'user-info';

        // 管理员额外看到管理后台链接
        if (getUserRole() === 'admin') {
            userInfo.innerHTML += '<a href="/admin/users.html">用户管理</a><a href="/admin/music.html">音乐管理</a>';
        }

        // 显示用户名
        userInfo.innerHTML += '<span>' + getUsername() + '</span>';

        // 退出按钮
        const logoutBtn = document.createElement('button');
        logoutBtn.className = 'btn-logout';
        logoutBtn.textContent = '退出';
        logoutBtn.onclick = logout;
        userInfo.appendChild(logoutBtn);

        links.appendChild(userInfo);
    } else {
        // 未登录显示登录/注册入口
        links.innerHTML += '<a href="/login.html">登录</a><a href="/register.html">注册</a>';
    }

    nav.appendChild(links);
    return nav;
}

// ============================================================
// fadeOutLoading —— 淡出全屏加载遮罩层
// 确保遮罩至少显示 minDuration 毫秒（1200ms），防止闪白
// 先添加 fade-out 类（CSS opacity 过渡），再移除 DOM 元素
// ============================================================
function fadeOutLoading() {
    const overlay = document.getElementById('loadingOverlay');
    if (!overlay) return;

    // 最短显示时长（保证看到 Logo 动画）
    const minDuration = 1200;
    const elapsed = performance.now();  // 页面加载到现在经过的毫秒数
    const remaining = Math.max(0, minDuration - elapsed);

    // 延迟 remaining 毫秒后开始淡出
    setTimeout(function() {
        overlay.classList.add('fade-out');  // CSS transition 0.5s
        // 过渡完成后移除 DOM 元素
        setTimeout(function() {
            if (overlay.parentNode) overlay.parentNode.removeChild(overlay);
        }, 600);  // 略大于 CSS transition 时间
    }, remaining);
}

// ============================================================
// DOMContentLoaded 自动执行
// 页面加载完成后：
//   1. 在 body 最前面插入导航栏（通过 renderNavbar() 生成）
//   2. 将 body 中原有的所有内容（导航栏之后的元素）移入
//      一个 div.container 容器中（统一页面布局）
//   3. 淡出全屏加载遮罩
// ============================================================
document.addEventListener('DOMContentLoaded', function() {
    // ----- 步骤 1：插入导航栏 -----
    const navbar = renderNavbar();
    document.body.insertBefore(navbar, document.body.firstChild);

    // ----- 步骤 2：包装内容容器 -----
    // 将导航栏之后的所有兄弟节点移入 .container
    const container = document.createElement('div');
    container.className = 'container';
    while (navbar.nextSibling) {
        container.appendChild(navbar.nextSibling);
    }
    document.body.appendChild(container);

    // ----- 步骤 3：淡出加载遮罩 -----
    fadeOutLoading();
});