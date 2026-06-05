/**
 * ============================================================
 * 页面：B 站音频下载（pages/bilibili.js）
 * ============================================================
 */
async function doDownload() {
    const input = document.getElementById('bvidInput');
    const url = input ? input.value.trim() : '';
    if (!url) { showToast('请输入B站视频链接', 'error'); return; }

    // 用 id 而非类选择器，避开页面中其他 .btn-primary
    const btn = document.getElementById('biliDownloadBtn');
    const originalText = btn ? btn.textContent : '开始下载';
    if (btn) { btn.disabled = true; btn.textContent = '下载中...'; }

    try {
        const response = await fetch('/api/bilibili/download', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + getToken() },
            body: JSON.stringify({ url })
        });
        if (!response.ok) {
            let errMsg = '下载失败';
            try { const errData = await response.json(); errMsg = errData.message || errMsg; } catch (_) {}
            showToast(errMsg, 'error');
            return;
        }
        const disposition = response.headers.get('Content-Disposition') || '';
        let filename = 'audio.m4a';
        const match = disposition.match(/filename\*=UTF-8''(.+?)(?:;|$)/);
        if (match) filename = decodeURIComponent(match[1]);
        const blob = await response.blob();
        const blobUrl = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = blobUrl;
        a.download = filename;
        document.body.appendChild(a); a.click();
        document.body.removeChild(a); URL.revokeObjectURL(blobUrl);
        showToast('下载成功', 'success');
    } catch (e) {
        showToast('下载失败: ' + e.message, 'error');
    } finally {
        if (btn) { btn.disabled = false; btn.textContent = originalText; }
    }
}

function init() {
    if (!isLogined()) {
        showToast('请先登录', 'error');
        setTimeout(() => { window.location.href = '/login.html'; }, 1000);
    }
}

document.addEventListener('DOMContentLoaded', init);
