/**
 * ============================================================
 * 页面：音乐详情（pages/detail.js）
 *
 * 功能：详情展示、播放、点赞（局部更新）、评论（局部增删）、
 *       歌词同步、上传封面、下载、删除。
 * ============================================================
 */
const urlParams = new URLSearchParams(window.location.search);
const musicId = urlParams.get('id');

let musicData = null;
let liked = false;
let parsedLyrics = [];
let currentLyricIndex = -1;
let currentUserId = null;

async function loadDetail() {
    if (!musicId) { showDetailError('未找到音乐'); return; }

    // 解析当前用户 ID（用于显示"我的评论"删除按钮与权限判断）
    const token = getToken();
    if (token) {
        const payload = parseJwt(token);
        if (payload && payload.sub) currentUserId = parseInt(payload.sub);
    }

    const res = await get('/api/music/' + musicId);
    if (!res || res.code !== 200) { showDetailError('音乐不存在'); return; }
    document.getElementById('detailSkeleton').classList.add('hidden');
    musicData = res.data.music;
    const comments = res.data.comments || [];
    renderDetail(musicData, comments);
    if (isLogined()) checkLikeStatus();
    loadLyrics();
}

function showDetailError(msg) {
    const content = document.getElementById('detailContent');
    if (content) content.innerHTML = '<div class="tip">' + escapeHtml(msg) + '</div>';
    const sk = document.getElementById('detailSkeleton');
    if (sk) sk.classList.add('hidden');
}

async function loadLyrics() {
    if (!musicId) return;
    const res = await get('/api/music/' + musicId + '/lyrics');
    if (res && res.code === 200 && res.data && res.data.lyrics) {
        parsedLyrics = parseLRC(res.data.lyrics);
        renderLyrics();
    }
}

function parseLRC(lrcText) {
    if (!lrcText) return [];
    const lines = lrcText.split('\n');
    const result = [];
    const timeRegex = /\[(\d{2}):(\d{2})\.(\d{2,3})\]/g;

    for (const line of lines) {
        const times = [];
        let match;
        while ((match = timeRegex.exec(line)) !== null) {
            const min = parseInt(match[1]);
            const sec = parseInt(match[2]);
            const ms = parseInt(match[3].padEnd(3, '0'));
            times.push(min * 60 + sec + ms / 1000);
        }
        const text = line.replace(/\[\d{2}:\d{2}\.\d{2,3}\]/g, '').trim();
        if (times.length > 0 && text) {
            for (const t of times) result.push({ time: t, text });
        }
    }
    return result.sort((a, b) => a.time - b.time);
}

function renderLyrics() {
    const container = document.getElementById('lyricsContent');
    if (!container) return;
    if (parsedLyrics.length === 0) {
        container.innerHTML = '<div class="lyrics-empty">暂无歌词</div>';
        return;
    }
    container.innerHTML = parsedLyrics.map((line, i) =>
        `<div class="lyrics-line" data-index="${i}">${escapeHtml(line.text)}</div>`
    ).join('');
    requestAnimationFrame(scrollToFirstLyric);
}

function updateLyricsHighlight(currentTime) {
    if (parsedLyrics.length === 0) return;
    let newIndex = -1;
    for (let i = parsedLyrics.length - 1; i >= 0; i--) {
        if (currentTime >= parsedLyrics[i].time) {
            newIndex = i;
            break;
        }
    }
    if (newIndex === currentLyricIndex) return;
    currentLyricIndex = newIndex;

    const container = document.getElementById('lyricsContent');
    if (!container) return;
    const lines = container.querySelectorAll('.lyrics-line');

    lines.forEach((line, i) => {
        line.classList.toggle('active', i === newIndex);
        line.classList.toggle('passed', i < newIndex);
    });

    if (newIndex < 0 || !lines[newIndex]) return;

    // 让当前行中心对齐到容器可视区的"略偏上"位置（约 40% 高度处）
    const line = lines[newIndex];
    const containerH = container.clientHeight;
    const lineH = line.offsetHeight;
    const centerRatio = 0.4;
    const target = line.offsetTop + lineH / 2 - containerH * centerRatio;
    const max = Math.max(0, container.scrollHeight - containerH);
    const scrollTop = Math.max(0, Math.min(target, max));
    container.scrollTo({ top: scrollTop, behavior: 'smooth' });
}

