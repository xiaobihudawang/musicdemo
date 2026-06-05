/**
 * ============================================================
 * 页面：登录（pages/login.js）
 *
 * - doLogin() 提交用户名 + 密码 + "记住我"
 * - 后端返回 { token, username, role }，保存到 localStorage（记住我）
 *   或 sessionStorage（不记住）
 * ============================================================
 */
async function doLogin() {
    const username   = document.getElementById('username').value.trim();
    const password   = document.getElementById('password').value;
    const rememberMe = document.getElementById('rememberMe').checked;
    if (!username || !password) { showToast('请输入用户名和密码', 'error'); return; }

    const btn = document.querySelector('.btn-primary');
    if (btn) { btn.disabled = true; btn.textContent = '登录中...'; }

    try {
        const res = await post('/api/auth/login', { username, password });
        if (res && res.code === 200) {
            setToken(res.data.token, rememberMe);
            saveUserInfo(res.data.username, res.data.role, rememberMe);
            showToast('登录成功', 'success');
            setTimeout(() => { window.location.href = '/'; }, 600);
        } else {
            showToast((res && res.message) || '登录失败', 'error');
        }
    } catch (e) {
        // api.js 已 toast 过网络错误
    } finally {
        if (btn) { btn.disabled = false; btn.textContent = '登录'; }
    }
}

// Enter 键触发登录
document.addEventListener('keydown', function (e) {
    if (e.key !== 'Enter') return;
    const username = document.getElementById('username');
    const password = document.getElementById('password');
    if (document.activeElement === username || document.activeElement === password) {
        doLogin();
    }
});

// 已登录则直接跳首页
if (isLogined()) {
    window.location.replace('/');
}
