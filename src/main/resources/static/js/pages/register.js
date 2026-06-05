/**
 * ============================================================
 * 页面：注册（pages/register.js）
 * ============================================================
 */
async function doRegister() {
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;
    const name     = document.getElementById('name').value.trim();
    const email    = document.getElementById('email').value.trim();
    if (!username || !password) { showToast('用户名和密码为必填项', 'error'); return; }
    if (password.length < 6 || password.length > 18) { showToast('密码长度应为 6-18 位', 'error'); return; }

    const btn = document.querySelector('.btn-primary');
    if (btn) { btn.disabled = true; btn.textContent = '注册中...'; }

    try {
        const res = await post('/api/auth/register', { username, password, name, email });
        if (res && res.code === 200) {
            showToast('注册成功，请登录', 'success');
            setTimeout(() => { window.location.href = '/login.html'; }, 1000);
        } else {
            showToast((res && res.message) || '注册失败', 'error');
        }
    } catch (e) {
        // api.js 已 toast 过网络错误
    } finally {
        if (btn) { btn.disabled = false; btn.textContent = '注册'; }
    }
}

document.addEventListener('keydown', function (e) {
    if (e.key !== 'Enter') return;
    const fields = ['username', 'password'];
    if (fields.includes(document.activeElement.id)) doRegister();
});

if (isLogined()) {
    window.location.replace('/');
}