function scrollToFirstLyric() {
    const container = document.getElementById('lyricsContent');
    if (!container || parsedLyrics.length === 0) return;
    const lines = container.querySelectorAll('.lyrics-line');
    if (!lines[0]) return;
    const containerH = container.clientHeight;
    const lineH = lines[0].offsetHeight;
    const target = lines[0].offsetTop + lineH / 2 - containerH * 0.4;
    container.scrollTop = Math.max(0, target);
}

async function regenerateLyrics() {
    if (!musicId) return;
    showToast('正在重新获取歌词...', 'info');
    const res = await post('/api/music/' + musicId + '/lyrics/regenerate');
    if (res && res.code === 200 && res.data && res.data.lyrics) {
        parsedLyrics = parseLRC(res.data.lyrics);
        currentLyricIndex = -1;
        renderLyrics();
        showToast('歌词获取成功', 'success');
    } else {
        showToast('未找到歌词', 'error');
    }
}

async function checkLikeStatus() {
    const res = await get('/api/music/' + musicId + '/like/status');
    if (res && res.code === 200) {
        liked = !!res.data.liked;
        updateLikeButton();
    }
}

function updateLikeButton() {
    const btn = document.getElementById('likeBtn');
    if (!btn) return;
    btn.textContent = liked ? '♥ 已赞' : '♡ 点赞';
    btn.className = liked ? 'btn-player btn-player-active' : 'btn-player';
}

function renderDetail(music, comments) {
    const container = document.getElementById('detailContent');
    const coverUrl = music.coverPath ? `/api/music/cover/${music.coverPath}` : '/video/logo.png';
    const canManage = isLogined() && (getUserRole() === 'admin' || currentUserId === music.userId);
    const isAdmin = getUserRole() === 'admin';

    let html = `
        <div class="player-page">
            <button class="player-back" onclick="window.history.length > 1 ? window.history.back() : (window.location.href = '/')">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
                返回
            </button>

            <div class="player-main">
                <div class="player-left">
                    <div class="player-cover-wrap">
                        <div class="player-cover">
                            <img src="${coverUrl}" alt="${escapeHtml(music.title)}">
                        </div>
                    </div>

                    <div class="player-info">
                        <h1 class="player-title">${escapeHtml(music.title)}</h1>
                        <div class="player-artist">${escapeHtml(music.artist)}</div>
                        <div class="player-meta">
                            <span>上传者 ${escapeHtml(music.username)}</span>
                            <span class="dot">·</span>
                            <span>${formatDate(music.createTime)}</span>
                        </div>
                        ${music.description ? `<div class="player-desc">${escapeHtml(music.description)}</div>` : ''}

                        <div class="player-actions">
                            <button id="likeBtn" class="btn-player" onclick="toggleLike()">♡ 点赞</button>
                            <button class="btn-player" onclick="document.getElementById('commentsArea').scrollIntoView({behavior:'smooth'})">💬 评论</button>
                            <button class="btn-player btn-player-primary" onclick="downloadMusic()">⬇ 下载</button>
                            ${canManage ? `<button class="btn-player" onclick="document.getElementById('coverInput').click()">🖼 封面</button>
                                <input type="file" id="coverInput" accept="image/*" style="display:none" onchange="uploadCover(this)">
                                <button class="btn-player btn-player-danger" onclick="removeMusic()">删除</button>` : ''}
                        </div>

                        <div class="player-stats">
                            <span id="likeCountStat">♥ ${music.likeCount || 0}</span>
                            <span id="commentCountStat">💬 ${music.commentCount || 0}</span>
                            <span>⬇ ${music.downloadCount || 0}</span>
                            <span>${formatSize(music.fileSize)}</span>
                        </div>
                    </div>
                </div>

                <div class="player-right">
                    <div class="lyrics-panel">
                        <div class="lyrics-header">
                            <span class="lyrics-title">歌词</span>
                            ${isAdmin ? '<button class="btn-icon" onclick="regenerateLyrics()" title="重新获取歌词">🔄</button>' : ''}
                        </div>
                        <div class="lyrics-viewport">
                            <div class="lyrics-container" id="lyricsContent">
                                <div class="lyrics-empty">加载中...</div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="player-footer">
                <audio id="audioPlayer" controls>
                    <source src="/api/music/${music.id}/stream">
                </audio>
            </div>

            <div class="comments-section" id="commentsArea">
                <div class="card">
                    <div class="card-header" id="commentsHeader">评论 (${comments.length})</div>`;

    if (isLogined()) {
        html += `
            <div class="comment-form">
                <div class="form-group">
                    <textarea id="commentContent" placeholder="写下你的评论..." rows="3"></textarea>
                </div>
                <button class="btn btn-primary" onclick="submitComment()">发表评论</button>
            </div>
        `;
    }
    html += '<ul class="comment-list" id="commentList">';
    if (comments.length === 0) {
        html += '<li class="tip" style="list-style:none;">暂无评论，快来抢沙发吧！</li>';
    } else {
        comments.forEach(c => {
            html += renderCommentItem(c, isAdmin);
        });
    }
    html += '</ul></div></div></div>';
    container.innerHTML = html;

    const audio = document.getElementById('audioPlayer');
    if (audio) {
        audio.addEventListener('timeupdate', function() {
            updateLyricsHighlight(this.currentTime);
        });
    }
}

