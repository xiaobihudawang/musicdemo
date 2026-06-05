/**
 * ============================================================
 * 页面：上传音乐（pages/upload.js）
 * ============================================================
 */
const ALLOWED_EXTS = ['.mp3', '.flac', '.wav', '.aac', '.ogg', '.m4a', '.mp4'];
const MAX_SIZE = 50 * 1024 * 1024;

function onFileChange(e) {
    const file = e.target.files[0];
    if (!file) return;
    document.getElementById('fileText').textContent = file.name;
    document.getElementById('fileInfo').textContent = '大小：' + formatSize(file.size);
    document.querySelector('.file-upload').classList.add('has-file');
}

async function generateDescription() {
    const title = document.getElementById('title').value.trim();
    const artist = document.getElementById('artist').value.trim();
    if (!title) { showToast('请先填写歌曲标题', 'error'); return; }

    const btn = document.getElementById('aiDescBtn');
    if (btn) { btn.disabled = true; btn.textContent = '🤖 生成中...'; }

    const res = await post('/api/ai/description', { title, artist });
    if (res && res.code === 200) {
        document.getElementById('description').value = res.data.description;
        showToast('简介生成成功', 'success');
    } else {
        showToast((res && res.message) || '生成失败', 'error');
    }
    if (btn) { btn.disabled = false; btn.textContent = '🤖 AI 生成'; }
}

async function doUpload() {
    const title = document.getElementById('title').value.trim();
    const artist = document.getElementById('artist').value.trim();
    const description = document.getElementById('description').value.trim();
    const file = document.getElementById('file').files[0];
    if (!title || !artist) { showToast('请填写标题和歌手', 'error'); return; }
    if (!file) { showToast('请选择音乐文件', 'error'); return; }
    const ext = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();
    if (!ALLOWED_EXTS.includes(ext)) { showToast('仅支持 MP3/FLAC/WAV/AAC/OGG/M4A/MP4 格式', 'error'); return; }
    if (file.size > MAX_SIZE) { showToast('文件大小不能超过 50MB', 'error'); return; }

    const formData = new FormData();
    formData.append('file', file);
    formData.append('title', title);
    formData.append('artist', artist);
    formData.append('description', description || '');

    const btn = document.querySelector('.card .btn-primary');
    if (btn) { btn.disabled = true; btn.textContent = '上传中...'; }

    // 使用 postForm 以便 401 时自动跳转登录页
    const res = await postForm('/api/music/upload', formData);
    if (res && res.code === 200) {
        showToast('上传成功', 'success');
        setTimeout(() => { window.location.href = '/detail.html?id=' + res.data.id; }, 800);
    } else {
        showToast((res && res.message) || '上传失败', 'error');
    }
    if (btn) { btn.disabled = false; btn.textContent = '上传'; }
}

function guard() {
    if (!isLogined()) {
        showToast('请先登录', 'error');
        setTimeout(() => { window.location.href = '/login.html'; }, 1000);
        return false;
    }
    return true;
}

function init() {
    if (!guard()) return;
    document.getElementById('file').addEventListener('change', onFileChange);
}

document.addEventListener('DOMContentLoaded', init);
