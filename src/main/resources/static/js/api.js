/**
 * ============================================================
 * API 工具模块（api.js）
 *
 * 本模块是前端与后端通信的核心层，提供以下功能：
 *
 *   1. Token 管理
 *      - getToken() / setToken() / clearToken()
 *      - parseJwt() 解码 JWT Payload（仅读取，不验签）
 *      - isTokenExpired() 检查过期
 *
 *   2. 用户信息管理
 *      - saveUserInfo() / getUsername() / getUserRole()
 *      - isLogined() 判断登录状态
 *
 *   3. HTTP 请求封装
 *      - request() 底层 fetch 封装，自动携带 Authorization Header
 *      - get() / post() / put() / del() 快捷方法
 *      - 401 自动跳转登录页
 *
 *   4. Toast 消息提示
 *      - showToast() 显示顶部通知
 *
 * 所有接口统一使用后端 Result<T> 格式：
 *   { code: 200, message: "成功", data: {...} }
 * ============================================================
 */

// ============================================================
// Token 管理
// 使用两个 localStorage key 存储 JWT：
//   - music_token    : JWT 字符串
//   - music_username : 用户名（方便前端展示）
//   - music_role     : 角色（user/admin）
// 支持持久化（localStorage）和会话级（sessionStorage）两种存储方式
// ============================================================

/** localStorage / sessionStorage 中存储 Token 的 key */
const TOKEN_KEY = 'music_token';
/** 存储用户名的 key */
const USERNAME_KEY = 'music_username';
/** 存储角色的 key */
const ROLE_KEY = 'music_role';

// ----------------------------------------------------------
// getToken —— 获取存储的 JWT Token
// 优先从 localStorage 读取，若不存在则从 sessionStorage 读取
// 返回值：Token 字符串或 null
// ----------------------------------------------------------
function getToken() {
    return localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY);
}

// ----------------------------------------------------------
// setToken —— 保存 JWT Token
// 参数：
//   token      - JWT 字符串
//   persistent - 是否持久化存储
//     true  → localStorage（关闭浏览器后仍存在）
//     false → sessionStorage（关闭标签页后自动清除）
// ----------------------------------------------------------
function setToken(token, persistent = true) {
    if (persistent) {
        localStorage.setItem(TOKEN_KEY, token);
    } else {
        sessionStorage.setItem(TOKEN_KEY, token);
    }
}

// ----------------------------------------------------------
// clearToken —— 清除所有登录状态
// 同时清除 localStorage 和 sessionStorage 中的
// Token、用户名、角色信息（防止残留）
// ----------------------------------------------------------
function clearToken() {
    localStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USERNAME_KEY);
    sessionStorage.removeItem(USERNAME_KEY);
    localStorage.removeItem(ROLE_KEY);
    sessionStorage.removeItem(ROLE_KEY);
}

// ----------------------------------------------------------
// parseJwt —— 解码 JWT 的 Payload 部分
// JWT 结构：Header.Payload.Signature
// Base64URL 解码后解析为 JSON 对象
// 注意：此函数不做签名验证，仅读取 exp、sub 等公开字段
// 参数：token - JWT 字符串
// 返回值：解析后的对象（如 { exp, sub, roles }）或 null（解析失败）
// ----------------------------------------------------------
function parseJwt(token) {
    try {
        // 提取 Payload（第二部分）
        const payloadBase64 = token.split('.')[1];
        // 将 Base64URL 转为标准 Base64（替换 - → +，_ → /）
        const base64 = payloadBase64.replace(/-/g, '+').replace(/_/g, '/');
        // 将二进制字符串转为 UTF-8 字符串（处理中文字符）
        const jsonStr = decodeURIComponent(atob(base64).split('')
            .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join(''));
        return JSON.parse(jsonStr);
    } catch (e) {
        return null;  // 任何解析异常都返回 null
    }
}

// ----------------------------------------------------------
// isTokenExpired —— 检查 JWT 是否过期
// 通过比较 exp 字段与当前时间判断
// 参数：token - JWT 字符串
// 返回值：true=已过期或无效，false=未过期
// ----------------------------------------------------------
function isTokenExpired(token) {
    const payload = parseJwt(token);
    if (!payload || !payload.exp) return true;  // 无 exp 字段视为过期
    return payload.exp * 1000 < Date.now();      // exp 单位是秒，转为毫秒比较
}

// ============================================================
// 用户信息管理
// ============================================================

// ----------------------------------------------------------
// saveUserInfo —— 保存用户名和角色到浏览器存储
// 参数：
//   username   - 用户名
//   role       - 角色（'user' 或 'admin'）
//   persistent - 是否持久化 localStorage
// ----------------------------------------------------------
function saveUserInfo(username, role, persistent = true) {
    const storage = persistent ? localStorage : sessionStorage;
    storage.setItem(USERNAME_KEY, username);
    storage.setItem(ROLE_KEY, role);
}

// ----------------------------------------------------------
// getUsername —— 获取当前登录的用户名
// 返回值：用户名字符串或 null
// ----------------------------------------------------------
function getUsername() {
    return localStorage.getItem(USERNAME_KEY) || sessionStorage.getItem(USERNAME_KEY);
}