function renderCommentItem(c, isAdmin) {
    const canDelete = isAdmin || (currentUserId !== null && currentUserId === c.userId);
    return `
        <li class="comment-item" data-id="${c.id}">
            <div><span class="comment-user">${escapeHtml(c.username)}</span><span class="comment-time">${formatDate(c.createTime)}</span></div>
            <div class="comment-content">${escapeHtml(c.content)}</div>
            ${canDelete ? `<button class="btn btn-danger btn-sm" style="margin-top:4px;" onclick="deleteComment(${c.id}, this)">删除</button>` : ''}
        </li>
    `;
}

async function toggleLike() {
    if (!isLogined()) { showToast('请先登录', 'error'); return; }
    const res = await post('/api/music/' + musicId + '/like');
    if (res && res.code === 200) {
        liked = !!res.data.liked;
        updateLikeButton();
        // 局部更新点赞数（不再整页重载，保护当前播放）
        if (musicData && typeof res.data.likeCount === 'number') {
            musicData.likeCount = res.data.likeCount;
        } else if (musicData) {
            musicData.likeCount = (musicData.likeCount || 0) + (liked ? 1 : -1);
        }
        const stat = document.getElementById('likeCountStat');
        if (stat) stat.textContent = '♥ ' + (musicData.likeCount || 0);
        showToast(liked ? '点赞成功' : '已取消点赞', 'success');
    } else {
        showToast((res && res.message) || '操作失败', 'error');
    }
}

async function submitComment() {
    if (!isLogined()) { showToast('请先登录', 'error'); return; }
    const textarea = document.getElementById('commentContent');
    const content = textarea ? textarea.value.trim() : '';
    if (!content) { showToast('请输入评论内容', 'error'); return; }
    if (containsForbiddenWord(content)) { showToast('评论包含不当内容，请修改后重试', 'error'); return; }

    const btn = document.querySelector('.comment-form .btn-primary');
    if (btn) { btn.disabled = true; btn.textContent = '发表中...'; }
    const res = await post('/api/music/' + musicId + '/comments', { content });
    if (btn) { btn.disabled = false; btn.textContent = '发表评论'; }

    if (res && res.code === 200) {
        if (textarea) textarea.value = '';
        // 局部追加评论，不重载整页
        appendComment(res.data);
        if (musicData) musicData.commentCount = (musicData.commentCount || 0) + 1;
        const stat = document.getElementById('commentCountStat');
        if (stat) stat.textContent = '💬 ' + (musicData.commentCount || 0);
        showToast('评论成功', 'success');
    } else {
        showToast((res && res.message) || '评论失败', 'error');
    }
}

function appendComment(c) {
    const list = document.getElementById('commentList');
    if (!list) return;
    // 替换"暂无评论"占位
    const emptyTip = list.querySelector('.tip');
    if (emptyTip) emptyTip.remove();
    const isAdmin = getUserRole() === 'admin';
    list.insertAdjacentHTML('beforeend', renderCommentItem(c, isAdmin));
    updateCommentsHeader();
}

