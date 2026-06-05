/**
 * ============================================================
 * 应用启动（app.js）
 *
 * 页面加载完成后：
 *   1. 若 body 不含 .auth-page，则在 body 最前面插入导航栏
 *   2. 淡出全屏加载遮罩
 *
 * 依赖：common.js（提供 renderNavbar / fadeOutLoading 等全局函数）。
 * ============================================================
 */
window.addEventListener('DOMContentLoaded', function () {
    if (!document.body.classList.contains('auth-page')) {
        const navbar = renderNavbar();
        document.body.insertBefore(navbar, document.body.firstChild);
    }
    fadeOutLoading();
});