// ----------------------------------------------------------
// getUserRole —— 获取当前用户的角色
// 返回值：'admin' 或 'user' 或 null
// ----------------------------------------------------------
function getUserRole() {
    return localStorage.getItem(ROLE_KEY) || sessionStorage.getItem(ROLE_KEY);
}

// ----------------------------------------------------------
// isLogined —— 判断用户是否已登录
// 判断标准：存在 Token 且未过期
// 返回值：true=已登录，false=未登录
// ----------------------------------------------------------
function isLogined() {
    const token = getToken();
    if (!token) return false;
    return !isTokenExpired(token);
}

// ============================================================
// HTTP 请求封装
// 基于 fetch API 封装，自动处理：
//   - 请求头合并（Content-Type + Authorization）
//   - 401 未授权自动跳转登录页
//   - 网络异常捕获和提示
// 所有请求方法（get/post/put/del）返回统一的 Promise<Result>
// ============================================================

// ----------------------------------------------------------
// request —— 底层 HTTP 请求函数
// 参数：
//   url     - 请求地址（相对路径如 '/api/music/list'）
//   options - fetch 配置对象 { method, headers, body }
// 返回值：解析后的 JSON 对象（Result<T> 格式）
// 异常情况：
//   - HTTP 401 → 清除 Token，跳转 /login.html?expired=1
//   - 网络错误 → 控制台输出错误，showToast 提示用户
// ----------------------------------------------------------
async function request(url, options = {}) {
    // 合并请求头：Content-Type 默认为 application/json
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers
    };

    // 如果用户已登录，自动在 Header 中添加 Bearer Token
    const token = getToken();
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }

    try {
        const response = await fetch(url, {
            ...options,
            headers
        });

        // ----- 401 未授权处理 -----
        // 可能原因：Token 过期、Token 被篡改、Token 无效
        // 处理方式：清除全部登录状态，强制跳转登录页
        if (response.status === 401) {
            clearToken();
            window.location.href = '/login.html?expired=1';
            return;  // 跳转后停止执行
        }

        // 解析响应 JSON
        const data = await response.json();
        return data;
    } catch (error) {
        // 网络层错误（如跨域、DNS 解析失败、服务器未响应）
        console.error('请求失败:', error);
        showToast('网络请求失败，请稍后重试', 'error');
        throw error;  // 抛出异常让调用方可选择捕获
    }
}

// ----------------------------------------------------------
// get —— HTTP GET 请求快捷方法
// 参数：url - 请求地址
// 返回值：Promise<Result>
// ----------------------------------------------------------
function get(url) {
    return request(url, { method: 'GET' });
}

// ----------------------------------------------------------
// post —— HTTP POST 请求快捷方法（JSON 格式）
// 参数：
//   url  - 请求地址
//   body - JavaScript 对象（自动 JSON.stringify）
// 返回值：Promise<Result>
// 注意：此方法不适用于 FormData 上传，上传文件请直接用 fetch
// ----------------------------------------------------------
function post(url, body) {
    return request(url, {
        method: 'POST',
        body: JSON.stringify(body)
    });
}

// ----------------------------------------------------------
// postForm —— HTTP POST 请求（FormData 文件上传）
// 参数：
//   url      - 请求地址
//   formData - FormData 对象（含文件）
// ----------------------------------------------------------
function postForm(url, formData) {
    const token = getToken();
    const headers = {};
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }
    return fetch(url, {
        method: 'POST',
        headers: headers,
        body: formData
    }).then(r => r.json());
}

// ----------------------------------------------------------
// put —— HTTP PUT 请求快捷方法
// 参数和用法同 post()
// ----------------------------------------------------------
function put(url, body) {
    return request(url, {
        method: 'PUT',
        body: JSON.stringify(body)
    });
}

// ----------------------------------------------------------
// del —— HTTP DELETE 请求快捷方法
// 参数：url - 请求地址
// ----------------------------------------------------------
function del(url) {
    return request(url, { method: 'DELETE' });
}

// ============================================================
// Toast 消息提示
// 动态创建并显示浮层通知，2.5 秒后自动消失
// 支持三种类型：success（蓝色）、error（红色）、info（灰色）
// CSS 样式定义在 style.css 的 .toast 相关规则中
// ============================================================

// ----------------------------------------------------------
// showToast —— 显示顶部提示消息
// 参数：
//   message - 要显示的文字内容
//   type    - 提示类型：'success' | 'error' | 'info'（默认 'info'）
// 使用方式：
//   showToast('操作成功', 'success');
//   showToast('出错了', 'error');
// ----------------------------------------------------------
function showToast(message, type = 'info') {
    // 创建 <div class="toast {type}">
    const toast = document.createElement('div');
    toast.className = 'toast ' + type;
    toast.textContent = message;
    document.body.appendChild(toast);

    // 2.5 秒后自动移除
    setTimeout(() => {
        toast.remove();
    }, 2500);
}