function updateCommentsHeader() {
    const list = document.getElementById('commentList');
    const header = document.getElementById('commentsHeader');
    if (!list || !header) return;
    const count = list.querySelectorAll('.comment-item').length;
    header.textContent = '评论 (' + count + ')';
}

async function deleteComment(commentId, btn) {
    if (!confirm('确定删除这条评论吗？')) return;
    const res = await del('/api/comments/' + commentId);
    if (res && res.code === 200) {
        // 局部移除 li
        const li = btn ? btn.closest('.comment-item') : document.querySelector(`.comment-item[data-id="${commentId}"]`);
        if (li) li.remove();
        if (musicData) musicData.commentCount = Math.max(0, (musicData.commentCount || 0) - 1);
        const stat = document.getElementById('commentCountStat');
        if (stat) stat.textContent = '💬 ' + (musicData.commentCount || 0);
        updateCommentsHeader();
        // 评论列表为空时显示占位
        const list = document.getElementById('commentList');
        if (list && list.querySelectorAll('.comment-item').length === 0) {
            list.innerHTML = '<li class="tip" style="list-style:none;">暂无评论，快来抢沙发吧！</li>';
        }
        showToast('删除成功', 'success');
    } else {
        showToast((res && res.message) || '删除失败', 'error');
    }
}

async function removeMusic() {
    if (!confirm('确定删除这首音乐吗？删除后不可恢复！')) return;
    const res = await del('/api/music/' + musicId);
    if (res && res.code === 200) {
        showToast('删除成功', 'success');
        setTimeout(() => { window.location.href = '/'; }, 800);
    } else {
        showToast((res && res.message) || '删除失败', 'error');
    }
}

async function uploadCover(input) {
    const file = input.files[0];
    if (!file) return;
    const formData = new FormData();
    formData.append('file', file);
    const res = await postForm('/api/admin/music/' + musicId + '/cover', formData);
    input.value = ''; // 清空以便同名文件再次上传
    if (res && res.code === 200) {
        showToast('封面上传成功', 'success');
        // 局部刷新音乐元信息（封面 URL）—— 直接重渲染左半部分
        if (musicData) {
            musicData.coverPath = res.data.coverPath;
            const wrap = document.querySelector('.player-cover-wrap');
            if (wrap) {
                const newUrl = `/api/music/cover/${musicData.coverPath}`;
                wrap.innerHTML = `<div class="player-cover"><img src="${newUrl}" alt="${escapeHtml(musicData.title)}"></div>`;
            }
        }
    } else {
        showToast((res && res.message) || '封面上传失败', 'error');
    }
}

async function downloadMusic() {
    if (!isLogined()) { showToast('请先登录后再下载', 'error'); return; }
    const token = getToken();
    let response;
    try {
        response = await fetch('/api/music/' + musicId + '/download', {
            headers: { 'Authorization': 'Bearer ' + token }
        });
    } catch (e) {
        showToast('下载请求失败', 'error'); return;
    }
    if (!response.ok) {
        const errMsg = response.status === 401 ? '登录已过期，请重新登录'
            : response.status === 404 ? '文件不存在'
            : '下载失败(' + response.status + ')';
        showToast(errMsg, 'error'); return;
    }
    try {
        const blob = await response.blob();
        const url = URL.createObjectURL(blob);
        const ext = (musicData.filePath || '').split('.').pop() || 'mp3';
        const a = document.createElement('a');
        a.href = url;
        a.download = musicData.title + '.' + ext;
        document.body.appendChild(a); a.click();
        document.body.removeChild(a); URL.revokeObjectURL(url);
        // 局部更新下载数
        if (musicData) musicData.downloadCount = (musicData.downloadCount || 0) + 1;
        const stats = document.querySelectorAll('.player-stats span');
        if (stats.length >= 3) stats[2].textContent = '⬇ ' + (musicData.downloadCount || 0);
        showToast('下载成功', 'success');
    } catch (e) {
        showToast('下载文件处理失败', 'error');
    }
}

document.addEventListener('DOMContentLoaded', loadDetail);
