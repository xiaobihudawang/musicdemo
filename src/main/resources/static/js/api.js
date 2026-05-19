/**
 * API 工具模块
 * 封装了所有后端 API 的调用，自动在请求头中携带 JWT Token
 */

// ==================== Token 管理 ====================

const TOKEN_KEY = 'music_token';
const USERNAME_KEY = 'music_username';
const ROLE_KEY = 'music_role';

/** 获取存储的 Token（优先 localStorage，再查 sessionStorage） */
function getToken() {
    return localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY);
}

/**
 * 保存 Token
 * @param {string}  token      - JWT 字符串
 * @param {boolean} persistent - true 存 localStorage，false 存 sessionStorage
 */
function setToken(token, persistent = true) {
    if (persistent) {
        localStorage.setItem(TOKEN_KEY, token);
    } else {
        sessionStorage.setItem(TOKEN_KEY, token);
    }
}

/** 清除 Token（两个存储都清） */
function clearToken() {
    localStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USERNAME_KEY);
    sessionStorage.removeItem(USERNAME_KEY);
    localStorage.removeItem(ROLE_KEY);
    sessionStorage.removeItem(ROLE_KEY);
}

/**
 * 解码 JWT 的 Payload（不含验签，仅用于前端读取 exp 等公开字段）
 * JWT 格式：Header.Payload.Signature
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

/** 检查 JWT 是否已过期（基于 exp 字段） */
function isTokenExpired(token) {
    const payload = parseJwt(token);
    if (!payload || !payload.exp) return true;
    return payload.exp * 1000 < Date.now(); // exp 是秒级，转为毫秒
}

/**
 * 保存用户信息
 * @param {string}  username   - 用户名
 * @param {string}  role       - 角色
 * @param {boolean} persistent - true 存 localStorage，false 存 sessionStorage
 */
function saveUserInfo(username, role, persistent = true) {
    const storage = persistent ? localStorage : sessionStorage;
    storage.setItem(USERNAME_KEY, username);
    storage.setItem(ROLE_KEY, role);
}

/** 获取用户名 */
function getUsername() {
    return localStorage.getItem(USERNAME_KEY) || sessionStorage.getItem(USERNAME_KEY);
}

/** 获取用户角色 */
function getUserRole() {
    return localStorage.getItem(ROLE_KEY) || sessionStorage.getItem(ROLE_KEY);
}

/** 判断是否已登录（检查 token 是否存在且未过期） */
function isLogined() {
    const token = getToken();
    if (!token) return false;
    return !isTokenExpired(token);
}

// ==================== HTTP 请求封装 ====================

/**
 * 发送 HTTP 请求
 * @param {string} url      - 请求地址
 * @param {object} options  - fetch 的选项
 * @returns {Promise} 返回解析后的 JSON 数据
 */
async function request(url, options = {}) {
    // 合并默认请求头：如果已登录，自动带上 Authorization
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers
    };

    const token = getToken();
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }

    try {
        const response = await fetch(url, {
            ...options,
            headers
        });

        // 如果返回 401，说明 Token 过期或无效，清除登录状态
        if (response.status === 401) {
            clearToken();
            window.location.href = '/login.html?expired=1';
            return;
        }

        // 解析 JSON
        const data = await response.json();
        return data;
    } catch (error) {
        console.error('请求失败:', error);
        showToast('网络请求失败，请稍后重试', 'error');
        throw error;
    }
}

/** GET 请求 */
function get(url) {
    return request(url, { method: 'GET' });
}

/** POST 请求（JSON 格式） */
function post(url, body) {
    return request(url, {
        method: 'POST',
        body: JSON.stringify(body)
    });
}

/** PUT 请求 */
function put(url, body) {
    return request(url, {
        method: 'PUT',
        body: JSON.stringify(body)
    });
}

/** DELETE 请求 */
function del(url) {
    return request(url, { method: 'DELETE' });
}

// ==================== Toast 消息提示 ====================

/** 显示提示消息 */
function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = 'toast ' + type;
    toast.textContent = message;
    document.body.appendChild(toast);
    setTimeout(() => {
        toast.remove();
    }, 2500);
}