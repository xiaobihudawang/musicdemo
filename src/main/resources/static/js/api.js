/**
 * ============================================================
 * HTTP 请求模块（api.js）
 *
 * 封装 fetch API，对外暴露全局函数：
 *   get(url)              → GET
 *   post(url, body)       → POST JSON
 *   put(url, body)        → PUT JSON
 *   del(url)              → DELETE
 *   postForm(url, fd)     → POST FormData（文件上传）
 *
 * 行为：
 *   - 自动从 localStorage/sessionStorage 读取 JWT，写入 Authorization Header
 *   - 401 自动清除登录态并跳转登录页
 *   - 网络异常统一通过 showToast 提示
 * ============================================================
 */

/**
 * 底层 HTTP 请求
 * @param {string} url 请求地址
 * @param {object} options fetch 配置
 * @returns {Promise<object>} 后端 Result<T> 格式响应
 */
async function request(url, options = {}) {
    const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
    const token = getToken();
    if (token) headers['Authorization'] = 'Bearer ' + token;

    try {
        const response = await fetch(url, { ...options, headers });

        if (response.status === 401) {
            clearToken();
            window.location.href = '/login.html?expired=1';
            return;
        }

        return await response.json();
    } catch (error) {
        console.error('请求失败:', error);
        showToast('网络请求失败，请稍后重试', 'error');
        throw error;
    }
}

function get(url) { return request(url, { method: 'GET' }); }
function post(url, body) { return request(url, { method: 'POST', body: JSON.stringify(body) }); }
function put(url, body) { return request(url, { method: 'PUT', body: JSON.stringify(body) }); }
function del(url) { return request(url, { method: 'DELETE' }); }

/**
 * FormData 文件上传
 * 浏览器无法为 FormData 设置 Content-Type（由浏览器自动加 boundary），
 * 因此不走 request()，单独构造 fetch，但仍复用 401 跳转逻辑。
 */
function postForm(url, formData) {
    const headers = {};
    const token = getToken();
    if (token) headers['Authorization'] = 'Bearer ' + token;

    return fetch(url, { method: 'POST', headers, body: formData }).then(async response => {
        if (response.status === 401) {
            clearToken();
            window.location.href = '/login.html?expired=1';
            return;
        }
        return response.json();
    });
}
